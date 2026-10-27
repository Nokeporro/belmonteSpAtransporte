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
import org.junit.Assert.*

@HiltAndroidTest
class HU16ScreenTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @BindValue @JvmField val auth: AuthRepository = AuthFalso()
    @Inject lateinit var sesiones: SesionStore
    @Inject lateinit var local: SoporteLocal
    private val folio = "TR-3000"
    private val hash = "hu16-pago"
    @Before fun preparar() {
        hilt.inject()
        runBlocking { sesiones.guardar(Sesion("prueba", "crios@andesconsult.cl", Rol.SOLICITANTE, System.currentTimeMillis() + 3_600_000)) }
    }
    private fun abrir(estadoPago: String, inicio: PantallaDemo = PantallaDemo.DETALLE_SOLICITUD, escala: Float = 1f) {
        runBlocking {
            val base = local.viaje("TR-1051")
            local.db.viajeDao().guardar(base.copy(folio = folio, folioCorrelativo = 3000, estado = "PENDIENTE",
                fhAgenda = FechaIso.texto(local.ahora().plusHours(3)), hashPago = hash, estadoPago = estadoPago, fhFin = null))
            local.db.pagoDao().insertar(PagoEntity(hash, folio, 50_001, estadoPago, "Banco", 12,
                "12345678-5", "TRANSFERENCIA", "BOLETA", null, null))
        }
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                var ruta by remember { mutableStateOf(inicio) }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, escala)) {
                    BelmonteTheme {
                        key(ruta) {
                            DestinoPantalla(ruta, folio, onNavegar = { p, _ -> ruta = p },
                                onVolver = { ruta = PantallaDemo.DETALLE_SOLICITUD }, onCerrarSesion = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("pantalla_contenido").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun esperarTexto(texto: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun confirmar() = compose.onNodeWithText(compose.activity.getString(R.string.accion_confirmar)).performScrollTo().performClick()
    private fun abrirConfirmacion() {
        val texto = compose.activity.getString(R.string.accion_cancelar_solicitud)
        esperarTexto(texto)
        compose.onNodeWithText(texto).performScrollTo().performClick()
    }

    @Test fun muestraLaMitadAntesDeConfirmarYCancelaConTextoAl200PorCiento() {
        abrir("PAGADO", escala = 2f)
        abrirConfirmacion()
        val mensaje = compose.activity.getString(R.string.cancelacion_reembolso_previsto, Formatos.monto(25_000))
        esperarTexto(mensaje)
        compose.onNodeWithText(mensaje).performScrollTo().assertIsDisplayed()
        confirmar()
        esperarTexto(compose.activity.getString(R.string.aviso_solicitud_cancelada))
        compose.onNodeWithText(Formatos.monto(25_000)).performScrollTo().assertIsDisplayed()
        runBlocking {
            assertEquals("CANCELADO", local.viaje(folio).estado); assertNull(local.viaje(folio).fhFin)
            val pago = local.db.pagoDao().obtener(hash)!!
            assertEquals("REEMBOLSADO", pago.estadoPago); assertEquals(25_000, pago.montoReembolsado)
        }
    }

    @Test fun volverNoCancelaYConfirmarAnulaElPagoPendiente() {
        abrir("PENDIENTE", PantallaDemo.CONFIRMAR_CANCELACION)
        esperarTexto(compose.activity.getString(R.string.cancelacion_anula_pago))
        compose.onNodeWithText(compose.activity.getString(R.string.accion_cancelar)).performScrollTo().performClick()
        runBlocking {
            assertEquals("PENDIENTE", local.viaje(folio).estado)
            assertEquals("PENDIENTE", local.db.pagoDao().obtener(hash)!!.estadoPago)
        }
        abrirConfirmacion()
        esperarTexto(compose.activity.getString(R.string.cancelacion_anula_pago)); confirmar()
        esperarTexto(compose.activity.getString(R.string.cancelacion_pago_anulado))
        runBlocking {
            assertEquals("ANULADO", local.db.pagoDao().obtener(hash)!!.estadoPago)
            assertNull(local.db.pagoDao().obtener(hash)!!.montoReembolsado)
        }
    }

    @Test fun siLaSolicitudSeVenceMientrasConfirmaElBotonSeDeshabilita() {
        abrir("PAGADO", PantallaDemo.CONFIRMAR_CANCELACION)
        esperarTexto(compose.activity.getString(R.string.cancelacion_reembolso_previsto, Formatos.monto(25_000)))
        runBlocking {
            val v = local.viaje(folio)
            local.db.viajeDao().guardar(v.copy(fhAgenda = FechaIso.texto(local.ahora().minusHours(1).minusSeconds(1))))
        }
        esperarTexto(compose.activity.getString(R.string.cancelacion_no_disponible))
        compose.onNodeWithText(compose.activity.getString(R.string.accion_confirmar)).performScrollTo().assertIsNotEnabled()
        runBlocking {
            assertEquals("PENDIENTE", local.viaje(folio).estado)
            assertEquals("PAGADO", local.db.pagoDao().obtener(hash)!!.estadoPago)
        }
    }
}
