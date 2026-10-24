package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.Api.Flota;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("Reloj: lo que escribe la app y lo que escribe la base están en hora de Chile (README, Fechas)")
class RelojIT extends IntegrationTest {

	private static final Duration TOLERANCIA = Duration.ofMinutes(1);

	@Autowired
	private JdbcTemplate jdbc;

	private static LocalDateTime ahoraEnChile() {
		return LocalDateTime.now(ClockConfig.ZONA);
	}

	private static void assertCerca(LocalDateTime real, LocalDateTime esperado, String que) {
		assertThat(Duration.between(esperado, real).abs()).as(que).isLessThan(TOLERANCIA);
	}

	@Test
	@DisplayName("fh_asignacion (la escribe la app) y fh_creacion (la escribe la base) coinciden con la hora de Chile")
	void asignacionYCreacion() {
		Cuenta solicitante = api.solicitante();
		Flota flota = api.flota(4);
		String folio = api.viajeAsignado(solicitante, flota);

		Resp viaje = Api.exigir(api.verViaje(folio), "ver viaje", 200);
		LocalDateTime creacion = LocalDateTime.parse(viaje.texto("$.fhCreacion"));
		LocalDateTime asignacion = LocalDateTime.parse(viaje.texto("$.fhAsignacion"));

		assertCerca(creacion, ahoraEnChile(), "fh_creacion contra la hora de Chile");
		assertCerca(asignacion, ahoraEnChile(), "fh_asignacion contra la hora de Chile");
		assertCerca(asignacion, creacion, "fh_asignacion contra fh_creacion");
	}

	@Test
	@DisplayName("fh_inicio y fh_fin salen del Clock")
	void inicioYFin() {
		Cuenta solicitante = api.solicitante();
		Flota flota = api.flota(4);
		String folio = api.viajeAsignado(solicitante, flota);
		api.iniciarOk(folio, flota.conductor());
		api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");

		Resp viaje = Api.exigir(api.verViaje(folio), "ver viaje", 200);
		assertCerca(LocalDateTime.parse(viaje.texto("$.fhInicio")), ahoraEnChile(), "fh_inicio");
		assertCerca(LocalDateTime.parse(viaje.texto("$.fhFin")), ahoraEnChile(), "fh_fin");
	}

	@Test
	@DisplayName("la fecha del pago sale del Clock")
	void fechaDePago() {
		Cuenta solicitante = api.solicitante();
		String hash = api.pagarOk(api.viaje(solicitante));
		assertThat(api.get("/pagos/" + hash, api.admin()).texto("$.fechaPago")).isNull();
		Api.exigir(api.cambiarEstadoPago(hash, api.admin(), "PAGADO"), "confirmar pago", 200);

		Resp pago = Api.exigir(api.get("/pagos/" + hash, api.admin()), "ver pago", 200);
		assertCerca(LocalDateTime.parse(pago.texto("$.fechaPago")), ahoraEnChile(), "fecha_pago");
	}

	@Test
	@DisplayName("la contraseña temporal vence a los 30 minutos contados en hora de Chile")
	void vencimientoDeLaTemporal() {
		Cuenta solicitante = api.solicitante();
		Api.exigir(api.post("/auth/recuperar-password", null, """
				{"correo":"%s"}""".formatted(solicitante.correo())), "recuperar contraseña", 204);

		LocalDateTime vence = jdbc.queryForObject(
				"SELECT password_temporal_expira FROM usuario WHERE correo = ?", LocalDateTime.class,
				solicitante.correo());

		assertThat(vence).isNotNull();
		assertCerca(vence, ahoraEnChile().plusMinutes(30), "vencimiento de la temporal");
	}
}
