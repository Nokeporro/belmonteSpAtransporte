package cl.belmontespa.myapplication.ui.navigation

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.local.repository.SoporteLocal
import cl.belmontespa.myapplication.data.local.repository.OperacionesViajeLocal
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.auth.LoginContenido
import cl.belmontespa.myapplication.ui.auth.LoginUiState
import cl.belmontespa.myapplication.ui.components.PantallaCargando
import cl.belmontespa.myapplication.ui.pantallas.BorradorAbordaje
import cl.belmontespa.myapplication.ui.pantallas.PantallaT06
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import dagger.hilt.android.testing.*
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Renderiza todas las variantes sobre Room en memoria, sin navegar al backend.
@HiltAndroidTest
class RecorridoT06Test {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    @Inject lateinit var operaciones: OperacionesViajeLocal
    @Inject lateinit var authLocal: cl.belmontespa.myapplication.data.local.repository.AuthLocalRepository
    @Inject lateinit var viajes: cl.belmontespa.myapplication.data.repository.ViajeRepository
    @Inject lateinit var borrador: BorradorAbordaje
    @Before fun preparar() { hilt.inject() }

    @Test fun las46VariantesCarganConDatosDemoSinErrores() {
        var seleccion by mutableStateOf(PantallaDemo.REGISTRO to "")
        val pagos = runBlocking { local.db.pagoDao().observarTodos().first() }
        // Prepara una cancelación real para verificar también la pantalla de resultado.
        runBlocking {
            sesiones.guardar(sesion(Rol.SOLICITANTE))
            assertTrue(operaciones.cancelar("TR-1051", null, null) is Resultado.Exito)
        }
        borrador.codigo = CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6")
        borrador.hora = runBlocking { local.db.viajePasajeroDao().obtenerDelViaje("TR-1042").first { it.fhAbordaje != null }.fhAbordaje?.let(java.time.LocalDateTime::parse) }
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                BelmonteTheme {
                    val (pantalla, id) = seleccion
                    when (pantalla) {
                        PantallaDemo.INICIO -> PantallaCargando(texto = compose.activity.getString(R.string.inicio_comprobando_sesion))
                        PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO -> LoginContenido(LoginUiState(), pantalla == PantallaDemo.LOGIN_DEMO, {}, {}, {}, {}, {}, {})
                        else -> PantallaT06(pantalla, id, { _, _ -> }, {}, {})
                    }
                }
            }
        }
        var recorridas = 0
        for (pantalla in PantallaDemo.entries) {
            val rol = pantalla.rol ?: if (pantalla in listOf(PantallaDemo.DETALLE_PAGO, PantallaDemo.NOTIFICACION, PantallaDemo.CAMBIAR_PASSWORD, PantallaDemo.CAMBIO_OBLIGATORIO)) Rol.ADMINISTRADOR else null
            runBlocking {
                if (pantalla == PantallaDemo.CAMBIO_OBLIGATORIO) sesiones.guardarTemporal(sesion(Rol.ADMINISTRADOR))
                else if (rol != null) sesiones.guardar(sesion(rol)) else sesiones.borrar()
            }
            val id = when(pantalla) {
                PantallaDemo.DETALLE_CONDUCTOR -> "ireyes@belmontespa.cl"
                PantallaDemo.DETALLE_VEHICULO -> "KXJR42"
                PantallaDemo.DETALLE_PAGO -> pagos.first().hash
                PantallaDemo.SOLICITUD_SIN_PASAJEROS, PantallaDemo.SIN_PASAJEROS_ADMIN -> "TR-1055"
                PantallaDemo.SOLICITUD_CANCELADA -> "TR-1051"
                PantallaDemo.ANTES_VENTANA -> "TR-1049"
                else -> "TR-1042"
            }
            compose.runOnIdle { seleccion = pantalla to id }
            if (pantalla.ordinal > 2) {
                compose.waitUntil(15_000) { compose.onAllNodesWithTag("t06_contenido").fetchSemanticsNodes().isNotEmpty() }
                compose.onAllNodesWithTag("t06_error").assertCountEquals(0)
            } else compose.waitForIdle()
            // Capturas de ejemplos para revisar el resultado fuera del emulador.
            if (pantalla in listOf(PantallaDemo.LOGIN_DEMO, PantallaDemo.MIS_SOLICITUDES, PantallaDemo.NUEVA_SOLICITUD, PantallaDemo.DETALLE_ADMIN, PantallaDemo.DETALLE_VEHICULO, PantallaDemo.PERFIL_CONDUCTOR)) {
                val directorio = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "t06-capturas").apply { mkdirs() }
                File(directorio, "${pantalla.name}.png").outputStream().use { salida -> compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, salida) }
            }
            recorridas++
        }
        assertEquals(46, recorridas)
    }
    @Test fun elFormularioPermiteLlegarAlBotonConTextoAlDoscientosPorCiento() {
        runBlocking { sesiones.guardar(sesion(Rol.SOLICITANTE)) }
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                val densidad = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(densidad.density, 2f)) {
                    BelmonteTheme { PantallaT06(PantallaDemo.NUEVA_SOLICITUD, onNavegar = { _, _ -> }, onVolver = {}, onCerrarSesion = {}) }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("t06_contenido").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.accion_guardar)).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("t06_error").assertCountEquals(0)
    }

    @Test fun unaCuentaSinHistorialPuedeCrearSuPrimeraSolicitudDesdeLaInterfaz() {
        val correo = "nueva@empresa-demo.cl"
        runBlocking {
            val registro = authLocal.registrarSolicitante(RegistroSolicitante("Cuenta nueva", correo, null,
                "Belmonte2026", "Empresa de demostración", "78123456-7"))
            assertTrue(registro is Resultado.Exito)
            assertEquals(EstadoSolicitante.APROBADO, (registro as Resultado.Exito).valor.estado)
            sesiones.guardar(Sesion("prueba", correo, Rol.SOLICITANTE, System.currentTimeMillis() + 3_600_000))
            assertTrue(viajes.observarViajes(FiltroViajes()).first().isEmpty())
        }
        var destino: Pair<PantallaDemo, String>? = null
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                BelmonteTheme { PantallaT06(PantallaDemo.NUEVA_SOLICITUD,
                    onNavegar = { p, id -> destino = p to id }, onVolver = {}, onCerrarSesion = {}) }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("t06_contenido").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.campo_origen)).performScrollTo().performClick()
        compose.onNodeWithText("Aeropuerto Arturo Merino Benítez").performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.campo_destino)).performScrollTo().performClick()
        compose.onNodeWithText("Av. Apoquindo 5400").performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.campo_nombre)).performScrollTo().performTextInput("Invitado")
        compose.onNodeWithText(compose.activity.getString(R.string.campo_tipo_documento)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.tipo_documento_pasaporte)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.campo_documento)).performScrollTo().performTextInput("DEMONUEVO")
        compose.onNodeWithText(compose.activity.getString(R.string.accion_agregar_pasajero)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.accion_quitar)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.accion_guardar)).performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15_000) { destino != null || compose.onAllNodesWithTag("t06_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("t06_error").assertCountEquals(0)
        assertEquals(PantallaDemo.DETALLE_SOLICITUD, destino!!.first)
        runBlocking {
            val guardado = viajes.observarViaje(requireNotNull(destino).second).first()!!
            assertEquals(correo, guardado.correoSolicitante)
            assertEquals("Invitado", guardado.pasajeros.single().nombre)
        }
    }

    private fun sesion(rol: Rol) = Sesion("prueba", when(rol) {
        Rol.SOLICITANTE -> "crios@andesconsult.cl"
        Rol.ADMINISTRADOR -> "recepcion@belmontespa.cl"
        Rol.CONDUCTOR -> "ireyes@belmontespa.cl"
    }, rol, System.currentTimeMillis() + 3_600_000)
}