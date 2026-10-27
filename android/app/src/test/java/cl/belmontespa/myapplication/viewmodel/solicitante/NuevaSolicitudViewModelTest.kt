package cl.belmontespa.myapplication.viewmodel.solicitante

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.viewmodel.ReposP1Falsos
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaSolicitudViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), ZoneId.of("America/Santiago"))
    private val lugares = listOf(
        Ubicacion("Origen", "Providencia", "Metropolitana", -33.42, -70.61),
        Ubicacion("Destino", "Las Condes", "Metropolitana", -33.41, -70.60),
    )
    private lateinit var repos: ReposP1Falsos
    private lateinit var vm: NuevaSolicitudViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        vm = NuevaSolicitudViewModel(aplicacionFalsa(), repos.viajes, clock, lugares)
    }

    @After fun cerrar() { Dispatchers.resetMain() }

    private fun formulario() {
        vm.elegirOrigen("Origen"); vm.elegirDestino("Destino")
        vm.escribirPasajeroNombre("Invitado"); vm.elegirTipoDocumento("PASAPORTE"); vm.escribirDocumento("DEMO123")
        vm.agregarPasajero()
    }

    @Test fun proponeLaHoraTresHorasDespuesYOfreceLosLugaresFicticios() {
        assertEquals(LocalDateTime.of(2026, 10, 22, 15, 0), LocalDateTime.of(vm.estado.value.fecha, vm.estado.value.hora))
        assertEquals(lugares, vm.estado.value.lugares)
        assertFalse(vm.estado.value.modificado); assertFalse(vm.estado.value.puedeGuardar)
    }

    @Test fun elegirOrigenYDestinoSoloPermiteGuardarConUnPasajero() {
        vm.elegirOrigen("Origen"); vm.elegirDestino("Destino")
        assertFalse(vm.estado.value.puedeGuardar)
        formulario()
        assertTrue(vm.estado.value.puedeGuardar); assertTrue(vm.estado.value.modificado)
    }

    @Test fun unDobleToqueNoEnviaDosSolicitudes() = runTest(dispatcher) {
        formulario(); repos.espera = CompletableDeferred()
        vm.guardar(); vm.guardar(); runCurrent()
        assertEquals(1, repos.envios); assertTrue(vm.estado.value.enviando)
        repos.espera!!.complete(Unit); runCurrent()
        assertEquals("TR-2000", vm.estado.value.creada)
        assertEquals("Invitado", repos.solicitud!!.pasajeros.single().nombre)
        assertEquals(lugares[0], repos.solicitud!!.origen); assertEquals(lugares[1], repos.solicitud!!.destino)
        assertNull(repos.solicitud!!.numeroVuelo)
        vm.consumirCreada(); assertNull(vm.estado.value.creada)
    }

    @Test fun elFalloConservaElFormularioYPermiteReintentar() = runTest(dispatcher) {
        formulario(); repos.fallo = ErrorApp.SinConexion
        vm.guardar(); runCurrent()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertEquals(1, vm.estado.value.pasajeros.size); assertEquals("Origen", vm.estado.value.origen)
        assertFalse(vm.estado.value.enviando); assertNull(vm.estado.value.creada)
        repos.fallo = null; vm.guardar(); runCurrent()
        assertEquals("TR-2000", vm.estado.value.creada)
    }

    @Test fun sinOrigenODestinoNoSeEnvia() = runTest(dispatcher) {
        formulario(); vm.elegirOrigen("")
        vm.guardar(); runCurrent()
        assertEquals(setOf(CAMPO_ORIGEN), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        vm.elegirOrigen("Origen"); vm.elegirDestino("Lugar que no está en la lista")
        vm.guardar(); runCurrent()
        assertEquals(setOf(CAMPO_DESTINO), (vm.estado.value.error as ErrorApp.Validacion).errores.keys)
        assertEquals(0, repos.envios); assertTrue(vm.estado.value.modificado)
    }

    @Test fun elDocumentoInvalidoSeMuestraEnSuCampo() {
        vm.escribirPasajeroNombre("Pasajero"); vm.elegirTipoDocumento(TipoDocumento.RUT.name); vm.escribirDocumento("12345678-4")
        vm.agregarPasajero()
        assertTrue(vm.estado.value.pasajeros.isEmpty())
        assertTrue(CAMPO_DOCUMENTO in (vm.estado.value.error as ErrorApp.Validacion).errores)
    }

    @Test fun faltarElNombreOElDocumentoAvisaYNoAgrega() {
        vm.escribirPasajeroNombre("Pasajero"); vm.agregarPasajero()
        assertTrue(vm.estado.value.pasajeros.isEmpty())
        assertEquals(R.string.aviso_campos_obligatorios, vm.estado.value.aviso)
    }

    @Test fun noAgregaDosVecesElMismoDocumento() {
        formulario()
        vm.escribirPasajeroNombre("Otro"); vm.escribirDocumento("demo123"); vm.agregarPasajero()
        assertEquals(1, vm.estado.value.pasajeros.size)
        assertTrue(CAMPO_DOCUMENTO in (vm.estado.value.error as ErrorApp.Validacion).errores)
    }

    @Test fun editarUnPasajeroLoDevuelveALosCamposYLoQuitaDeLaLista() {
        formulario()
        vm.editarPasajero(0)
        assertTrue(vm.estado.value.pasajeros.isEmpty())
        assertEquals("Invitado", vm.estado.value.pasajeroNombre); assertEquals("DEMO123", vm.estado.value.documento)
        assertEquals(TipoDocumento.PASAPORTE, vm.estado.value.tipoDocumento)
    }
}
