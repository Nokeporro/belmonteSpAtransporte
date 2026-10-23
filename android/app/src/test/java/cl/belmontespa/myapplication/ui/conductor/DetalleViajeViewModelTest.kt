package cl.belmontespa.myapplication.ui.conductor

import cl.belmontespa.myapplication.RelojMovil
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
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

/** Detalle del viaje del Conductor (HU08): datos, arqueo, ventana de inicio y controles de estado. */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViajeViewModelTest {

    private val zona = ZoneId.of("America/Santiago")
    private val reloj = RelojMovil(LocalDateTime.of(2026, 10, 28, 12, 0), zona)
    private val viajes = DetalleDeViajeFalso()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear(folio: String = "TR-1042") = DetalleViajeViewModel(viajes, reloj).also { it.cargar(folio) }

    private fun viaje(
        estado: EstadoViaje = EstadoViaje.ASIGNADO,
        minutosDesdeAhora: Long = 30,
        folio: String = "TR-1042",
        vararg pasajeros: String,
    ) = viajeDePrueba(
        fhAgenda = reloj.ahora.plusMinutes(minutosDesdeAhora),
        folio = folio,
        estado = estado,
        cantidadPasajeros = pasajeros.size.coerceAtLeast(1),
        correoConductor = "ireyes@ejemplo.cl",
        patenteMovil = "KXJR42",
        pasajeros = pasajeros.map { pasajeroDePrueba(it) },
    )

    @Test
    fun cargaElViajeYPideSuRefresco() {
        viajes.viaje.value = viaje()

        val vm = crear()

        val estado = vm.estado.value
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
    fun unViajeQueNoEsDelConductorSeVeComoNoEncontrado() {
        viajes.viaje.value = null

        val vm = crear("TR-9999")

        assertNull(vm.estado.value.viaje)
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun siElAdministradorLoReasignaConLaPantallaAbiertaDejaDeVerse() {
        viajes.viaje.value = viaje()
        val vm = crear()
        assertNull(vm.estado.value.error)

        viajes.viaje.value = null

        assertNull(vm.estado.value.viaje)
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
        assertFalse(vm.estado.value.conControles)
    }

    @Test
    fun siElViajeVuelveAAparecerElNoEncontradoSeQuita() {
        viajes.viaje.value = null
        val vm = crear()
        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)

        viajes.viaje.value = viaje()

        assertNull(vm.estado.value.error)
        assertEquals("TR-1042", vm.estado.value.viaje?.folio)
    }

    @Test
    fun laVentanaDeInicioAbreUnaHoraAntesYCierraUnaHoraDespuesConAmbosBordesIncluidos() {
        fun inicio(minutos: Long): Inicio {
            viajes.viaje.value = viaje(minutosDesdeAhora = minutos)
            return DetalleViajeViewModel(viajes, reloj).also { it.cargar("TR-1042") }.estado.value.inicio
        }

        assertEquals(Inicio.ANTES_DE_LA_VENTANA, inicio(61))
        assertEquals(Inicio.HABILITADO, inicio(60))
        assertEquals(Inicio.HABILITADO, inicio(0))
        assertEquals(Inicio.HABILITADO, inicio(-60))
        assertEquals(Inicio.VENCIDO, inicio(-61))
    }

    @Test
    fun antesDeLaVentanaDiceDesdeQueHoraSeHabilita() {
        viajes.viaje.value = viaje(minutosDesdeAhora = 360)

        val estado = crear().estado.value

        assertEquals(reloj.ahora.plusMinutes(300), estado.disponibleDesde)
        assertFalse(estado.puedeIniciar)
    }

    @Test
    fun dentroDeLaVentanaIniciarEstaHabilitadoYNoHayPista() {
        viajes.viaje.value = viaje(minutosDesdeAhora = 30)

        val estado = crear().estado.value

        assertTrue(estado.puedeIniciar)
        assertNull(estado.disponibleDesde)
    }

    @Test
    fun vencidoNoSePuedeIniciar() {
        viajes.viaje.value = viaje(minutosDesdeAhora = -120)

        val estado = crear().estado.value

        assertEquals(Inicio.VENCIDO, estado.inicio)
        assertFalse(estado.puedeIniciar)
        assertEquals(Retraso.VENCIDO, estado.retraso)
        assertNull(estado.disponibleDesde)
    }

    @Test
    fun atrasadoPeroDentroDeLaVentanaSiSePuedeIniciar() {
        viajes.viaje.value = viaje(minutosDesdeAhora = -30)

        val estado = crear().estado.value

        assertEquals(Retraso.ATRASADO, estado.retraso)
        assertTrue(estado.puedeIniciar)
    }

    @Test
    fun losControlesDependenDelEstadoDelViaje() {
        data class Esperado(val controles: Boolean, val iniciar: Boolean, val finalizar: Boolean, val rechazar: Boolean)

        val esperados = mapOf(
            EstadoViaje.ASIGNADO to Esperado(controles = true, iniciar = true, finalizar = false, rechazar = true),
            EstadoViaje.EN_CURSO to Esperado(controles = true, iniciar = false, finalizar = true, rechazar = false),
            EstadoViaje.FINALIZADO to Esperado(controles = false, iniciar = false, finalizar = false, rechazar = false),
            EstadoViaje.CANCELADO to Esperado(controles = false, iniciar = false, finalizar = false, rechazar = false),
            EstadoViaje.PENDIENTE to Esperado(controles = false, iniciar = false, finalizar = false, rechazar = false),
        )

        esperados.forEach { (estadoViaje, esperado) ->
            viajes.viaje.value = viaje(estado = estadoViaje, minutosDesdeAhora = 10)
            val estado = DetalleViajeViewModel(viajes, reloj).also { it.cargar("TR-1042") }.estado.value
            val real = Esperado(estado.conControles, estado.puedeIniciar, estado.puedeFinalizar, estado.puedeRechazar)
            assertEquals(estadoViaje.name, esperado, real)
        }
    }

    @Test
    fun alIniciarseElViajeLosControlesCambianSinRecargar() {
        viajes.viaje.value = viaje(estado = EstadoViaje.ASIGNADO, minutosDesdeAhora = 10)
        val vm = crear()
        assertTrue(vm.estado.value.puedeIniciar)
        assertFalse(vm.estado.value.puedeFinalizar)
        val refrescos = viajes.refrescados.size

        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO, minutosDesdeAhora = 10)

        assertFalse(vm.estado.value.puedeIniciar)
        assertTrue(vm.estado.value.puedeFinalizar)
        assertFalse(vm.estado.value.puedeRechazar)
        assertEquals(refrescos, viajes.refrescados.size)
    }

    @Test
    fun elArqueoSeActualizaCuandoUnPasajeroAborda() {
        viajes.viaje.value = viaje(minutosDesdeAhora = 10, pasajeros = arrayOf("Constanza Ríos", "Tomás Vidal"))
        val vm = crear()
        assertEquals(0, vm.estado.value.arqueo?.abordaron)
        assertEquals(listOf("Constanza Ríos", "Tomás Vidal"), vm.estado.value.arqueo?.faltan)

        viajes.viaje.value = viajes.viaje.value!!.copy(
            pasajeros = listOf(pasajeroDePrueba("Constanza Ríos", reloj.ahora), pasajeroDePrueba("Tomás Vidal")),
        )

        assertEquals(1, vm.estado.value.arqueo?.abordaron)
        assertEquals(listOf("Tomás Vidal"), vm.estado.value.arqueo?.faltan)
    }

    @Test
    fun alVolverAPrimerPlanoSeRecalculaLaVentana() {
        viajes.viaje.value = viaje(minutosDesdeAhora = 90)
        val vm = crear()
        assertEquals(Inicio.ANTES_DE_LA_VENTANA, vm.estado.value.inicio)

        reloj.ahora = reloj.ahora.plusMinutes(40)
        // Sin volver a primer plano, la pantalla conserva lo último que calculó.
        assertEquals(Inicio.ANTES_DE_LA_VENTANA, vm.estado.value.inicio)
        vm.alVolverAPrimerPlano()

        assertEquals(Inicio.HABILITADO, vm.estado.value.inicio)
        assertNull(vm.estado.value.disponibleDesde)
    }

    @Test
    fun finalizarPideConfirmacionYLlamaAFinalizado() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()

        vm.pedirFinalizar()
        assertTrue(vm.estado.value.confirmandoFin)
        assertTrue(viajes.estados.isEmpty())
        vm.confirmarFinalizar()

        assertEquals(listOf("TR-1042" to EstadoViaje.FINALIZADO), viajes.estados)
        assertFalse(vm.estado.value.confirmandoFin)
        assertFalse(vm.estado.value.enviando)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun descartarLaConfirmacionNoFinalizaNada() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()
        vm.pedirFinalizar()

        vm.descartarFinalizar()

        assertFalse(vm.estado.value.confirmandoFin)
        assertTrue(viajes.estados.isEmpty())
    }

    @Test
    fun soloSePuedeFinalizarUnViajeEnCurso() {
        viajes.viaje.value = viaje(estado = EstadoViaje.ASIGNADO)
        val vm = crear()

        vm.pedirFinalizar()
        vm.confirmarFinalizar()

        assertFalse(vm.estado.value.confirmandoFin)
        assertTrue(viajes.estados.isEmpty())
    }

    @Test
    fun unSegundoToqueMientrasFinalizaNoDuplicaElEnvio() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()
        val respuesta = CompletableDeferred<Resultado<Unit>>()
        viajes.respuestaEstado = { respuesta.await() }

        vm.pedirFinalizar()
        vm.confirmarFinalizar()
        assertTrue(vm.estado.value.enviando)
        assertFalse(vm.estado.value.puedeFinalizar)
        vm.pedirFinalizar()
        vm.confirmarFinalizar()
        respuesta.complete(Resultado.Exito(Unit))

        assertEquals(1, viajes.estados.size)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun unConflictoAlFinalizarMuestraElMensajeYVuelveAPedirElViaje() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()
        val refrescos = viajes.refrescados.size
        viajes.respuestaEstado = { Resultado.Fallo(ErrorApp.Conflicto("El viaje cambió de estado. Actualiza el detalle.")) }

        vm.pedirFinalizar()
        vm.confirmarFinalizar()

        assertEquals(ErrorApp.Conflicto("El viaje cambió de estado. Actualiza el detalle."), vm.estado.value.error)
        assertFalse(vm.estado.value.enviando)
        assertTrue(viajes.refrescados.size > refrescos)
    }

    @Test
    fun unFalloDeConexionAlFinalizarMuestraElErrorSinRecargar() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()
        val refrescos = viajes.refrescados.size
        viajes.respuestaEstado = { Resultado.Fallo(ErrorApp.SinConexion) }

        vm.pedirFinalizar()
        vm.confirmarFinalizar()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertEquals(refrescos, viajes.refrescados.size)
        assertTrue(vm.estado.value.puedeFinalizar)
    }

    @Test
    fun unCambioEnColaNoSeMuestraComoConfirmadoNiComoError() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()
        viajes.respuestaEstado = { Resultado.Encolado }

        vm.pedirFinalizar()
        vm.confirmarFinalizar()

        assertNull(vm.estado.value.error)
        assertFalse(vm.estado.value.enviando)
        assertEquals(EstadoViaje.EN_CURSO, vm.estado.value.viaje?.estado)
    }

    @Test
    fun rechazarExigeUnMotivo() {
        viajes.viaje.value = viaje()
        val vm = crear()
        vm.pedirRechazo()
        assertTrue(vm.estado.value.rechazando)
        assertFalse(vm.estado.value.puedeConfirmarRechazo)

        vm.escribirMotivo("   ")
        assertFalse(vm.estado.value.puedeConfirmarRechazo)
        vm.confirmarRechazo()

        assertTrue(viajes.rechazos.isEmpty())
        assertTrue(vm.estado.value.rechazando)
    }

    @Test
    fun rechazarEnviaElMotivoSinEspaciosYVuelveAMisViajes() {
        viajes.viaje.value = viaje()
        val vm = crear()

        vm.pedirRechazo()
        vm.escribirMotivo("  Vehículo con falla  ")
        vm.confirmarRechazo()

        assertEquals(listOf("TR-1042" to "Vehículo con falla"), viajes.rechazos)
        assertTrue(vm.estado.value.rechazado)
        assertFalse(vm.estado.value.rechazando)
        assertFalse(vm.estado.value.enviando)
        assertEquals("", vm.estado.value.motivo)
    }

    @Test
    fun soloSePuedeRechazarUnViajeAsignado() {
        viajes.viaje.value = viaje(estado = EstadoViaje.EN_CURSO)
        val vm = crear()

        vm.pedirRechazo()

        assertFalse(vm.estado.value.rechazando)
    }

    @Test
    fun siRechazarFallaElMotivoSeConservaYElErrorSeMuestra() {
        viajes.viaje.value = viaje()
        val vm = crear()
        viajes.respuestaRechazo = { Resultado.Fallo(ErrorApp.SinConexion) }

        vm.pedirRechazo()
        vm.escribirMotivo("Vehículo con falla")
        vm.confirmarRechazo()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertFalse(vm.estado.value.rechazado)
        assertFalse(vm.estado.value.rechazando)
        assertEquals("Vehículo con falla", vm.estado.value.motivo)

        viajes.respuestaRechazo = { Resultado.Exito(Unit) }
        vm.pedirRechazo()
        vm.confirmarRechazo()

        assertTrue(vm.estado.value.rechazado)
        assertEquals(2, viajes.rechazos.size)
    }

    @Test
    fun unSegundoToqueMientrasRechazaNoDuplicaElEnvio() {
        viajes.viaje.value = viaje()
        val vm = crear()
        val respuesta = CompletableDeferred<Resultado<Unit>>()
        viajes.respuestaRechazo = { respuesta.await() }

        vm.pedirRechazo()
        vm.escribirMotivo("Vehículo con falla")
        vm.confirmarRechazo()
        vm.pedirRechazo()
        vm.confirmarRechazo()
        respuesta.complete(Resultado.Exito(Unit))

        assertEquals(1, viajes.rechazos.size)
    }

    @Test
    fun unFalloAlPedirElViajeSeMuestraSinBorrarLoYaCargado() {
        viajes.viaje.value = viaje()
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.SinConexion)

        val vm = crear()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertEquals("TR-1042", vm.estado.value.viaje?.folio)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun reintentarLimpiaElErrorYVuelveAPedirElViaje() {
        viajes.viaje.value = viaje()
        viajes.resultadoRefresco = Resultado.Fallo(ErrorApp.SinConexion)
        val vm = crear()

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

/** Un solo viaje observable; guarda los cambios de estado y rechazos que le piden y responde con lo que la prueba diga. */
private class DetalleDeViajeFalso : ViajeRepository {
    val viaje = MutableStateFlow<Viaje?>(null)
    val refrescados = mutableListOf<String>()
    val estados = mutableListOf<Pair<String, EstadoViaje>>()
    val rechazos = mutableListOf<Pair<String, String>>()
    var resultadoRefresco: Resultado<Unit> = Resultado.Exito(Unit)
    var respuestaEstado: suspend () -> Resultado<Unit> = { Resultado.Exito(Unit) }
    var respuestaRechazo: suspend () -> Resultado<Unit> = { Resultado.Exito(Unit) }
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

    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> {
        rechazos += folio to motivo
        return respuestaRechazo()
    }

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = sinUso()
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = sinUso()
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = sinUso()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = sinUso()
    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = sinUso()
    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> = sinUso()
}

private fun sinUso(): Nothing = throw UnsupportedOperationException("Esta prueba no usa esa operación")
