package cl.belmontespa.myapplication.data.local

import cl.belmontespa.myapplication.util.FechaIso
import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La semilla de la build demo cumple el mínimo de `database/demo.sql` y las reglas del modelo. */
class DatosDemoTest {

    private val ahora = LocalDateTime.of(2026, 9, 24, 14, 0)
    private val semilla = DatosDemo.crear(ahora)

    @Test
    fun cumpleElMinimoDeDemoSql() {
        val porRol = semilla.usuarios.groupBy { it.rol }
        assertEquals(2, porRol.getValue("ADMINISTRADOR").size)
        assertEquals(4, porRol.getValue("CONDUCTOR").size)
        assertEquals(1, porRol.getValue("CONDUCTOR").count { !it.activo })
        assertEquals(3, porRol.getValue("SOLICITANTE").size)
        assertEquals(1, porRol.getValue("SOLICITANTE").count { it.estadoSolicitante == "PENDIENTE" })
        assertEquals(2, porRol.getValue("SOLICITANTE").map { it.rutEmpresa }.toSet().size)
        assertEquals(4, semilla.moviles.size)
        assertEquals(1, semilla.moviles.count { !it.activo })
        assertEquals(
            setOf("PENDIENTE", "ASIGNADO", "EN_CURSO", "FINALIZADO", "CANCELADO"),
            semilla.viajes.map { it.estado }.toSet(),
        )
        assertEquals(
            setOf("PENDIENTE", "PAGADO", "RECHAZADO", "ANULADO", "REEMBOLSADO"),
            semilla.pagos.map { it.estadoPago }.toSet(),
        )
    }

    @Test
    fun cadaConductorActivoTieneUnVehiculoActivoHabilitado() {
        val activos = semilla.moviles.filter { it.activo }.map { it.patente }.toSet()
        semilla.usuarios.filter { it.rol == "CONDUCTOR" && it.activo }.forEach { conductor ->
            assertTrue(
                conductor.correo,
                semilla.habilitaciones.any { it.correoConductor == conductor.correo && it.patente in activos },
            )
        }
    }

    @Test
    fun elViajeEnVentanaTieneTresPasajerosYUnoAbordado() {
        val viaje = semilla.viajes.single { it.folio == "TR-1042" }
        assertEquals("ASIGNADO", viaje.estado)
        assertEquals(ahora.plusMinutes(30), FechaIso.leer(viaje.fhAgenda))
        val pasajeros = semilla.viajePasajeros.filter { it.folio == viaje.folio }
        assertEquals(3, pasajeros.size)
        assertEquals(1, pasajeros.count { it.fhAbordaje != null })
    }

    @Test
    fun hayUnViajeAtrasadoYUnoVencido() {
        val sinIniciar = semilla.viajes.filter { it.estado == "PENDIENTE" || it.estado == "ASIGNADO" }
        val agendas = sinIniciar.map { FechaIso.leer(it.fhAgenda) }
        assertTrue(agendas.any { it < ahora && ahora <= it.plusHours(1) })
        assertTrue(agendas.any { ahora > it.plusHours(1) })
    }

    @Test
    fun cantidadDePasajerosCoincideConSusFilasYNoSuperaLaCapacidad() {
        val capacidad = semilla.moviles.associate { it.patente to it.capacidad }
        semilla.viajes.forEach { viaje ->
            assertEquals(viaje.folio, semilla.viajePasajeros.count { it.folio == viaje.folio }, viaje.cantidadPasajeros)
            viaje.patenteMovil?.let { assertTrue(viaje.folio, viaje.cantidadPasajeros <= capacidad.getValue(it)) }
        }
    }

    @Test
    fun losPagosSonCoherentesConSusViajes() {
        semilla.pagos.forEach { pago ->
            val viaje = semilla.viajes.single { it.folio == pago.folio }
            assertEquals(pago.hash, viaje.hashPago)
            assertEquals(pago.estadoPago, viaje.estadoPago)
            // Un viaje cancelado nunca queda con un pago Pendiente ni Pagado.
            if (viaje.estado == "CANCELADO") assertTrue(pago.estadoPago !in setOf("PENDIENTE", "PAGADO"))
            // fecha_pago solo existe desde que el pago pasó a Pagado.
            if (pago.estadoPago == "PENDIENTE" || pago.estadoPago == "RECHAZADO") assertEquals(null, pago.fechaPago)
            if (pago.estadoPago == "REEMBOLSADO") assertNotNull(pago.montoReembolsado)
        }
    }

