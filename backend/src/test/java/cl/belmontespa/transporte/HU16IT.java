package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Resp;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** HU16/RF15: V5, P4 y P5 sobre la API real, incluyendo el límite horario exacto. */
@Import(HU16IT.RelojConfig.class)
class HU16IT extends IntegrationTest {
    @TestConfiguration
    static class RelojConfig {
        @Bean @Primary Clock relojCancelacion() {
            return Clock.fixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ClockConfig.ZONA);
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    private Resp ver(String folio) { return Api.exigir(api.verViaje(folio), "ver viaje", 200); }
    private String pago(String folio, int monto, boolean pagado) {
        String hash = Api.exigir(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", monto), "registrar pago", 201).texto("$.hash");
        if (pagado) Api.exigir(api.cambiarEstadoPago(hash, api.admin(), "PAGADO"), "marcar pagado", 200);
        return hash;
    }

    @Test
    void cancelaUnaSolicitudPropiaSinFinalizarlaNiBorrarSusDatos() {
        var solicitante = api.solicitante();
        String folio = api.viaje(solicitante);
        api.conPasajero(solicitante, folio);
        Resp antes = ver(folio);
        Resp cancelado = Api.exigir(api.cancelar(folio, solicitante.token()), "cancelar pendiente", 200);
        assertThat(cancelado.texto("$.estado")).isEqualTo("CANCELADO");
        assertThat(cancelado.texto("$.fhFin")).isNull();
        assertThat(cancelado.texto("$.fhInicio")).isNull();
        assertThat(cancelado.texto("$.fhAgenda")).isEqualTo(antes.texto("$.fhAgenda"));
        assertThat(cancelado.entero("$.cantidadPasajeros")).isEqualTo(1);
        assertThat(cancelado.<List<?>>json("$.pasajeros")).hasSize(1);
        assertThat(api.cancelar(folio, solicitante.token()).status()).isEqualTo(409);
    }

    @Test
    void reembolsaLaMitadRedondeadaHaciaAbajoInclusoSiResultaCero() {
        var solicitante = api.solicitante();
        for (int monto : List.of(50_001, 1)) {
            String folio = api.viaje(solicitante);
            String hash = pago(folio, monto, true);
            String fecha = api.get("/pagos/" + hash, solicitante.token()).texto("$.fechaPago");
            api.cancelarOk(folio, solicitante);
            Resp reembolso = Api.exigir(api.get("/pagos/" + hash, solicitante.token()), "consultar reembolso", 200);
            assertThat(reembolso.texto("$.estadoPago")).isEqualTo("REEMBOLSADO");
            assertThat(reembolso.entero("$.montoReembolsado")).isEqualTo(monto / 2);
            assertThat(reembolso.entero("$.monto")).isEqualTo(monto);
            assertThat(reembolso.texto("$.fechaPago")).isEqualTo(fecha);
            assertThat(ver(folio).texto("$.estadoPago")).isEqualTo("REEMBOLSADO");
            assertThat(api.cancelar(folio, solicitante.token()).status()).isEqualTo(409);
            assertThat(api.get("/pagos/" + hash, solicitante.token()).entero("$.montoReembolsado")).isEqualTo(monto / 2);
        }
    }

    @Test
    void anulaElPagoPendienteSinRegistrarReembolso() {
        var solicitante = api.solicitante();
        String folio = api.viaje(solicitante);
        String hash = pago(folio, 25_000, false);
        api.cancelarOk(folio, solicitante);
        Resp resultado = api.get("/pagos/" + hash, solicitante.token());
        assertThat(resultado.texto("$.estadoPago")).isEqualTo("ANULADO");
        assertThat(resultado.texto("$.montoReembolsado")).isNull();
    }

    @Test
    void conservaLosPagosQueYaSonTerminales() {
        var solicitante = api.solicitante();
        for (String estado : List.of("RECHAZADO", "ANULADO", "REEMBOLSADO")) {
            String folio = api.viaje(solicitante);
            String hash = pago(folio, 20_000, estado.equals("REEMBOLSADO"));
            // Prepara estados terminales sin ejecutar V6 ni cancelar antes este viaje.
            jdbc.update("UPDATE pago SET estado_pago_id = (SELECT id FROM estado_pago WHERE estado = ?), monto_reembolsado = ? WHERE hash = ?",
                    estado, estado.equals("REEMBOLSADO") ? 7_000 : null, hash);
            Resp antes = api.get("/pagos/" + hash, solicitante.token());
            api.cancelarOk(folio, solicitante);
            assertThat(api.get("/pagos/" + hash, solicitante.token()).cuerpo()).isEqualTo(antes.cuerpo());
        }
    }

    @Test
    void incluyeElLimiteDeUnaHoraYRechazaElSegundoSiguienteSinTocarElPago() {
        var solicitante = api.solicitante();
        LocalDateTime limite = LocalDateTime.now(clock).minusHours(1);
        String enLimite = api.viaje(solicitante);
        jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", limite, enLimite);
        api.cancelarOk(enLimite, solicitante);
        String vencido = api.viaje(solicitante);
        String hash = pago(vencido, 20_001, true);
        jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", limite.minusSeconds(1), vencido);
        Resp rechazo = api.cancelar(vencido, solicitante.token());
        assertThat(rechazo.status()).isEqualTo(409);
        assertThat(rechazo.texto("$.message")).contains("vencido");
        assertThat(api.estadoViaje(vencido)).isEqualTo("PENDIENTE");
        assertThat(api.estadoPago(hash)).isEqualTo("PAGADO");
        assertThat(api.get("/pagos/" + hash, solicitante.token()).texto("$.montoReembolsado")).isNull();
    }

    @Test
    void noLiberaLaFlotaOcupadaPorOtroViajeYConservaLaAsignacion() {
        var solicitante = api.solicitante();
        var flota = api.flota(4);
        LocalDateTime base = LocalDateTime.now(clock).plusDays(3);
        String cancelar = api.viajeAsignado(solicitante, flota, base);
        String otro = api.viajeAsignado(solicitante, flota, base.plusDays(1));
        jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", LocalDateTime.now(clock), otro);
        api.cambiarEstadoOk(otro, flota.conductor(), "EN_CURSO");
        String asignacion = ver(cancelar).texto("$.fhAsignacion");
        api.cancelarOk(cancelar, solicitante);
        assertThat(api.estadoConductor(flota.conductor().correo())).isEqualTo("OCUPADO");
        assertThat(api.estadoMovil(flota.patente())).isEqualTo("EN_USO");
        assertThat(api.estadoViaje(otro)).isEqualTo("EN_CURSO");
        Resp resultado = ver(cancelar);
        assertThat(resultado.texto("$.correoConductor")).isEqualTo(flota.conductor().correo());
        assertThat(resultado.texto("$.patenteMovil")).isEqualTo(flota.patente());
        assertThat(resultado.texto("$.fhAsignacion")).isEqualTo(asignacion);
        assertThat(resultado.texto("$.fhFin")).isNull();
    }

    @Test
    void soloElDuenoPuedeCancelarYLosEstadosCerradosSeRechazan() {
        var solicitante = api.solicitante();
        var flota = api.flota(4);
        String folio = api.viajeAsignado(solicitante, flota);
        assertThat(api.cancelar(folio, api.solicitante().token()).status()).isEqualTo(403);
        assertThat(api.cancelar(folio, flota.conductor().token()).status()).isEqualTo(403);
        // El Administrador cancela con motivo (V7, HU24): sin cuerpo no hay motivo.
        assertThat(api.cancelar(folio, api.admin()).status()).isEqualTo(400);
        assertThat(api.cancelar(folio, null).status()).isEqualTo(401);
        assertThat(api.cancelar("TR-99999999", solicitante.token()).status()).isEqualTo(404);
        jdbc.update("UPDATE viaje SET fh_agenda = ? WHERE folio = ?", LocalDateTime.now(clock), folio);
        api.cambiarEstadoOk(folio, flota.conductor(), "EN_CURSO");
        assertThat(api.cancelar(folio, solicitante.token()).status()).isEqualTo(409);
        api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");
        assertThat(api.cancelar(folio, solicitante.token()).status()).isEqualTo(409);
    }

    @Test
    void dosCancelacionesConcurrentesSoloAplicanUnaVezYNotificanTrasConfirmar() throws Exception {
        var solicitante = api.solicitante();
        var flota = api.flota(4);
        String tokenSolicitante = api.dispositivo(solicitante);
        String tokenConductor = api.dispositivo(flota.conductor());
        String folio = api.viajeAsignado(solicitante, flota);
        String hash = pago(folio, 50_001, true);
        await().atMost(Duration.ofSeconds(5)).until(() -> fcm.envios().size() == 3);
        fcm.limpiar();
        CountDownLatch salida = new CountDownLatch(1);
        var primera = CompletableFuture.supplyAsync(() -> cancelarAlSalir(salida, folio, solicitante.token()));
        var segunda = CompletableFuture.supplyAsync(() -> cancelarAlSalir(salida, folio, solicitante.token()));
        salida.countDown();
        assertThat(List.of(primera.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        assertThat(api.get("/pagos/" + hash, solicitante.token()).entero("$.montoReembolsado")).isEqualTo(25_000);
        await().atMost(Duration.ofSeconds(5)).until(() -> fcm.envios().size() == 2);
        assertThat(fcm.para(tokenConductor)).singleElement().satisfies(envio -> {
            assertThat(envio.titulo()).isEqualTo("Viaje cancelado");
            assertThat(envio.datos()).containsOnlyKeys("tipo", "folio").containsEntry("tipo", "VIAJE").containsEntry("folio", folio);
        });
        assertThat(fcm.para(tokenSolicitante)).singleElement().satisfies(envio -> assertThat(envio.mensaje()).isEqualTo("Estado: Reembolsado"));
        Resp agenda = api.get("/viajes?estado=ASIGNADO", flota.conductor().token());
        assertThat(agenda.<List<String>>json("$[*].folio")).doesNotContain(folio);
        Resp historial = api.get("/viajes?estado=CANCELADO", solicitante.token());
        assertThat(historial.<List<String>>json("$[*].folio")).contains(folio);
    }

    private int cancelarAlSalir(CountDownLatch salida, String folio, String token) {
        try { salida.await(); return api.cancelar(folio, token).status(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
}
