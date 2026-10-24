package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.viajeDePrueba
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

/** «Asignar conductor y vehículo» (HU05, HU23): opciones, elección, envío y errores. */
@OptIn(ExperimentalCoroutinesApi::class)
class AsignarViewModelTest {

    private val zona = ZoneId.of("America/Santiago")
    private val ahora = LocalDateTime.of(2026, 10, 28, 12, 0)
    private val reloj = Clock.fixed(ahora.atZone(zona).toInstant(), zona)
    private val viajes = ViajeObservadoFalso()
    private val flota = FlotaFalsa()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear() = AsignarViewModel(viajes, flota, reloj)

    private fun viaje(
        folio: String = "TR-1051",
        estado: EstadoViaje = EstadoViaje.PENDIENTE,
        minutosDesdeAhora: Long = 1440,
        pasajeros: Int = 2,
        correoConductor: String? = null,
        patente: String? = null,
    ) = viajeDePrueba(
        fhAgenda = ahora.plusMinutes(minutosDesdeAhora), folio = folio, estado = estado,
        cantidadPasajeros = pasajeros, correoConductor = correoConductor, patenteMovil = patente,
    )

    private fun conductor(
        correo: String,
        estado: EstadoConductor = EstadoConductor.DISPONIBLE,
        activo: Boolean = true,
        asignados: Int = 0,
        enCurso: String? = null,
    ) = Conductor("Nombre de $correo", correo, null, ahora.minusDays(30), activo, estado, asignados, enCurso)

    private fun habilitacion(
        patente: String,
        capacidad: Int = 4,
        estado: EstadoMovil = EstadoMovil.DISPONIBLE,
        activo: Boolean = true,
    ) = Habilitacion(patente, capacidad, estado, activo, "", "", EstadoConductor.DISPONIBLE, true)

    private fun escenarioBase() {
        viajes.viaje.value = viaje()
        flota.conductores = listOf(
            conductor("ivan@ejemplo.cl", asignados = 1),
            conductor("paula@ejemplo.cl"),
            conductor("jorge@ejemplo.cl", EstadoConductor.OCUPADO, enCurso = "TR-1045"),
            conductor("inactivo@ejemplo.cl", activo = false),
        )
        flota.habilitaciones = mapOf(
            "ivan@ejemplo.cl" to listOf(
                habilitacion("KXJR42", capacidad = 4),
                habilitacion("LPHT17", capacidad = 6),
                habilitacion("CHICO1", capacidad = 1),
                habilitacion("ENUSO1", estado = EstadoMovil.EN_USO),
                habilitacion("BAJA11", activo = false),
            ),
            "paula@ejemplo.cl" to listOf(habilitacion("PAUL01")),
        )
    }

    private fun listoParaConfirmar(): AsignarViewModel {
        escenarioBase()
        val vm = crear()
        vm.cargar("TR-1051")
        vm.elegirConductor("ivan@ejemplo.cl")
        vm.elegirVehiculo("KXJR42")
        return vm
    }

    @Test
    fun cargaElViajeYSoloLosConductoresActivos() {
        escenarioBase()
        val vm = crear()

        vm.cargar("TR-1051")

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("TR-1051", estado.viaje?.folio)
        assertEquals(listOf("ivan@ejemplo.cl", "paula@ejemplo.cl", "jorge@ejemplo.cl"), estado.conductores.map { it.correo })
        assertNull(estado.noAsignable)
    }

    @Test
    fun losOcupadosEstanDeshabilitadosYMuestranSuViajeEnCurso() {
        escenarioBase()
        val vm = crear()
        vm.cargar("TR-1051")

        val opciones = vm.estado.value.conductores.associateBy { it.correo }

        assertTrue(opciones.getValue("ivan@ejemplo.cl").habilitado)
        assertEquals(1, opciones.getValue("ivan@ejemplo.cl").viajesAsignados)
        assertFalse(opciones.getValue("jorge@ejemplo.cl").habilitado)
        assertEquals("TR-1045", opciones.getValue("jorge@ejemplo.cl").folioEnCurso)

        vm.elegirConductor("jorge@ejemplo.cl")

        assertNull(vm.estado.value.conductorElegido)
    }

