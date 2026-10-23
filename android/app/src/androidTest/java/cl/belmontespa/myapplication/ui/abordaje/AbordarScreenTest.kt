package cl.belmontespa.myapplication.ui.abordaje

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Abordar viaje» (HU29) con un estado fijo: qué se ofrece, qué se avisa y el diálogo «Ingresar código», sin cámara ni ViewModel. */
class AbordarScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val llamadas = mutableListOf<String>()

    private fun mostrar(estado: AbordarUiState = AbordarUiState(), pantalla: PantallaDemo = PantallaDemo.ABORDAR) {
        compose.setContent {
            BelmonteTheme {
                AbordarContenido(
                    pantalla = pantalla, estado = estado,
                    onEscanear = { llamadas += "escanear" },
                    onIngresar = { llamadas += "ingresar" },
                    onAbrirAjustes = { llamadas += "ajustes" },
                    onVolver = { llamadas += "volver" },
                    onPatente = { llamadas += "patente:$it" },
                    onCodigo = { llamadas += "codigo:$it" },
                    onContinuar = { llamadas += "continuar" },
                    onCerrarIngreso = { llamadas += "cerrar" },
                )
            }
        }
    }

    private fun texto(id: Int) = textos.getString(id)

    @Test
    fun ofreceEscanearEIngresarElCodigoSinPedirLaCamaraAlEntrar() {
        mostrar()

        compose.onNodeWithTag("t06_contenido").assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_indicacion)).assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_escanear)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.abordaje_ingresar_codigo)).assertIsEnabled()
        compose.onNodeWithTag("t06_aviso_abordaje").assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.abordaje_abrir_ajustes)).assertDoesNotExist()
        assertEquals(emptyList<String>(), llamadas)
    }

    @Test
    fun laVarianteEscanearUsaElTituloYLaIndicacionDelLector() {
        mostrar(pantalla = PantallaDemo.ESCANEAR)

        compose.onNodeWithText(texto(R.string.abordaje_lector_indicacion)).assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_indicacion)).assertDoesNotExist()
        // El título de la barra y el botón comparten el mismo texto.
        compose.onAllNodesWithText(texto(R.string.abordaje_escanear)).assertCountEquals(2)
        assertEquals(emptyList<String>(), llamadas)
    }

    @Test
    fun losBotonesAvisanALaPantalla() {
        mostrar()

        compose.onNodeWithText(texto(R.string.abordaje_escanear)).performClick()
        compose.onNodeWithText(texto(R.string.abordaje_ingresar_codigo)).performClick()

        assertEquals(listOf("escanear", "ingresar"), llamadas)
    }

    @Test
    fun unQrAjenoDiceQueNoEsDeUnVehiculoBelmonte() {
        mostrar(AbordarUiState(aviso = AvisoAbordar.QR_AJENO))

        compose.onNodeWithTag("t06_aviso_abordaje").assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_qr_ajeno)).assertExists()
    }

    @Test
    fun conLaCamaraNegadaAunPedibleNoOfreceAbrirAjustes() {
        mostrar(AbordarUiState(aviso = AvisoAbordar.CAMARA_NEGADA))

        compose.onNodeWithText(texto(R.string.abordaje_camara_negada)).assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_ingresar_codigo)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.abordaje_abrir_ajustes)).assertDoesNotExist()
    }

    @Test
    fun conLaCamaraNegadaParaSiempreOfreceAbrirAjustesYElIngresoManual() {
        mostrar(AbordarUiState(aviso = AvisoAbordar.CAMARA_NEGADA_PARA_SIEMPRE))

        compose.onNodeWithText(texto(R.string.abordaje_camara_negada)).assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_ingresar_codigo)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.abordaje_abrir_ajustes)).performClick()

        assertEquals(listOf("ajustes"), llamadas)
    }

    @Test
    fun elDialogoPideLaPatenteYElCodigoYNoDejaContinuarSinLosDos() {
        mostrar(AbordarUiState(ingresando = true, patente = "KXJR42"))

        compose.onNodeWithText(texto(R.string.campo_patente)).assertExists()
        compose.onNodeWithText(texto(R.string.campo_codigo)).assertExists()
        compose.onNodeWithText(texto(R.string.accion_continuar)).assertIsNotEnabled()
    }

    @Test
    fun conLosDosCamposEscritosSePuedeContinuarOCancelar() {
        mostrar(AbordarUiState(ingresando = true, patente = "KXJR42", codigo = "7F3A9C21B04E58D6"))

        compose.onNodeWithText(texto(R.string.accion_continuar)).assertIsEnabled().performClick()
        compose.onNodeWithText(texto(R.string.accion_cancelar)).performClick()

        assertEquals(listOf("continuar", "cerrar"), llamadas)
    }

    @Test
    fun unIngresoMalFormadoMuestraElErrorBajoLaPatente() {
        mostrar(AbordarUiState(ingresando = true, patente = "KXJR4", codigo = "7F3A", ingresoInvalido = true))

        compose.onNodeWithText(texto(R.string.abordaje_codigo_invalido)).assertExists()
    }

    @Test
    fun sinElDialogoAbiertoNoSeMuestranLosCamposDelIngreso() {
        mostrar()

        compose.onNodeWithText(texto(R.string.campo_patente)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.campo_codigo)).assertDoesNotExist()
    }
}
