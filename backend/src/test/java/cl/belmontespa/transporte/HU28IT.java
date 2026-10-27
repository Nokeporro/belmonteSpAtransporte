package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.config.JwtProvider;
import cl.belmontespa.transporte.dto.request.AdministradorDTORequest;
import cl.belmontespa.transporte.service.AdministradorService;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class HU28IT extends IntegrationTest {
    private static final String INICIAL = "Inicial12345";
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AdministradorService administradores;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired JwtProvider jwt;
    private String correo() { return "hu28-" + System.nanoTime() + "@test.cl"; }
    private String cuerpo(String correo) {
        return "{\"nombre\":\"Nueva Administradora\",\"correo\":\"" + correo + "\",\"password\":\"" + INICIAL + "\"}";
    }
    private String crear() {
        String correo = correo(); Api.exigir(api.post("/administradores", api.admin(), cuerpo(correo)), "alta", 201); return correo;
    }
    private String columna(String correo, String campo) {
        return jdbc.queryForObject("select " + campo + " from usuario where correo=?", String.class, correo);
    }
    private String cambio(String correo, String actual) {
        return "{\"nombre\":\"Nueva Administradora\",\"correo\":\"" + correo + "\",\"password\":\"NuevaClave987\",\"passwordActual\":\"" + actual + "\"}";
    }

    @Test void altaNormalizadaSoloConTemporalBcryptDe24HorasYRespuestaSinSecretos() {
        String correo = correo(); LocalDateTime antes = LocalDateTime.now(ClockConfig.ZONA);
        var alta = Api.exigir(api.post("/administradores", api.admin(), cuerpo("  " + correo.toUpperCase(java.util.Locale.ROOT) + "  ")
                .replace("Nueva Administradora", "  Nueva Administradora  ").replace("\"password\"", "\"telefono\":\"9 5555-0101\",\"password\"")), "alta normalizada", 201);
        assertThat(alta.texto("$.nombre")).isEqualTo("Nueva Administradora");
        assertThat(alta.texto("$.correo")).isEqualTo(correo); assertThat(alta.texto("$.telefono")).isEqualTo("+56955550101");
        assertThat(alta.texto("$.fechaRegistro")).isNotBlank();
        assertThat(alta.cuerpo()).doesNotContain(INICIAL, "password", "Hash", "Temporal", "$2");
        assertThat(columna(correo, "password_hash")).isEqualTo("*");
        String temporal = columna(correo, "password_temporal_hash");
        assertThat(temporal).startsWith("$2").isNotEqualTo(INICIAL); assertThat(encoder.matches(INICIAL, temporal)).isTrue();
        LocalDateTime vence = jdbc.queryForObject("select password_temporal_expira from usuario where correo=?", LocalDateTime.class, correo);
        assertThat(vence).isBetween(antes.plusHours(24).minusSeconds(1), LocalDateTime.now(ClockConfig.ZONA).plusHours(24));
        var guardado = Api.exigir(api.get("/administradores/" + correo.toUpperCase(java.util.Locale.ROOT), api.admin()), "consultar", 200);
        assertThat(guardado.cuerpo()).isEqualTo(alta.cuerpo());
    }

    @Test void loginInicialEsTemporalYNoPermiteOperarHastaCambiarLaClave() {
        String correo = crear();
        var login = Api.exigir(api.iniciarSesion(correo, INICIAL), "login inicial", 200);
        assertThat(login.<Boolean>json("$.cambioPasswordRequerido")).isTrue();
        String token = login.texto("$.accessToken");
        assertThat(jwt.decoder().decode(token).<Boolean>getClaim("temporal")).isTrue();
        assertThat(api.get("/usuarios/" + correo, token).status()).isEqualTo(200);
        for (String ruta : List.of("/viajes", "/administradores", "/usuarios/" + Api.ADMIN_CORREO))
            assertThat(api.get(ruta, token).status()).isEqualTo(403);
        assertThat(api.post("/administradores", token, cuerpo(correo())).status()).isEqualTo(403);
        String hash = columna(correo, "password_temporal_hash");
        assertThat(api.put("/usuarios/" + correo, token, cambio(correo, "Incorrecta123")).status()).isEqualTo(400);
        assertThat(columna(correo, "password_temporal_hash")).isEqualTo(hash);
        Api.exigir(api.put("/usuarios/" + correo, token, cambio(correo, INICIAL)), "cambio obligatorio", 200);
        assertThat(columna(correo, "password_temporal_hash")).isNull();
        assertThat(columna(correo, "password_temporal_expira")).isNull();
        assertThat(api.iniciarSesion(correo, INICIAL).status()).isEqualTo(401);
        var definitivo = Api.exigir(api.iniciarSesion(correo, "NuevaClave987"), "login definitivo", 200);
        assertThat(definitivo.<Boolean>json("$.cambioPasswordRequerido")).isFalse();
        assertThat(api.get("/administradores", definitivo.texto("$.accessToken")).status()).isEqualTo(200);
    }

    @Test void inicialVencidaNoFuncionaComoPermanenteYEl401EsGenerico() {
        String correo = crear();
        jdbc.update("update usuario set password_temporal_expira=? where correo=?", LocalDateTime.now(ClockConfig.ZONA).minusSeconds(1), correo);
        var login = api.iniciarSesion(correo, INICIAL);
        assertThat(login.status()).isEqualTo(401); assertThat(login.texto("$.message")).isEqualTo("Credenciales inválidas");
        assertThat(api.iniciarSesion(correo, "Incorrecta123").texto("$.message")).isEqualTo(login.texto("$.message"));
        assertThat(columna(correo, "password_hash")).isEqualTo("*");
    }

    @Test void soloAdministradorAutenticadoDaDeAltaYCorreosRepetidosNoSeSobrescriben() {
        String correo = crear(), hash = columna(correo, "password_temporal_hash");
        assertThat(api.post("/administradores", null, cuerpo(correo())).status()).isEqualTo(401);
        for (String token : List.of(api.conductor().token(), api.solicitante().token()))
            assertThat(api.post("/administradores", token, cuerpo(correo())).status()).isEqualTo(403);
        for (String repetido : List.of(correo.toUpperCase(java.util.Locale.ROOT), Api.ADMIN_CORREO, api.conductor().correo(), api.solicitante().correo()))
            assertThat(api.post("/administradores", api.admin(), cuerpo(repetido)).status()).isEqualTo(409);
        assertThat(columna(correo, "password_temporal_hash")).isEqualTo(hash);
    }

    @Test void validarAntesDeCrearEvitaCuentasIncompletasYElListadoOrdenaNombreYCorreo() {
        String correo = correo(), valido = cuerpo(correo);
        for (String invalido : List.of(valido.replace(INICIAL, "corta"), valido.replace(INICIAL, "abcdefgh"),
                valido.replace(INICIAL, "ñ".repeat(36) + "1"), valido.replace("Nueva Administradora", "A"),
                valido.replace(correo, "invalido"), valido.replace("\"password\":\"" + INICIAL + "\"", "\"password\":null"),
                valido.replace(",\"password\":\"" + INICIAL + "\"", ""))) {
            assertThat(api.post("/administradores", api.admin(), invalido).status()).isEqualTo(400);
        }
        assertThat(jdbc.queryForObject("select count(*) from usuario where correo=?", Integer.class, correo)).isZero();
        String prefijo = "OrdenHU28" + System.nanoTime(), a = "a-" + correo, b = "b-" + correo, z = "z-" + correo;
        for (String c : List.of(b, z, a)) Api.exigir(api.post("/administradores", api.admin(), cuerpo(c)
                .replace("Nueva Administradora", prefijo + (c.equals(z) ? " Zeta" : " Ana"))), "alta orden", 201);
        var lista = Api.exigir(api.get("/administradores", api.admin()), "listar", 200);
        assertThat(lista.<List<String>>json("$[?(@.nombre =~ /" + prefijo + ".*/)].correo")).containsExactly(a, b, z);
    }

    @Test void altasSimultaneasDejanUnaCuentaYUnRol() throws Exception {
        String correo = correo(), token = api.admin(); var inicio = new CountDownLatch(1);
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(() -> { inicio.await(); return api.post("/administradores", token, cuerpo(correo)).status(); });
            var b = hilos.submit(() -> { inicio.await(); return api.post("/administradores", token, cuerpo(correo.toUpperCase(java.util.Locale.ROOT))).status(); });
            inicio.countDown(); assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from usuario where correo=?", Integer.class, correo)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from administrador a join usuario u on a.id_usuario=u.id_usuario where u.correo=?", Integer.class, correo)).isEqualTo(1);
    }

    @Test void rollbackNoDejaUsuarioNiAdministrador() {
        String correo = correo();
        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            administradores.crear(new AdministradorDTORequest("Administrador Revertido", correo, null, INICIAL));
            throw new IllegalStateException("Revertir alta");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from usuario where correo=?", Integer.class, correo)).isZero();
        assertThat(api.get("/administradores/" + correo, api.admin()).status()).isEqualTo(404);
    }
}
