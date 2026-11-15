package cl.belmontespa.myapplication.notificaciones

import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroTokenFcmTest {
    private fun sesion(rol: Rol = Rol.SOLICITANTE) = Sesion("jwt", "cuenta@test.cl", rol, Long.MAX_VALUE)
    private val autenticada = EstadoSesion.Autenticada(sesion())

    @Test fun registraElTokenSinCambiarloParaLosTresRoles() = runTest {
        val enviados = mutableListOf<String>()
        for (rol in Rol.entries) {
            val resultado = registrarTokenFcm(EstadoSesion.Autenticada(sesion(rol)), "token:AbC-123") {
                enviados += it
                Resultado.Exito(Unit)
            }
            assertEquals(ResultadoRegistroToken.LISTO, resultado)
        }
        assertEquals(listOf("token:AbC-123", "token:AbC-123", "token:AbC-123"), enviados)
    }

    @Test fun sinSesionOConTemporalConservaElTokenSinIntentarRegistrarlo() = runTest {
        for (estado in listOf(EstadoSesion.SinSesion, EstadoSesion.Temporal(sesion()))) {
            val resultado = registrarTokenFcm(estado, "token-dispositivo") {
                error("No debe llamar al backend sin una sesión normal")
            }
            assertEquals(ResultadoRegistroToken.LISTO, resultado)
        }
    }

    @Test fun unTokenAusenteOVacioNoSeEnvia() = runTest {
        for (token in listOf(null, "", "  ")) {
            assertEquals(ResultadoRegistroToken.LISTO, registrarTokenFcm(autenticada, token) {
                error("No debe enviar un token vacío")
            })
        }
    }

    @Test fun reintentaLosFallosTransitorios() = runTest {
        for (error in listOf(ErrorApp.SinConexion, ErrorApp.Inesperado)) {
            assertEquals(ResultadoRegistroToken.REINTENTAR, registrarTokenFcm(autenticada, "token") {
                Resultado.Fallo(error)
            })
        }
    }

    @Test fun noReintentaUnaSesionRechazadaONoVigenteNiUnTokenInvalido() = runTest {
        for (error in listOf(ErrorApp.SesionExpirada, ErrorApp.SinPermiso(),
            ErrorApp.Validacion(mapOf("tokenFcm" to "Inválido")), ErrorApp.NoEncontrado)) {
            assertEquals(ResultadoRegistroToken.LISTO, registrarTokenFcm(autenticada, "token") {
                Resultado.Fallo(error)
            })
        }
    }

    @Test fun laCancelacionSePropagaParaDetenerUnTrabajoReemplazado() = runTest {
        val error = runCatching {
            registrarTokenFcm(autenticada, "token") { throw CancellationException("Reemplazado") }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }

    private fun permiso(
        estado: EstadoSesion? = autenticada, local: Boolean = false, configurado: Boolean = true,
        sdk: Int = 33, concedido: Boolean = false, solicitado: Boolean = false,
    ) = debePedirPermisoNotificaciones(estado, local, configurado, sdk, concedido, solicitado)

    @Test fun pidePermisoDespuesDelLoginNormalEnAndroid13OSuperior() {
        for (rol in Rol.entries) {
            assertTrue(permiso(estado = EstadoSesion.Autenticada(sesion(rol))))
        }
        assertTrue(permiso(sdk = 36))
    }

    @Test fun noPidePermisoEnDemoSinFirebaseOSinUnaSesionNormal() {
        assertFalse(permiso(local = true))
        assertFalse(permiso(configurado = false))
        assertFalse(permiso(estado = null))
        assertFalse(permiso(estado = EstadoSesion.SinSesion))
        assertFalse(permiso(estado = EstadoSesion.Temporal(sesion())))
    }

    @Test fun noRepiteElPermisoDenegadoNiElYaConcedido() {
        assertFalse(permiso(solicitado = true))
        assertFalse(permiso(concedido = true))
    }

    @Test fun androidAnteriorAl13NoNecesitaPermisoEnTiempoDeEjecucion() {
        assertFalse(permiso(sdk = 26))
        assertFalse(permiso(sdk = 32))
    }
}
