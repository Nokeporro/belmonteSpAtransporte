package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.viajeDePrueba
import java.time.Clock
import java.time.Instant
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** «Cancelar viaje» del Administrador (HU24, V7): qué se pide, qué se valida y qué se envía. */
@OptIn(ExperimentalCoroutinesApi::class)
class CancelarViajeViewModelTest {

    private val zona = ZoneId.of("America/Santiago")
    private val ahora = LocalDateTime.of(2026, 10, 28, 12, 0)
    private val reloj = RelojMutable(ahora, zona)
    private val viajes = ViajeCancelableFalso()
    private val pagos = PagosFalsos()

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear() = CancelarViajeViewModel(viajes, pagos, reloj)

    private fun pago(estado: EstadoPago = EstadoPago.PAGADO, monto: Int = 52_000) = Pago(
        "pago-1", "TR-1038", monto, estado, "Banco de prueba", 12, "76543210-3", MedioPago.TRANSFERENCIA,
        TipoRecibo.FACTURA, ahora.minusDays(2), null,
    )

    /** Un viaje con pago; por omisión está vencido (más de una hora de atraso). */
    private fun escenario(
        minutosDeAtraso: Long = 120,
        estado: EstadoViaje = EstadoViaje.PENDIENTE,
        pago: Pago? = pago(),
    ): CancelarViajeViewModel {
        val base = viajeDePrueba(fhAgenda = ahora.minusMinutes(minutosDeAtraso), folio = "TR-1038", estado = estado)
        viajes.viaje.value = if (pago == null) base else base.copy(hashPago = pago.hash, estadoPago = pago.estado)
        pago?.let { pagos.pagos[it.hash] = it }
        return crear().also { it.cargar("TR-1038") }
    }

    @Test
    fun cargaElViajeYSuPagoYPrecargaElTotalDelReembolso() {
        val vm = escenario()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("TR-1038", estado.viaje?.folio)
        assertEquals(EstadoPago.PAGADO, estado.pago?.estado)
        assertEquals("52000", estado.reembolso)
        assertTrue(estado.vencido)
        assertTrue(estado.pideReembolso)
        assertTrue(estado.puedeConfirmar)
        assertNull(estado.error)
    }

    @Test
    fun conElViajeAlDiaElMontoNoSePideYSeReembolsaTodo() {
        val vm = escenario(minutosDeAtraso = 30)
        vm.escribirMotivo("Conductor sin disponibilidad")

        assertFalse(vm.estado.value.vencido)
        assertFalse(vm.estado.value.pideReembolso)
        vm.confirmar()

        // El monto precargado no se envía: el backend reembolsa el total sin preguntar.
        assertEquals(listOf(Cancelacion("TR-1038", "Conductor sin disponibilidad", null)), viajes.cancelaciones)
        assertTrue(vm.estado.value.cancelado)
    }

    @Test
    fun elLimiteDeUnaHoraNoEstaVencidoPeroUnSegundoDespuesSi() {
        val enElLimite = escenario(minutosDeAtraso = 60)
        assertFalse(enElLimite.estado.value.vencido)

        viajes.viaje.value = viajes.viaje.value!!.copy(fhAgenda = ahora.minusHours(1).minusSeconds(1))
        assertTrue(enElLimite.estado.value.vencido)
        assertTrue(enElLimite.estado.value.pideReembolso)
    }

    @Test
    fun conUnPagoPendienteORechazadoOSinPagoNoSePideMonto() {
        for (pago in listOf(pago(EstadoPago.PENDIENTE), pago(EstadoPago.RECHAZADO), pago(EstadoPago.REEMBOLSADO), null)) {
            val vm = escenario(pago = pago)
            vm.escribirMotivo("Cambio de planes")

            assertTrue(vm.estado.value.vencido)
            assertFalse(vm.estado.value.pideReembolso)
            assertTrue(vm.estado.value.puedeConfirmar)
            vm.confirmar()

            assertNull(viajes.cancelaciones.last().monto)
            assertTrue(vm.estado.value.cancelado)
        }
    }