    @Test
    fun elVehiculoSeEligeEntreLosHabilitadosActivosDisponiblesYConCapacidad() {
        escenarioBase()
        val vm = crear()
        vm.cargar("TR-1051")

        vm.elegirConductor("ivan@ejemplo.cl")

        assertEquals(listOf("ivan@ejemplo.cl"), flota.consultasDeHabilitaciones)
        // Dos pasajeros: sin el de capacidad 1, ni el que está En uso, ni el inactivo.
        assertEquals(listOf("KXJR42", "LPHT17"), vm.estado.value.vehiculos.map { it.patente })
        assertFalse(vm.estado.value.cargandoVehiculos)
    }

    @Test
    fun alCambiarDeConductorSeLimpiaElVehiculo() {
        val vm = listoParaConfirmar()
        assertEquals("KXJR42", vm.estado.value.vehiculoElegido)

        vm.elegirConductor("paula@ejemplo.cl")

        assertNull(vm.estado.value.vehiculoElegido)
        assertEquals(listOf("PAUL01"), vm.estado.value.vehiculos.map { it.patente })
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun elegirUnVehiculoQueNoEstaEnLaListaSeIgnora() {
        escenarioBase()
        val vm = crear()
        vm.cargar("TR-1051")
        vm.elegirConductor("ivan@ejemplo.cl")

        vm.elegirVehiculo("CHICO1")

        assertNull(vm.estado.value.vehiculoElegido)
    }

    @Test
    fun alReasignarElConductorYElVehiculoActualesSiguenSiendoElegiblesAunqueEstenEnUso() {
        escenarioBase()
        viajes.viaje.value = viaje(estado = EstadoViaje.ASIGNADO, correoConductor = "jorge@ejemplo.cl", patente = "ENUSO1")
        flota.habilitaciones = mapOf("jorge@ejemplo.cl" to listOf(habilitacion("ENUSO1", capacidad = 1, estado = EstadoMovil.EN_USO)))
        val vm = crear()
        vm.cargar("TR-1051")

        assertTrue(vm.estado.value.conductores.single { it.correo == "jorge@ejemplo.cl" }.habilitado)
        vm.elegirConductor("jorge@ejemplo.cl")

        assertEquals(listOf("ENUSO1"), vm.estado.value.vehiculos.map { it.patente })
    }

    /** `TR-1051` ya asignado a Iván en `KXJR42`: el punto de partida de «Reasignar» (HU23). */
    private fun reasignando(): AsignarViewModel {
        escenarioBase()
        viajes.viaje.value = viaje(estado = EstadoViaje.ASIGNADO, correoConductor = "ivan@ejemplo.cl", patente = "KXJR42")
        return crear().also { it.cargar("TR-1051") }
    }

    @Test
    fun reasignarUnViajeAsignadoAOtroConductorYVehiculoLoEnviaYAvisaQueTermino() {
        val vm = reasignando()
        assertNull(vm.estado.value.noAsignable)

        vm.elegirConductor("paula@ejemplo.cl")
        vm.elegirVehiculo("PAUL01")
        vm.confirmar()

        assertEquals(listOf(Triple("TR-1051", "paula@ejemplo.cl", "PAUL01")), viajes.asignaciones)
        assertTrue(vm.estado.value.asignado)
        assertFalse(vm.estado.value.enviando)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun reasignarSoloElVehiculoConservaAlConductorActual() {
        val vm = reasignando()

        vm.elegirConductor("ivan@ejemplo.cl")
        vm.elegirVehiculo("LPHT17")
        vm.confirmar()

        assertEquals(listOf(Triple("TR-1051", "ivan@ejemplo.cl", "LPHT17")), viajes.asignaciones)
        assertTrue(vm.estado.value.asignado)
    }

    @Test
    fun alReasignarUnConductorOcupadoQueNoEsElActualSigueDeshabilitado() {
        val vm = reasignando()

        val opciones = vm.estado.value.conductores.associate { it.correo to it.habilitado }

        assertEquals(true, opciones["ivan@ejemplo.cl"])
        assertEquals(true, opciones["paula@ejemplo.cl"])
        assertEquals(false, opciones["jorge@ejemplo.cl"])
        vm.elegirConductor("jorge@ejemplo.cl")
        assertNull(vm.estado.value.conductorElegido)
    }

    @Test
    fun unConflictoAlReasignarConservaLaEleccionYElViajeSigueAsignado() {
        val vm = reasignando()
        vm.elegirConductor("paula@ejemplo.cl")
        vm.elegirVehiculo("PAUL01")
        viajes.respuestaAsignar = { Resultado.Fallo(ErrorApp.Conflicto("El conductor ya tiene un viaje a menos de 2 horas")) }

        vm.confirmar()

        assertEquals(ErrorApp.Conflicto("El conductor ya tiene un viaje a menos de 2 horas"), vm.estado.value.error)
        assertFalse(vm.estado.value.asignado)
        assertEquals("paula@ejemplo.cl", vm.estado.value.conductorElegido)
        assertEquals("PAUL01", vm.estado.value.vehiculoElegido)
        assertEquals(EstadoViaje.ASIGNADO, vm.estado.value.viaje?.estado)
        assertEquals("ivan@ejemplo.cl", vm.estado.value.viaje?.correoConductor)
    }

    @Test
    fun unViajeVencidoNoSeAsigna() {
        viajes.viaje.value = viaje(minutosDesdeAhora = -61)
        flota.conductores = listOf(conductor("ivan@ejemplo.cl"))
        val vm = crear()

        vm.cargar("TR-1051")

        assertEquals(NoAsignable.YA_NO_SE_PUEDE, vm.estado.value.noAsignable)
        vm.elegirConductor("ivan@ejemplo.cl")
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun unViajeAtrasadoPeroNoVencidoSiSeAsigna() {
        viajes.viaje.value = viaje(minutosDesdeAhora = -30)

        val vm = crear().also { it.cargar("TR-1051") }

        assertNull(vm.estado.value.noAsignable)
    }

    @Test
    fun unViajeSinPasajerosNoSeAsigna() {
        viajes.viaje.value = viaje(pasajeros = 0)

        val vm = crear().also { it.cargar("TR-1051") }

        assertEquals(NoAsignable.SIN_PASAJEROS, vm.estado.value.noAsignable)
    }

    @Test
    fun unViajeQueYaEstaEnCursoNoSeAsigna() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)

        val vm = crear().also { it.cargar("TR-1051") }

        assertEquals(NoAsignable.YA_NO_SE_PUEDE, vm.estado.value.noAsignable)
    }

    @Test
    fun soloSePuedeConfirmarConConductorYVehiculo() {
        escenarioBase()
        val vm = crear().also { it.cargar("TR-1051") }
        assertFalse(vm.estado.value.puedeConfirmar)

        vm.elegirConductor("ivan@ejemplo.cl")
        assertFalse(vm.estado.value.puedeConfirmar)
        vm.confirmar()
        assertTrue(viajes.asignaciones.isEmpty())

        vm.elegirVehiculo("KXJR42")
        assertTrue(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun confirmarAsignaYAvisaQueTermino() {
        val vm = listoParaConfirmar()

        vm.confirmar()

        assertEquals(listOf(Triple("TR-1051", "ivan@ejemplo.cl", "KXJR42")), viajes.asignaciones)
        assertTrue(vm.estado.value.asignado)
        assertFalse(vm.estado.value.enviando)
        assertNull(vm.estado.value.error)
        assertFalse(vm.estado.value.modificado)
    }

    @Test
    fun unSegundoToqueMientrasEnviaNoDuplicaLaAsignacion() {
        val vm = listoParaConfirmar()
        val respuesta = CompletableDeferred<Resultado<Unit>>()
        viajes.respuestaAsignar = { respuesta.await() }

        vm.confirmar()
        assertTrue(vm.estado.value.enviando)
        assertFalse(vm.estado.value.puedeConfirmar)
        vm.confirmar()
        vm.elegirVehiculo("LPHT17")
        vm.elegirConductor("paula@ejemplo.cl")
        respuesta.complete(Resultado.Exito(Unit))

        assertEquals(1, viajes.asignaciones.size)
        assertEquals("KXJR42", vm.estado.value.vehiculoElegido)
        assertTrue(vm.estado.value.asignado)
    }

    @Test
    fun unConflictoMuestraElMensajeDelServidorYRecargaConservandoLaEleccion() {
        val vm = listoParaConfirmar()
        val lecturasDeConductores = flota.lecturasDeConductores
        val refrescos = viajes.refrescos
        viajes.respuestaAsignar = { Resultado.Fallo(ErrorApp.Conflicto("El conductor ya tiene un viaje a menos de 2 horas")) }

        vm.confirmar()

        val error = vm.estado.value.error
        assertEquals(ErrorApp.Conflicto("El conductor ya tiene un viaje a menos de 2 horas"), error)
        assertFalse(vm.estado.value.asignado)
        assertFalse(vm.estado.value.enviando)
        assertTrue(flota.lecturasDeConductores > lecturasDeConductores)
        assertTrue(viajes.refrescos > refrescos)
        assertEquals("ivan@ejemplo.cl", vm.estado.value.conductorElegido)
        assertEquals("KXJR42", vm.estado.value.vehiculoElegido)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun trasUnConflictoElVehiculoQueYaNoSirveSeQuitaDeLaEleccion() {
        val vm = listoParaConfirmar()
        viajes.respuestaAsignar = {
            flota.habilitaciones = mapOf("ivan@ejemplo.cl" to listOf(habilitacion("KXJR42", estado = EstadoMovil.EN_USO), habilitacion("LPHT17")))
            Resultado.Fallo(ErrorApp.Conflicto("El móvil no está disponible"))
        }

        vm.confirmar()

        assertEquals(listOf("LPHT17"), vm.estado.value.vehiculos.map { it.patente })
        assertNull(vm.estado.value.vehiculoElegido)
    }

    @Test
    fun unFalloDeConexionMuestraElErrorYSePuedeVolverAConfirmar() {
        val vm = listoParaConfirmar()
        viajes.respuestaAsignar = { Resultado.Fallo(ErrorApp.SinConexion) }

        vm.confirmar()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertFalse(vm.estado.value.asignado)
        assertTrue(vm.estado.value.puedeConfirmar)

        viajes.respuestaAsignar = { Resultado.Exito(Unit) }
        vm.confirmar()

        assertTrue(vm.estado.value.asignado)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun hayCambiosSinGuardarSoloConUnaEleccionPendiente() {
        escenarioBase()
        val vm = crear().also { it.cargar("TR-1051") }
        assertFalse(vm.estado.value.modificado)

        vm.elegirConductor("paula@ejemplo.cl")

        assertTrue(vm.estado.value.modificado)
    }

    @Test
    fun cargarElMismoFolioDosVecesNoVuelveAPedirNada() {
        escenarioBase()
        val vm = crear().also { it.cargar("TR-1051") }
        vm.elegirConductor("ivan@ejemplo.cl")
        val refrescos = viajes.refrescos

        vm.cargar("TR-1051")

        assertEquals(refrescos, viajes.refrescos)
        assertEquals("ivan@ejemplo.cl", vm.estado.value.conductorElegido)
    }

    @Test
    fun cargarOtroFolioEmpiezaDeNuevo() {
        escenarioBase()
        val vm = crear().also { it.cargar("TR-1051") }
        vm.elegirConductor("ivan@ejemplo.cl")

        viajes.viaje.value = viaje(folio = "TR-1053")
        vm.cargar("TR-1053")

        assertEquals("TR-1053", vm.estado.value.viaje?.folio)
        assertNull(vm.estado.value.conductorElegido)
    }

    @Test
    fun unViajeInexistenteMuestraNoEncontrado() {
        viajes.viaje.value = null

        val vm = crear().also { it.cargar("TR-9999") }

        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun unFalloAlLeerLosConductoresMuestraElErrorYReintentarLoRecupera() {
        escenarioBase()
        flota.falloConductores = ErrorApp.SinConexion
        val vm = crear().also { it.cargar("TR-1051") }

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)

        flota.falloConductores = null
        vm.reintentar()

        assertNull(vm.estado.value.error)
        assertEquals(3, vm.estado.value.conductores.size)
        assertEquals("TR-1051", vm.estado.value.viaje?.folio)
    }

    @Test
    fun unFalloAlLeerLosVehiculosSeAvisaYNoQuedaCargando() {
        escenarioBase()
        flota.falloHabilitaciones = ErrorApp.Inesperado
        val vm = crear().also { it.cargar("TR-1051") }

        vm.elegirConductor("ivan@ejemplo.cl")

        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargandoVehiculos)
        assertTrue(vm.estado.value.vehiculos.isEmpty())
    }
}

/** Un solo viaje observable; guarda lo que le piden y responde `asignar` con lo que la prueba diga. */
private class ViajeObservadoFalso : ViajeRepository {
    val viaje = MutableStateFlow<Viaje?>(null)
    val asignaciones = mutableListOf<Triple<String, String, String>>()
    var refrescos = 0
    var respuestaAsignar: suspend () -> Resultado<Unit> = { Resultado.Exito(Unit) }

    override fun observarViaje(folio: String): Flow<Viaje?> = viaje

    override suspend fun refrescarViaje(folio: String): Resultado<Unit> {
        refrescos++
        return Resultado.Exito(Unit)
    }

    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> {
        asignaciones += Triple(folio, correoConductor, patente)
        return respuestaAsignar()
    }

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = noSeUsa()
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = noSeUsa()
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = noSeUsa()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = noSeUsa()
    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = noSeUsa()
    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = noSeUsa()
    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = noSeUsa()
}

private class FlotaFalsa : FlotaRepository {
    var conductores: List<Conductor> = emptyList()
    var habilitaciones: Map<String, List<Habilitacion>> = emptyMap()
    var falloConductores: ErrorApp? = null
    var falloHabilitaciones: ErrorApp? = null
    var lecturasDeConductores = 0
    val consultasDeHabilitaciones = mutableListOf<String?>()

    override suspend fun listarConductores(): Resultado<List<Conductor>> {
        lecturasDeConductores++
        return falloConductores?.let { Resultado.Fallo(it) } ?: Resultado.Exito(conductores)
    }

    override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> {
        consultasDeHabilitaciones += correo
        return falloHabilitaciones?.let { Resultado.Fallo(it) } ?: Resultado.Exito(habilitaciones[correo].orEmpty())
    }

    override suspend fun obtenerConductor(correo: String): Resultado<Conductor> = noSeUsa()
    override suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> = noSeUsa()
    override suspend fun actualizarConductor(correo: String, nombre: String, telefono: String?, password: String?): Resultado<Conductor> = noSeUsa()
    override suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> = noSeUsa()
    override suspend fun listarMoviles(): Resultado<List<Movil>> = noSeUsa()
    override suspend fun obtenerMovil(patente: String): Resultado<Movil> = noSeUsa()
    override suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = noSeUsa()
    override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = noSeUsa()
    override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> = noSeUsa()
}

private fun noSeUsa(): Nothing = throw UnsupportedOperationException("Esta prueba no usa esa operación")
