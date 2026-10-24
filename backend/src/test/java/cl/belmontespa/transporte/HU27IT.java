package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import cl.belmontespa.transporte.config.JwtProvider;
import cl.belmontespa.transporte.service.SolicitanteService;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class HU27IT extends IntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SolicitanteService solicitantes;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired JwtProvider jwt;

    private String rutDeEmpresa() {
        var dueno = api.solicitante();
        return api.get("/solicitantes/" + dueno.correo(), api.admin()).texto("$.rutEmpresa");
    }
    private Api.Resp pendiente(String rut, String nombre, String correo) {
        return Api.exigir(api.post("/solicitantes", null, api.cuerpoSolicitante(correo, rut, "Nombre que no reemplaza la empresa")
                .replace("\"nombre\":\"Solicitante\"", "\"nombre\":\"" + nombre + "\"")), "registro pendiente", 201);
    }
    private String pendiente() {
        String correo = "pendiente-" + System.nanoTime() + "@test.cl";
        assertThat(pendiente(rutDeEmpresa(), "Persona Pendiente", correo).texto("$.estadoSolicitante")).isEqualTo("PENDIENTE");
        return correo;
    }
    private Api.Resp revisar(String correo, String accion, String token) {
        return api.post("/solicitantes/" + correo + "/" + accion, token, null);
    }
    private void esperarEnvios() {
        await().pollDelay(Duration.ofMillis(400)).atMost(Duration.ofSeconds(5)).until(() -> true);
    }
    private String estado(String correo) {
        return api.get("/solicitantes/" + correo, api.admin()).texto("$.estadoSolicitante");
    }

    @Test void filtraLosTresEstadosYOrdenaPorNombreYCorreoSinExponerClaves() {
        String rut = rutDeEmpresa(), prefijo = "filtro-" + System.nanoTime();
        String a = prefijo + "a@test.cl", b = prefijo + "b@test.cl", c = prefijo + "c@test.cl", d = prefijo + "d@test.cl";
        pendiente(rut, "Zoé", d); pendiente(rut, "Álvaro", b); pendiente(rut, "beta", c); pendiente(rut, "Álvaro", a);
        var lista = Api.exigir(api.get("/solicitantes?estado=PENDIENTE&ignorado=x", api.admin()), "listar pendientes", 200);
        assertThat(lista.<List<String>>json("$[?(@.rutEmpresa=='" + rut + "')].correo")).containsExactly(a, b, c, d);
        assertThat(lista.<List<String>>json("$[*].estadoSolicitante")).containsOnly("PENDIENTE");
        assertThat(lista.cuerpo()).doesNotContain("password", "tokenFcm");
        Api.exigir(revisar(a, "aprobar", api.admin()), "aprobar", 200);
        Api.exigir(revisar(b, "rechazar", api.admin()), "rechazar", 200);
        assertThat(api.get("/solicitantes?estado=PENDIENTE", api.admin()).<List<String>>json("$[*].correo")).doesNotContain(a, b).contains(c, d);
        assertThat(api.get("/solicitantes?estado=APROBADO", api.admin()).<List<String>>json("$[*].correo")).contains(a).doesNotContain(b, c, d);
        assertThat(api.get("/solicitantes?estado=RECHAZADO", api.admin()).<List<String>>json("$[*].correo")).contains(b).doesNotContain(a, c, d);
        assertThat(api.get("/solicitantes?ignorado=1", api.admin()).cuerpo()).isEqualTo(api.get("/solicitantes", api.admin()).cuerpo());
        for (String filtro : List.of("pendiente", "OTRO", "CANCELADO")) assertThat(api.get("/solicitantes?estado=" + filtro, api.admin()).status()).isEqualTo(400);
    }

    @Test void aprobarHabilitaLoginYEnviaUnSoloCorreoConEmpresaCanonica() throws Exception {
        String correo = pendiente();
        assertThat(api.iniciarSesion(correo, Api.CLAVE).status()).isEqualTo(403);
        String hash = jdbc.queryForObject("select password_hash from usuario where correo=?", String.class, correo);
        var antes = api.get("/solicitantes/" + correo, api.admin());
        var revisada = Api.exigir(revisar(correo.toUpperCase(), "aprobar", api.admin()), "aprobar", 200);
        assertThat(revisada.texto("$.estadoSolicitante")).isEqualTo("APROBADO");
        for (String campo : List.of("correo", "nombre", "telefono", "fechaRegistro", "razonSocial", "rutEmpresa")) {
            assertThat(revisada.texto("$." + campo)).isEqualTo(antes.texto("$." + campo));
        }
        assertThat(jdbc.queryForObject("select password_hash from usuario where correo=?", String.class, correo)).isEqualTo(hash);
        assertThat(api.iniciarSesion(correo, Api.CLAVE).status()).isEqualTo(200);
        assertThat(CORREO.waitForIncomingEmail(5000, 1)).isTrue(); esperarEnvios();
        assertThat(CORREO.getReceivedMessages()).hasSize(1);
        var mensaje = CORREO.getReceivedMessages()[0];
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo(correo);
        assertThat(mensaje.getSubject()).isEqualTo("Belmonte SpA: tu cuenta fue aprobada");
        assertThat(((String) mensaje.getContent()).replace("\r\n", "\n").stripTrailing()).isEqualTo(
                "Hola Persona Pendiente: tu cuenta para " + antes.texto("$.razonSocial") + " fue aprobada. Ya puedes iniciar sesión con tu correo y contraseña.\n\nBelmonte SpA");
        for (String accion : List.of("aprobar", "rechazar")) assertThat(revisar(correo, accion, api.admin()).status()).isEqualTo(409);
        esperarEnvios(); assertThat(CORREO.getReceivedMessages()).hasSize(1); assertThat(estado(correo)).isEqualTo("APROBADO");
    }

    @Test void rechazarImpideLoginEsTerminalYEnviaCorreoDeRechazo() throws Exception {
        String correo = pendiente();
        var antes = api.get("/solicitantes/" + correo, api.admin());
        var respuesta = Api.exigir(revisar(correo, "rechazar", api.admin()), "rechazar", 200);
        assertThat(respuesta.texto("$.estadoSolicitante")).isEqualTo("RECHAZADO");
        var login = api.iniciarSesion(correo, Api.CLAVE);
        assertThat(login.status()).isEqualTo(403); assertThat(login.texto("$.message")).isEqualTo("Tu cuenta no fue aprobada. Contacta a Belmonte SpA.");
        assertThat(CORREO.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(CORREO.getReceivedMessages()[0].getSubject()).isEqualTo("Belmonte SpA: tu cuenta fue rechazada");
        assertThat(((String) CORREO.getReceivedMessages()[0].getContent()).replace("\r\n", "\n").stripTrailing()).isEqualTo(
                "Hola Persona Pendiente: tu cuenta para " + antes.texto("$.razonSocial") + " no fue aprobada. Si crees que es un error, contacta a Belmonte SpA.\n\nBelmonte SpA");
        assertThat(revisar(correo, "aprobar", api.admin()).status()).isEqualTo(409);
        assertThat(revisar(correo, "rechazar", api.admin()).status()).isEqualTo(409);
        esperarEnvios(); assertThat(CORREO.getReceivedMessages()).hasSize(1);
    }

    @Test void soloAdministradorRevisaYListaSinCambiosAnteErroresDeAcceso() {
        String correo = pendiente(); var ajena = api.solicitante(); var conductor = api.conductor();
        for (String accion : List.of("aprobar", "rechazar")) {
            assertThat(revisar(correo, accion, null).status()).isEqualTo(401);
            assertThat(revisar(correo, accion, ajena.token()).status()).isEqualTo(403);
            assertThat(revisar(correo, accion, conductor.token()).status()).isEqualTo(403);
            assertThat(revisar(correo, accion, jwt.generar("admin@belmontespa.cl", "ADMINISTRADOR", true)).status()).isEqualTo(403);
        }
        assertThat(api.get("/solicitantes?estado=PENDIENTE", ajena.token()).status()).isEqualTo(403);
        assertThat(api.get("/solicitantes?estado=PENDIENTE", conductor.token()).status()).isEqualTo(403);
        assertThat(estado(correo)).isEqualTo("PENDIENTE"); esperarEnvios(); assertThat(CORREO.getReceivedMessages()).isEmpty();
    }

    @Test void recursosInexistentesYTiposDeCuentaIncorrectosDan404YLaAprobadaInicialDa409() {
        var aprobada = api.solicitante(); var conductor = api.conductor();
        for (String accion : List.of("aprobar", "rechazar")) {
            assertThat(revisar("ausente@test.cl", accion, api.admin()).status()).isEqualTo(404);
            assertThat(revisar(conductor.correo(), accion, api.admin()).status()).isEqualTo(404);
            assertThat(revisar("admin@belmontespa.cl", accion, api.admin()).status()).isEqualTo(404);
            assertThat(revisar(aprobada.correo(), accion, api.admin()).status()).isEqualTo(409);
        }
        esperarEnvios(); assertThat(CORREO.getReceivedMessages()).isEmpty();
    }

    @Test void dosAdministradoresNoConfirmanDosDecisionesNiDuplicanCorreo() throws Exception {
        String correo = pendiente(), token = api.admin(); var inicio = new CountDownLatch(1);
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(() -> { inicio.await(); return revisar(correo, "aprobar", token).status(); });
            var b = hilos.submit(() -> { inicio.await(); return revisar(correo, "rechazar", token).status(); });
            inicio.countDown();
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(estado(correo)).isIn("APROBADO", "RECHAZADO");
        assertThat(CORREO.waitForIncomingEmail(5000, 1)).isTrue(); esperarEnvios(); assertThat(CORREO.getReceivedMessages()).hasSize(1);
    }

    @Test void rollbackNoRevisaLaCuentaNiEnviaCorreo() {
        String correo = pendiente();
        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            solicitantes.revisar(correo, true);
            jdbc.update("update usuario set nombre='Cambio revertido' where correo=?", correo);
            throw new IllegalStateException("Revertir toda la revisión");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(estado(correo)).isEqualTo("PENDIENTE"); esperarEnvios(); assertThat(CORREO.getReceivedMessages()).isEmpty();
    }
}
