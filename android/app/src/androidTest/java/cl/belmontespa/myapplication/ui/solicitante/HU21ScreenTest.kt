package cl.belmontespa.myapplication.ui.solicitante

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.local.entity.PagoEntity
import cl.belmontespa.myapplication.data.local.repository.SoporteLocal
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.AuthFalso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.navigation.DestinoPantalla
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Formatos
import dagger.hilt.android.testing.*
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.*

@HiltAndroidTest
class HU21ScreenTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    private val folio = "TR-3021"
    private val hash = "hu21-pago"
    @Before fun preparar() {
        hilt.inject()
        runBlocking { sesiones.guardar(Sesion("prueba", "crios@andesconsult.cl", Rol.SOLICITANTE, local.clock.millis() + 3600000)) }
    }
    private fun texto(id: Int) = compose.activity.getString(id)
    private fun pago(estado: String) = PagoEntity(hash, folio, 50001, estado, "Banco de prueba", 12,
        "12345678-5", "TRANSFERENCIA", "BOLETA", FechaIso.texto(local.ahora().minusDays(1)), if (estado == "REEMBOLSADO") 25000 else null)
    private fun esperar(id: Int) = compose.waitUntil(15_000) { compose.onAllNodesWithText(texto(id)).fetchSemanticsNodes().isNotEmpty() }
    private fun abrir(estado: String? = "REEMBOLSADO", insertar: Boolean = true, escala: Float = 1f) {
        runBlocking {
            local.db.viajeDao().guardar(local.viaje("TR-1051").copy(folio = folio, folioCorrelativo = 3021,
                hashPago = estado?.let { hash }, estadoPago = estado))
            if (estado != null && insertar) local.db.pagoDao().insertar(pago(estado))
        }
        compose.activity.runOnUiThread { compose.activity.setContent {
            var ruta by remember { mutableStateOf(PantallaDemo.DETALLE_SOLICITUD) }
            var referencia by remember { mutableStateOf(folio) }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                BelmonteTheme { key(ruta) {
                    DestinoPantalla(ruta, referencia, onNavegar = { p, id -> ruta = p; referencia = id },
                        onVolver = { ruta = PantallaDemo.DETALLE_SOLICITUD; referencia = folio }, onCerrarSesion = {})
                } }
            }
        } }
    }
    private fun resumen(texto: String) = compose.onNode(hasText(texto) and hasAnyAncestor(hasTestTag("pago_resumen")))
    @Test fun reembolsoYDetalleSoloLecturaAl200PorCientoYRecargaAlVolver() {
        abrir(escala = 2f); esperar(R.string.pago_consulta_ver_detalle)
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pago_resumen").fetchSemanticsNodes().isNotEmpty() }
        resumen(Formatos.monto(50001)).performScrollTo().assertIsDisplayed()
        resumen(Formatos.monto(25000)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.pago_consulta_ver_detalle)).performScrollTo().performClick()
        esperar(R.string.pago_fecha_label)
        compose.onNodeWithText("Banco de prueba").performScrollTo().assertIsDisplayed()
        for (id in listOf(R.string.accion_pagar, R.string.accion_rechazar, R.string.accion_anular))
            compose.onAllNodes(hasText(texto(id)) and hasClickAction()).assertCountEquals(0)
        runBlocking { local.db.pagoDao().guardar(pago("PAGADO")) }
        compose.onNodeWithContentDescription(texto(R.string.accion_volver)).performClick()
        val pagado = hasContentDescription(compose.activity.getString(R.string.chip_descripcion, texto(R.string.estado_pago_pagado))) and hasAnyAncestor(hasTestTag("pago_resumen"))
        compose.waitUntil(15_000) { compose.onAllNodes(pagado).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(pagado).performScrollTo().assertIsDisplayed()
    }
    @Test fun sinPagoNoMuestraEnlaceNiEstadoDePago() {
        abrir(estado = null); esperar(R.string.aviso_sin_pago)
        compose.onNodeWithText(texto(R.string.aviso_sin_pago)).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("pago_resumen").assertCountEquals(0)
        compose.onAllNodesWithText(texto(R.string.pago_consulta_ver_detalle)).assertCountEquals(0)
    }
    @Test fun pagoIlegibleConservaLaSolicitudYReintentarRecuperaElResumen() {
        abrir(estado = "PENDIENTE", insertar = false); esperar(R.string.pago_consulta_error)
        compose.onNodeWithText(folio).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText(texto(R.string.aviso_sin_pago)).assertCountEquals(0)
        runBlocking { local.db.pagoDao().insertar(pago("PENDIENTE")) }
        compose.onNodeWithText(texto(R.string.pago_consulta_reintentar)).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pago_resumen").fetchSemanticsNodes().isNotEmpty() }
        resumen(Formatos.monto(50001)).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText(texto(R.string.pago_consulta_error)).assertCountEquals(0)
    }
}
