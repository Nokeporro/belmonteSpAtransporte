package cl.belmontespa.myapplication.ui.conductor

import androidx.lifecycle.SavedStateHandle
import cl.belmontespa.myapplication.RelojMovil
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.viajeDePrueba
import java.io.IOException
import java.time.LocalDate
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

/** «Mis viajes» del Conductor (HU07): la agenda del día, el selector de fecha, el retraso y los errores. */
@OptIn(ExperimentalCoroutinesApi::class)
class MisViajesViewModelTest {

    private val zona = ZoneId.of("America/Santiago")
    private val reloj = RelojMovil(LocalDateTime.of(2026, 10, 28, 12, 0), zona)
    private val hoy = LocalDate.of(2026, 10, 28)
    private val viajes = AgendaDelConductorFalsa()
    private val cuentas = PerfilFalso()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear(guardado: SavedStateHandle = SavedStateHandle()) =
        MisViajesViewModel(viajes, cuentas, reloj, guardado)

    private fun viaje(folio: String, estado: EstadoViaje, minutosDesdeAhora: Long, pasajeros: Int = 1) =
        viajeDePrueba(
            fhAgenda = reloj.ahora.plusMinutes(minutosDesdeAhora),
            folio = folio,
            estado = estado,
            cantidadPasajeros = pasajeros,
        )

    @Test
    fun abreEnHoyYPideLaAgendaDeHoy() {
        val vm = crear()

        val estado = vm.estado.value
        assertEquals(hoy, estado.fecha)
        assertTrue(estado.esHoy)
        assertFalse(estado.cargando)
        assertEquals(FiltroViajes(fecha = hoy), viajes.observados.last())
        assertEquals(listOf(FiltroViajes(fecha = hoy)), viajes.refrescados)
    }

    @Test
    fun elegirOtraFechaPideEsaAgendaYLaRecuerda() {
        val guardado = SavedStateHandle()
        val vm = crear(guardado)
        val otra = hoy.plusDays(2)

        vm.elegirFecha(otra)

        assertEquals(otra, vm.estado.value.fecha)
        assertFalse(vm.estado.value.esHoy)
        assertEquals(FiltroViajes(fecha = otra), viajes.observados.last())
        assertEquals(FiltroViajes(fecha = otra), viajes.refrescados.last())
        assertEquals("2026-10-30", guardado.get<String>("fecha"))
    }

    @Test
    fun elegirLaFechaDeHoyVuelveASeguirAlRelojYNoSeRecuerda() {
        val guardado = SavedStateHandle()
        val vm = crear(guardado)
        vm.elegirFecha(hoy.plusDays(2))

        vm.elegirFecha(hoy)

        assertTrue(vm.estado.value.esHoy)
        assertFalse(guardado.contains("fecha"))
        assertEquals(FiltroViajes(fecha = hoy), viajes.observados.last())
    }

    @Test
    fun elegirLaFechaQueYaSeMuestraNoRepiteLaConsulta() {
        val vm = crear()
        vm.elegirFecha(hoy.plusDays(2))
        val consultas = viajes.observados.size
        val refrescos = viajes.refrescados.size

        vm.elegirFecha(hoy.plusDays(2))
        vm.elegirFecha(hoy.plusDays(2))

        assertEquals(consultas, viajes.observados.size)
        assertEquals(refrescos, viajes.refrescados.size)
    }

    @Test
    fun laFechaElegidaSobreviveALaMuerteDelProceso() {
        val guardado = SavedStateHandle()
        crear(guardado).elegirFecha(hoy.plusDays(3))

        val restaurado = crear(guardado)

        assertEquals(hoy.plusDays(3), restaurado.estado.value.fecha)
        assertEquals(FiltroViajes(fecha = hoy.plusDays(3)), viajes.observados.last())
    }

    @Test
    fun unaFechaGuardadaQueNoEsFechaVuelveAHoy() {
        val vm = crear(SavedStateHandle(mapOf("fecha" to "ayer")))

        assertEquals(hoy, vm.estado.value.fecha)
        assertTrue(vm.estado.value.esHoy)
    }

