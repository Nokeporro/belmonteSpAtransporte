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

/**
 * Las migraciones sobre una base vacía y sobre una que ya tiene datos. Comparte {@code belmonte_test}
 * con el resto de las IT, así que cada prueba la deja limpia y migrada al terminar.
 */
@DisplayName("Migraciones: la cadena completa y V4 sobre una base con datos")
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

	// UNIQUE sobre esa sola columna.
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
		assertThat(flyway.info().applied()).extracting(m -> m.getVersion().getVersion()).contains("1", "2", "3", "4");

		assertThat(existeColumna("usuario", "password_temporal_hash")).isTrue();
		assertThat(existeColumna("solicitante", "estado_solicitante_id")).isTrue();
		assertThat(existeColumna("conductor", "activo")).isTrue();
		assertThat(existeColumna("movil", "activo")).isTrue();
		assertThat(existeColumna("pago", "monto_reembolsado")).isTrue();
		assertThat(admiteNull("pago", "fecha_pago")).isTrue();
		assertThat(esUnica("usuario", "token_fcm")).isTrue();
		assertThat(esUnica("viaje", "pago_id")).isTrue();

		assertThat(valores("SELECT estado FROM estado_solicitante"))
				.containsExactlyInAnyOrder("PENDIENTE", "APROBADO", "RECHAZADO");
		assertThat(valores("SELECT estado FROM estado_pago")).contains("REEMBOLSADO");
		assertThat(valores("SELECT nombre FROM estado_movil")).containsExactlyInAnyOrder("DISPONIBLE", "EN_USO");

		// La contraseña de desarrollo de V3 queda como temporal del Administrador.
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
	@DisplayName("sobre una base con datos convierte cuentas, flota y pagos sin perderlos")
	void baseConDatos() {
		Flyway hastaV3 = hasta("3");
		hastaV3.clean();
		hastaV3.migrate();

		// Datos como los dejó V3: sin estado de solicitante, activo ni contraseña temporal aparte.
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

		flyway.migrate();
		assertThat(flyway.info().pending()).isEmpty();

		// Solicitantes existentes: aprobados.
		assertThat(valores("""
				SELECT e.estado FROM solicitante s JOIN estado_solicitante e ON e.id = s.estado_solicitante_id"""))
				.hasSize(3).containsOnly("APROBADO");

		// La temporal pendiente pasa a su columna; la contraseña normal no se toca.
		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'ana@mig.cl'"))
				.containsExactly("hash-ana");
		assertThat(valores("SELECT password_temporal_hash FROM usuario WHERE correo = 'ana@mig.cl'"))
				.containsOnlyNulls();
		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'beto@mig.cl'")).containsExactly("*");
		assertThat(valores("SELECT password_temporal_hash FROM usuario WHERE correo = 'beto@mig.cl'"))
				.containsExactly("hash-temporal-de-beto");
		assertThat(valores("SELECT password_hash FROM usuario WHERE correo = 'admin@belmontespa.cl'"))
				.containsExactly("*");

		// Un token compartido no lo conserva nadie; uno propio sigue y ahora es único.
		assertThat(valores("SELECT token_fcm FROM usuario WHERE correo IN ('cami@mig.cl', 'dani@mig.cl')"))
				.containsOnlyNulls();
		assertThat(valores("SELECT token_fcm FROM usuario WHERE correo = 'eli@mig.cl'"))
				.containsExactly("fcm-unico");
		assertThatThrownBy(() -> jdbc.update("UPDATE usuario SET token_fcm = 'fcm-unico' WHERE correo = 'ana@mig.cl'"))
				.isInstanceOf(DataIntegrityViolationException.class);

		// MANTENCION desaparece: ese vehículo queda Disponible pero inactivo.
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

		// El pago existente conserva sus datos.
		assertThat(valores("SELECT CAST(fecha_pago AS CHAR) FROM pago WHERE hash = 'pago-viejo'"))
				.containsExactly("2026-10-01 10:00:00");
		assertThat(valores("SELECT monto_reembolsado FROM pago WHERE hash = 'pago-viejo'")).containsOnlyNulls();
	}

	// Usuario como lo guardaba V3: la temporal vigente estaba en password_hash.
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

	// Pago PENDIENTE de la empresa 'Empresa de pago'; devuelve su hash.
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
