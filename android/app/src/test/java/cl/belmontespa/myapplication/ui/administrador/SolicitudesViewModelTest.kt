package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.SavedStateHandle
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.viajeDePrueba
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** «Solicitudes» del Administrador (HU04): filtro, retraso, «Asignar» directo y errores. */
@OptIn(ExperimentalCoroutinesApi::class)
class SolicitudesViewModelTest {

    private val zona = ZoneId.of("America/Santiago")
    private val reloj = RelojMovil(LocalDateTime.of(2026, 10, 28, 12, 0), zona)
    private val viajes = ViajesFalso()
    private val cuentas = CuentasFalso()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear(guardado: SavedStateHandle = SavedStateHandle()) =
        SolicitudesViewModel(viajes, cuentas, reloj, guardado)

    private fun viaje(folio: String, estado: EstadoViaje, minutosDesdeAhora: Long, pasajeros: Int = 2) =
        viajeDePrueba(
            fhAgenda = reloj.ahora.plusMinutes(minutosDesdeAhora),
            folio = folio,
            estado = estado,
            cantidadPasajeros = pasajeros,
        )

    @Test
    fun empiezaEnPendientesYPideSoloLasPendientes() {
        val vm = crear()

        assertEquals(FiltroSolicitudes.PENDIENTES, vm.estado.value.filtro)
        assertEquals(FiltroViajes(estado = EstadoViaje.PENDIENTE), viajes.observados.last())
        assertEquals(listOf(FiltroViajes(estado = EstadoViaje.PENDIENTE)), viajes.refrescados)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun cadaFiltroPideSuEstadoYTodasNoPideNinguno() {
        val vm = crear()

        FiltroSolicitudes.entries.forEach { filtro ->
            vm.elegirFiltro(filtro)
            assertEquals(filtro, vm.estado.value.filtro)
            assertEquals(FiltroViajes(estado = filtro.estado), viajes.observados.last())
        }
        assertNull(FiltroSolicitudes.TODAS.estado)
    }

    @Test
    fun volverAElegirElMismoFiltroNoRepiteLaConsulta() {
        val vm = crear()
        val consultas = viajes.observados.size

        vm.elegirFiltro(FiltroSolicitudes.PENDIENTES)

        assertEquals(consultas, viajes.observados.size)
    }

    @Test
    fun soloLasPendientesVigentesConPasajerosOfrecenAsignar() {
        viajes.todos.value = listOf(
            viaje("TR-1038", EstadoViaje.PENDIENTE, -180), // vencida
            viaje("TR-1047", EstadoViaje.PENDIENTE, -30), // atrasada, aún asignable
            viaje("TR-1051", EstadoViaje.PENDIENTE, 1440),
            viaje("TR-1055", EstadoViaje.PENDIENTE, 2880, pasajeros = 0),
            viaje("TR-1042", EstadoViaje.ASIGNADO, 30),
            viaje("TR-1045", EstadoViaje.EN_CURSO, -20),
        )
        val vm = crear()
        vm.elegirFiltro(FiltroSolicitudes.TODAS)

        val puedeAsignar = vm.estado.value.filas.associate { it.viaje.folio to it.puedeAsignar }

        assertEquals(
            mapOf(
                "TR-1038" to false,
                "TR-1047" to true,
                "TR-1051" to true,
                "TR-1055" to false,
                "TR-1042" to false,
                "TR-1045" to false,
            ),
            puedeAsignar,
        )
    }

    @Test
    fun cadaFilaLlevaSuRetrasoSoloSiEstaPendienteOAsignado() {
        viajes.todos.value = listOf(
            viaje("TR-1038", EstadoViaje.PENDIENTE, -180),
            viaje("TR-1047", EstadoViaje.PENDIENTE, -30),
            viaje("TR-1051", EstadoViaje.PENDIENTE, 1440),
            viaje("TR-1040", EstadoViaje.ASIGNADO, -10),
            viaje("TR-1045", EstadoViaje.EN_CURSO, -200),
        )
        val vm = crear()
        vm.elegirFiltro(FiltroSolicitudes.TODAS)

        val retrasos = vm.estado.value.filas.associate { it.viaje.folio to it.retraso }

        assertEquals(
            mapOf(
                "TR-1038" to Retraso.VENCIDO,
                "TR-1047" to Retraso.ATRASADO,
                "TR-1051" to null,
                "TR-1040" to Retraso.ATRASADO,
                "TR-1045" to null,
            ),
            retrasos,
        )
    }

    @Test
    fun alVolverAPrimerPlanoSeRecalculaElRetraso() {
        viajes.todos.value = listOf(viaje("TR-1047", EstadoViaje.PENDIENTE, -30))
        val vm = crear()
        assertEquals(Retraso.ATRASADO, vm.estado.value.filas.single().retraso)
        assertTrue(vm.estado.value.filas.single().puedeAsignar)

        reloj.ahora = reloj.ahora.plusMinutes(40)
        // Sin volver a primer plano, la pantalla conserva lo último que calculó.
        assertEquals(Retraso.ATRASADO, vm.estado.value.filas.single().retraso)
        vm.alVolverAPrimerPlano()

        assertEquals(Retraso.VENCIDO, vm.estado.value.filas.single().retraso)
        assertFalse(vm.estado.value.filas.single().puedeAsignar)
    }

    @Test
    fun cuentaLasCuentasPorAprobarYLaActualiza() {
        cuentas.pendientes = listOf(solicitante("a@ejemplo.cl"), solicitante("b@ejemplo.cl"))
        val vm = crear()
        assertEquals(2, vm.estado.value.cuentasPorAprobar)

        cuentas.pendientes = cuentas.pendientes + solicitante("c@ejemplo.cl")
        vm.actualizar()

        assertEquals(3, vm.estado.value.cuentasPorAprobar)
    }

    @Test
    fun unFalloAlActualizarMuestraElErrorYReintentarLoLimpia() {
        val vm = crear()
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.SinConexion)

        vm.actualizar()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)

