package cl.belmontespa.myapplication.viewmodel.solicitante

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.lugares.ComponenteDeLugar
import cl.belmontespa.myapplication.lugares.LugarCrudo
import cl.belmontespa.myapplication.lugares.ResultadoDeBusqueda
import cl.belmontespa.myapplication.lugares.ServicioDeLugaresFalso
import cl.belmontespa.myapplication.lugares.aUbicacion
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
    private val aeropuerto = LugarCrudo(
        "Aeropuerto Arturo Merino Benítez", listOf("airport", "establishment"), null,
        listOf(
            ComponenteDeLugar(listOf("locality"), "Pudahuel"),
            ComponenteDeLugar(listOf("administrative_area_level_1"), "Región Metropolitana de Santiago"),
        ),
        -33.393, -70.7858,
    )
    private val mirador = LugarCrudo("Mirador sin comuna", listOf("point_of_interest"), null, emptyList(), -33.01, -70.02)
    private lateinit var repos: ReposP1Falsos
    private lateinit var servicio: ServicioDeLugaresFalso
    private lateinit var vm: NuevaSolicitudViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        repos = ReposP1Falsos()
        servicio = ServicioDeLugaresFalso().apply { lugares["aeropuerto"] = aeropuerto; lugares["mirador"] = mirador }
        vm = NuevaSolicitudViewModel(aplicacionFalsa(), repos.viajes, clock, lugares, servicio, repos.catalogos)
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

    private fun pasajero() {
        vm.escribirPasajeroNombre("Invitado"); vm.elegirTipoDocumento("PASAPORTE"); vm.escribirDocumento("DEMO123")
        vm.agregarPasajero()
    }

    @Test fun laBusquedaDeLugaresSeOfreceSoloConElServicioDisponible() {
        assertTrue(vm.estado.value.conBusqueda)
        val sinBusqueda = NuevaSolicitudViewModel(
            aplicacionFalsa(), repos.viajes, clock, lugares, ServicioDeLugaresFalso(disponible = false), repos.catalogos,
        )
        assertFalse(sinBusqueda.estado.value.conBusqueda)
    }

    @Test fun elegirUnLugarFijaElCampoYGuardarEnviaLaUbicacionDeGoogle() = runTest(dispatcher) {
        vm.elegirLugar(true, "aeropuerto"); vm.elegirLugar(false, "mirador"); runCurrent()
        assertEquals(aeropuerto.nombre, vm.estado.value.origen); assertEquals(mirador.nombre, vm.estado.value.destino)
        vm.corregirComuna(false, "Metropolitana", "Las Condes"); pasajero()
        assertTrue(vm.estado.value.puedeGuardar)
        vm.guardar(); runCurrent()
        assertEquals("TR-2000", vm.estado.value.creada)
        assertEquals(aeropuerto.aUbicacion(), repos.solicitud!!.origen)
        assertEquals(mirador.aUbicacion()!!.copy(comuna = "Las Condes", region = "Metropolitana"), repos.solicitud!!.destino)
    }

    @Test fun unLugarSinComunaPideElSelectorYBloqueaElGuardado() = runTest(dispatcher) {
        pasajero(); vm.elegirLugar(true, "mirador"); vm.elegirDestino("Destino"); runCurrent()
        assertTrue(vm.estado.value.origenSinComuna); assertFalse(vm.estado.value.destinoSinComuna)
        assertFalse(vm.estado.value.puedeGuardar)
        assertEquals(listOf("Metropolitana"), vm.estado.value.regiones.map { it.nombre })
        vm.guardar(); runCurrent()
        assertEquals(0, repos.envios)
    }

    @Test fun corregirLaComunaConservaLaDireccionYLasCoordenadas() = runTest(dispatcher) {
        pasajero(); vm.elegirLugar(true, "mirador"); vm.elegirDestino("Destino"); runCurrent()
        vm.corregirComuna(true, "Metropolitana", "Valdivia")
        assertTrue(vm.estado.value.origenSinComuna)
        vm.corregirComuna(true, "Metropolitana", "Providencia")
        assertEquals(Ubicacion("Mirador sin comuna", "Providencia", "Metropolitana", -33.01, -70.02), vm.estado.value.origenLugar)
        assertTrue(vm.estado.value.puedeGuardar)
    }

    @Test fun unRechazoDeComunaDelServidorReactivaElSelectorDeEseExtremo() = runTest(dispatcher) {
        pasajero(); vm.elegirLugar(true, "aeropuerto"); vm.elegirLugar(false, "aeropuerto"); runCurrent()
        repos.fallo = ErrorApp.Validacion(mapOf("destino.comuna" to "No existe en el catálogo."))
        vm.guardar(); runCurrent()
        assertTrue(vm.estado.value.destinoSinComuna); assertFalse(vm.estado.value.origenSinComuna)
        assertFalse(vm.estado.value.puedeGuardar)
        assertEquals(1, vm.estado.value.regiones.size)
        repos.fallo = null; vm.corregirComuna(false, "Metropolitana", "Las Condes")
        vm.guardar(); runCurrent()
        assertEquals("TR-2000", vm.estado.value.creada)
        assertEquals("Las Condes", repos.solicitud!!.destino.comuna); assertEquals("Pudahuel", repos.solicitud!!.origen.comuna)
    }

    @Test fun unFalloAlObtenerElLugarNoCambiaElCampoYAvisa() = runTest(dispatcher) {
        vm.elegirOrigen("Origen")
        servicio.fallo = ErrorApp.SinConexion
        vm.elegirLugar(true, "aeropuerto"); runCurrent()
        assertEquals("Origen", vm.estado.value.origen); assertEquals(lugares[0], vm.estado.value.origenLugar)
        assertEquals(R.string.aviso_lugar_no_disponible, vm.estado.value.aviso)
    }

    @Test fun unLugarSinTextoUtilNoSeAcepta() = runTest(dispatcher) {
        servicio.lugares["vacio"] = LugarCrudo(null, emptyList(), null, emptyList(), -33.0, -70.0)
        vm.elegirLugar(true, "vacio"); runCurrent()
        assertEquals("", vm.estado.value.origen); assertNull(vm.estado.value.origenLugar)
        assertEquals(R.string.aviso_lugar_no_disponible, vm.estado.value.aviso)
    }

    @Test fun elResultadoDeLaBusquedaSoloConsultaCuandoHayUnaEleccion() = runTest(dispatcher) {
        servicio.resultado = ResultadoDeBusqueda.Cancelado
        vm.resultadoDeBusqueda(true, 0, null); runCurrent()
        assertTrue(servicio.consultas.isEmpty()); assertNull(vm.estado.value.aviso)
        servicio.resultado = ResultadoDeBusqueda.Fallido
        vm.resultadoDeBusqueda(true, 2, null); runCurrent()
        assertTrue(servicio.consultas.isEmpty()); assertEquals(R.string.aviso_lugar_no_disponible, vm.estado.value.aviso)
        servicio.resultado = ResultadoDeBusqueda.Elegido("aeropuerto")
        vm.resultadoDeBusqueda(false, -1, null); runCurrent()
        assertEquals(listOf("aeropuerto"), servicio.consultas); assertEquals(aeropuerto.nombre, vm.estado.value.destino)
        assertNull(vm.estado.value.origenLugar)
    }

    @Test fun sinCatalogoElSelectorDeComunaPermiteReintentar() = runTest(dispatcher) {
        repos.errorCatalogos = ErrorApp.SinConexion
        vm.elegirLugar(true, "mirador"); runCurrent()
        assertTrue(vm.estado.value.regiones.isEmpty()); assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        repos.errorCatalogos = null
        vm.cargarRegiones(); runCurrent()
        assertEquals(1, vm.estado.value.regiones.size)
    }
}
