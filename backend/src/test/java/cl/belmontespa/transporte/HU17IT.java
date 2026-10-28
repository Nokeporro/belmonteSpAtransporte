package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HU17IT extends IntegrationTest {
    @Test
    void altaDevuelveEstadoActivoGpsYCodigoPersistidoEnDetalleYLista() {
        String patente = api.movil(20);
        var detalle = Api.exigir(api.get("/moviles/" + patente, api.admin()), "detalle", 200);
        assertThat(detalle.<Boolean>json("$.activo")).isTrue();
        assertThat(detalle.texto("$.estadoMovil")).isEqualTo("DISPONIBLE");
        assertThat(detalle.texto("$.gpsSerial")).isNull();
        assertThat(detalle.texto("$.codigoAbordaje")).matches("[0-9A-F]{16}");
        var lista = Api.exigir(api.get("/moviles", api.admin()), "lista", 200);
        List<Map<String, Object>> filas = lista.json("$[?(@.patente=='" + patente + "')]");
        assertThat(filas).hasSize(1);
        assertThat(filas.getFirst()).containsEntry("activo", true)
                .containsEntry("codigoAbordaje", detalle.texto("$.codigoAbordaje"));
    }

    @Test
    void normalizaAltaConGpsYNoReemplazaLaPatenteDuplicada() {
        String patente = api.movil(1);
        String libre = "XY" + patente.substring(2);
        var alta = Api.exigir(api.post("/moviles", api.admin(), """
                {"patente":" xy-%s ","capacidad":20,"gpsSerial":2147483647}
                """.formatted(patente.substring(2))), "alta con GPS", 201);
        assertThat(alta.texto("$.patente")).isEqualTo(libre);
        assertThat(alta.entero("$.gpsSerial")).isEqualTo(Integer.MAX_VALUE);
        assertThat(alta.<Boolean>json("$.activo")).isTrue();
        String codigo = alta.texto("$.codigoAbordaje");
        assertThat(api.post("/moviles", api.admin(), api.cuerpoMovil("xy-" + patente.substring(2), 1)).status()).isEqualTo(409);
        var despues = Api.exigir(api.get("/moviles/" + libre, api.admin()), "sin reemplazo", 200);
        assertThat(despues.entero("$.capacidad")).isEqualTo(20);
        assertThat(despues.texto("$.codigoAbordaje")).isEqualTo(codigo);
    }

    @Test
    void codigosSonDistintosYEstablesAlConsultar() {
        var codigos = new HashSet<String>();
        for (int i = 0; i < 12; i++) {
            String patente = api.movil(2);
            var detalle = Api.exigir(api.get("/moviles/" + patente, api.admin()), "detalle", 200);
            String codigo = detalle.texto("$.codigoAbordaje");
            assertThat(codigo).matches("[0-9A-F]{16}");
            assertThat(codigos.add(codigo)).isTrue();
            assertThat(api.get("/moviles/" + patente, api.admin()).texto("$.codigoAbordaje")).isEqualTo(codigo);
        }
    }

    @Test
    void dosAltasSimultaneasDeLaMismaPatenteCreanSoloUnVehiculo() throws Exception {
        String patente = "XZ" + api.movil(1).substring(2);
        String token = api.admin();
        var inicio = new CountDownLatch(1);
        Callable<Integer> alta = () -> {
            assertThat(inicio.await(10, TimeUnit.SECONDS)).isTrue();
            return api.post("/moviles", token, api.cuerpoMovil(patente, 3)).status();
        };
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(alta);
            var dos = ejecutor.submit(alta);
            inicio.countDown();
            assertThat(List.of(uno.get(30, TimeUnit.SECONDS), dos.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(api.get("/moviles", token).<List<?>>json("$[?(@.patente=='" + patente + "')]")).hasSize(1);
    }

    @Test
    void codigoNoEsAccesibleParaOtrosRolesNiSinSesion() {
        String patente = api.movil(1);
        for (String token : List.of(api.conductor().token(), api.solicitante().token())) {
            assertThat(api.get("/moviles/" + patente, token).status()).isEqualTo(403);
            assertThat(api.get("/moviles", token).status()).isEqualTo(403);
            assertThat(api.post("/moviles", token, api.cuerpoMovil(patente, 1)).status()).isEqualTo(403);
        }
        assertThat(api.get("/moviles/" + patente, null).status()).isEqualTo(401);
        assertThat(api.get("/moviles", null).status()).isEqualTo(401);
    }
}
