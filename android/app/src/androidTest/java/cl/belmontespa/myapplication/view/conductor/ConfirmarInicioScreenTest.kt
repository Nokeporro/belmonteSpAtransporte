package cl.belmontespa.myapplication.view.conductor

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.conductor.ConfirmarInicioUiState
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmarInicioScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val agenda = LocalDateTime.of(2026, 10, 28, 14, 30)

    private fun viaje(): Viaje {
        val lugar = Ubicacion("Aeropuerto A. Merino Benítez", "Pudahuel", "Metropolitana de Santiago", -33.39, -70.79)
        return Viaje(
            folio = "TR-1042", estado = EstadoViaje.ASIGNADO, correoSolicitante = "crios@ejemplo.cl",
            nombreSolicitante = "Constanza Ríos", telefonoSolicitante = null, razonSocial = "Empresa",
            origen = lugar, destino = lugar, fhCreacion = agenda.minusDays(2), fhAgenda = agenda,
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = null,
            cantidadPasajeros = 2, correoConductor = "ireyes@ejemplo.cl", nombreConductor = "Iván Reyes",
            patenteMovil = "KXJR42", hashPago = null, estadoPago = null, motivo = null,
            pasajeros = listOf(PasajeroViaje("h1", "Constanza Ríos", null), PasajeroViaje("h2", "Tomás Vidal", null)),
        )
    }

    private fun estado(
        faltan: Int = 1,
        inicio: Inicio = Inicio.HABILITADO,
        viaje: Viaje? = viaje(),
        error: ErrorApp? = null,
        enviando: Boolean = false,
    ) = ConfirmarInicioUiState(cargando = false, viaje = viaje, faltan = faltan, inicio = inicio, enviando = enviando, error = error)

    private val llamadas = mutableListOf<String>()

    private fun mostrar(estado: ConfirmarInicioUiState) {
        compose.setContent {
            BelmonteTheme {
                ConfirmarInicioContenido(
                    folio = "TR-1042", estado = estado,
                    onActualizar = { llamadas += "actualizar" },
                    onVolver = { llamadas += "volver" },
                    onIniciar = { llamadas += "iniciar" },
                )
            }
        }
    }

    @Test
    fun preguntaYDiceQueQuedaOcupadoYElVehiculoEnUso() {
        mostrar(estado())

        compose.onNodeWithText("¿Iniciar el viaje TR-1042?").assertExists()
        compose.onNodeWithText("Pasará a En curso. Quedarás Ocupado y el vehículo KXJR42 En uso hasta finalizarlo.").assertExists()
        compose.onNodeWithTag("pantalla_contenido").assertExists()
        compose.onNodeWithTag("pantalla_error").assertDoesNotExist()
    }

    @Test
    fun avisaEnSingularYEnPluralCuantosPasajerosFaltan() {
        mostrar(estado(faltan = 1))
        compose.onNodeWithText("Falta 1 pasajero por abordar.").assertExists()
    }

    @Test
    fun avisaEnPluralCuandoFaltanVarios() {
        mostrar(estado(faltan = 2))
        compose.onNodeWithText("Faltan 2 pasajeros por abordar.").assertExists()
    }

    @Test
    fun sinPasajerosPendientesNoHayAviso() {
        mostrar(estado(faltan = 0))
        compose.onNodeWithText("Falta 1 pasajero por abordar.").assertDoesNotExist()
        compose.onNodeWithText("Faltan 2 pasajeros por abordar.").assertDoesNotExist()
    }

    @Test
    fun dentroDeLaVentanaIniciarEstaHabilitadoYAvisa() {
        mostrar(estado())

        compose.onNodeWithText(textos.getString(R.string.accion_iniciar)).assertIsEnabled().performClick()

        assertEquals(listOf("iniciar"), llamadas)
    }

    @Test
    fun volverNoIniciaNada() {
        mostrar(estado())

        compose.onNodeWithText(textos.getString(R.string.accion_volver)).performClick()

        assertEquals(listOf("volver"), llamadas)
    }

    @Test
    fun antesDeLaVentanaIniciarEstaDeshabilitadoYDiceDesdeCuando() {
        mostrar(estado(inicio = Inicio.ANTES_DE_LA_VENTANA))

        compose.onNodeWithText(textos.getString(R.string.accion_iniciar)).assertIsNotEnabled()
        compose.onNodeWithText(textos.getString(R.string.iniciar_disponible_desde, "13:30")).assertExists()
    }

    @Test
    fun vencidoDiceQueSeAvisaAlAdministrador() {
        mostrar(estado(inicio = Inicio.VENCIDO))

        compose.onNodeWithText(textos.getString(R.string.iniciar_vencido)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_iniciar)).assertIsNotEnabled()
    }

    @Test
    fun mientrasEnviaIniciarEstaDeshabilitado() {
        mostrar(estado(enviando = true))

        compose.onNodeWithText(textos.getString(R.string.accion_iniciar)).assertIsNotEnabled()
    }

    @Test
    fun unConflictoMuestraElMensajeDelServidor() {
        mostrar(estado(error = ErrorApp.Conflicto("El conductor ya tiene un viaje en curso")))

        compose.onNodeWithText("El conductor ya tiene un viaje en curso").assertExists()
        compose.onNodeWithTag("pantalla_error").assertExists()
    }

    @Test
    fun unViajeNoEncontradoOfreceReintentar() {
        mostrar(estado(viaje = null, error = ErrorApp.NoEncontrado))

        compose.onNodeWithTag("pantalla_error").assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_reintentar)).performClick()

        assertEquals(listOf("actualizar"), llamadas)
    }
}
