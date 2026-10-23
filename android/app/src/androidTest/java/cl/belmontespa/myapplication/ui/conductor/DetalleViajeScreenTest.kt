package cl.belmontespa.myapplication.ui.conductor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.arqueo
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Detalle del viaje del Conductor (HU08) con un estado fijo: lo que se ve y lo que se avisa, sin Room ni ViewModel. */
class DetalleViajeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val agenda = LocalDateTime.of(2026, 10, 28, 14, 30)

    private fun viaje(
        estado: EstadoViaje = EstadoViaje.ASIGNADO,
        pasajeros: List<PasajeroViaje> = listOf(
            PasajeroViaje("h1", "Constanza Ríos", agenda.minusMinutes(25)),
            PasajeroViaje("h2", "Tomás Vidal", null),
        ),
        motivo: String? = null,
    ): Viaje {
        val origen = Ubicacion("Aeropuerto A. Merino Benítez", "Pudahuel", "Metropolitana de Santiago", -33.39, -70.79)
        val destino = Ubicacion("Av. Apoquindo 5400", "Las Condes", "Metropolitana de Santiago", -33.41, -70.57)
        return Viaje(
            folio = "TR-1042", estado = estado, correoSolicitante = "crios@ejemplo.cl",
            nombreSolicitante = "Constanza Ríos", telefonoSolicitante = "+56955550101", razonSocial = "Empresa",
            origen = origen, destino = destino, fhCreacion = agenda.minusDays(2), fhAgenda = agenda,
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = "LA1502",
            cantidadPasajeros = pasajeros.size, correoConductor = "ireyes@ejemplo.cl", nombreConductor = "Iván Reyes",
            patenteMovil = "KXJR42", hashPago = null, estadoPago = null, motivo = motivo, pasajeros = pasajeros,
        )
    }

    private fun estado(
        viaje: Viaje? = viaje(),
        inicio: Inicio = Inicio.HABILITADO,
        retraso: Retraso? = null,
        error: ErrorApp? = null,
        confirmandoFin: Boolean = false,
        rechazando: Boolean = false,
        motivo: String = "",
    ) = DetalleViajeUiState(
        cargando = false, viaje = viaje, retraso = retraso, arqueo = viaje?.arqueo(), inicio = inicio,
        confirmandoFin = confirmandoFin, rechazando = rechazando, motivo = motivo, error = error,
    )

    private class Avisos {
        val llamadas = mutableListOf<String>()
        var motivo = ""
    }

    private fun mostrar(estado: DetalleViajeUiState, avisos: Avisos = Avisos()) {
        compose.setContent {
            BelmonteTheme {
                DetalleViajeContenido(
                    folio = "TR-1042", estado = estado,
                    onActualizar = { avisos.llamadas += "actualizar" },
                    onVolver = { avisos.llamadas += "volver" },
                    onNavegar = { avisos.llamadas += "navegar" },
                    onIniciar = { avisos.llamadas += "iniciar" },
                    onFinalizar = { avisos.llamadas += "finalizar" },
                    onConfirmarFinalizar = { avisos.llamadas += "confirmarFinalizar" },
                    onDescartarFinalizar = { avisos.llamadas += "descartarFinalizar" },
                    onRechazar = { avisos.llamadas += "rechazar" },
                    onMotivo = { avisos.motivo = it },
                    onConfirmarRechazo = { avisos.llamadas += "confirmarRechazo" },
                    onDescartarRechazo = { avisos.llamadas += "descartarRechazo" },
                )
            }
        }
    }

    private fun texto(id: Int) = textos.getString(id)

    @Test
    fun muestraLosDatosDelViaje() {
        mostrar(estado())

        compose.onNodeWithText(textos.getString(R.string.detalle_viaje_titulo, "TR-1042")).assertExists()
        compose.onNodeWithText("28-10-2026 · 14:30").assertExists()
        compose.onNodeWithText("LA1502").assertExists()
        compose.onAllNodesWithText("Constanza Ríos").assertCountEquals(2) // solicitante y pasajero
        compose.onNodeWithText("+56 9 5555 0101").assertExists()
        compose.onNodeWithText("Aeropuerto A. Merino Benítez").assertExists()
        compose.onNodeWithText("Av. Apoquindo 5400").assertExists()
        compose.onNodeWithText("KXJR42").assertExists()
        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, texto(R.string.estado_viaje_asignado)))
            .assertCountEquals(1)
    }

    @Test
    fun elArqueoDiceCuantosAbordaronYQuienesFaltan() {
        mostrar(estado())

        compose.onNodeWithText("Abordaron 1 de 2. Faltan: Tomás Vidal.").assertExists()
        compose.onNodeWithText(textos.getString(R.string.detalle_viaje_abordo_a_las, "14:05")).assertExists()
        compose.onNodeWithText(texto(R.string.etiqueta_abordado)).assertExists()
        compose.onNodeWithText(texto(R.string.etiqueta_por_abordar)).assertExists()
    }

    @Test
    fun conTodosABordoElArqueoLoDice() {
        val todos = listOf(PasajeroViaje("h1", "Constanza Ríos", agenda.minusMinutes(25)), PasajeroViaje("h2", "Tomás Vidal", agenda.minusMinutes(20)))
        mostrar(estado(viaje(pasajeros = todos)))

        compose.onNodeWithText(texto(R.string.arqueo_todos)).assertExists()
    }

    @Test
    fun dentroDeLaVentanaSoloIniciarYRechazarEstanHabilitados() {
        mostrar(estado())

        compose.onNodeWithText(texto(R.string.accion_iniciar)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.accion_finalizar)).assertIsNotEnabled()
        compose.onNodeWithText(texto(R.string.accion_navegar_origen)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.accion_rechazar)).assertIsEnabled()
    }

    @Test
    fun antesDeLaVentanaIniciarEstaDeshabilitadoYDiceDesdeCuandoSePuede() {
        val v = viaje()
        mostrar(estado(v, inicio = Inicio.ANTES_DE_LA_VENTANA).copy(viaje = v))

        compose.onNodeWithText(texto(R.string.accion_iniciar)).assertIsNotEnabled()
        compose.onNodeWithText(textos.getString(R.string.iniciar_disponible_desde, "13:30")).assertExists()
        assertEquals(agenda.minusHours(1), v.disponibleDesde())
    }

    @Test
    fun vencidoAvisaAlAdministradorYNoSePuedeIniciar() {
        mostrar(estado(inicio = Inicio.VENCIDO, retraso = Retraso.VENCIDO))

        compose.onNodeWithText(texto(R.string.accion_iniciar)).assertIsNotEnabled()
        compose.onNodeWithText(texto(R.string.iniciar_vencido)).assertExists()
        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, texto(R.string.etiqueta_vencido)))
            .assertCountEquals(1)
    }

    @Test
    fun enCursoSoloFinalizarEstaHabilitadoYNavegaAlDestino() {
        mostrar(estado(viaje(EstadoViaje.EN_CURSO), inicio = Inicio.NO_APLICA))

        compose.onNodeWithText(texto(R.string.accion_iniciar)).assertIsNotEnabled()
        compose.onNodeWithText(texto(R.string.accion_finalizar)).assertIsEnabled()
        compose.onNodeWithText(texto(R.string.accion_navegar_destino)).assertExists()
        compose.onNodeWithText(texto(R.string.accion_rechazar)).assertDoesNotExist()
    }

    @Test
    fun unViajeFinalizadoNoTieneBotonesDeEstado() {
        mostrar(estado(viaje(EstadoViaje.FINALIZADO), inicio = Inicio.NO_APLICA))

        compose.onNodeWithText(texto(R.string.accion_iniciar)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.accion_finalizar)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.accion_navegar_origen)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.accion_rechazar)).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.arqueo_todos)).assertDoesNotExist()
    }

    @Test
    fun unViajeCanceladoMuestraSuMotivo() {
        mostrar(estado(viaje(EstadoViaje.CANCELADO, motivo = "Cancelado por la empresa"), inicio = Inicio.NO_APLICA))

        compose.onNodeWithText("Cancelado por la empresa").assertExists()
        compose.onNodeWithText(texto(R.string.accion_iniciar)).assertDoesNotExist()
    }

    @Test
    fun cadaBotonAvisaSuAccion() {
        val avisos = Avisos()
        mostrar(estado(), avisos)

        compose.onNodeWithText(texto(R.string.accion_iniciar)).performClick()
        compose.onNodeWithText(texto(R.string.accion_navegar_origen)).performClick()
        compose.onNodeWithText(texto(R.string.accion_rechazar)).performClick()
        compose.onNodeWithText(texto(R.string.accion_actualizar)).performClick()

        assertEquals(listOf("iniciar", "navegar", "rechazar", "actualizar"), avisos.llamadas)
    }

    @Test
    fun finalizarPideConfirmacion() {
        val avisos = Avisos()
        mostrar(estado(viaje(EstadoViaje.EN_CURSO), inicio = Inicio.NO_APLICA, confirmandoFin = true), avisos)

        compose.onNodeWithText(texto(R.string.aviso_finalizar)).assertExists()
        compose.onNodeWithText(texto(R.string.accion_confirmar)).performClick()

        assertEquals(listOf("confirmarFinalizar"), avisos.llamadas)
    }

    @Test
    fun rechazarPideMotivoYNoSePuedeConfirmarSinEl() {
        mostrar(estado(rechazando = true))

        compose.onNodeWithText(texto(R.string.campo_motivo)).assertExists()
        compose.onAllNodesWithText(texto(R.string.accion_rechazar))[1].assertIsNotEnabled()
    }

    @Test
    fun conMotivoSePuedeConfirmarElRechazo() {
        val avisos = Avisos()
        mostrar(estado(rechazando = true, motivo = "Vehículo con falla"), avisos)

        compose.onAllNodesWithText(texto(R.string.accion_rechazar))[1].assertIsEnabled().performClick()

        assertEquals(listOf("confirmarRechazo"), avisos.llamadas)
    }

    @Test
    fun unConflictoSeMuestraSobreElDetalleSinOfrecerReintentar() {
        mostrar(estado(error = ErrorApp.Conflicto("El viaje cambió de estado. Actualiza el detalle.")))

        compose.onNodeWithTag("t06_error").assertExists()
        compose.onNodeWithText("El viaje cambió de estado. Actualiza el detalle.").assertExists()
        compose.onNodeWithTag("t06_contenido").assertExists()
        compose.onNodeWithText(texto(R.string.accion_reintentar)).assertDoesNotExist()
    }

    @Test
    fun unViajeQueNoSeEncuentraOfreceReintentar() {
        val avisos = Avisos()
        mostrar(estado(viaje = null, error = ErrorApp.NoEncontrado), avisos)

        compose.onNodeWithTag("t06_error").assertExists()
        compose.onNodeWithText(texto(R.string.error_no_encontrado)).assertExists()
        compose.onNodeWithText(texto(R.string.accion_reintentar)).performClick()

        assertEquals(listOf("actualizar"), avisos.llamadas)
    }

    @Test
    fun laFlechaAtrasVuelve() {
        val avisos = Avisos()
        mostrar(estado(), avisos)

        compose.onNodeWithContentDescription(texto(R.string.accion_volver)).performClick()

        assertEquals(listOf("volver"), avisos.llamadas)
    }
}