    @Test
    fun pasadaLaMedianocheLaAgendaDeHoyEsLaDelDiaNuevo() {
        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.ASIGNADO, 30))
        val vm = crear()
        assertEquals(listOf("TR-1042"), vm.estado.value.filas.map { it.viaje.folio })

        reloj.ahora = LocalDateTime.of(2026, 10, 29, 0, 30)
        vm.alVolverAPrimerPlano()

        assertEquals(LocalDate.of(2026, 10, 29), vm.estado.value.fecha)
        assertEquals(LocalDate.of(2026, 10, 29), vm.estado.value.hoy)
        assertTrue(vm.estado.value.esHoy)
        assertEquals(FiltroViajes(fecha = LocalDate.of(2026, 10, 29)), viajes.observados.last())
        assertEquals(FiltroViajes(fecha = LocalDate.of(2026, 10, 29)), viajes.refrescados.last())
        assertTrue(vm.estado.value.filas.isEmpty())
    }

    @Test
    fun unaFechaElegidaNoSeMueveConLaMedianoche() {
        val vm = crear()
        vm.elegirFecha(hoy.plusDays(5))

        reloj.ahora = LocalDateTime.of(2026, 10, 29, 0, 30)
        vm.alVolverAPrimerPlano()

        assertEquals(hoy.plusDays(5), vm.estado.value.fecha)
        assertEquals(LocalDate.of(2026, 10, 29), vm.estado.value.hoy)
        assertFalse(vm.estado.value.esHoy)
    }

    @Test
    fun unViajeQueLlegaConLaPantallaAbiertaApareceSinRecargar() {
        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.ASIGNADO, 30))
        val vm = crear()
        val refrescos = viajes.refrescados.size

        // El Administrador asigna otro viaje al Conductor.
        viajes.todos.value = viajes.todos.value + viaje("TR-1049", EstadoViaje.ASIGNADO, 255)

        assertEquals(listOf("TR-1042", "TR-1049"), vm.estado.value.filas.map { it.viaje.folio })
        assertEquals(refrescos, viajes.refrescados.size)
    }

    @Test
    fun unViajeQueCambiaDeEstadoSeActualizaEnSuFila() {
        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.ASIGNADO, 30))
        val vm = crear()

        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.EN_CURSO, 30))

        assertEquals(EstadoViaje.EN_CURSO, vm.estado.value.filas.single().viaje.estado)
    }

    @Test
    fun soloUnViajeAsignadoLlevaRetraso() {
        viajes.todos.value = listOf(
            viaje("TR-1", EstadoViaje.ASIGNADO, -90),
            viaje("TR-2", EstadoViaje.ASIGNADO, -30),
            viaje("TR-3", EstadoViaje.ASIGNADO, 120),
            viaje("TR-4", EstadoViaje.EN_CURSO, -200),
            viaje("TR-5", EstadoViaje.FINALIZADO, -300),
        )
        val vm = crear()

        val retrasos = vm.estado.value.filas.associate { it.viaje.folio to it.retraso }

        assertEquals(
            mapOf(
                "TR-1" to Retraso.VENCIDO,
                "TR-2" to Retraso.ATRASADO,
                "TR-3" to null,
                "TR-4" to null,
                "TR-5" to null,
            ),
            retrasos,
        )
    }

    @Test
    fun laVentanaDeInicioAbreExactamenteUnaHoraAntes() {
        viajes.todos.value = listOf(
            viaje("TR-1", EstadoViaje.ASIGNADO, 61),
            viaje("TR-2", EstadoViaje.ASIGNADO, 60),
            viaje("TR-3", EstadoViaje.ASIGNADO, -10),
            viaje("TR-4", EstadoViaje.EN_CURSO, 120),
            viaje("TR-5", EstadoViaje.FINALIZADO, 180),
        )
        val vm = crear()

        val antes = vm.estado.value.filas.associate { it.viaje.folio to it.antesDeLaVentana }

        assertEquals(
            mapOf("TR-1" to true, "TR-2" to false, "TR-3" to false, "TR-4" to false, "TR-5" to false),
            antes,
        )
    }

    @Test
    fun alVolverAPrimerPlanoSeRecalculanElRetrasoYLaVentana() {
        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.ASIGNADO, 30))
        val vm = crear()
        assertNull(vm.estado.value.filas.single().retraso)

        reloj.ahora = reloj.ahora.plusMinutes(40)
        // Sin volver a primer plano, la pantalla conserva lo último que calculó.
        assertNull(vm.estado.value.filas.single().retraso)
        vm.alVolverAPrimerPlano()

        assertEquals(Retraso.ATRASADO, vm.estado.value.filas.single().retraso)
        assertFalse(vm.estado.value.filas.single().antesDeLaVentana)
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
    fun unFalloAlActualizarNoBorraLaAgendaYaCargada() {
        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.ASIGNADO, 30))
        val vm = crear()
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.SinConexion)

        vm.actualizar()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertEquals(listOf("TR-1042"), vm.estado.value.filas.map { it.viaje.folio })
    }

    @Test
    fun unFalloAlAbrirTambienSeMuestra() {
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.Inesperado)

        val vm = crear()

        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun unFalloAlLeerLaAgendaMuestraUnErrorInesperadoYReintentarLaRecupera() {
        viajes.todos.value = listOf(viaje("TR-1042", EstadoViaje.ASIGNADO, 30))
        viajes.falla = true
        val vm = crear()
        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)

        viajes.falla = false
        vm.actualizar()

        assertNull(vm.estado.value.error)
        assertEquals(listOf("TR-1042"), vm.estado.value.filas.map { it.viaje.folio })
    }

    @Test
    fun elSubtituloLlevaElNombreDelConductor() {
        cuentas.perfil = Resultado.Exito(cuenta("Iván Reyes"))

        val vm = crear()

        assertEquals("Iván Reyes", vm.estado.value.conductor)
    }

    @Test
    fun siNoSePuedeLeerElPerfilNoHayNombreNiError() {
        cuentas.perfil = Resultado.Fallo(ErrorApp.SinConexion)

        val vm = crear()

        assertNull(vm.estado.value.conductor)
        assertNull(vm.estado.value.error)
    }

    private fun cuenta(nombre: String) =
        Cuenta(nombre, "ireyes@ejemplo.cl", null, Rol.CONDUCTOR, reloj.ahora.minusDays(30))
}