    @Test
    fun sinMotivoOConUnoDemasiadoCortoOLargoNoSeEnvia() {
        val vm = escenario()

        vm.confirmar()
        assertEquals("Es obligatorio.", vm.estado.value.errores[CAMPO_MOTIVO])
        vm.escribirMotivo("1234")
        vm.confirmar()
        assertEquals("Debe tener entre 5 y 200 caracteres.", vm.estado.value.errores[CAMPO_MOTIVO])
        vm.escribirMotivo("a".repeat(201))
        vm.confirmar()
        assertEquals("Debe tener entre 5 y 200 caracteres.", vm.estado.value.errores[CAMPO_MOTIVO])

        assertTrue(viajes.cancelaciones.isEmpty())
        assertFalse(vm.estado.value.enviando)
        assertFalse(vm.estado.value.cancelado)
    }

    @Test
    fun escribirDeNuevoQuitaElErrorDeEseCampo() {
        val vm = escenario()
        vm.confirmar()
        assertNotNull(vm.estado.value.errores[CAMPO_MOTIVO])

        vm.escribirMotivo("Motivo válido")

        assertNull(vm.estado.value.errores[CAMPO_MOTIVO])
    }

    @Test
    fun vencidoConPagoPagadoElMontoEsObligatorioYDebeEstarEnRango() {
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")

        val casos = mapOf(
            "" to "Es obligatorio.",
            "abc" to "Ingresa un monto entero.",
            "-1" to "Debe estar entre \$0 y \$52.000.",
            "52001" to "Debe estar entre \$0 y \$52.000.",
        )
        for ((texto, error) in casos) {
            vm.escribirReembolso(texto)
            vm.confirmar()
            assertEquals(error, vm.estado.value.errores[CAMPO_REEMBOLSO])
        }

        assertTrue(viajes.cancelaciones.isEmpty())
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun enviaElMotivoRecortadoYElMontoQueFijaElAdministrador() {
        val vm = escenario()
        vm.escribirMotivo("  Nadie lo atendió  ")
        vm.escribirReembolso(" 20000 ")

        vm.confirmar()

        assertEquals(listOf(Cancelacion("TR-1038", "Nadie lo atendió", 20_000)), viajes.cancelaciones)
        assertTrue(vm.estado.value.cancelado)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun unMontoDeCeroSeEnviaComoCero() {
        val vm = escenario()
        vm.escribirMotivo("Sin reembolso")
        vm.escribirReembolso("0")

        vm.confirmar()

        assertEquals(0, viajes.cancelaciones.single().monto)
    }

    @Test
    fun elMontoEscritoNoLoPisaUnaRecarga() {
        val vm = escenario()
        vm.escribirReembolso("1000")

        viajes.viaje.value = viajes.viaje.value!!.copy(numeroVuelo = "LA1502")

        assertEquals("1000", vm.estado.value.reembolso)
    }

    @Test
    fun unDobleToqueEnviaUnaSolaVez() {
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")
        val respuesta = CompletableDeferred<Resultado<Unit>>()
        viajes.respuesta = { respuesta.await() }

        vm.confirmar()
        vm.confirmar()

        assertEquals(1, viajes.cancelaciones.size)
        assertTrue(vm.estado.value.enviando)
        assertFalse(vm.estado.value.puedeConfirmar)
        // Mientras envía no se puede cambiar lo que se está enviando.
        vm.escribirMotivo("Otro motivo distinto")
        assertEquals("Nadie lo atendió", vm.estado.value.motivo)

        respuesta.complete(Resultado.Exito(Unit))
        assertTrue(vm.estado.value.cancelado)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun unFalloConservaLoEscritoYPermiteReintentar() {
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")
        vm.escribirReembolso("20000")
        viajes.respuesta = { Resultado.Fallo(ErrorApp.SinConexion) }

        vm.confirmar()

        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertFalse(vm.estado.value.enviando)
        assertFalse(vm.estado.value.cancelado)
        assertEquals("Nadie lo atendió", vm.estado.value.motivo)
        assertEquals("20000", vm.estado.value.reembolso)

        viajes.respuesta = { Resultado.Exito(Unit) }
        vm.confirmar()

        assertEquals(2, viajes.cancelaciones.size)
        assertTrue(vm.estado.value.cancelado)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun unConflictoVuelveALeerElViajeYConservaLoEscrito() {
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")
        val lecturas = viajes.refrescos
        viajes.respuesta = { Resultado.Fallo(ErrorApp.Conflicto("El viaje está EN_CURSO")) }

        vm.confirmar()
        // La lectura trae el estado real del viaje.
        viajes.viaje.value = viajes.viaje.value!!.copy(estado = EstadoViaje.EN_CURSO)

        assertEquals(ErrorApp.Conflicto("El viaje está EN_CURSO"), vm.estado.value.error)
        assertTrue(viajes.refrescos > lecturas)
        assertEquals("Nadie lo atendió", vm.estado.value.motivo)
        assertFalse(vm.estado.value.cancelable)
        assertFalse(vm.estado.value.puedeConfirmar)
    }

    @Test
    fun unaValidacionDelRepositorioQuedaBajoSuCampo() {
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")
        viajes.respuesta = {
            Resultado.Fallo(ErrorApp.Validacion(mapOf(CAMPO_REEMBOLSO to "Debe estar entre 0 y 52000.", "otro" to "Revisa los datos.")))
        }

        vm.confirmar()

        assertEquals("Debe estar entre 0 y 52000.", vm.estado.value.errores[CAMPO_REEMBOLSO])
        assertEquals(ErrorApp.Validacion(mapOf("otro" to "Revisa los datos.")), vm.estado.value.error)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun unViajeQueVenceMientrasSeMiraPideElMontoAntesDeEnviar() {
        val vm = escenario(minutosDeAtraso = 30)
        assertFalse(vm.estado.value.pideReembolso)
        vm.escribirMotivo("Nadie lo atendió")

        // Pasan 31 minutos con la pantalla abierta: el viaje ya está vencido al confirmar.
        reloj.ahora = ahora.plusMinutes(31)
        vm.confirmar()

        // El monto no estaba a la vista: se muestra, precargado con el total, y no se envía nada.
        assertTrue(vm.estado.value.pideReembolso)
        assertEquals("52000", vm.estado.value.reembolso)
        assertEquals(MONTO_POR_CONFIRMAR, vm.estado.value.errores[CAMPO_REEMBOLSO])
        assertTrue(viajes.cancelaciones.isEmpty())

        // Con el campo ya visible, «Confirmar» envía lo que decidió el Administrador.
        vm.escribirReembolso("26000")
        vm.confirmar()
        assertEquals(26_000, viajes.cancelaciones.single().monto)
    }

    @Test
    fun unViajeQueVenceMientrasSeMiraSeEnviaConElTotalSiSeConfirmaTalCual() {
        val vm = escenario(minutosDeAtraso = 30)
        vm.escribirMotivo("Nadie lo atendió")
        reloj.ahora = ahora.plusMinutes(31)
        vm.confirmar()
        vm.confirmar()

        assertEquals(52_000, viajes.cancelaciones.single().monto)
    }

    @Test
    fun unViajeQueYaNoSePuedeCancelarNoSeConfirma() {
        val vm = escenario(estado = EstadoViaje.EN_CURSO)
        vm.escribirMotivo("Nadie lo atendió")

        assertFalse(vm.estado.value.cancelable)
        assertFalse(vm.estado.value.puedeConfirmar)
        vm.confirmar()

        assertTrue(viajes.cancelaciones.isEmpty())
    }

    @Test
    fun unPagoQueNoSePudoLeerImpideConfirmar() {
        pagos.fallo = ErrorApp.SinConexion
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")

        assertNull(vm.estado.value.pago)
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)
        assertFalse(vm.estado.value.puedeConfirmar)
        vm.confirmar()

        assertTrue(viajes.cancelaciones.isEmpty())
    }

    @Test
    fun soloUnMotivoEscritoCuentaComoCambioPorDescartar() {
        val vm = escenario()
        assertFalse(vm.estado.value.modificado)

        vm.escribirMotivo("abc")
        assertTrue(vm.estado.value.modificado)

        vm.escribirMotivo("Motivo válido")
        vm.confirmar()
        assertFalse(vm.estado.value.modificado)
    }

    @Test
    fun cargarDosVecesConElMismoFolioNoBorraLoEscrito() {
        val vm = escenario()
        vm.escribirMotivo("Nadie lo atendió")

        vm.cargar("TR-1038")

        assertEquals("Nadie lo atendió", vm.estado.value.motivo)
    }

    @Test
    fun unViajeInexistenteMuestraElErrorYNoQuedaCargando() {
        viajes.viaje.value = null
        val vm = crear().also { it.cargar("TR-9999") }

        assertEquals(ErrorApp.NoEncontrado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
        assertNull(vm.estado.value.viaje)
    }

    @Test
    fun unFalloAlLeerElViajeSeAvisaYNoQuedaCargando() {
        viajes.falloRefresco = ErrorApp.Inesperado
        val vm = crear().also { it.cargar("TR-1038") }

        assertEquals(ErrorApp.Inesperado, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
        assertNull(vm.estado.value.viaje)
    }

    @Test
    fun sinConexionSeUsaLoQueYaEstaGuardado() {
        viajes.falloRefresco = ErrorApp.SinConexion
        val vm = escenario()

        assertNull(vm.estado.value.error)
        assertEquals("TR-1038", vm.estado.value.viaje?.folio)
    }
}

private data class Cancelacion(val folio: String, val motivo: String?, val monto: Int?)

/** Un reloj que avanza cuando la prueba quiere, en la hora de Chile. */
private class RelojMutable(var ahora: LocalDateTime, private val zona: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zona
    override fun withZone(zone: ZoneId): Clock = RelojMutable(ahora, zone)
    override fun instant(): Instant = ahora.atZone(zona).toInstant()
}

/** Un solo viaje observable; guarda lo que le cancelan y responde con lo que la prueba diga. */
private class ViajeCancelableFalso : ViajeRepository {
    val viaje = MutableStateFlow<Viaje?>(null)
    val cancelaciones = mutableListOf<Cancelacion>()
    var refrescos = 0
    var falloRefresco: ErrorApp? = null
    var respuesta: suspend () -> Resultado<Unit> = { Resultado.Exito(Unit) }

    override fun observarViaje(folio: String): Flow<Viaje?> = viaje

    override suspend fun refrescarViaje(folio: String): Resultado<Unit> {
        refrescos++
        return falloRefresco?.let { Resultado.Fallo(it) } ?: Resultado.Exito(Unit)
    }

    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> {
        cancelaciones += Cancelacion(folio, motivo, montoReembolso)
        return respuesta()
    }

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = noSeUsa()
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = noSeUsa()
    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = noSeUsa()
    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = noSeUsa()
    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = noSeUsa()
    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = noSeUsa()
    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = noSeUsa()
}

private class PagosFalsos : PagoRepository {
    val pagos = mutableMapOf<String, Pago>()
    var fallo: ErrorApp? = null

    override suspend fun obtenerPago(hash: String): Resultado<Pago> =
        fallo?.let { Resultado.Fallo(it) } ?: pagos[hash]?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorApp.NoEncontrado)

    override suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> = noSeUsa()
    override suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> = noSeUsa()
}

private fun noSeUsa(): Nothing = throw UnsupportedOperationException("Esta prueba no usa esa operación")
