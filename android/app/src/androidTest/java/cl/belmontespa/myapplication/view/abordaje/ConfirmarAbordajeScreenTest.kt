package cl.belmontespa.myapplication.view.abordaje

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.abordaje.ConfirmarAbordajeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Confirmar abordaje» (HU29) con un estado fijo: el vehículo, el documento y cada error, sin repositorio ni ViewModel. */
class ConfirmarAbordajeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val llamadas = mutableListOf<String>()
    private val codigo = CodigoAbordaje("KXJR42")

    private fun mostrar(estado: ConfirmarAbordajeUiState) {
        compose.setContent {
            BelmonteTheme {
                ConfirmarAbordajeContenido(
                    estado = estado,
                    onTipo = { llamadas += "tipo:$it" },
                    onDocumento = { llamadas += "documento" },
                    onConfirmar = { llamadas += "confirmar" },
                    onVolver = { llamadas += "volver" },
                )
            }
        }
    }

    private fun texto(id: Int) = textos.getString(id)

    // El título de la pantalla dice lo mismo que el botón: el botón es el que se puede tocar.
    private fun botonConfirmar() = hasText(texto(R.string.abordaje_confirmar)) and hasClickAction()

    @Test
    fun muestraElVehiculoYPideElTipoYElDocumento() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo))

        compose.onNodeWithTag("pantalla_contenido").assertExists()
        compose.onNodeWithText("KXJR42").assertExists()
        compose.onNodeWithText(texto(R.string.tipo_documento_rut)).assertExists()
        compose.onNodeWithText(texto(R.string.tipo_documento_pasaporte)).assertExists()
        compose.onNodeWithText(texto(R.string.campo_documento)).assertExists()
        compose.onNodeWithTag("pantalla_error").assertDoesNotExist()
    }

    @Test
    fun noDejaConfirmarSinDocumento() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo))

        compose.onNode(botonConfirmar()).assertIsNotEnabled()
    }

    @Test
    fun conDocumentoDejaConfirmar() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo, documento = "15.234.567-4"))

        compose.onNode(botonConfirmar()).assertIsEnabled().performClick()

        assertEquals(listOf("confirmar"), llamadas)
    }

    @Test
    fun elTipoSeCambiaConLosBotonesSegmentados() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo))

        compose.onNodeWithText(texto(R.string.tipo_documento_pasaporte)).performClick()

        assertEquals(listOf("tipo:${TipoDocumento.PASAPORTE}"), llamadas)
    }

    @Test
    fun unDocumentoInvalidoMuestraSuErrorBajoElCampo() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo, documento = "15.234.567-0", documentoInvalido = true))

        compose.onNodeWithText(texto(R.string.abordaje_documento_invalido)).assertExists()
        compose.onNodeWithTag("pantalla_error").assertDoesNotExist()
    }

    @Test
    fun elViajeNoEncontradoUsaElTextoDelAbordajeYConservaElFormulario() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo, documento = "15.234.567-4", error = ErrorApp.NoEncontrado))

        compose.onNodeWithTag("pantalla_error").assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_no_encontrado)).assertExists()
        compose.onNode(botonConfirmar()).assertIsEnabled()
    }

    @Test
    fun sinConexionMuestraSuTextoYSePuedeReintentar() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo, documento = "15.234.567-4", error = ErrorApp.SinConexion))

        compose.onNodeWithTag("pantalla_error").assertExists()
        compose.onNodeWithText(texto(R.string.error_sin_conexion)).assertExists()
        compose.onNode(botonConfirmar()).assertIsEnabled()
    }

    @Test
    fun mientrasSeEnviaNoSePuedeConfirmarDeNuevo() {
        mostrar(ConfirmarAbordajeUiState(codigo = codigo, documento = "15.234.567-4", enviando = true))

        compose.onNode(botonConfirmar()).assertIsNotEnabled()
    }

    @Test
    fun unaRutaSinCodigoValidoNoOfreceElFormulario() {
        mostrar(ConfirmarAbordajeUiState(codigo = null))

        compose.onNodeWithTag("pantalla_error").assertExists()
        compose.onNodeWithText(texto(R.string.abordaje_qr_ajeno)).assertExists()
        compose.onNode(botonConfirmar()).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.accion_volver)).performClick()

        assertEquals(listOf("volver"), llamadas)
    }
}