/** Los viajes de `todos` que caen en la fecha del filtro; guarda lo que le piden. */
private class AgendaDelConductorFalsa : ViajeRepository {
    val todos = MutableStateFlow<List<Viaje>>(emptyList())
    val observados = mutableListOf<FiltroViajes>()
    val refrescados = mutableListOf<FiltroViajes>()
    var resultadoRefresco: Resultado<Unit> = Resultado.Exito(Unit)
    var falla = false

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> {
        observados += filtro
        if (falla) return flow { throw IOException("sin acceso a los datos") }
        return todos.map { lista -> lista.filter { filtro.fecha == null || it.fhAgenda.toLocalDate() == filtro.fecha } }
    }

    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> {
        refrescados += filtro
        return resultadoRefresco
    }

    override fun observarViaje(folio: String): Flow<Viaje?> = sinUso()
    override suspend fun refrescarViaje(folio: String): Resultado<Unit> = sinUso()
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = sinUso()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = sinUso()
    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = sinUso()
    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = sinUso()
    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = sinUso()
    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = sinUso()
}

private class PerfilFalso : CuentaRepository {
    var perfil: Resultado<Cuenta> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun obtenerPerfil(): Resultado<Cuenta> = perfil

    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = sinUso()
    override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = sinUso()
    override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> = sinUso()
    override suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante> = sinUso()
    override suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante> = sinUso()
    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = sinUso()
}

private fun sinUso(): Nothing = throw UnsupportedOperationException("Esta prueba no usa esa operación")
