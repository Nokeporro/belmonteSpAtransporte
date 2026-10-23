package cl.belmontespa.myapplication.ui.conductor

import cl.belmontespa.myapplication.RelojMovil
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.pasajeroDePrueba
import cl.belmontespa.myapplication.model.viajeDePrueba
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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

/** Confirmación de «Iniciar viaje» (HU09, V3): aviso de abordaje, ventana, envío único y errores del servidor. */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmarInicioViewModelTest {

    private val zona = ZoneId.of("America/Santiago")
    private val reloj = RelojMovil(LocalDateTime.of(2026, 10, 28, 12, 0), zona)
    private val viajes = ViajeParaIniciarFalso()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear(folio: String = "TR-1042") = ConfirmarInicioViewModel(viajes, reloj).also { it.cargar(folio) }

    private fun viaje(
        estado: EstadoViaje = EstadoViaje.ASIGNADO,
        minutosDesdeAhora: Long = 30,
        pasajeros: List<PasajeroViaje> = listOf(pasajeroDePrueba("Ana")),
    ) = viajeDePrueba(
        fhAgenda = reloj.ahora.plusMinutes(minutosDesdeAhora),
        folio = "TR-1042",
        estado = estado,
        cantidadPasajeros = pasajeros.size,
        correoConductor = "ireyes@ejemplo.cl",
        patenteMovil = "KXJR42",
        pasajeros = pasajeros,
    )

    @Test
    fun cargaElViajeYPideSuRefresco() {
        viajes.viaje.value = viaje()

        val estado = crear().estado.value

        assertFalse(estado.cargando)
        assertEquals("TR-1042", estado.viaje?.folio)
        assertNull(estado.error)
        assertEquals(listOf("TR-1042"), viajes.refrescados)
    }

    @Test
    fun cargarElMismoFolioDosVecesNoVuelveAPedirNada() {
        viajes.viaje.value = viaje()
        val vm = crear()

        vm.cargar("TR-1042")

        assertEquals(1, viajes.refrescados.size)
    }

    @Test
    fun unViajeAjenoOReasignadoSeVeComoNoEncontrado() {
        viajes.viaje.value = viaje()
        val vm = crear()

        viajes.viaje.value = null

        assertNull(vm.estado.value.viaje)
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
        assertFalse(vm.estado.value.puedeIniciar)
    }

    @Test
    fun siElViajeVuelveAAparecerElNoEncontradoSeQuita() {
        viajes.viaje.value = null
        val vm = crear()
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)

        viajes.viaje.value = viaje()

        assertNull(vm.estado.value.error)
        assertTrue(vm.estado.value.puedeIniciar)
    }

    @Test
    fun avisaCuantosPasajerosFaltanPeroNoImpideIniciar() {
        viajes.viaje.value = viaje(
            pasajeros = listOf(
                pasajeroDePrueba("Ana", reloj.ahora.minusMinutes(5)),
                pasajeroDePrueba("Beto"),
                pasajeroDePrueba("Carla"),
            ),
        )

        val estado = crear().estado.value

        assertEquals(2, estado.faltan)
        assertTrue(estado.puedeIniciar)
    }

    @Test
    fun sinPasajerosPorAbordarNoHayAviso() {
        viajes.viaje.value = viaje(pasajeros = listOf(pasajeroDePrueba("Ana", reloj.ahora.minusMinutes(5))))
        assertEquals(0, crear().estado.value.faltan)

        viajes.viaje.value = viaje(pasajeros = emptyList())
        assertEquals(0, ConfirmarInicioViewModel(viajes, reloj).also { it.cargar("TR-1042") }.estado.value.faltan)
    }

    @Test
    fun laVentanaDeInicioAbreUnaHoraAntesYCierraUnaHoraDespuesConAmbosBordesIncluidos() {
        fun inicio(minutos: Long): Inicio {
            viajes.viaje.value = viaje(minutosDesdeAhora = minutos)
            return ConfirmarInicioViewModel(viajes, reloj).also { it.cargar("TR-1042") }.estado.value.inicio
        }

        assertEquals(Inicio.ANTES_DE_LA_VENTANA, inicio(61))
        assertEquals(Inicio.HABILITADO, inicio(60))
        assertEquals(Inicio.HABILITADO, inicio(0))
        assertEquals(Inicio.HABILITADO, inicio(-60))
        assertEquals(Inicio.VENCIDO, inicio(-61))
    }

    @Test
    fun antesDeLaVentanaNoSeInicia() {
        viajes.viaje.value = viaje(minutosDesdeAhora = 360)

        val vm = crear()
        vm.confirmar()

        assertFalse(vm.estado.value.puedeIniciar)
        assertEquals(reloj.ahora.plusMinutes(300), vm.estado.value.disponibleDesde)
        assertTrue(viajes.estados.isEmpty())
    }

    @Test
    fun vencidoNoSeIniciaNiHayPistaDeHora() {
        viajes.viaje.value = viaje(minutosDesdeAhora = -120)

        val vm = crear()
        vm.confirmar()

        assertEquals(Inicio.VENCIDO, vm.estado.value.inicio)
        assertFalse(vm.estado.value.puedeIniciar)
        assertNull(vm.estado.value.disponibleDesde)
        assertTrue(viajes.estados.isEmpty())
    }

    @Test
    fun unViajeQueNoEstaAsignadoNoSeInicia() {
        for (estadoViaje in listOf(EstadoViaje.PENDIENTE, EstadoViaje.EN_CURSO, EstadoViaje.FINALIZADO, EstadoViaje.CANCELADO)) {
            viajes.viaje.value = viaje(estado = estadoViaje, minutosDesdeAhora = 10)
            val vm = ConfirmarInicioViewModel(viajes, reloj).also { it.cargar("TR-1042") }

            vm.confirmar()

            assertEquals(estadoViaje.name, Inicio.NO_APLICA, vm.estado.value.inicio)
            assertFalse(estadoViaje.name, vm.estado.value.puedeIniciar)
        }
        assertTrue(viajes.estados.isEmpty())
    }

    @Test
    fun confirmarPasaElViajeAEnCursoYAvisaQueTermino() {
        viajes.viaje.value = viaje()
        val vm = crear()

        vm.confirmar()

        assertEquals(listOf("TR-1042" to EstadoViaje.EN_CURSO), viajes.estados)
        assertTrue(vm.estado.value.iniciado)
        assertFalse(vm.estado.value.enviando)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun unSegundoToqueMientrasIniciaNoDuplicaElEnvio() {
        viajes.viaje.value = viaje()
        val vm = crear()
        val respuesta = CompletableDeferred<Resultado<Unit>>()
        viajes.respuestaEstado = { respuesta.await() }

        vm.confirmar()
        assertTrue(vm.estado.value.enviando)
        assertFalse(vm.estado.value.puedeIniciar)
        vm.confirmar()
        respuesta.complete(Resultado.Exito(Unit))

        assertEquals(1, viajes.estados.size)
        assertTrue(vm.estado.value.iniciado)
    }

    @Test
    fun unaVezIniciadoNoSeVuelveAEnviar() {
        viajes.viaje.value = viaje()
        val vm = crear()

        vm.confirmar()
        vm.confirmar()

        assertEquals(1, viajes.estados.size)
    }

    @Test
    fun unConflictoMuestraElMensajeDelServidorYVuelveAPedirElViaje() {
        viajes.viaje.value = viaje()
        val vm = crear()
        val refrescos = viajes.refrescados.size
        viajes.respuestaEstado = { Resultado.Fallo(ErrorApp.Conflicto("El conductor ya tiene un viaje en curso")) }

        vm.confirmar()

        assertEquals(ErrorApp.Conflicto("El conductor ya tiene un viaje en curso"), vm.estado.value.error)
        assertFalse(vm.estado.value.iniciado)
        assertFalse(vm.estado.value.enviando)
        assertTrue(viajes.refrescados.size > refrescos)
    }

    @Test
    fun unFalloDeConexionMuestraElErrorSinRecargarYPermiteReintentar() {
        viajes.viaje.value = viaje()
        val vm = crear()
        val refrescos = viajes.refrescados.size
        viajes.respuestaEstado = { Resultado.Fallo(ErrorApp.SinConexion) }

        vm.confirmar()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertEquals(refrescos, viajes.refrescados.size)
        assertTrue(vm.estado.value.puedeIniciar)

        viajes.respuestaEstado = { Resultado.Exito(Unit) }
        vm.confirmar()

        assertTrue(vm.estado.value.iniciado)
        assertNull(vm.estado.value.error)
        assertEquals(2, viajes.estados.size)
    }

    @Test
    fun unCambioEnColaNoSeMuestraComoIniciadoNiComoError() {
        viajes.viaje.value = viaje()
        val vm = crear()
        viajes.respuestaEstado = { Resultado.Encolado }

        vm.confirmar()

        assertFalse(vm.estado.value.iniciado)
        assertFalse(vm.estado.value.enviando)
        assertNull(vm.estado.value.error)
        assertEquals(EstadoViaje.ASIGNADO, vm.estado.value.viaje?.estado)
    }

    @Test
    fun alVolverAPrimerPlanoLaVentanaSeRecalculaConElReloj() {
        viajes.viaje.value = viaje(minutosDesdeAhora = 90)
        val vm = crear()
        assertEquals(Inicio.ANTES_DE_LA_VENTANA, vm.estado.value.inicio)

        reloj.ahora = reloj.ahora.plusMinutes(30)
        vm.alVolverAPrimerPlano()

        assertEquals(Inicio.HABILITADO, vm.estado.value.inicio)
        assertTrue(vm.estado.value.puedeIniciar)
    }

    @Test
    fun siElViajeYaEmpezoPorOtroLadoElBotonSeDeshabilita() {
        viajes.viaje.value = viaje()
        val vm = crear()
        assertTrue(vm.estado.value.puedeIniciar)

        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)

        assertFalse(vm.estado.value.puedeIniciar)
    }

    @Test
    fun reintentarLimpiaElErrorYVuelveAPedirElViaje() {
        viajes.viaje.value = viaje()
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.SinConexion)
        val vm = crear()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)

        viajes.resultadoRefresco = Resultado.Exito(Unit)
        vm.actualizar()

        assertNull(vm.estado.value.error)
        assertEquals(2, viajes.refrescados.size)
    }

    @Test
    fun unFalloAlLeerElViajeEsUnErrorInesperadoQueReintentarRecupera() {
        viajes.viaje.value = viaje()
        viajes.falla = true
        val vm = crear()
        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)

        viajes.falla = false
        vm.actualizar()

        assertNull(vm.estado.value.error)
        assertEquals("TR-1042", vm.estado.value.viaje?.folio)
    }
}

/** Un solo viaje observable; guarda los cambios de estado que le piden y responde con lo que la prueba diga. */
private class ViajeParaIniciarFalso : ViajeRepository {
    val viaje = MutableStateFlow<Viaje?>(null)
    val refrescados = mutableListOf<String>()
    val estados = mutableListOf<Pair<String, EstadoViaje>>()
    var resultadoRefresco: Resultado<Unit> = Resultado.Exito(Unit)
    var respuestaEstado: suspend () -> Resultado<Unit> = { Resultado.Exito(Unit) }
    var falla = false

    override fun observarViaje(folio: String): Flow<Viaje?> {
        if (falla) return flow { throw IOException("sin acceso a los datos") }
        return viaje
    }

    override suspend fun refrescarViaje(folio: String): Resultado<Unit> {
        refrescados += folio
        return resultadoRefresco
    }

    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> {
        estados += folio to estado
        return respuestaEstado()
    }

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = ningunUso()
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = ningunUso()
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = ningunUso()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = ningunUso()
    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = ningunUso()
    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = ningunUso()
    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = ningunUso()
}

private fun ningunUso(): Nothing = throw UnsupportedOperationException("Esta prueba no usa esa operación")
