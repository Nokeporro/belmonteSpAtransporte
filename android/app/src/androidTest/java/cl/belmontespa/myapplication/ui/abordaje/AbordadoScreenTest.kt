package cl.belmontespa.myapplication.ui.abordaje

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Abordaje registrado» (HU29): la hora que viene en la ruta y la vuelta al inicio. */
class AbordadoScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val llamadas = mutableListOf<String>()

    private fun mostrar(hora: LocalDateTime?) {
        compose.setContent {
            BelmonteTheme {
                AbordadoContenido(hora, onVolverAlInicio = { llamadas += "inicio" }, onVolver = { llamadas += "volver" })
            }
        }
    }

    @Test
    fun diceLaHoraDelAbordajeYDespideAlPasajero() {
        mostrar(LocalDateTime.of(2026, 10, 28, 14, 5, 9))

        compose.onNodeWithTag("pantalla_contenido").assertExists()
        compose.onNodeWithText(textos.getString(R.string.abordaje_registrado, "14:05")).assertExists()
    }

    @Test
    fun volverAlInicioAvisaALaPantalla() {
        mostrar(LocalDateTime.of(2026, 10, 28, 14, 5))

        compose.onNodeWithText(textos.getString(R.string.abordaje_volver_inicio)).performClick()

        assertEquals(listOf("inicio"), llamadas)
    }

    @Test
    fun unaHoraIlegibleNoInventaUnAbordaje() {
        mostrar(null)

        compose.onNodeWithText(textos.getString(R.string.abordaje_no_encontrado)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.abordaje_volver_inicio)).assertExists()
    }
}
