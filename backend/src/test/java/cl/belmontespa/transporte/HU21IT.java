package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class HU21IT extends IntegrationTest {
    @Test void solicitanteConsultaSusCincoEstadosConFolioMontoFechaYReembolsoSinPoderModificarlos() {
        var sol = api.solicitante();
        for (String estado : List.of("PENDIENTE", "PAGADO", "RECHAZADO", "ANULADO", "REEMBOLSADO")) {
            String folio = api.viaje(sol);
            String hash = Api.exigir(api.pagar(api.admin(), folio, 12, "TRANSFERENCIA", 50001), "registrar", 201).texto("$.hash");
            if (List.of("PAGADO", "REEMBOLSADO").contains(estado))
                Api.exigir(api.cambiarEstadoPago(hash, api.admin(), "PAGADO"), "pagar", 200);
            if (estado.equals("REEMBOLSADO")) api.cancelarOk(folio, sol);
            if (List.of("RECHAZADO", "ANULADO").contains(estado))
                Api.exigir(api.cambiarEstadoPago(hash, api.admin(), estado), "cambiar estado", 200);
            var viaje = Api.exigir(api.get("/viajes/" + folio, sol.token()), "consultar solicitud", 200);
            assertThat(viaje.texto("$.hashPago")).isEqualTo(hash);
            assertThat(viaje.texto("$.estadoPago")).isEqualTo(estado);
            var pago = Api.exigir(api.get("/pagos/" + hash, sol.token()), "consultar pago propio", 200);
            assertThat(pago.texto("$.folio")).isEqualTo(folio);
            assertThat(pago.texto("$.estadoPago")).isEqualTo(estado);
            assertThat(pago.entero("$.monto")).isEqualTo(50001);
            assertThat(pago.texto("$.banco")).isNotBlank();
            assertThat(pago.texto("$.medioPago")).isEqualTo("TRANSFERENCIA");
            assertThat(pago.texto("$.tipoRecibo")).isEqualTo("BOLETA");
            if (List.of("PAGADO", "REEMBOLSADO").contains(estado)) assertThat(pago.texto("$.fechaPago")).isNotBlank();
            else assertThat(pago.texto("$.fechaPago")).isNull();
            if (estado.equals("REEMBOLSADO")) assertThat(pago.entero("$.montoReembolsado")).isEqualTo(25000);
            else assertThat(pago.texto("$.montoReembolsado")).isNull();
            assertThat(api.cambiarEstadoPago(hash, sol.token(), "ANULADO").status()).isEqualTo(403);
            assertThat(api.get("/pagos/" + hash, sol.token()).texto("$.estadoPago")).isEqualTo(estado);
        }
    }

    @Test void compartirEmpresaNoDaAccesoALosPagosDeOtroSolicitante() {
        var ana = api.solicitante();
        String rut = api.get("/solicitantes/" + ana.correo(), api.admin()).texto("$.rutEmpresa");
        String correo = "hu21-" + System.nanoTime() + "@test.cl";
        Api.exigir(api.post("/solicitantes", null, api.cuerpoSolicitante(correo, rut, "Empresa compartida")), "registrar colega", 201);
        Api.exigir(api.post("/solicitantes/" + correo + "/aprobar", api.admin(), null), "aprobar colega", 200);
        String colega = api.login(correo, Api.CLAVE), folio = api.viaje(ana), hash = api.pagarOk(folio);
        assertThat(api.get("/pagos/" + hash, colega).status()).isEqualTo(403);
        assertThat(api.get("/viajes/" + folio, colega).status()).isEqualTo(403);
        assertThat(api.get("/pagos", colega).<List<String>>json("$[*].hash")).doesNotContain(hash);
        assertThat(api.get("/pagos/" + hash, api.conductor().token()).status()).isEqualTo(403);
        assertThat(api.get("/pagos/" + hash, null).status()).isEqualTo(401);
        assertThat(api.get("/pagos/00000000000000000000000000000000", ana.token()).status()).isEqualTo(404);
    }
}
