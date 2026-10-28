package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.belmontespa.transporte.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("Migraciones: la cadena completa, V4 y V5 sobre una base con datos")
class MigracionesIT extends IntegrationTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void restaurarEsquema() {
		flyway.clean();
		flyway.migrate();
	}

	private Flyway hasta(String version) {
		return Flyway.configure().configuration(flyway.getConfiguration())
				.target(MigrationVersion.fromVersion(version)).load();
	}

	private boolean existeColumna(String tabla, String columna) {
		return jdbc.queryForObject("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?""", Integer.class, tabla,
				columna) == 1;
	}

	private boolean admiteNull(String tabla, String columna) {
		return "YES".equals(jdbc.queryForObject("""
				SELECT is_nullable FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?""", String.class, tabla,
				columna));
	}

	private boolean esUnica(String tabla, String columna) {
		return jdbc.queryForObject("""
				SELECT COUNT(*) FROM (
				  SELECT index_name FROM information_schema.statistics
				  WHERE table_schema = DATABASE() AND table_name = ? AND non_unique = 0
				  GROUP BY index_name HAVING COUNT(*) = 1 AND MIN(column_name) = ?) u""", Integer.class, tabla,
				columna) >= 1;
	}

	private List<String> valores(String sql, Object... args) {
		return jdbc.queryForList(sql, String.class, args);
	}

	@Test
	@DisplayName("sobre una base vacía aplica todo y deja el modelo del README")
	void baseVacia() {
		flyway.clean();
		flyway.migrate();

		assertThat(flyway.info().pending()).isEmpty();
		assertThat(flyway.info().applied()).extracting(m -> m.getVersion().getVersion()).contains("1", "2", "3", "4", "5");

		assertThat(existeColumna("usuario", "password_temporal_hash")).isTrue();
		assertThat(existeColumna("solicitante", "estado_solicitante_id")).isTrue();
		assertThat(existeColumna("conductor", "activo")).isTrue();
		assertThat(existeColumna("movil", "activo")).isTrue();
		assertThat(existeColumna("pago", "monto_reembolsado")).isTrue();
		assertThat(admiteNull("pago", "fecha_pago")).isTrue();
		assertThat(esUnica("usuario", "token_fcm")).isTrue();
		assertThat(esUnica("viaje", "pago_id")).isTrue();
		assertThat(existeColumna("viaje", "motivo")).isTrue();
		assertThat(existeColumna("detalle_viaje_pasajero", "fh_abordaje")).isTrue();
		assertThat(admiteNull("detalle_viaje_pasajero", "fh_abordaje")).isTrue();
		assertThat(existeColumna("movil", "codigo_abordaje")).isTrue();
		assertThat(admiteNull("movil", "codigo_abordaje")).isFalse();
		assertThat(esUnica("movil", "codigo_abordaje")).isTrue();

		assertThat(valores("SELECT estado FROM estado_solicitante"))
				.containsExactlyInAnyOrder("PENDIENTE", "APROBADO", "RECHAZADO");
		assertThat(valores("SELECT estado FROM estado_pago")).contains("REEMBOLSADO");
		assertThat(valores("SELECT nombre FROM estado_movil")).containsExactlyInAnyOrder("DISPONIBLE", "EN_USO");

		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'admin@belmontespa.cl'"))
				.containsExactly("*");
		assertThat(valores("SELECT password_temporal_hash FROM usuario WHERE correo = 'admin@belmontespa.cl'"))
				.singleElement().asString().startsWith("$2a$");
	}

	@Test
	@DisplayName("bu_pago acepta PAGADO → REEMBOLSADO y no deja salir de un estado terminal")
	void transicionesDePago() {
		flyway.clean();
		flyway.migrate();
		jdbc.update("INSERT INTO empresa (razon_social, rut) VALUES ('Empresa de pago', '76.222.222-2')");

		String reembolsado = nuevoPago("pago-reembolsado");
		cambiarEstadoPago(reembolsado, "PAGADO");
		cambiarEstadoPago(reembolsado, "REEMBOLSADO");
		assertThat(valores("""
				SELECT e.estado FROM pago p JOIN estado_pago e ON e.id = p.estado_pago_id WHERE p.hash = ?""",
				reembolsado)).containsExactly("REEMBOLSADO");
		assertThatThrownBy(() -> cambiarEstadoPago(reembolsado, "ANULADO"))
				.isInstanceOf(DataIntegrityViolationException.class);

		String anulado = nuevoPago("pago-anulado");
		cambiarEstadoPago(anulado, "PAGADO");
		cambiarEstadoPago(anulado, "ANULADO");

		String pendiente = nuevoPago("pago-pendiente");
		assertThatThrownBy(() -> cambiarEstadoPago(pendiente, "REEMBOLSADO"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("el folio es TR- y un correlativo desde 1001 que asigna la base y no cambia")
	void folios() {
		flyway.clean();
		flyway.migrate();
		escenarioDeViajes();

		long primero = viaje("elegido");
		long segundo = viaje("otro");

		assertThat(folio(primero)).isEqualTo("TR-1001");
		assertThat(folio(segundo)).isEqualTo("TR-1002");
		assertThatThrownBy(() -> jdbc.update("UPDATE viaje SET folio = 'TR-9999' WHERE id_viaje = ?", primero))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(folio(primero)).isEqualTo("TR-1001");
	}

	@Test
	@DisplayName("bu_viaje acepta V8 (ASIGNADO → PENDIENTE) y no deja volver a PENDIENTE desde otro estado")
	void rechazoDelConductor() {
		flyway.clean();
		flyway.migrate();
		escenarioDeViajes();
		long viaje = viaje("v8");

		cambiarEstadoViaje(viaje, "ASIGNADO");
		cambiarEstadoViaje(viaje, "PENDIENTE");
		assertThat(estadoViaje(viaje)).isEqualTo("PENDIENTE");

		cambiarEstadoViaje(viaje, "ASIGNADO");
		cambiarEstadoViaje(viaje, "EN_CURSO");
		assertThatThrownBy(() -> cambiarEstadoViaje(viaje, "PENDIENTE"))
				.isInstanceOf(DataIntegrityViolationException.class);
		cambiarEstadoViaje(viaje, "FINALIZADO");
		assertThatThrownBy(() -> cambiarEstadoViaje(viaje, "PENDIENTE"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("au_viaje audita estado y motivo, pero no las fechas, el origen ni el número de vuelo")
	void historialDeViaje() {
		flyway.clean();
		flyway.migrate();
		escenarioDeViajes();
		long viaje = viaje("historial");

		jdbc.update("""
				UPDATE viaje SET numero_vuelo = 'LA800', fh_agenda = '2026-11-02 11:00:00',
				                 fh_asignacion = '2026-10-30 09:00:00',
				                 id_origen = (SELECT MAX(id_ubicacion) FROM ubicacion)
				WHERE id_viaje = ?""", viaje);
		assertThat(camposAuditados(viaje)).isEmpty();

		cambiarEstadoViaje(viaje, "ASIGNADO");
		jdbc.update("UPDATE viaje SET motivo = 'Cambio de planes' WHERE id_viaje = ?", viaje);
		assertThat(camposAuditados(viaje)).containsExactlyInAnyOrder("estado", "motivo");
		assertThat(valores("SELECT valor FROM historial_viaje WHERE id_viaje = ? AND campo_modificado = 'motivo'",
				viaje)).containsExactly("Cambio de planes");
	}

	@Test
	@DisplayName("sobre una base con datos convierte cuentas, flota y pagos sin perderlos")
	void baseConDatos() {
		Flyway hastaV3 = hasta("3");
		hastaV3.clean();
		hastaV3.migrate();

		jdbc.update("INSERT INTO empresa (razon_social, rut) VALUES ('Empresa vieja', '76.111.111-1')");
		long empresa = jdbc.queryForObject("SELECT id_empresa FROM empresa", Long.class);
		long ana = usuario("ana@mig.cl", "hash-ana", null, false);
		long beto = usuario("beto@mig.cl", "hash-temporal-de-beto", null, true);
		long cami = usuario("cami@mig.cl", "hash-cami", "fcm-repetido", false);
		long dani = usuario("dani@mig.cl", "hash-dani", "fcm-repetido", false);
		long eli = usuario("eli@mig.cl", "hash-eli", "fcm-unico", false);
		for (long id : List.of(ana, beto, dani)) {
			jdbc.update("INSERT INTO solicitante (id_usuario, empresa_id_empresa) VALUES (?, ?)", id, empresa);
		}
		for (long id : List.of(cami, eli)) {
			jdbc.update("""
					INSERT INTO conductor (id_usuario, estado_conductor_id_estado)
					VALUES (?, (SELECT id_estado FROM estado_conductor WHERE estado = 'DISPONIBLE'))""", id);
		}
		movil("MIG001", "MANTENCION");
		movil("MIG002", "EN_USO");
		jdbc.update("""
				INSERT INTO pago (hash, monto, estado_pago_id, banco_id, empresa_id_empresa, medio_pago_id,
				                  tipo_recibo_id, fecha_pago)
				VALUES ('pago-viejo', 50000, (SELECT id FROM estado_pago WHERE estado = 'PAGADO'),
				        (SELECT MIN(id) FROM banco), ?, (SELECT MIN(id) FROM medio_pago),
				        (SELECT MIN(id) FROM tipo_recibo), '2026-10-01 10:00:00')""", empresa);

		ubicacion("Av. Vieja 1");
		viaje("VJAAAAAAAAAAAAAAAAAA");
		viaje("VJBBBBBBBBBBBBBBBBBB");

		flyway.migrate();
		assertThat(flyway.info().pending()).isEmpty();

		assertThat(valores("""
				SELECT e.estado FROM solicitante s JOIN estado_solicitante e ON e.id = s.estado_solicitante_id"""))
				.hasSize(3).containsOnly("APROBADO");

		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'ana@mig.cl'"))
				.containsExactly("hash-ana");
		assertThat(valores("SELECT password_temporal_hash FROM usuario WHERE correo = 'ana@mig.cl'"))
				.containsOnlyNulls();
		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'beto@mig.cl'")).containsExactly("*");
		assertThat(valores("SELECT password_temporal_hash FROM usuario WHERE correo = 'beto@mig.cl'"))
				.containsExactly("hash-temporal-de-beto");
		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'admin@belmontespa.cl'"))
				.containsExactly("*");

		assertThat(valores("SELECT token_fcm FROM usuario WHERE correo IN ('cami@mig.cl', 'dani@mig.cl')"))
				.containsOnlyNulls();
		assertThat(valores("SELECT token_fcm FROM usuario WHERE correo = 'eli@mig.cl'"))
				.containsExactly("fcm-unico");
		assertThatThrownBy(() -> jdbc.update("UPDATE usuario SET token_fcm = 'fcm-unico' WHERE correo = 'ana@mig.cl'"))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(valores("SELECT nombre FROM estado_movil")).containsExactlyInAnyOrder("DISPONIBLE", "EN_USO");
		assertThat(valores("""
				SELECT CONCAT(e.nombre, '/', m.activo) FROM movil m
				JOIN estado_movil e ON e.id_estado_mov = m.estado_movil_id_estado_mov WHERE m.patente = 'MIG001'"""))
				.containsExactly("DISPONIBLE/0");
		assertThat(valores("""
				SELECT CONCAT(e.nombre, '/', m.activo) FROM movil m
				JOIN estado_movil e ON e.id_estado_mov = m.estado_movil_id_estado_mov WHERE m.patente = 'MIG002'"""))
				.containsExactly("EN_USO/1");
		assertThat(valores("SELECT CAST(activo AS CHAR) FROM conductor")).hasSize(2).containsOnly("1");

		assertThat(valores("SELECT CAST(fecha_pago AS CHAR) FROM pago WHERE hash = 'pago-viejo'"))
				.containsExactly("2026-10-01 10:00:00");
		assertThat(valores("SELECT monto_reembolsado FROM pago WHERE hash = 'pago-viejo'")).containsOnlyNulls();

		assertThat(valores("SELECT folio FROM viaje ORDER BY id_viaje")).containsExactly("TR-1001", "TR-1002");
		assertThat(folio(viaje("lo-ignora-la-base"))).isEqualTo("TR-1003");

		assertThat(valores("SELECT codigo_abordaje FROM movil")).hasSize(2)
				.allMatch(codigo -> codigo.matches("[0-9A-F]{16}")).doesNotHaveDuplicates();
	}

	private void escenarioDeViajes() {
		jdbc.update("INSERT INTO empresa (razon_social, rut) VALUES ('Empresa de viajes', '76.333.333-3')");
		long usuario = usuario("viajes@mig.cl", "hash-viajes", null, false);
		jdbc.update("""
				INSERT INTO solicitante (id_usuario, empresa_id_empresa, estado_solicitante_id)
				VALUES (?, (SELECT MIN(id_empresa) FROM empresa),
				        (SELECT id FROM estado_solicitante WHERE estado = 'APROBADO'))""", usuario);
		ubicacion("Av. Origen 100");
		ubicacion("Av. Destino 200");
	}

	private void ubicacion(String direccion) {
		jdbc.update("""
				INSERT INTO ubicacion (direccion, id_comuna, latitud, longitud)
				VALUES (?, (SELECT MIN(id_comuna) FROM comuna), -33.426100, -70.611000)""", direccion);
	}

	private long viaje(String folio) {
		jdbc.update("""
				INSERT INTO viaje (folio, id_solicitante, id_origen, id_destino, id_estado, fh_agenda)
				VALUES (?, (SELECT MIN(id_usuario) FROM solicitante), (SELECT MIN(id_ubicacion) FROM ubicacion),
				        (SELECT MIN(id_ubicacion) FROM ubicacion),
				        (SELECT id_estado FROM estado_solicitud WHERE nombre_estado = 'PENDIENTE'),
				        '2026-11-01 10:00:00')""", folio);
		return jdbc.queryForObject("SELECT MAX(id_viaje) FROM viaje", Long.class);
	}

	private String folio(long idViaje) {
		return jdbc.queryForObject("SELECT folio FROM viaje WHERE id_viaje = ?", String.class, idViaje);
	}

	private String estadoViaje(long idViaje) {
		return jdbc.queryForObject("""
				SELECT e.nombre_estado FROM viaje v JOIN estado_solicitud e ON e.id_estado = v.id_estado
				WHERE v.id_viaje = ?""", String.class, idViaje);
	}

	private void cambiarEstadoViaje(long idViaje, String estado) {
		jdbc.update("""
				UPDATE viaje SET id_estado = (SELECT id_estado FROM estado_solicitud WHERE nombre_estado = ?)
				WHERE id_viaje = ?""", estado, idViaje);
	}

	private List<String> camposAuditados(long idViaje) {
		return valores("SELECT campo_modificado FROM historial_viaje WHERE id_viaje = ?", idViaje);
	}

	private long usuario(String correo, String passwordHash, String tokenFcm, boolean temporalPendiente) {
		jdbc.update("""
				INSERT INTO usuario (nombre, correo, password_hash, token_fcm, password_temporal_expira)
				VALUES (?, ?, ?, ?, ?)""", correo, correo, passwordHash, tokenFcm,
				temporalPendiente ? Timestamp.valueOf(LocalDateTime.now().plusDays(1)) : null);
		return jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE correo = ?", Long.class, correo);
	}

	private void movil(String patente, String estado) {
		jdbc.update("""
				INSERT INTO movil (patente, capacidad, estado_movil_id_estado_mov)
				VALUES (?, 4, (SELECT id_estado_mov FROM estado_movil WHERE nombre = ?))""", patente, estado);
	}

	private String nuevoPago(String hash) {
		jdbc.update("""
				INSERT INTO pago (hash, monto, estado_pago_id, banco_id, empresa_id_empresa, medio_pago_id,
				                  tipo_recibo_id)
				VALUES (?, 50000, (SELECT id FROM estado_pago WHERE estado = 'PENDIENTE'),
				        (SELECT MIN(id) FROM banco), (SELECT MIN(id_empresa) FROM empresa),
				        (SELECT MIN(id) FROM medio_pago), (SELECT MIN(id) FROM tipo_recibo))""", hash);
		return hash;
	}

	private void cambiarEstadoPago(String hash, String estado) {
		jdbc.update("UPDATE pago SET estado_pago_id = (SELECT id FROM estado_pago WHERE estado = ?) WHERE hash = ?",
				estado, hash);
	}
}
