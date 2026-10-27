package cl.belmontespa.myapplication.ui.conductor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Mis viajes» (HU07) con un estado fijo: lo que se ve y lo que se avisa, sin Room ni ViewModel. */
class MisViajesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val hoy = LocalDate.of(2026, 10, 28)

    private fun viaje(
        folio: String,
        estado: EstadoViaje,
        agenda: LocalDateTime,
        pasajeros: List<String>,
        origen: String = "Aeropuerto",
        destino: String = "Las Condes",
    ): Viaje {
        fun lugar(direccion: String) = Ubicacion(direccion, "Santiago", "Metropolitana de Santiago", -33.4261, -70.611)
        return Viaje(
            folio = folio, estado = estado, correoSolicitante = "solicitante@ejemplo.cl",
            nombreSolicitante = "Constanza Ríos", telefonoSolicitante = null, razonSocial = "Empresa",
            origen = lugar(origen), destino = lugar(destino), fhCreacion = agenda.minusDays(1), fhAgenda = agenda,
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = null,
            cantidadPasajeros = pasajeros.size, correoConductor = "ireyes@ejemplo.cl", nombreConductor = "Iván Reyes",
            patenteMovil = "KXJR42", hashPago = null, estadoPago = null, motivo = null,
            pasajeros = pasajeros.map { PasajeroViaje("hash-$it", it, null) },
        )
    }

    private fun fila(viaje: Viaje, retraso: Retraso? = null, antesDeLaVentana: Boolean = false) =
        ViajeFila(viaje, retraso, antesDeLaVentana)

    private fun estado(
        filas: List<ViajeFila> = emptyList(),
        fecha: LocalDate = hoy,
        error: ErrorApp? = null,
        conductor: String? = "Iván Reyes",
    ) = MisViajesUiState(fecha = fecha, hoy = hoy, conductor = conductor, filas = filas, cargando = false, error = error)

    private fun mostrar(
        estado: MisViajesUiState,
        onFecha: (LocalDate) -> Unit = {},
        onActualizar: () -> Unit = {},
        onVerDetalle: (ViajeFila) -> Unit = {},
    ) {
        compose.setContent {
            BelmonteTheme {
                MisViajesContenido(estado = estado, onFecha = onFecha, onActualizar = onActualizar, onVerDetalle = onVerDetalle)
            }
        }
    }

    private val agendaDelDia = listOf(
        fila(viaje("TR-1039", EstadoViaje.FINALIZADO, hoy.atTime(7, 15), listOf("Roberto Aguilar"), "Apoquindo", "Aeropuerto")),
        fila(viaje("TR-1042", EstadoViaje.ASIGNADO, hoy.atTime(14, 30), listOf("Constanza Ríos", "Tomás Vidal"))),
        fila(
            viaje("TR-1049", EstadoViaje.ASIGNADO, hoy.atTime(18, 45), listOf("Felipe Mora"), "Las Condes", "Aeropuerto"),
            retraso = Retraso.VENCIDO,
        ),
    )

    @Test
    fun muestraLaAgendaPorHoraConPasajeroYEstado() {
        mostrar(estado(agendaDelDia))

        compose.onNodeWithText("Iván Reyes").assertExists()
        compose.onNodeWithText("07:15").assertExists()
        compose.onNodeWithText("Roberto Aguilar").assertExists()
        compose.onNodeWithText("Apoquindo → Aeropuerto").assertExists()
        compose.onNodeWithText("14:30").assertExists()
        compose.onNodeWithText("Constanza Ríos +1").assertExists()
        compose.onNodeWithText("Aeropuerto → Las Condes").assertExists()
        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, textos.getString(R.string.estado_viaje_asignado)))
            .assertCountEquals(2)
        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, textos.getString(R.string.estado_viaje_finalizado)))
            .assertCountEquals(1)
    }

    @Test
    fun elRetrasoSeMuestraComoChipSoloEnElViajeQueLoLleva() {
        mostrar(estado(agendaDelDia))

        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, textos.getString(R.string.etiqueta_vencido)))
            .assertCountEquals(1)
        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, textos.getString(R.string.etiqueta_atrasado)))
            .assertCountEquals(0)
    }

    @Test
    fun elSelectorDiceHoyConLaFechaYSoloLaFechaEnOtroDia() {
        mostrar(estado())
        compose.onNodeWithText(textos.getString(R.string.mis_viajes_hoy, "28-10-2026")).assertExists()
    }

    @Test
    fun enOtroDiaElSelectorMuestraSoloLaFecha() {
        mostrar(estado(fecha = hoy.plusDays(2)))

        compose.onNodeWithText("30-10-2026").assertExists()
        compose.onNodeWithText(textos.getString(R.string.mis_viajes_hoy, "30-10-2026")).assertDoesNotExist()
    }

    @Test
    fun elSelectorAbreElCalendario() {
        mostrar(estado())

        compose.onNodeWithText(textos.getString(R.string.mis_viajes_hoy, "28-10-2026")).performClick()

        compose.onNodeWithText(textos.getString(R.string.accion_aceptar)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_cancelar)).performClick()
        compose.onNodeWithText(textos.getString(R.string.accion_aceptar)).assertDoesNotExist()
    }

    @Test
    fun unViajeDeUnDiaAnteriorMuestraSuFechaYNoSoloLaHora() {
        val deAyer = viaje("TR-1045", EstadoViaje.EN_CURSO, hoy.minusDays(1).atTime(23, 30), listOf("Javiera Núñez"))
        mostrar(estado(listOf(fila(deAyer))))

        compose.onNodeWithText("27-10 · 23:30").assertExists()
    }

    @Test
    fun tocarUnViajeAvisaCualEs() {
        val tocados = mutableListOf<String>()
        mostrar(estado(agendaDelDia), onVerDetalle = { tocados += it.viaje.folio })

        compose.onNodeWithText("Constanza Ríos +1").performClick()

        assertEquals(listOf("TR-1042"), tocados)
    }

    @Test
    fun sinViajesLoDiceYSigueOfreciendoElSelector() {
        mostrar(estado())

        compose.onNodeWithText(textos.getString(R.string.vacio_mis_viajes)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.mis_viajes_hoy, "28-10-2026")).assertExists()
    }

    @Test
    fun unErrorSinViajesOfreceReintentar() {
        var reintentos = 0
        mostrar(estado(error = ErrorApp.SinConexion), onActualizar = { reintentos++ })

        compose.onNodeWithTag("pantalla_error").assertExists()
        compose.onNodeWithText(textos.getString(R.string.error_sin_conexion)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_reintentar)).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun unErrorConViajesAvisaSobreLaListaSinEsconderla() {
        mostrar(estado(agendaDelDia, error = ErrorApp.SinConexion))

        compose.onNodeWithText(textos.getString(R.string.error_sin_conexion)).assertExists()
        compose.onNodeWithText("Constanza Ríos +1").assertExists()
    }

    @Test
    fun actualizarSeOfreceComoAlternativaAlGesto() {
        var actualizaciones = 0
        mostrar(estado(agendaDelDia), onActualizar = { actualizaciones++ })

        compose.onNodeWithText(textos.getString(R.string.accion_actualizar)).performClick()

        assertEquals(1, actualizaciones)
    }
}
