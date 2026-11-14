package cl.belmontespa.myapplication.e2e

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.database.DatosDemo
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Flujos principales de los 4 actores contra el backend real, con `database/demo.sql` recién cargado. Solo
 * corre con `-e e2e true` y en un emulador (corta la red con `svc`). Las pruebas consumen los datos de la
 * demostración: hay que recargar `demo.sql` antes de cada corrida y correr dentro de los 90 minutos siguientes.
 *
 *   adb shell am instrument -w -e e2e true -e class cl.belmontespa.myapplication.e2e.FlujosE2ETest \
 *     cl.belmontespa.myapplication.test/androidx.test.runner.AndroidJUnitRunner
 */
class FlujosE2ETest {

    @get:Rule(order = 0)
    val entorno = EntornoE2E()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val api get() = entorno.api

    private companion object {
        const val RUTA_TR_1005 = "Aeropuerto Arturo Merino Benítez → Av. Apoquindo 5400"
    }

    @Test
    fun solicitanteCreaUnaSolicitudYLaCancela() {
        val nombre = "Pasajero E2E " + System.currentTimeMillis() % 1_000_000
        val (origen, destino) = DatosDemo.lugares.take(2)
        entorno.exigirBackend()

        entrar("crios@andesconsult.cl")
        esperarTexto(texto(R.string.accion_nueva_solicitud))
        compose.onNodeWithText(texto(R.string.accion_nueva_solicitud)).performClick()
        esperarTexto(texto(R.string.campo_origen))
        compose.onNodeWithText(texto(R.string.campo_origen)).performScrollTo().performClick()
        compose.onNodeWithText("${origen.direccion} · ${origen.comuna}").performClick()
        compose.onNodeWithText(texto(R.string.campo_destino)).performScrollTo().performClick()
        compose.onNodeWithText("${destino.direccion} · ${destino.comuna}").performClick()
        compose.onNodeWithText(texto(R.string.campo_nombre)).performScrollTo().performTextReplacement(nombre)
        compose.onNodeWithText(texto(R.string.campo_tipo_documento)).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.tipo_documento_pasaporte)).performClick()
        compose.onNodeWithText(texto(R.string.campo_documento)).performScrollTo().performTextReplacement("E2E" + nombre.takeLast(6))
        compose.onNodeWithText(texto(R.string.accion_agregar_pasajero)).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.accion_guardar)).performScrollTo().performClick()

        esperarTexto(texto(R.string.accion_cancelar_solicitud))
        assertEquals("PENDIENTE", viajeConPasajero(nombre).getString("estado"))

        compose.onNodeWithText(texto(R.string.accion_cancelar_solicitud)).performScrollTo().performClick()
        esperarTexto(texto(R.string.accion_confirmar))
        compose.onNodeWithText(texto(R.string.accion_confirmar)).performScrollTo().performClick()
        esperarTexto(texto(R.string.aviso_solicitud_cancelada))
        assertEquals("CANCELADO", viajeConPasajero(nombre).getString("estado"))
    }

    @Test
    fun administradorAsignaUnViajePendiente() {
        entorno.exigirPendiente("TR-1009")
        entrar("recepcion@belmontespa.cl")
        irAlViajePendiente("TR-1009")
        compose.onNode(hasText(texto(R.string.accion_asignar)) and hasAnySibling(hasText("TR-1009"))).performScrollTo().performClick()
        esperarTexto("Paula Muñoz")
        compose.onNodeWithText("Paula Muñoz").performClick()
        esperarTexto("KXJR42")
        compose.onNodeWithText("KXJR42").performClick()
        compose.onNodeWithText(texto(R.string.accion_confirmar_asignacion)).performScrollTo().performClick()

        val viaje = esperarViaje("TR-1009") { it.getString("estado") == "ASIGNADO" }
        assertEquals("pmunoz@belmontespa.cl", viaje.getString("correoConductor"))
        assertEquals("KXJR42", viaje.getString("patenteMovil"))
    }

    @Test
    fun administradorCancelaUnViajeConMotivo() {
        val motivo = "Cancelado por la prueba E2E"
        entorno.exigirPendiente("TR-1010")
        entrar("recepcion@belmontespa.cl")
        irAlViajePendiente("TR-1010")
        compose.onNodeWithText("TR-1010").performTouchInput { click(Offset(12f, 12f)) }
        esperarTexto(texto(R.string.t06_cancelar_viaje))
        compose.onNodeWithText(texto(R.string.t06_cancelar_viaje)).performScrollTo().performClick()
        esperarTexto(texto(R.string.campo_motivo))
        compose.onNodeWithText(texto(R.string.campo_motivo)).performTextReplacement(motivo)
        compose.onNodeWithText(texto(R.string.accion_confirmar)).performScrollTo().performClick()

        val viaje = esperarViaje("TR-1010") { it.getString("estado") == "CANCELADO" }
        assertEquals(motivo, viaje.getString("motivo"))
    }

    @Test
    fun pasajeroAbordaYElConductorIniciaYFinalizaSinConexion() {
        entorno.exigirEnVentanaDeInicio("TR-1005")
        esperarTexto(texto(R.string.login_abordar_viaje))
        compose.onNodeWithText(texto(R.string.login_abordar_viaje)).performClick()
        esperarTexto(texto(R.string.abordaje_ingresar_codigo))
        compose.onNodeWithText(texto(R.string.abordaje_ingresar_codigo)).performClick()
        esperarTexto(texto(R.string.campo_patente))
        compose.onNodeWithText(texto(R.string.campo_patente)).performTextReplacement("KXJR42")
        compose.onNodeWithText(texto(R.string.campo_codigo)).performTextReplacement("7F3A9C21B04E58D6")
        compose.onNodeWithText(texto(R.string.accion_continuar)).performClick()
        esperarTexto(texto(R.string.abordaje_confirmar))
        compose.onNodeWithText(texto(R.string.tipo_documento_pasaporte)).performClick()
        compose.onNodeWithText(texto(R.string.campo_documento)).performTextReplacement("P4471023")
        compose.onNode(hasText(texto(R.string.abordaje_confirmar)) and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Abordaje registrado", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, abordados(api.viaje("TR-1005")))
        compose.onNodeWithText(texto(R.string.abordaje_volver_inicio)).performClick()

        entrar("ireyes@belmontespa.cl")
        esperarTexto(RUTA_TR_1005)
        compose.onNodeWithText(RUTA_TR_1005).performClick()
        esperarTexto(texto(R.string.arqueo_faltan, 2, 3, "Marcela Soto"))
        compose.onNodeWithText(texto(R.string.accion_iniciar)).performScrollTo().performClick()
        esperarTexto(texto(R.string.confirmar_inicio_titulo, "TR-1005"))
        compose.onNodeWithText(texto(R.string.accion_iniciar)).performClick()
        esperarViaje("TR-1005") { it.getString("estado") == "EN_CURSO" }
        val finalizar = hasText(texto(R.string.accion_finalizar)) and isEnabled()
        compose.waitUntil(30_000) { compose.onAllNodes(finalizar).fetchSemanticsNodes().isNotEmpty() }

        entorno.cortarRed()
        compose.onNode(finalizar).performScrollTo().performClick()
        esperarTexto(texto(R.string.aviso_finalizar))
        compose.onNodeWithText(texto(R.string.accion_confirmar)).performClick()
        val pendiente = hasContentDescription(texto(R.string.chip_descripcion, texto(R.string.etiqueta_pendiente_sincronizar)))
        compose.waitUntil(30_000) { compose.onAllNodes(pendiente).fetchSemanticsNodes().isNotEmpty() }
        esperarTexto(texto(R.string.banner_sin_conexion_conductor))

        entorno.restaurarRed()
        compose.waitUntil(60_000) { compose.onAllNodes(pendiente).fetchSemanticsNodes().isEmpty() }
        esperarViaje("TR-1005") { it.getString("estado") == "FINALIZADO" }
    }

    @Test
    fun lasCuentasPorAprobarOInactivasNoEntran() {
        entorno.exigirBackend()
        for ((correo, error) in listOf(
            "rperez@andesconsult.cl" to R.string.error_cuenta_pendiente,
            "msoto@belmontespa.cl" to R.string.error_credenciales,
        )) {
            entrar(correo)
            esperarTexto(texto(error))
            compose.onAllNodesWithText(texto(R.string.nav_mis_viajes)).assertCountEquals(0)
            compose.onAllNodesWithText(texto(R.string.nav_solicitudes)).assertCountEquals(0)
        }
    }

    private fun texto(@StringRes id: Int, vararg argumentos: Any): String = compose.activity.getString(id, *argumentos)

    private fun esperarTexto(texto: String, milisegundos: Long = 30_000) {
        compose.waitUntil(milisegundos) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun abordados(viaje: JSONObject): Int {
        val pasajeros = viaje.getJSONArray("pasajeros")
        return (0 until pasajeros.length()).count { !pasajeros.getJSONObject(it).isNull("fhAbordaje") }
    }

    private fun irAlViajePendiente(folio: String) {
        esperarTexto(texto(R.string.filtro_pendientes))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(folio))
    }

    private fun esperarViaje(folio: String, condicion: (JSONObject) -> Boolean): JSONObject {
        val limite = System.currentTimeMillis() + 20_000
        var viaje = api.viaje(folio)
        while (!condicion(viaje) && System.currentTimeMillis() < limite) {
            Thread.sleep(500)
            viaje = api.viaje(folio)
        }
        assertTrue("$folio no llegó al estado esperado: ${viaje.getString("estado")}", condicion(viaje))
        return viaje
    }

    private fun viajeConPasajero(nombre: String): JSONObject {
        val viaje = api.viajes().find { viaje ->
            val pasajeros = viaje.getJSONArray("pasajeros")
            (0 until pasajeros.length()).any { pasajeros.getJSONObject(it).getString("nombre") == nombre }
        }
        assertNotNull("El servidor no tiene un viaje con el pasajero $nombre", viaje)
        return viaje!!
    }

    private fun entrar(correo: String) {
        esperarTexto(texto(R.string.login_iniciar_sesion))
        compose.onNodeWithText(texto(R.string.login_correo)).performTextReplacement(correo)
        compose.onNodeWithText(texto(R.string.login_password)).performTextReplacement(CLAVE_DEMO)
        compose.onNodeWithText(texto(R.string.login_iniciar_sesion)).performClick()
    }
}
