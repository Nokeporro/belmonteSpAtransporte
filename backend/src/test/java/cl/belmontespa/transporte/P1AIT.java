package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import cl.belmontespa.transporte.config.ClockConfig;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class P1AIT extends IntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    private LocalDateTime ahora() { return LocalDateTime.now(ClockConfig.ZONA); }

    @Test void conductorInactivoTieneElMismo401QueUnaClaveIncorrecta() {
        var conductor = api.conductor();
        var error = api.iniciarSesion(conductor.correo(), "ClaveIncorrecta1");
        jdbc.update("UPDATE conductor c JOIN usuario u ON u.id_usuario=c.id_usuario SET c.activo=false WHERE u.correo=?", conductor.correo());
        var inactivo = api.iniciarSesion(conductor.correo(), Api.CLAVE);
        assertThat(inactivo.status()).isEqualTo(401);
        assertThat(inactivo.texto("$.message")).isEqualTo(error.texto("$.message"));
    }

    @Test void laTemporalSoloPermitePerfilPropioCambioDeClaveYLogout() {
        var cuenta = api.solicitante();
        jdbc.update("UPDATE usuario SET password_temporal_hash=?, password_temporal_expira=? WHERE correo=?",
                encoder.encode("Temporal12345"), ahora().plusMinutes(30), cuenta.correo());
        var login = Api.exigir(api.iniciarSesion(cuenta.correo(), "Temporal12345"), "login temporal", 200);
        assertThat(login.<Boolean>json("$.cambioPasswordRequerido")).isTrue();
        String token = login.texto("$.accessToken");
        assertThat(api.get("/usuarios/" + cuenta.correo(), token).status()).isEqualTo(200);
        var bloqueado = api.get("/viajes", token);
        assertThat(bloqueado.status()).isEqualTo(403);
        assertThat(bloqueado.texto("$.message")).isEqualTo("Debes cambiar la contraseña");
        assertThat(api.get("/usuarios/ajena@test.cl", token).status()).isEqualTo(403);
        String cuerpo = """
                {"nombre":"Solicitante","correo":"%s","password":"NuevaClave123","passwordActual":"Temporal12345"}
                """.formatted(cuenta.correo());
        assertThat(api.put("/usuarios/" + cuenta.correo(), token, cuerpo).status()).isEqualTo(200);
        assertThat(api.iniciarSesion(cuenta.correo(), "Temporal12345").status()).isEqualTo(401);
        assertThat(api.iniciarSesion(cuenta.correo(), "NuevaClave123").<Boolean>json("$.cambioPasswordRequerido")).isFalse();
        assertThat(api.post("/auth/logout", token, null).status()).isEqualTo(204);
    }

    @Test void validaLaAgendaYTruncaAlMinuto() {
        var cuenta = api.solicitante();
        assertThat(api.post("/viajes", cuenta.token(), api.cuerpoViaje(ahora().plusHours(1), "Providencia")).status()).isEqualTo(400);
        assertThat(api.post("/viajes", cuenta.token(), api.cuerpoViaje(ahora().plusDays(91), "Providencia")).status()).isEqualTo(400);
        LocalDateTime fecha = ahora().plusHours(3).withSecond(37).withNano(0);
        var creado = Api.exigir(api.post("/viajes", cuenta.token(), api.cuerpoViaje(fecha, "Providencia")), "crear solicitud", 201);
        assertThat(LocalDateTime.parse(creado.texto("$.fhAgenda"))).isEqualTo(fecha.withSecond(0));
    }

    @Test void lasComunasSeCotejanNormalizadasYLaRegionNoLasRechaza() {
        var cuenta = api.solicitante();
        String cuerpo = api.cuerpoViaje(ahora().plusHours(3), "  PROVIDÉNCIA  ").replace("Metropolitana de Santiago", "Región de Valparaíso");
        var creado = Api.exigir(api.post("/viajes", cuenta.token(), cuerpo), "cotejar comuna", 201);
        assertThat(creado.texto("$.origen.comuna")).isEqualTo("Providencia");
        assertThat(creado.texto("$.origen.region")).isEqualTo("Metropolitana de Santiago");
        var error = api.post("/viajes", cuenta.token(), api.cuerpoViaje(ahora().plusHours(3), "Narnia"));
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.texto("$.message")).isEqualTo("origen.comuna: No existe en el catálogo.");
    }

    @Test void validaTipoDocumentoYNoRevelaElNombreDeUnoExistente() {
        var cuenta = api.solicitante();
        String pasaporte = "P" + System.nanoTime();
        var primera = Api.exigir(api.post("/pasajeros", cuenta.token(), """
                {"tipoDocumento":"PASAPORTE","dni":"%s","nombre":"Pasajero privado"}
                """.formatted(pasaporte)), "crear pasajero", 201);
        var repetida = Api.exigir(api.post("/pasajeros", cuenta.token(), """
                {"tipoDocumento":"PASAPORTE","dni":"%s","nombre":"Otro nombre"}
                """.formatted(pasaporte.toLowerCase())), "repetir pasajero", 200);
        assertThat(repetida.texto("$.dniHash")).isEqualTo(primera.texto("$.dniHash"));
        assertThat(repetida.texto("$.nombre")).isNull();
        assertThat(api.pasajero(cuenta.token(), "12.345.678-4", "Pasajero").status()).isEqualTo(400);
        assertThat(api.post("/pasajeros", cuenta.token(), "{\"dni\":\"12345678-5\",\"nombre\":\"Pasajero\"}").status()).isEqualTo(400);
        String folio = api.viaje(cuenta);
        assertThat(api.agregarPasajero(cuenta.token(), folio, primera.texto("$.dniHash")).status()).isEqualTo(201);
        assertThat(api.agregarPasajero(cuenta.token(), folio, primera.texto("$.dniHash")).status()).isEqualTo(200);
    }

    @Test void historialYFiltrosSoloIncluyenLasSolicitudesPropias() {
        var cuenta = api.solicitante();
        String folio = api.viaje(cuenta);
        String ajeno = api.viaje(api.solicitante());
        var lista = Api.exigir(api.get("/viajes?estado=PENDIENTE&fecha=" + ahora().plusDays(3).toLocalDate(), cuenta.token()), "historial propio", 200);
        assertThat(lista.<List<String>>json("$[*].folio")).containsExactly(folio).doesNotContain(ajeno);
        assertThat(api.get("/viajes?estado=INVENTADO", cuenta.token()).status()).isEqualTo(400);
        assertThat(api.get("/viajes?fecha=ayer", cuenta.token()).status()).isEqualTo(400);
    }

    @Test void catalogosExigenSesionYContienenLasComunas() {
        assertThat(api.get("/catalogos", null).status()).isEqualTo(401);
        var catalogos = Api.exigir(api.get("/catalogos", api.solicitante().token()), "catálogos", 200);
        assertThat(catalogos.<List<String>>json("$.regiones[*].comunas[*]")).contains("Providencia", "Las Condes");
        assertThat(catalogos.<List<String>>json("$.mediosPago")).contains("TRANSFERENCIA");
    }
}
