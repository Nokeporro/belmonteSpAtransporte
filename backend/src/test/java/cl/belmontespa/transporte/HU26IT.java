package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.belmontespa.transporte.dto.request.AsignarViajeDTORequest;
import cl.belmontespa.transporte.service.ConductorService;
import cl.belmontespa.transporte.service.MovilService;
import cl.belmontespa.transporte.service.ViajeService;
import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.IntegrationTest;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class HU26IT extends IntegrationTest {
    @Autowired ConductorService conductores;
    @Autowired MovilService moviles;
    @Autowired ViajeService viajes;
    @Autowired PlatformTransactionManager transacciones;

    private String conductor(Api.Flota f) { return "/conductores/" + f.conductor().correo() + "/activo"; }
    private String movil(Api.Flota f) { return "/moviles/" + f.patente() + "/activo"; }
    private Api.Resp cambiar(String ruta, boolean activo) {
        return api.post(ruta, api.admin(), "{\"activo\":" + activo + "}");
    }

    @Test void desactivarReactivarEsIdempotenteYConservaDatosHabilitacionesYQr() {
        var f = api.flota(4);
        var originalCon = api.get("/conductores/" + f.conductor().correo(), api.admin());
        var originalMovil = api.get("/moviles/" + f.patente(), api.admin());
        for (boolean activo : List.of(false, false, true, true)) {
            for (String ruta : List.of(conductor(f), movil(f))) {
                var r = Api.exigir(cambiar(ruta, activo), "cambiar activo", 200);
                assertThat(r.<Boolean>json("$.activo")).isEqualTo(activo);
            }
            assertThat(api.get("/conductores/" + f.conductor().correo(), api.admin()).texto("$.nombre"))
                    .isEqualTo(originalCon.texto("$.nombre"));
            var m = api.get("/moviles/" + f.patente(), api.admin());
            assertThat(m.texto("$.codigoAbordaje")).isEqualTo(originalMovil.texto("$.codigoAbordaje"));
            assertThat(m.entero("$.capacidad")).isEqualTo(4);
            assertThat(api.get("/habilitaciones-movil-conductor/" + f.patente() + "/" + f.conductor().correo(), api.admin()).status()).isEqualTo(200);
        }
    }

    @Test void inactivoNoIniciaSesionNiSeAsignaYReactivarRestauraAmbasOperaciones() {
        var f = api.flota(4); var sol = api.solicitante(); var folio = api.viaje(sol); api.conPasajero(sol, folio);
        Api.exigir(cambiar(conductor(f), false), "desactivar", 200);
        var login = api.iniciarSesion(f.conductor().correo(), Api.CLAVE);
        assertThat(login.status()).isEqualTo(401);
        assertThat(login.texto("$.message")).isEqualTo(api.iniciarSesion(f.conductor().correo(), "Incorrecta123").texto("$.message"));
        assertThat(api.asignar(folio, f.conductor().correo(), f.patente()).status()).isEqualTo(409);
        Api.exigir(cambiar(conductor(f), true), "reactivar", 200);
        assertThat(api.iniciarSesion(f.conductor().correo(), Api.CLAVE).status()).isEqualTo(200);
        Api.exigir(cambiar(movil(f), false), "desactivar móvil", 200);
        assertThat(api.asignar(folio, f.conductor().correo(), f.patente()).status()).isEqualTo(409);
        Api.exigir(cambiar(movil(f), true), "reactivar móvil", 200);
        api.asignarOk(folio, f.conductor().correo(), f.patente());
    }

    @Test void asignadoYEnCursoImpidenDesactivarYFinalizadoPermiteLaBaja() {
        var f = api.flota(4); var folio = api.viajeAsignado(api.solicitante(), f);
        for (String estado : List.of("ASIGNADO", "EN_CURSO")) {
            if (estado.equals("EN_CURSO")) api.iniciarOk(folio, f.conductor());
            for (String ruta : List.of(conductor(f), movil(f))) {
                assertThat(cambiar(ruta, false).status()).isEqualTo(409);
                assertThat(cambiar(ruta, true).status()).isEqualTo(200);
            }
        }
        api.cambiarEstadoOk(folio, f.conductor(), "FINALIZADO");
        assertThat(cambiar(conductor(f), false).status()).isEqualTo(200);
        assertThat(cambiar(movil(f), false).status()).isEqualTo(200);
    }

    @Test void canceladoNoBloqueaYElCorreoEsInsensibleAMayusculas() {
        var sol = api.solicitante(); var f = api.flota(4); var folio = api.viajeAsignado(sol, f);
        api.cancelarOk(folio, sol);
        assertThat(cambiar(conductor(f).replace(f.conductor().correo(), f.conductor().correo().toUpperCase(java.util.Locale.ROOT)), false).status()).isEqualTo(200);
        assertThat(cambiar(movil(f), false).status()).isEqualTo(200);
    }

    @Test void soloAdminYCuerpoObligatorioSinModificarAnteError() {
        var f = api.flota(4); var sol = api.solicitante();
        for (String ruta : List.of(conductor(f), movil(f))) {
            assertThat(api.post(ruta, null, "{\"activo\":false}").status()).isEqualTo(401);
            for (String token : List.of(sol.token(), f.conductor().token()))
                assertThat(api.post(ruta, token, "{\"activo\":false}").status()).isEqualTo(403);
            for (String cuerpo : List.of("{}", "{\"activo\":null}", "{\"activo\":{}}", "[]", "{", ""))
                assertThat(api.post(ruta, api.admin(), cuerpo).status()).as(cuerpo).isEqualTo(400);
            assertThat(cambiar(ruta, true).<Boolean>json("$.activo")).isTrue();
        }
        for (String ruta : List.of("/conductores/ausente@test.cl/activo", "/conductores/" + sol.correo() + "/activo",
                "/conductores/" + Api.ADMIN_CORREO + "/activo", "/moviles/ZZ0000/activo"))
            assertThat(cambiar(ruta, false).status()).isEqualTo(404);
    }

    @Test void desactivacionQueObtieneElBloqueoAntesImpideLaAsignacionConcurrente() throws Exception {
        for (boolean conductor : List.of(true, false)) {
            var f = api.flota(4); var sol = api.solicitante(); var folio = api.viaje(sol); api.conPasajero(sol, folio);
            String token = api.admin();
            try (var hilos = Executors.newSingleThreadExecutor()) {
                new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
                    if (conductor) conductores.cambiarActivo(f.conductor().correo(), false);
                    else moviles.cambiarActivo(f.patente(), false);
                    var inicio = new CountDownLatch(1);
                    var asignar = hilos.submit(() -> { inicio.countDown(); return api.post("/viajes/" + folio + "/asignar", token,
                            "{\"correoConductor\":\"" + f.conductor().correo() + "\",\"patente\":\"" + f.patente() + "\"}").status(); });
                    try {
                        assertThat(inicio.await(5, TimeUnit.SECONDS)).isTrue();
                        assertThatThrownBy(() -> asignar.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    pendientes.add(asignar);
                });
                assertThat(pendientes.removeFirst().get(30, TimeUnit.SECONDS)).isEqualTo(409);
            }
            assertThat(api.get("/viajes/" + folio, token).texto("$.estado")).isEqualTo("PENDIENTE");
        }
    }
    private final java.util.LinkedList<Future<Integer>> pendientes = new java.util.LinkedList<>();

    @Test void asignacionQueConfirmaAntesHaceQueLaDesactivacionConcurrenteResponda409() throws Exception {
        for (boolean conductor : List.of(true, false)) {
            var f = api.flota(4); var sol = api.solicitante(); var folio = api.viaje(sol); api.conPasajero(sol, folio);
            String token = api.admin(), ruta = conductor ? conductor(f) : movil(f);
            try (var hilos = Executors.newSingleThreadExecutor()) {
                new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
                    viajes.asignar(folio, new AsignarViajeDTORequest(f.conductor().correo(), f.patente()));
                    var inicio = new CountDownLatch(1);
                    var desactivar = hilos.submit(() -> { inicio.countDown(); return api.post(ruta, token, "{\"activo\":false}").status(); });
                    try {
                        assertThat(inicio.await(5, TimeUnit.SECONDS)).isTrue();
                        assertThatThrownBy(() -> desactivar.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    pendientes.add(desactivar);
                });
                assertThat(pendientes.removeFirst().get(30, TimeUnit.SECONDS)).isEqualTo(409);
            }
        }
    }
}
