package cl.belmontespa.myapplication.view.auth

import androidx.activity.compose.setContent
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.OperacionPendienteEntity
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.navigation.DestinoPantalla
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CierreConPendientesScreenTest {

    @get:Rule(order = 0)
    val entorno = EntornoPruebas()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val cerrados = mutableListOf<String>()
    private val correo = "ireyes@belmontespa.cl"

    private fun texto(id: Int) = compose.activity.getString(id)

    private fun esperar(id: Int) =
        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarCierre() = compose.waitUntil(15_000) { cerrados.isNotEmpty() }

    private fun boton(id: Int) = compose.onNode(hasText(texto(id)) and hasClickAction())

    private fun abrirPerfil(conPendientes: Boolean) {
        runBlocking {
            entorno.sesiones.guardar(Sesion("prueba", correo, Rol.CONDUCTOR, System.currentTimeMillis() + 3_600_000))
            if (conPendientes) {
                entorno.db.operacionPendienteDao().insertar(OperacionPendienteEntity("op-1", correo, "TR-1042", "EN_CURSO", 1, "PENDIENTE", null))
            }
        }
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                BelmonteTheme {
                    DestinoPantalla(PantallaDemo.PERFIL_CONDUCTOR, onNavegar = { _, _ -> }, onVolver = {}, onCerrarSesion = { cerrados += "cerrar" })
                }
            }
        }
        esperar(R.string.accion_cerrar_sesion)
    }

    @Test
    fun sinCambiosPendientesCierraLaSesionDeInmediato() {
        abrirPerfil(conPendientes = false)

        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()

        esperarCierre()
        assertTrue(compose.onAllNodesWithText(texto(R.string.aviso_cierre_con_pendientes)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun conCambiosPendientesPideSincronizarODescartarAntesDeCerrar() {
        abrirPerfil(conPendientes = true)

        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()
        esperar(R.string.aviso_cierre_con_pendientes)

        assertTrue(cerrados.isEmpty())
        boton(R.string.accion_sincronizar).assertExists()
        boton(R.string.accion_descartar).assertExists()
        boton(R.string.accion_cancelar).assertExists()
    }

    @Test
    fun descartarCierraLaSesion() {
        abrirPerfil(conPendientes = true)
        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()
        esperar(R.string.aviso_cierre_con_pendientes)

        boton(R.string.accion_descartar).performClick()

        esperarCierre()
        assertEquals(1, cerrados.size)
    }

    @Test
    fun sincronizarCierraLaSesionCuandoYaNoQuedaNada() {
        abrirPerfil(conPendientes = true)
        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()
        esperar(R.string.aviso_cierre_con_pendientes)

        boton(R.string.accion_sincronizar).performClick()

        esperarCierre()
    }

    @Test
    fun cancelarVuelveAlPerfilSinCerrar() {
        abrirPerfil(conPendientes = true)
        compose.onNodeWithText(texto(R.string.accion_cerrar_sesion)).performClick()
        esperar(R.string.aviso_cierre_con_pendientes)

        boton(R.string.accion_cancelar).performClick()

        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(R.string.aviso_cierre_con_pendientes)).fetchSemanticsNodes().isEmpty() }
        assertTrue(cerrados.isEmpty())
    }
}
