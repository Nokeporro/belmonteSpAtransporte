package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Solicitudes» (HU04) con un estado fijo: lo que se ve y lo que se avisa, sin Room ni ViewModel. */
class SolicitudesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val ahora = LocalDateTime.of(2026, 10, 28, 12, 0)

    private fun viaje(folio: String, estado: EstadoViaje, agenda: LocalDateTime, pasajeros: Int = 2): Viaje {
        val lugar = Ubicacion("Av. Providencia 1234", "Providencia", "Metropolitana de Santiago", -33.4261, -70.611)
        return Viaje(
            folio = folio, estado = estado, correoSolicitante = "solicitante@ejemplo.cl",
            nombreSolicitante = "Marcela Soto", telefonoSolicitante = null, razonSocial = "Empresa",
            origen = lugar, destino = lugar, fhCreacion = agenda.minusDays(1), fhAgenda = agenda,
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = null,
            cantidadPasajeros = pasajeros, correoConductor = null, nombreConductor = null, patenteMovil = null,
            hashPago = null, estadoPago = null, motivo = null, pasajeros = emptyList(),
        )
    }

    private fun mostrar(
        estado: SolicitudesUiState,
        onFiltro: (FiltroSolicitudes) -> Unit = {},
        onActualizar: () -> Unit = {},
        onAsignar: (Viaje) -> Unit = {},
    ) {
        compose.setContent {
            BelmonteTheme {
                SolicitudesContenido(
                    estado = estado,
                    onFiltro = onFiltro,
                    onActualizar = onActualizar,
                    onCuentasPorAprobar = {},
                    onVerDetalle = {},
                    onAsignar = onAsignar,
                )
            }
        }
    }

    private val filasDelDia = listOf(
        SolicitudFila(viaje("TR-1038", EstadoViaje.PENDIENTE, ahora.minusHours(3)), Retraso.VENCIDO, puedeAsignar = false),
        SolicitudFila(viaje("TR-1047", EstadoViaje.PENDIENTE, ahora.minusMinutes(30)), Retraso.ATRASADO, puedeAsignar = true),
        SolicitudFila(viaje("TR-1051", EstadoViaje.PENDIENTE, ahora.plusDays(1)), retraso = null, puedeAsignar = true),
    )

    @Test
    fun abreEnPendientesYMuestraLaCantidadDeCuentasPorAprobar() {
        mostrar(SolicitudesUiState(filas = filasDelDia, cuentasPorAprobar = 3, cargando = false))

        compose.onNodeWithText(textos.getString(R.string.filtro_pendientes)).assertIsSelected()
        compose.onNodeWithText(textos.getString(R.string.cuentas_pendientes_cantidad, 3)).assertExists()
        compose.onNodeWithText("TR-1051").assertExists()
    }

    @Test
    fun asignarSoloEstaEnLasFilasQueLoPermiten() {
        val pedidos = mutableListOf<String>()
        mostrar(
            SolicitudesUiState(filas = filasDelDia, cargando = false),
            onAsignar = { pedidos += it.folio },
        )

        compose.onAllNodesWithText(textos.getString(R.string.accion_asignar)).assertCountEquals(2)
        compose.onAllNodesWithText(textos.getString(R.string.accion_asignar))[0].performClick()
        assertEquals(listOf("TR-1047"), pedidos)
    }

    @Test
    fun elRetrasoSeMuestraComoChip() {
        mostrar(SolicitudesUiState(filas = filasDelDia, cargando = false))

        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, textos.getString(R.string.etiqueta_vencido)))
            .assertCountEquals(1)
        compose.onAllNodesWithContentDescription(textos.getString(R.string.chip_descripcion, textos.getString(R.string.etiqueta_atrasado)))
            .assertCountEquals(1)
    }

    @Test
    fun sinSolicitudesPendientesLoDiceYSigueOfreciendoLosFiltros() {
        mostrar(SolicitudesUiState(filas = emptyList(), cargando = false))

        compose.onNodeWithText(textos.getString(R.string.vacio_solicitudes_pendientes)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.filtro_todas)).assertExists()
    }

    @Test
    fun elegirUnFiltroLoAvisa() {
        val elegidos = mutableListOf<FiltroSolicitudes>()
        mostrar(
            SolicitudesUiState(filas = filasDelDia, cargando = false),
            onFiltro = { elegidos += it },
        )

        compose.onNodeWithText(textos.getString(R.string.filtro_todas)).performClick()

        assertEquals(listOf(FiltroSolicitudes.TODAS), elegidos)
    }

    @Test
    fun unErrorSinFilasOfreceReintentar() {
        var reintentos = 0
        mostrar(
            SolicitudesUiState(cargando = false, error = ErrorApp.SinConexion),
            onActualizar = { reintentos++ },
        )

        compose.onNodeWithText(textos.getString(R.string.error_sin_conexion)).assertExists()
        compose.onNodeWithText(textos.getString(R.string.accion_reintentar)).performClick()

        assertEquals(1, reintentos)
    }
}