        viajes.resultadoRefresco = Resultado.Exito(Unit)
        vm.actualizar()

        assertNull(vm.estado.value.error)
    }

    @Test
    fun unFalloAlAbrirTambienSeMuestra() {
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.Inesperado)

        val vm = crear()

        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun unFalloAlLeerLaListaMuestraUnErrorInesperado() {
        viajes.falla = true

        val vm = crear()

        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun reintentarRecuperaUnaListaQueNoSePudoLeer() {
        viajes.todos.value = listOf(viaje("TR-1047", EstadoViaje.PENDIENTE, -30))
        viajes.falla = true
        val vm = crear()
        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)

        viajes.falla = false
        vm.actualizar()

        assertNull(vm.estado.value.error)
        assertEquals(listOf("TR-1047"), vm.estado.value.filas.map { it.viaje.folio })
    }

    @Test
    fun elFiltroSobreviveALaMuerteDelProceso() {
        val guardado = SavedStateHandle()
        crear(guardado).elegirFiltro(FiltroSolicitudes.ASIGNADO)

        val restaurado = crear(guardado)

        assertEquals(FiltroSolicitudes.ASIGNADO, restaurado.estado.value.filtro)
        assertEquals(FiltroViajes(estado = EstadoViaje.ASIGNADO), viajes.observados.last())
    }

    @Test
    fun unFiltroGuardadoDesconocidoVuelveAPendientes() {
        val vm = crear(SavedStateHandle(mapOf("filtro" to "OTRO")))

        assertEquals(FiltroSolicitudes.PENDIENTES, vm.estado.value.filtro)
    }

    private fun solicitante(correo: String) = Solicitante(
        nombre = "Rodrigo Pérez",
        correo = correo,
        telefono = null,
        fechaRegistro = reloj.ahora.minusDays(1),
        razonSocial = "Andes Consultores SpA",
        rutEmpresa = "76543210-3",
        estado = EstadoSolicitante.PENDIENTE,
    )
}

/** Reloj que se puede mover, para recalcular el retraso sin esperar. */
private class RelojMovil(var ahora: LocalDateTime, private val zona: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zona
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = ahora.atZone(zona).toInstant()
}

/** Guarda lo que le piden y devuelve los viajes de `todos` que calzan con el estado del filtro. */
private class ViajesFalso : ViajeRepository {
    val todos = MutableStateFlow<List<Viaje>>(emptyList())
    val observados = mutableListOf<FiltroViajes>()
    val refrescados = mutableListOf<FiltroViajes>()
    var resultadoRefresco: Resultado<Unit> = Resultado.Exito(Unit)
    var falla = false

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> {
        observados += filtro
        if (falla) return flow { throw IOException("sin acceso a los datos") }
        return todos.map { lista -> lista.filter { filtro.estado == null || it.estado == filtro.estado } }
    }

    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> {
        refrescados += filtro
        return resultadoRefresco
    }

    override fun observarViaje(folio: String): Flow<Viaje?> = noSeUsa()
    override suspend fun refrescarViaje(folio: String): Resultado<Unit> = noSeUsa()
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = noSeUsa()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = noSeUsa()
    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = noSeUsa()
    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = noSeUsa()
    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = noSeUsa()
    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = noSeUsa()
}

private class CuentasFalso : CuentaRepository {
    var pendientes: List<Solicitante> = emptyList()

    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = Resultado.Exito(pendientes)

    override suspend fun obtenerPerfil(): Resultado<Cuenta> = noSeUsa()
    override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = noSeUsa()
    override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> = noSeUsa()
    override suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante> = noSeUsa()
    override suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante> = noSeUsa()
    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = noSeUsa()
}

private fun noSeUsa(): Nothing = throw UnsupportedOperationException("Esta prueba no usa esa operación")
