package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

class HU18IT extends IntegrationTest {
    private static final String RUTA = "/habilitaciones-movil-conductor";
    @Autowired JdbcTemplate jdbc;

    private String cuerpo(String patente, String correo) {
        return """
                {"patente":"%s","correoConductor":"%s"}
                """.formatted(patente, correo);
    }

    @Test
    void relacionMuchosAMuchosConFiltrosNormalizadosYCombinados() {
        var a = api.conductor();
        var b = api.conductor();
        String uno = api.movil(4);
        String dos = api.movil(7);
        api.habilitarOk(uno, a.correo()); api.habilitarOk(uno, b.correo()); api.habilitarOk(dos, a.correo());
        assertThat(api.get(RUTA + "?patente=" + uno, api.admin()).<List<?>>json("$")).hasSize(2);
        assertThat(api.get(RUTA + "?correo=" + a.correo().toUpperCase(java.util.Locale.ROOT), api.admin()).<List<?>>json("$")).hasSize(2);
        var combinada = Api.exigir(api.get(RUTA + "?patente=" + uno.toLowerCase(java.util.Locale.ROOT)
                + "&correo=" + b.correo(), api.admin()), "filtros combinados", 200);
        assertThat(combinada.<List<?>>json("$")).hasSize(1);
        assertThat(combinada.texto("$[0].correoConductor")).isEqualTo(b.correo());
        assertThat(api.get(RUTA + "?patente=" + dos + "&correo=" + b.correo(), api.admin()).<List<?>>json("$")).isEmpty();
        assertThat(api.get(RUTA + "?patente=ZZ0000", api.admin()).<List<?>>json("$")).isEmpty();
    }

    @Test
    void primeraAlta201Repeticion200YDetalleConTodosLosEstados() {
        var conductor = api.conductor();
        String patente = api.movil(8);
        var primera = Api.exigir(api.post(RUTA, api.admin(), cuerpo(" " + patente.toLowerCase(java.util.Locale.ROOT) + " ",
                conductor.correo().toUpperCase(java.util.Locale.ROOT))), "alta normalizada", 201);
        var repetida = Api.exigir(api.habilitar(patente, conductor.correo()), "repetición", 200);
        assertThat(repetida.cuerpo()).isEqualTo(primera.cuerpo());
        var detalle = Api.exigir(api.get(RUTA + "/" + patente + "/" + conductor.correo(), api.admin()), "detalle", 200);
        assertThat(detalle.entero("$.capacidad")).isEqualTo(8);
        assertThat(detalle.texto("$.estadoMovil")).isEqualTo("DISPONIBLE");
        assertThat(detalle.texto("$.estadoConductor")).isEqualTo("DISPONIBLE");
        assertThat(detalle.<Boolean>json("$.activoMovil")).isTrue();
        assertThat(detalle.<Boolean>json("$.activoConductor")).isTrue();
        assertThat(detalle.cuerpo()).doesNotContain("codigoAbordaje", "idMovil", "idUsuario", "password");
        assertThat(api.get(RUTA + "?patente=" + patente, api.admin()).<List<?>>json("$")).hasSize(1);
    }

    @Test
    void peticionesSimultaneasDevuelven201Y200ConUnaSolaRelacion() throws Exception {
        var conductor = api.conductor();
        String patente = api.movil(1);
        String token = api.admin();
        var inicio = new CountDownLatch(1);
        Callable<Integer> alta = () -> {
            assertThat(inicio.await(10, TimeUnit.SECONDS)).isTrue();
            return api.post(RUTA, token, cuerpo(patente, conductor.correo())).status();
        };
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(alta); var dos = ejecutor.submit(alta); inicio.countDown();
            assertThat(List.of(uno.get(30, TimeUnit.SECONDS), dos.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 200);
        }
        assertThat(api.get(RUTA + "?patente=" + patente, token).<List<?>>json("$")).hasSize(1);
    }

    @Test
    void rechazaFiltrosMalFormadosEIgnoraDesconocidos() {
        for (String filtro : List.of("patente=ABC123", "patente=AB12", "correo=invalido")) {
            assertThat(api.get(RUTA + "?" + filtro, api.admin()).status()).isEqualTo(400);
        }
        assertThat(api.get(RUTA + "?patente=&correo=", api.admin()).status()).isEqualTo(200);
        var conductor = api.conductor(); String patente = api.movil(2); api.habilitarOk(patente, conductor.correo());
        assertThat(api.get(RUTA + "?patente=" + patente + "&desconocido=x", api.admin()).<List<?>>json("$")).hasSize(1);
    }

    @Test
    void conservaRelacionesInactivasYNoAgregaNuevasHastaReactivar() {
        var a = api.conductor(); var b = api.conductor(); String patente = api.movil(2);
        api.habilitarOk(patente, a.correo());
        jdbc.update("update movil set activo = false where patente = ?", patente);
        jdbc.update("update conductor c join usuario u on c.id_usuario = u.id_usuario set c.activo = false where u.correo = ?", a.correo());
        var lista = api.get(RUTA + "?patente=" + patente, api.admin());
        assertThat(lista.<Boolean>json("$[0].activoMovil")).isFalse();
        assertThat(lista.<Boolean>json("$[0].activoConductor")).isFalse();
        assertThat(api.habilitar(patente, a.correo()).status()).isEqualTo(200);
        assertThat(api.habilitar(patente, b.correo()).status()).isEqualTo(409);
        assertThat(api.habilitar(api.movil(2), a.correo()).status()).isEqualTo(409);
        assertThat(api.get(RUTA + "?patente=" + patente, api.admin()).<List<?>>json("$")).hasSize(1);
    }

    @Test
    void soloAdministradorConsultaYAgregaSinRutaDeEliminacion() {
        var conductor = api.conductor(); String patente = api.movil(2); api.habilitarOk(patente, conductor.correo());
        String detalle = RUTA + "/" + patente + "/" + conductor.correo();
        for (String token : List.of(conductor.token(), api.solicitante().token())) {
            assertThat(api.get(RUTA, token).status()).isEqualTo(403);
            assertThat(api.get(detalle, token).status()).isEqualTo(403);
            assertThat(api.post(RUTA, token, cuerpo(patente, conductor.correo())).status()).isEqualTo(403);
        }
        assertThat(api.get(RUTA, null).status()).isEqualTo(401);
        assertThat(api.enviar(HttpMethod.DELETE, detalle, api.admin(), null).status()).isEqualTo(405);
    }

    @Test
    void inexistentesYCuentaDeOtroRolNoCreanRelaciones() {
        String patente = api.movil(2);
        assertThat(api.habilitar("ZZ0000", api.conductor().correo()).status()).isEqualTo(404);
        assertThat(api.habilitar(patente, "nadie@test.cl").status()).isEqualTo(404);
        assertThat(api.habilitar(patente, api.solicitante().correo()).status()).isEqualTo(404);
        assertThat(api.get(RUTA + "?patente=" + patente, api.admin()).<List<?>>json("$")).isEmpty();
    }
}
