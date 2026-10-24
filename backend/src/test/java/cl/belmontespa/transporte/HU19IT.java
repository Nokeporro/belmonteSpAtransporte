package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class HU19IT extends IntegrationTest {
    @Autowired JdbcTemplate jdbc;
    private long pagos() { return jdbc.queryForObject("select count(*) from pago", Long.class); }

    @Test
    void altaPendienteConFolioEmpresaYFechaNulaEnAltaDetalleYLista() {
        var solicitante = api.solicitante();
        String folio = api.viaje(solicitante);
        var alta = Api.exigir(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", 1), "alta", 201);
        String hash = alta.texto("$.hash");
        assertThat(hash).matches("[0-9a-f]{32}");
        assertThat(alta.texto("$.folio")).isEqualTo(folio);
        assertThat(alta.texto("$.estadoPago")).isEqualTo("PENDIENTE");
        assertThat(alta.texto("$.fechaPago")).isNull();
        assertThat(alta.texto("$.montoReembolsado")).isNull();
        String rut = jdbc.queryForObject("select e.rut from empresa e join solicitante s on s.empresa_id_empresa = e.id_empresa join usuario u on u.id_usuario = s.id_usuario where u.correo = ?", String.class, solicitante.correo());
        assertThat(alta.texto("$.rutEmpresa")).isEqualTo(rut);
        var detalle = Api.exigir(api.get("/pagos/" + hash, api.admin()), "detalle", 200);
        assertThat(detalle.cuerpo()).isEqualTo(alta.cuerpo());
        assertThat(api.get("/pagos?ignorado=x", api.admin()).<List<String>>json("$[?(@.hash=='" + hash + "')].folio")).containsExactly(folio);
        assertThat(api.get("/pagos/" + hash, solicitante.token()).texto("$.folio")).isEqualTo(folio);
    }

    @Test
    void catalogosInvalidosNoDejanPagoNiAsociacion() {
        String folio = api.viaje(api.solicitante()); long antes = pagos();
        assertThat(api.pagar(api.admin(), folio, 999999, "TRANSFERENCIA", 100).status()).isEqualTo(400);
        assertThat(api.pagar(api.admin(), folio, 12, "INEXISTENTE", 100).status()).isEqualTo(400);
        assertThat(api.post("/pagos", api.admin(), """
                {"folio":"%s","monto":100,"codSbif":12,"medioPago":"EFECTIVO","tipoRecibo":"OTRO"}
                """.formatted(folio)).status()).isEqualTo(400);
        assertThat(pagos()).isEqualTo(antes);
        assertThat(api.verViaje(folio).texto("$.hashPago")).isNull();
        assertThat(api.pagarOk(folio)).matches("[0-9a-f]{32}");
    }

    @Test
    void montosLimiteYRechazoSinFechaDePago() {
        String folio = api.viaje(api.solicitante());
        var alta = Api.exigir(api.pagar(api.admin(), folio, 12, "EFECTIVO", Integer.MAX_VALUE), "máximo entero", 201);
        assertThat(alta.entero("$.monto")).isEqualTo(Integer.MAX_VALUE);
        var rechazado = Api.exigir(api.cambiarEstadoPago(alta.texto("$.hash"), api.admin(), "RECHAZADO"), "rechazar", 200);
        assertThat(rechazado.texto("$.fechaPago")).isNull();
        assertThat(rechazado.texto("$.folio")).isEqualTo(folio);
    }

    @Test
    void dosRegistrosSimultaneosCreanUnPagoSinHuerfanos() throws Exception {
        String folio = api.viaje(api.solicitante()); String token = api.admin(); long antes = pagos();
        var inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(() -> { inicio.await(); return api.pagar(token, folio, 12, "TRANSFERENCIA", 100).status(); });
            var dos = ejecutor.submit(() -> { inicio.await(); return api.pagar(token, folio, 12, "TRANSFERENCIA", 200).status(); });
            inicio.countDown();
            assertThat(List.of(uno.get(30, TimeUnit.SECONDS), dos.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(pagos()).isEqualTo(antes + 1);
        assertThat(api.verViaje(folio).texto("$.hashPago")).isNotBlank();
    }

    @Test
    void registrarYCancelarSimultaneamenteNoDejaUnPagoPendienteEnUnViajeCancelado() throws Exception {
        var solicitante = api.solicitante(); String folio = api.viaje(solicitante); String token = api.admin(); long antes = pagos();
        var inicio = new CountDownLatch(1);
        int alta;
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(() -> { inicio.await(); return api.pagar(token, folio, 12, "TRANSFERENCIA", 100).status(); });
            var dos = ejecutor.submit(() -> { inicio.await(); return api.cancelar(folio, solicitante.token()).status(); });
            inicio.countDown(); alta = uno.get(30, TimeUnit.SECONDS);
            assertThat(dos.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(alta).isIn(201, 409);
        assertThat(api.estadoViaje(folio)).isEqualTo("CANCELADO");
        if (alta == 201) {
            assertThat(api.verViaje(folio).texto("$.estadoPago")).isEqualTo("ANULADO");
            assertThat(pagos()).isEqualTo(antes + 1);
        } else {
            assertThat(api.verViaje(folio).texto("$.hashPago")).isNull();
            assertThat(pagos()).isEqualTo(antes);
        }
    }

    @Test
    void permiteRegistrarMientrasElViajeEstaEnCurso() {
        var solicitante = api.solicitante(); var flota = api.flota(2);
        String folio = api.viajeAsignado(solicitante, flota); api.iniciarOk(folio, flota.conductor());
        assertThat(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", 100).status()).isEqualTo(201);
        assertThat(api.estadoViaje(folio)).isEqualTo("EN_CURSO");
    }
}
