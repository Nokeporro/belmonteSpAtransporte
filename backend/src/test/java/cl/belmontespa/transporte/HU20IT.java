package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import cl.belmontespa.transporte.dto.request.CambiarEstadoPagoDTORequest;
import cl.belmontespa.transporte.service.PagoService;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class HU20IT extends IntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PagoService pagos;
    @Autowired PlatformTransactionManager transacciones;

    private Api.Resp cambio(String hash, String estado) {
        return api.cambiarEstadoPago(hash, api.admin(), estado);
    }
    private Api.Resp pago(String hash) { return api.get("/pagos/" + hash, api.admin()); }
    private void esperarAvisos() {
        await().pollDelay(Duration.ofMillis(400)).atMost(Duration.ofSeconds(5)).until(() -> true);
    }
    private List<Integer> simultaneos(Callable<Integer> uno, Callable<Integer> dos) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(() -> { inicio.await(); return uno.call(); });
            var b = hilos.submit(() -> { inicio.await(); return dos.call(); });
            inicio.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }

    @Test void pendientePermiteLosTresDestinosYNotificaSinAlterarElMonto() {
        var solicitante = api.solicitante(); String token = api.dispositivo(solicitante);
        for (String destino : List.of("PAGADO", "RECHAZADO", "ANULADO")) {
            String folio = api.viaje(solicitante), hash = api.pagarOk(folio);
            esperarAvisos(); fcm.limpiar();
            var respuesta = Api.exigir(cambio(hash, destino.toLowerCase()), "P2", 200);
            assertThat(respuesta.texto("$.folio")).isEqualTo(folio);
            assertThat(respuesta.texto("$.estadoPago")).isEqualTo(destino);
            assertThat(respuesta.entero("$.monto")).isEqualTo(50_000);
            assertThat(respuesta.texto("$.montoReembolsado")).isNull();
            if (destino.equals("PAGADO")) assertThat(respuesta.texto("$.fechaPago")).isNotBlank();
            else assertThat(respuesta.texto("$.fechaPago")).isNull();
            assertThat(api.estadoViaje(folio)).isEqualTo(destino.equals("ANULADO") ? "CANCELADO" : "PENDIENTE");
            String etiqueta = destino.substring(0, 1) + destino.substring(1).toLowerCase();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(fcm.para(token))
                    .anySatisfy(e -> {
                        assertThat(e.titulo()).isEqualTo("Pago del viaje " + folio);
                        assertThat(e.mensaje()).isEqualTo("Estado: " + etiqueta);
                        assertThat(e.datos()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("tipo", "VIAJE", "folio", folio));
                    }));
            esperarAvisos();
            assertThat(fcm.para(token)).hasSize(destino.equals("ANULADO") ? 2 : 1);
        }
    }

    @Test void pagadoSoloSeAnulaSinReembolsoYConservaAsignacionFechasYFlota() {
        var solicitante = api.solicitante(); var flota = api.flota(4);
        String tokenSol = api.dispositivo(solicitante), tokenCon = api.dispositivo(flota.conductor());
        String folio = api.viajeAsignado(solicitante, flota), hash = api.pagarOk(folio);
        var pagado = Api.exigir(cambio(hash, "PAGADO"), "pagar", 200);
        var viaje = api.verViaje(folio);
        var conductor = api.get("/conductores/" + flota.conductor().correo(), api.admin());
        var movil = api.get("/moviles/" + flota.patente(), api.admin());
        api.vencer(folio); // V6 también cancela un viaje vencido.
        esperarAvisos(); fcm.limpiar();
        Api.exigir(cambio(hash, "ANULADO"), "P3", 200);
        assertThat(pago(hash).texto("$.fechaPago")).isEqualTo(pagado.texto("$.fechaPago"));
        assertThat(pago(hash).texto("$.montoReembolsado")).isNull();
        var cancelado = api.verViaje(folio);
        assertThat(cancelado.texto("$.estado")).isEqualTo("CANCELADO");
        for (String campo : List.of("correoConductor", "patenteMovil", "fhAsignacion", "fhInicio", "fhFin", "motivo")) {
            assertThat(cancelado.texto("$." + campo)).as(campo).isEqualTo(viaje.texto("$." + campo));
        }
        assertThat(api.get("/conductores/" + flota.conductor().correo(), api.admin()).texto("$.estadoConductor"))
                .isEqualTo(conductor.texto("$.estadoConductor"));
        assertThat(api.get("/moviles/" + flota.patente(), api.admin()).texto("$.estadoMovil")).isEqualTo(movil.texto("$.estadoMovil"));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(fcm.envios()).hasSize(3));
        assertThat(fcm.para(tokenCon)).singleElement().satisfies(e -> {
            assertThat(e.titulo()).isEqualTo("Viaje cancelado");
            assertThat(e.mensaje()).isEqualTo("El viaje " + folio + " fue cancelado");
            assertThat(e.datos()).containsEntry("tipo", "VIAJE").containsEntry("folio", folio);
        });
        assertThat(fcm.para(tokenSol)).extracting(e -> e.titulo()).containsExactlyInAnyOrder("Viaje cancelado", "Pago del viaje " + folio);
        assertThat(cambio(hash, "ANULADO").status()).isEqualTo(409);
        esperarAvisos(); assertThat(fcm.envios()).hasSize(3);
        assertThat(api.pagar(api.admin(), folio, 12, "EFECTIVO", 100).status()).isEqualTo(409);
    }

    @Test void anularNoCancelaViajesEnCursoNiFinalizadosNiLiberaSuFlota() {
        var solicitante = api.solicitante();
        for (boolean finalizar : List.of(false, true)) {
            var flota = api.flota(4); String folio = api.viajeAsignado(solicitante, flota);
            api.iniciarOk(folio, flota.conductor());
            if (finalizar) api.cambiarEstadoOk(folio, flota.conductor(), "FINALIZADO");
            var antes = api.verViaje(folio);
            String hash = api.pagarOk(folio);
            Api.exigir(cambio(hash, "ANULADO"), "anular", 200);
            var despues = api.verViaje(folio);
            for (String campo : List.of("estado", "correoConductor", "patenteMovil", "fhAsignacion", "fhInicio", "fhFin")) {
                assertThat(despues.texto("$." + campo)).as(campo).isEqualTo(antes.texto("$." + campo));
            }
            assertThat(api.get("/conductores/" + flota.conductor().correo(), api.admin()).texto("$.estadoConductor"))
                    .isEqualTo(finalizar ? "DISPONIBLE" : "OCUPADO");
        }
    }

    @Test void terminalesYTransicionesInvalidasNoCambianNada() {
        var solicitante = api.solicitante();
        for (String terminal : List.of("RECHAZADO", "ANULADO", "REEMBOLSADO")) {
            String folio = api.viaje(solicitante), hash = api.pagarOk(folio);
            if (terminal.equals("REEMBOLSADO")) {
                cambio(hash, "PAGADO"); api.cancelarOk(folio, solicitante);
            } else cambio(hash, terminal);
            String antes = pago(hash).cuerpo();
            for (String destino : List.of("PAGADO", "RECHAZADO", "ANULADO")) assertThat(cambio(hash, destino).status()).isEqualTo(409);
            assertThat(pago(hash).cuerpo()).isEqualTo(antes);
        }
        String hash = api.pagarOk(api.viaje(solicitante));
        for (String destino : List.of("", "OTRO", "PENDIENTE", "REEMBOLSADO")) assertThat(cambio(hash, destino).status()).isEqualTo(400);
        assertThat(api.cambiarEstadoPago(hash, solicitante.token(), "ANULADO").status()).isEqualTo(403);
        assertThat(api.cambiarEstadoPago(hash, api.conductor().token(), "PAGADO").status()).isEqualTo(403);
        assertThat(api.cambiarEstadoPago(hash, null, "PAGADO").status()).isEqualTo(401);
        assertThat(cambio("00000000000000000000000000000000", "ANULADO").status()).isEqualTo(404);
        assertThat(api.estadoPago(hash)).isEqualTo("PENDIENTE");
    }

    @Test void dosCambiosIncompatiblesSimultaneosConfirmanSoloUno() throws Exception {
        String hash = api.pagarOk(api.viaje(api.solicitante())); String token = api.admin();
        assertThat(simultaneos(() -> api.cambiarEstadoPago(hash, token, "PAGADO").status(),
                () -> api.cambiarEstadoPago(hash, token, "RECHAZADO").status())).containsExactlyInAnyOrder(200, 409);
        assertThat(api.estadoPago(hash)).isIn("PAGADO", "RECHAZADO");
    }

    @Test void pagarYCancelarSimultaneamenteNoResucitaUnPagoAnulado() throws Exception {
        for (int i = 0; i < 5; i++) {
            var solicitante = api.solicitante(); String folio = api.viaje(solicitante), hash = api.pagarOk(folio), token = api.admin();
            var estados = simultaneos(() -> api.cambiarEstadoPago(hash, token, "PAGADO").status(),
                    () -> api.cancelar(folio, solicitante.token()).status());
            assertThat(estados.get(0)).isIn(200, 409); assertThat(estados.get(1)).isEqualTo(200);
            assertThat(api.estadoViaje(folio)).isEqualTo("CANCELADO");
            assertThat(api.estadoPago(hash)).isEqualTo(estados.get(0) == 200 ? "REEMBOLSADO" : "ANULADO");
        }
    }

    @Test void anularEIniciarSimultaneamenteRespetaElEstadoDelViaje() throws Exception {
        var solicitante = api.solicitante(); var flota = api.flota(4);
        String folio = api.viajeAsignado(solicitante, flota), hash = api.pagarOk(folio), token = api.admin();
        jdbc.update("update viaje set fh_agenda = ? where folio = ?", LocalDateTime.now(cl.belmontespa.transporte.config.ClockConfig.ZONA), folio);
        var estados = simultaneos(() -> api.cambiarEstadoPago(hash, token, "ANULADO").status(),
                () -> api.cambiarEstado(folio, flota.conductor().token(), "EN_CURSO").status());
        assertThat(estados.get(0)).isEqualTo(200); assertThat(estados.get(1)).isIn(200, 409);
        assertThat(api.estadoViaje(folio)).isEqualTo(estados.get(1) == 200 ? "EN_CURSO" : "CANCELADO");
        assertThat(api.estadoPago(hash)).isEqualTo("ANULADO");
    }

    @Test void rollbackReviertePagoYViajeYNoEnviaPush() {
        var solicitante = api.solicitante(); api.dispositivo(solicitante);
        String folio = api.viaje(solicitante), hash = api.pagarOk(folio);
        esperarAvisos(); fcm.limpiar();
        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            pagos.cambiarEstado(hash, new CambiarEstadoPagoDTORequest("ANULADO"));
            throw new IllegalStateException("Revertir la operación completa");
        })).isInstanceOf(IllegalStateException.class);
        esperarAvisos(); assertThat(fcm.envios()).isEmpty();
        assertThat(api.estadoPago(hash)).isEqualTo("PENDIENTE");
        assertThat(api.estadoViaje(folio)).isEqualTo("PENDIENTE");
    }
}
