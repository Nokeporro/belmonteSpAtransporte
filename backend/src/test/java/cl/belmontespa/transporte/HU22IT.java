package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.dto.request.RecuperarPasswordDTORequest;
import cl.belmontespa.transporte.service.AuthService;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class HU22IT extends IntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthService auth;
    @Autowired PlatformTransactionManager transacciones;

    private Api.Resp recuperar(String correo) {
        return api.recuperarPassword(api.admin(), correo);
    }
    private String columna(String correo, String columna) {
        return jdbc.queryForObject("select " + columna + " from usuario where correo = ?", String.class, correo);
    }
    private String pedirClave(String correo) {
        return Api.exigir(recuperar(correo), "recuperar contraseña", 200).texto("$.passwordTemporal");
    }
    private String cuerpo(String correo, String actual, String nueva) {
        return """
                {"nombre":"Nombre Actualizado","correo":"%s","passwordActual":"%s","password":"%s"}
                """.formatted(correo, actual, nueva);
    }

    @Test void recuperaSolicitanteYAdministradorConCorreoExactoHashYVencimiento() {
        String administrador = "hu22-" + System.nanoTime() + "@test.cl";
        Api.exigir(api.post("/administradores", api.admin(), """
                {"nombre":"Administrador HU22","correo":"%s","password":"ClaveOriginal123"}
                """.formatted(administrador)), "alta", 201);
        for (String correo : List.of(api.solicitante().correo(), administrador)) {
            String anterior = columna(correo, "password_hash");
            var respuesta = Api.exigir(recuperar("  " + correo.toUpperCase() + "  "), "recuperar contraseña", 200);
            String clave = respuesta.texto("$.passwordTemporal"), hash = columna(correo, "password_temporal_hash");
            assertThat(respuesta.texto("$.correo")).isEqualTo(correo);
            assertThat(clave).matches("[A-Za-z0-9]{12}");
            assertThat(hash).startsWith("$2").isNotEqualTo(clave); assertThat(encoder.matches(clave, hash)).isTrue();
            assertThat(columna(correo, "password_hash")).isEqualTo(anterior);
            LocalDateTime vence = jdbc.queryForObject("select password_temporal_expira from usuario where correo = ?", LocalDateTime.class, correo);
            assertThat(vence).isBetween(LocalDateTime.now(ClockConfig.ZONA).plusHours(24).minusMinutes(1), LocalDateTime.now(ClockConfig.ZONA).plusHours(24).plusMinutes(1));
            assertThat(LocalDateTime.parse(respuesta.texto("$.expira"))).isEqualTo(vence);
            assertThat(CORREO.getReceivedMessages()).isEmpty();
            var login = Api.exigir(api.iniciarSesion(correo, clave), "temporal", 200);
            assertThat(login.<Boolean>json("$.cambioPasswordRequerido")).isTrue();
            assertThat(api.get("/viajes", login.texto("$.accessToken")).status()).isEqualTo(403);
        }
    }

    @Test void segundoPedidoReemplazaLaTemporalYLoginNormalLaInvalida() {
        var cuenta = api.solicitante(); String primera = pedirClave(cuenta.correo()); String segunda = pedirClave(cuenta.correo());
        assertThat(segunda).isNotEqualTo(primera);
        assertThat(api.iniciarSesion(cuenta.correo(), primera).status()).isEqualTo(401);
        assertThat(api.iniciarSesion(cuenta.correo(), segunda).<Boolean>json("$.cambioPasswordRequerido")).isTrue();
        assertThat(api.iniciarSesion(cuenta.correo(), Api.CLAVE).<Boolean>json("$.cambioPasswordRequerido")).isFalse();
        assertThat(columna(cuenta.correo(), "password_temporal_hash")).isNull();
        assertThat(columna(cuenta.correo(), "password_temporal_expira")).isNull();
        assertThat(api.iniciarSesion(cuenta.correo(), segunda).status()).isEqualTo(401);
    }

    @Test void temporalVencidaNoSirveParaLoginNiCambioYElErrorNoModificaElPerfil() {
        var cuenta = api.solicitante(); String clave = pedirClave(cuenta.correo());
        String token = Api.exigir(api.iniciarSesion(cuenta.correo(), clave), "temporal", 200).texto("$.accessToken");
        jdbc.update("update usuario set password_temporal_expira = ? where correo = ?", LocalDateTime.now(ClockConfig.ZONA).minusSeconds(1), cuenta.correo());
        assertThat(api.iniciarSesion(cuenta.correo(), clave).status()).isEqualTo(401);
        String nombre = columna(cuenta.correo(), "nombre"), hash = columna(cuenta.correo(), "password_hash");
        var error = api.put("/usuarios/" + cuenta.correo(), token, cuerpo(cuenta.correo(), clave, "NuevaClave987"));
        assertThat(error.status()).isEqualTo(400); assertThat(error.texto("$.message")).isEqualTo("passwordActual: No coincide.");
        assertThat(columna(cuenta.correo(), "nombre")).isEqualTo(nombre); assertThat(columna(cuenta.correo(), "password_hash")).isEqualTo(hash);
        assertThat(api.iniciarSesion(cuenta.correo(), Api.CLAVE).status()).isEqualTo(200);
    }

    @Test void cambioExigeClaveActualCorreoInmutableYAnulaLaTemporal() {
        var cuenta = api.solicitante(); String clave = pedirClave(cuenta.correo());
        String token = Api.exigir(api.iniciarSesion(cuenta.correo(), clave), "temporal", 200).texto("$.accessToken");
        String ruta = "/usuarios/" + cuenta.correo(), original = api.get(ruta, token).cuerpo();
        assertThat(api.put(ruta, token, cuerpo(cuenta.correo(), "", "NuevaClave987")).status()).isEqualTo(400);
        assertThat(api.put(ruta, token, cuerpo("otra@test.cl", clave, "NuevaClave987")).status()).isEqualTo(400);
        assertThat(api.put(ruta, token, cuerpo(cuenta.correo(), clave, "sinNumeros")).status()).isEqualTo(400);
        assertThat(api.get(ruta, token).cuerpo()).isEqualTo(original);
        Api.exigir(api.put(ruta, token, cuerpo(cuenta.correo(), clave, "NuevaClave987")), "cambiar", 200);
        assertThat(columna(cuenta.correo(), "password_temporal_hash")).isNull();
        assertThat(api.iniciarSesion(cuenta.correo(), clave).status()).isEqualTo(401);
        assertThat(api.iniciarSesion(cuenta.correo(), Api.CLAVE).status()).isEqualTo(401);
        assertThat(api.iniciarSesion(cuenta.correo(), "NuevaClave987").<Boolean>json("$.cambioPasswordRequerido")).isFalse();
    }

    @Test void conductorYCorreoInexistenteDanNotFoundYNoDejanTemporal() {
        var conductor = api.conductor(); String antes = columna(conductor.correo(), "password_hash");
        for (String correo : List.of(conductor.correo(), "ausente-" + System.nanoTime() + "@test.cl")) {
            assertThat(recuperar(correo).status()).isEqualTo(404);
        }
        assertThat(columna(conductor.correo(), "password_temporal_hash")).isNull(); assertThat(columna(conductor.correo(), "password_hash")).isEqualTo(antes);
        jdbc.update("update usuario set password_temporal_hash=?,password_temporal_expira=? where correo=?", encoder.encode("Temporal1234"), LocalDateTime.now(ClockConfig.ZONA).plusHours(1), conductor.correo());
        assertThat(api.iniciarSesion(conductor.correo(), "Temporal1234").status()).isEqualTo(401);
    }

    @Test void cuentaPendienteRecibeTemporalPeroNoPuedeEntrar() {
        var pendiente = api.solicitante();
        jdbc.update("update solicitante s join usuario u on u.id_usuario=s.id_usuario set s.estado_solicitante_id=(select id from estado_solicitante where estado='PENDIENTE') where u.correo=?", pendiente.correo());
        String clave = pedirClave(pendiente.correo());
        assertThat(api.iniciarSesion(pendiente.correo(), clave).status()).isEqualTo(403);
    }

    @Test void soloElAdministradorPuedePedirla() {
        var cuenta = api.solicitante(); var conductor = api.conductor();
        assertThat(api.recuperarPassword(null, cuenta.correo()).status()).isEqualTo(401);
        assertThat(api.recuperarPassword(cuenta.token(), cuenta.correo()).status()).isEqualTo(403);
        assertThat(api.recuperarPassword(conductor.token(), cuenta.correo()).status()).isEqualTo(403);
        assertThat(columna(cuenta.correo(), "password_temporal_hash")).isNull();
    }

    @Test void unaSesionTemporalNoPuedePedirla() {
        String administrador = "hu22-temporal-" + System.nanoTime() + "@test.cl";
        Api.exigir(api.post("/administradores", api.admin(), """
                {"nombre":"Administrador HU22","correo":"%s","password":"ClaveOriginal123"}
                """.formatted(administrador)), "alta", 201);
        String token = Api.exigir(api.iniciarSesion(administrador, "ClaveOriginal123"), "temporal inicial", 200).texto("$.accessToken");
        assertThat(api.recuperarPassword(token, api.solicitante().correo()).status()).isEqualTo(403);
    }

    @Test void validaElCorreo() {
        assertThat(recuperar("no-es-correo").status()).isEqualTo(400);
        assertThat(api.post("/auth/recuperar-password", api.admin(), "{}").status()).isEqualTo(400);
    }

    @Test void dosCambiosSimultaneosNoAceptanDosVecesLaClaveAnterior() throws Exception {
        var cuenta = api.solicitante(); String ruta = "/usuarios/" + cuenta.correo();
        var inicio = new CountDownLatch(1);
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(() -> { inicio.await(); return api.put(ruta, cuenta.token(), cuerpo(cuenta.correo(), Api.CLAVE, "NuevaClave111")).status(); });
            var b = hilos.submit(() -> { inicio.await(); return api.put(ruta, cuenta.token(), cuerpo(cuenta.correo(), Api.CLAVE, "NuevaClave222")).status(); });
            inicio.countDown();
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 400);
        }
    }

    @Test void rollbackDeRecuperacionNoDejaTemporal() {
        var cuenta = api.solicitante();
        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            auth.recuperarPassword(new RecuperarPasswordDTORequest(cuenta.correo()));
            throw new IllegalStateException("Revertir");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(columna(cuenta.correo(), "password_temporal_hash")).isNull();
    }
}