    @Test
    fun ningunConductorNiVehiculoTieneViajesSolapados() {
        val activos = semilla.viajes.filter { it.estado == "ASIGNADO" || it.estado == "EN_CURSO" }
        val porConductor = activos.groupBy { it.correoConductor }.values
        val porVehiculo = activos.groupBy { it.patenteMovil }.values
        (porConductor + porVehiculo).forEach { viajes ->
            viajes.forEach { a ->
                viajes.filter { it !== a }.forEach { b ->
                    val separacion = Duration.between(FechaIso.leer(a.fhAgenda), FechaIso.leer(b.fhAgenda)).abs()
                    assertTrue("${a.folio} y ${b.folio}", separacion >= Duration.ofHours(2))
                }
            }
        }
    }

    @Test
    fun lasComunasExistenEnElCatalogoDeSuRegion() {
        val catalogo = DatosDemo.regiones.associate { it.nombre to it.comunas.toSet() }
        semilla.viajes.forEach {
            assertTrue(it.origenComuna, it.origenComuna in catalogo.getValue(it.origenRegion))
            assertTrue(it.destinoComuna, it.destinoComuna in catalogo.getValue(it.destinoRegion))
        }
        assertEquals(52, catalogo.getValue("Metropolitana de Santiago").size)
        assertEquals(38, catalogo.getValue("Valparaíso").size)
    }

    @Test
    fun losCodigosCumplenLasValidaciones() {
        semilla.usuarios.mapNotNull { it.rutEmpresa }.forEach { assertTrue(it, rutValido(it)) }
        semilla.usuarios.mapNotNull { it.telefono }.forEach { assertTrue(it, Regex("""^\+56[0-9]{9}$""").matches(it)) }
        semilla.moviles.forEach {
            assertTrue(it.patente, Regex("^([A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2})$").matches(it.patente))
            assertTrue(it.codigoAbordaje, Regex("^[0-9A-F]{16}$").matches(it.codigoAbordaje))
        }
        assertEquals(semilla.moviles.size, semilla.moviles.map { it.codigoAbordaje }.toSet().size)
        semilla.viajes.forEach { assertTrue(it.folio, Regex("^TR-[1-9][0-9]{3,16}$").matches(it.folio)) }
        semilla.pagos.forEach { assertTrue(it.hash, Regex("^[0-9a-f]{32}$").matches(it.hash)) }
        semilla.pasajeros.forEach { assertTrue(it.dniHash, Regex("^[A-Za-z0-9_-]{43}$").matches(it.dniHash)) }
    }

    @Test
    fun losDocumentosSonRutValidosOPasaportes() {
        DatosDemo.pasajerosDemo.forEach {
            val documento = Hashes.normalizarDocumento(it.documento)
            val esRut = documento.first().isDigit()
            if (esRut) {
                assertTrue(it.documento, rutValido(it.documento))
            } else {
                assertTrue(it.documento, Regex("^[A-Z0-9]{5,20}$").matches(documento))
            }
        }
    }

    @Test
    fun lasCuentasEntranConLaContrasenaComun() {
        semilla.usuarios.forEach { assertEquals(Hashes.password(DatosDemo.PASSWORD), it.passwordSha256) }
    }

    /** Dígito verificador por módulo 11 (factores 2 a 7 de derecha a izquierda). */
    private fun rutValido(rut: String): Boolean {
        val limpio = rut.replace(".", "").replace("-", "").uppercase()
        val cuerpo = limpio.dropLast(1)
        if (cuerpo.length !in 7..8 || !cuerpo.all { it.isDigit() }) return false
        val suma = cuerpo.reversed().mapIndexed { i, c -> c.digitToInt() * (2 + i % 6) }.sum()
        val dv = when (val resto = 11 - suma % 11) {
            11 -> '0'
            10 -> 'K'
            else -> resto.digitToChar()
        }
        return limpio.last() == dv
    }
}
