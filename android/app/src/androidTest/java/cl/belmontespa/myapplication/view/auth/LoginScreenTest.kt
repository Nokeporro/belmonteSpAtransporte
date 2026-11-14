package cl.belmontespa.myapplication.view.auth

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.auth.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext

    private fun mostrar(estado: LoginUiState, llamadas: MutableList<String> = mutableListOf()) {
        compose.setContent {
            BelmonteTheme {
                LoginContenido(
                    estado = estado, modoDemo = false,
                    onCorreo = {}, onPassword = {}, onIniciarSesion = {}, onCrearCuenta = {}, onAbordarViaje = {},
                    onConfirmarOtraCuenta = { llamadas += "continuar" },
                    onCancelarOtraCuenta = { llamadas += "cancelar" },
                )
            }
        }
    }

    @Test
    fun conCambiosDeOtraCuentaPideConfirmarYPermiteContinuar() {
        val llamadas = mutableListOf<String>()
        mostrar(LoginUiState(correo = "otra@belmontespa.cl", confirmandoOtraCuenta = true), llamadas)

        compose.onNodeWithText(textos.getString(R.string.aviso_pendientes_otra_cuenta)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_continuar)).performClick()

        assertEquals(listOf("continuar"), llamadas)
    }

    @Test
    fun cancelarLaConfirmacionAvisaAlViewModel() {
        val llamadas = mutableListOf<String>()
        mostrar(LoginUiState(correo = "otra@belmontespa.cl", confirmandoOtraCuenta = true), llamadas)

        compose.onNodeWithText(textos.getString(R.string.accion_cancelar)).performClick()

        assertEquals(listOf("cancelar"), llamadas)
    }

    @Test
    fun sinConfirmacionPendienteNoHayDialogo() {
        mostrar(LoginUiState(correo = "otra@belmontespa.cl"))

        compose.onNodeWithText(textos.getString(R.string.aviso_pendientes_otra_cuenta)).assertDoesNotExist()
    }
}
