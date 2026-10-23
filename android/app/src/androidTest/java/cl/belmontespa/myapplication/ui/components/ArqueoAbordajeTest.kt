package cl.belmontespa.myapplication.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Arqueo
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test

/** Arqueo de abordaje (HU30): el mismo componente en el detalle del Conductor y en el del Administrador. */
class ArqueoAbordajeTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val agenda = LocalDateTime.of(2026, 10, 28, 14, 30)

    private fun mostrar(pasajeros: List<PasajeroViaje>) {
        val faltan = pasajeros.filter { it.fhAbordaje == null }.map { it.nombre }
        val arqueo = Arqueo(abordaron = pasajeros.size - faltan.size, total = pasajeros.size, faltan = faltan)
        compose.setContent { BelmonteTheme { ArqueoDeAbordaje(arqueo, pasajeros) } }
    }

    private fun chip(id: Int) = textos.getString(R.string.chip_descripcion, textos.getString(id))

    @Test
    fun conFaltantesDiceCuantosAbordaronYQuienesFaltan() {
        mostrar(
            listOf(
                PasajeroViaje("h1", "Constanza Ríos", agenda.minusMinutes(25)),
                PasajeroViaje("h2", "Tomás Vidal", null),
                PasajeroViaje("h3", "Marcela Soto", null),
            ),
        )

        compose.onNodeWithText(textos.getString(R.string.campo_abordaje)).assertExists()
        compose.onNodeWithText("Abordaron 1 de 3. Faltan: Tomás Vidal, Marcela Soto.").assertExists()
        compose.onNodeWithText(textos.getString(R.string.detalle_viaje_abordo_a_las, "14:05")).assertExists()
    }

    @Test
    fun cadaPasajeroTieneUnChipConSuEstado() {
        mostrar(
            listOf(
                PasajeroViaje("h1", "Constanza Ríos", agenda.minusMinutes(25)),
                PasajeroViaje("h2", "Tomás Vidal", null),
                PasajeroViaje("h3", "Marcela Soto", null),
            ),
        )

        compose.onAllNodesWithContentDescription(chip(R.string.etiqueta_abordado)).assertCountEquals(1)
        compose.onAllNodesWithContentDescription(chip(R.string.etiqueta_por_abordar)).assertCountEquals(2)
    }

    @Test
    fun conTodosABordoLoDiceYNoHayFaltantes() {
        mostrar(
            listOf(
                PasajeroViaje("h1", "Constanza Ríos", agenda.minusMinutes(25)),
                PasajeroViaje("h2", "Tomás Vidal", agenda.minusMinutes(20)),
            ),
        )

        compose.onNodeWithText(textos.getString(R.string.arqueo_todos)).assertExists()
        compose.onAllNodesWithContentDescription(chip(R.string.etiqueta_por_abordar)).assertCountEquals(0)
        compose.onAllNodesWithContentDescription(chip(R.string.etiqueta_abordado)).assertCountEquals(2)
    }
}
