package cl.belmontespa.myapplication.ui.pantallas

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.repository.*
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.auth.LoginViewModel
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class P1ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), ZoneId.of("America/Santiago"))
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: PantallaBaseViewModel
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = PantallaBaseViewModel(DependenciasPantallas(repos, repos, repos, repos, repos, repos, clock, emptyList()))
    }
    @After fun cerrar() { Dispatchers.resetMain() }
    private suspend fun TestScope.formulario() {
        vm.cargar(PantallaDemo.NUEVA_SOLICITUD, ""); runCurrent()
        vm.seleccionarLugar("origen", Ubicacion("Origen", "Providencia", "Metropolitana", -33.42, -70.61))
        vm.seleccionarLugar("destino", Ubicacion("Destino", "Las Condes", "Metropolitana", -33.41, -70.60))
        vm.campo("pasajeroNombre", "Invitado"); vm.campo("tipoDocumento", "PASAPORTE"); vm.campo("documento", "DEMO123")
        vm.agregarPasajero()
    }
    @Test fun corregirComunaConservaLaDireccionYLasCoordenadasDePlaces() = runTest(dispatcher) {
        vm.cargar(PantallaDemo.NUEVA_SOLICITUD, ""); runCurrent()
        val lugar = Ubicacion("Dirección de Places", "Comuna desconocida", "Región de prueba", -33.42, -70.61)
        vm.seleccionarLugar("origen", lugar)
        assertEquals("", vm.estado.value.ubicaciones.getValue("origen").comuna)
        vm.corregirComuna("origen", "Providencia")
        assertEquals(lugar.copy(comuna = "Providencia", region = "Metropolitana"), vm.estado.value.ubicaciones["origen"])
    }
    @Test fun unDobleToqueNoEnviaDosSolicitudes() = runTest(dispatcher) {
        formulario()
        repos.espera = CompletableDeferred()
        vm.ejecutar("guardar"); vm.ejecutar("guardar"); runCurrent()
        assertEquals(1, repos.envios)
        assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals(PantallaDemo.DETALLE_SOLICITUD to "TR-2000", vm.estado.value.destino)
        assertEquals("Invitado", repos.solicitud!!.pasajeros.single().nombre)
    }
    @Test fun elFalloConservaElFormularioYPermiteReintentar() = runTest(dispatcher) {
        formulario(); repos.fallo = ErrorApp.SinConexion
        vm.ejecutar("guardar"); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertEquals(1, vm.estado.value.pasajeros.size)
        assertEquals("Origen", vm.estado.value.ubicaciones["origen"]?.direccion)
        assertFalse(vm.estado.value.enviando); assertNull(vm.estado.value.destino)
        repos.fallo = null; vm.ejecutar("guardar"); runCurrent()
        assertEquals(PantallaDemo.DETALLE_SOLICITUD to "TR-2000", vm.estado.value.destino)
    }
    @Test fun elDocumentoInvalidoSeMuestraEnSuCampo() = runTest(dispatcher) {
        vm.campo("pasajeroNombre", "Pasajero"); vm.campo("tipoDocumento", "RUT"); vm.campo("documento", "12345678-4")
        vm.agregarPasajero()
        assertTrue(vm.estado.value.pasajeros.isEmpty())
        assertTrue("documento" in (vm.estado.value.error as ErrorApp.Validacion).errores)
    }
    @Test fun elLoginConservaElErrorGenericoYDistingueLaCuentaPendiente() = runTest(dispatcher) {
        val login = LoginViewModel(repos)
        login.cambiarCorreo("cuenta@test.cl"); login.cambiarPassword("Clave12345")
        repos.fallo = ErrorApp.SesionExpirada; login.iniciarSesion(); runCurrent()
        assertEquals(R.string.error_credenciales, login.estado.value.error)
        repos.fallo = ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE); login.iniciarSesion(); runCurrent()
        assertEquals(R.string.error_cuenta_pendiente, login.estado.value.error)
        repos.fallo = null; login.iniciarSesion(); runCurrent()
        assertEquals("", login.estado.value.password)
    }
}

/** Fakes manuales: una operación inesperada falla sin acceder a Room, red ni Android. */
private class ReposP1Falsos : ViajeRepository, CuentaRepository, FlotaRepository, PagoRepository,
    AuthRepository, CatalogoRepository {
    var espera: CompletableDeferred<Unit>? = null
    var fallo: ErrorApp? = null
    var envios = 0
    var solicitud: NuevaSolicitud? = null
    private fun sinUso(): Nothing = error("Operación no prevista en esta prueba")
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> {
        envios++; this.solicitud = solicitud; espera?.await()
        return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito("TR-2000")
    }
    override suspend fun obtenerPerfil() = Resultado.Exito(Cuenta("Cuenta", "cuenta@test.cl", null, Rol.SOLICITANTE, LocalDateTime.of(2026, 10, 22, 12, 0)))
    override suspend fun obtenerCatalogos() = Resultado.Exito(Catalogos(emptyList(), emptyList(), emptyList(), listOf(Region("Metropolitana", listOf("Providencia", "Las Condes")))))
    override suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> = fallo?.let { Resultado.Fallo(it) }
        ?: Resultado.Exito(EstadoSesion.Autenticada(Sesion("demo", correo, Rol.SOLICITANTE, Long.MAX_VALUE)))
    override fun observarSesion(): Flow<EstadoSesion> = flowOf(EstadoSesion.SinSesion)
    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = flowOf(emptyList())
    override fun observarViaje(folio: String): Flow<Viaje?> = flowOf(null)
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = sinUso()
    override suspend fun refrescarViaje(folio: String): Resultado<Unit> = sinUso()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = sinUso()
    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = sinUso()
    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = sinUso()
    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = sinUso()
    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = sinUso()
    override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = sinUso()
    override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> = sinUso()
    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = sinUso()
    override suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante> = sinUso()
    override suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante> = sinUso()
    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = sinUso()
    override suspend fun listarConductores(): Resultado<List<Conductor>> = sinUso()
    override suspend fun obtenerConductor(correo: String): Resultado<Conductor> = sinUso()
    override suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> = sinUso()
    override suspend fun actualizarConductor(correo: String, nombre: String, telefono: String?, password: String?): Resultado<Conductor> = sinUso()
    override suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> = sinUso()
    override suspend fun listarMoviles(): Resultado<List<Movil>> = sinUso()
    override suspend fun obtenerMovil(patente: String): Resultado<Movil> = sinUso()
    override suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = sinUso()
    override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = sinUso()
    override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> = sinUso()
    override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> = sinUso()
    override suspend fun obtenerPago(hash: String): Resultado<Pago> = sinUso()
    override suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = sinUso()
    override suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> = sinUso()
    override suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> = sinUso()
    override suspend fun recuperarPassword(correo: String): Resultado<Unit> = sinUso()
    override suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = sinUso()
    override suspend fun cerrarSesion(): Resultado<Unit> = sinUso()
}
