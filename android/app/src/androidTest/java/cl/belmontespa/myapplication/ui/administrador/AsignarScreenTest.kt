package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Asignar conductor y vehículo» (HU05) con un estado fijo: lo que se ve y lo que se avisa, sin Room ni ViewModel. */
class AsignarScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val agenda = LocalDateTime.of(2026, 10, 29, 12, 0)

    private val viaje: Viaje = run {
        val lugar = Ubicacion("Av. Providencia 1234", "Providencia", "Metropolitana de Santiago", -33.4261, -70.611)
        Viaje(
            folio = "TR-1051", estado = EstadoViaje.PENDIENTE, correoSolicitante = "solicitante@ejemplo.cl",
            nombreSolicitante = "Marcela Soto", telefonoSolicitante = null, razonSocial = "Empresa",
            origen = lugar, destino = lugar, fhCreacion = agenda.minusDays(1), fhAgenda = agenda,
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = null, cantidadPasajeros = 2,
            correoConductor = null, nombreConductor = null, patenteMovil = null, hashPago = null,
            estadoPago = null, motivo = null, pasajeros = emptyList(),
        )
    }

    private val conductores = listOf(
        ConductorOpcion("ivan@ejemplo.cl", "Iván Reyes", EstadoConductor.DISPONIBLE, 1, null, habilitado = true),
        ConductorOpcion("paula@ejemplo.cl", "Paula Muñoz", EstadoConductor.DISPONIBLE, 0, null, habilitado = true),
        ConductorOpcion("jorge@ejemplo.cl", "Jorge Lara", EstadoConductor.OCUPADO, 1, "TR-1045", habilitado = false),
    )

    private val vehiculos = listOf(
        VehiculoOpcion("KXJR42", 4, EstadoMovil.DISPONIBLE),
        VehiculoOpcion("LPHT17", 6, EstadoMovil.DISPONIBLE),
    )

    private fun mostrar(
        estado: AsignarUiState,
        onConductor: (String) -> Unit = {},
        onVehiculo: (String) -> Unit = {},
        onConfirmar: () -> Unit = {},
        onReintentar: () -> Unit = {},
    ) {
        compose.setContent {
            BelmonteTheme {
                AsignarContenido(
                    estado = estado, onConductor = onConductor, onVehiculo = onVehiculo,
                    onConfirmar = onConfirmar, onReintentar = onReintentar, onVolver = {},
                )
            }
        }
    }

    private fun base() = AsignarUiState(cargando = false, viaje = viaje, conductores = conductores)

    @Test
    fun muestraElViajeEnElSubtituloYLosConductoresConSuEstado() {
        mostrar(base())

        compose.onNodeWithText("TR-1051 · 2 pasajeros").assertExists()
        compose.onNodeWithText("Iván Reyes").assertExists()
        compose.onNodeWithText(textos.resources.getQuantityString(R.plurals.asignar_viajes_asignados, 1, 1)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.asignar_sin_viajes)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.asignar_en_curso, "TR-1045")).assertExists()
    }

    @Test
    fun unConductorOcupadoNoSePuedeElegir() {
        val elegidos = mutableListOf<String>()
        mostrar(base(), onConductor = { elegidos += it })

        compose.onNodeWithText("Jorge Lara").assertIsNotEnabled()
        compose.onNodeWithText("Iván Reyes").assertIsEnabled().performClick()

        assertEquals(listOf("ivan@ejemplo.cl"), elegidos)
    }

    @Test
    fun sinConductorElegidoNoHayVehiculosNiResumen() {
        mostrar(base())

        compose.onNodeWithText(textos.getString(R.string.asignar_resumen)).assertDoesNotExist()
        compose.onNodeWithText(textos.getString(R.string.asignar_vehiculo_habilitado_para, "Iván Reyes")).assertDoesNotExist()
        compose.onNodeWithText(textos.getString(R.string.accion_confirmar_asignacion)).assertIsNotEnabled()
    }

    @Test
    fun conUnConductorElegidoSeOfrecenSusVehiculos() {
        val elegidos = mutableListOf<String>()
        mostrar(base().copy(conductorElegido = "ivan@ejemplo.cl", vehiculos = vehiculos), onVehiculo = { elegidos += it })

        compose.onNodeWithText(textos.getString(R.string.asignar_vehiculo_habilitado_para, "Iván Reyes")).assertExists()
        compose.onNodeWithText("Iván Reyes").assertIsSelected()
        compose.onNodeWithText("LPHT17").performClick()

        assertEquals(listOf("LPHT17"), elegidos)
    }

    @Test
    fun sinVehiculosElegiblesLoDice() {
        mostrar(base().copy(conductorElegido = "paula@ejemplo.cl"))

        compose.onNodeWithText(textos.getString(R.string.asignar_sin_vehiculos)).assertExists()
    }

    @Test
    fun conLaEleccionCompletaMuestraElResumenYPermiteConfirmar() {
        var confirmaciones = 0
        mostrar(
            base().copy(conductorElegido = "ivan@ejemplo.cl", vehiculos = vehiculos, vehiculoElegido = "KXJR42"),
            onConfirmar = { confirmaciones++ },
        )

        compose.onNodeWithText(textos.getString(R.string.asignar_resumen)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_confirmar_asignacion)).assertIsEnabled().performClick()

        assertEquals(1, confirmaciones)
    }

    @Test
    fun mientrasEnviaElBotonNoSePuedeVolverATocar() {
        mostrar(base().copy(conductorElegido = "ivan@ejemplo.cl", vehiculos = vehiculos, vehiculoElegido = "KXJR42", enviando = true))

        compose.onNodeWithText(textos.getString(R.string.accion_confirmar_asignacion)).assertIsNotEnabled()
    }

    @Test
    fun sinConductoresDisponiblesLoDiceYMuestraLosOcupadosDeshabilitados() {
        mostrar(base().copy(conductores = listOf(conductores[2])))

        compose.onNodeWithText(textos.getString(R.string.vacio_conductores_disponibles)).assertExists()
        compose.onNodeWithText("Jorge Lara").assertIsNotEnabled()
    }

    @Test
    fun unViajeQueNoSeAsignaMuestraElAvisoEnVezDelFormulario() {
        mostrar(base().copy(noAsignable = NoAsignable.YA_NO_SE_PUEDE))

        compose.onNodeWithText(textos.getString(R.string.aviso_viaje_no_asignable)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_confirmar_asignacion)).assertDoesNotExist()
    }

    @Test
    fun unViajeSinPasajerosUsaElAvisoDeSinPasajeros() {
        mostrar(base().copy(noAsignable = NoAsignable.SIN_PASAJEROS))

        compose.onNodeWithText(textos.getString(R.string.aviso_sin_pasajeros_asignar)).assertExists()
    }

    @Test
    fun unConflictoMuestraElMensajeDelServidorSinOfrecerReintentar() {
        mostrar(base().copy(error = ErrorApp.Conflicto("El móvil ya tiene un viaje a menos de 2 horas")))

        compose.onNodeWithTag("t06_error").assertExists()
        compose.onNodeWithText("El móvil ya tiene un viaje a menos de 2 horas").assertExists()
        compose.onAllNodesWithTag("t06_contenido").assertCountEquals(1)
        compose.onNodeWithText(textos.getString(R.string.accion_reintentar)).assertDoesNotExist()
    }

    @Test
    fun unErrorDeLecturaSinViajeOfreceReintentar() {
        var reintentos = 0
        mostrar(AsignarUiState(cargando = false, error = ErrorApp.SinConexion), onReintentar = { reintentos++ })

        compose.onNodeWithText(textos.getString(R.string.error_sin_conexion)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_reintentar)).performClick()

        assertEquals(1, reintentos)
    }
}
