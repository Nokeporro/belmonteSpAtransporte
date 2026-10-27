package cl.belmontespa.myapplication.util

import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.model.Ubicacion
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class ValidacionesTest {
    private val ahora = LocalDateTime.of(2026, 10, 22, 12, 0)
    private val origen = Ubicacion("Origen", "Providencia", "Metropolitana de Santiago", -33.42, -70.61)
    private val destino = origen.copy(direccion = "Destino", longitud = -70.60)
    private val pasajero = NuevoPasajero(TipoDocumento.PASAPORTE, "DEMO123", "Pasajero")
    private fun solicitud(fecha: LocalDateTime) = NuevaSolicitud(fecha, origen, destino, "la 1502", listOf(pasajero))
    @Test fun normalizaCorreoTelefonoRutYVuelo() {
        assertEquals("ana@test.cl", Validaciones.correo(" ANA@Test.cl "))
        assertEquals("+56955550101", Validaciones.telefono("(9) 5555-0101"))
        assertEquals("12345678-5", Validaciones.rut("12.345.678-5"))
        assertEquals("LA1502", Validaciones.vuelo("la 1502"))
    }
    @Test fun validaDocumentoSegunTipo() {
        assertTrue(Validaciones.documentoValido(TipoDocumento.RUT, "12.345.678-5"))
        assertFalse(Validaciones.documentoValido(TipoDocumento.RUT, "12.345.678-4"))
        assertTrue(Validaciones.documentoValido(TipoDocumento.PASAPORTE, "abc 123"))
        assertFalse(Validaciones.documentoValido(TipoDocumento.PASAPORTE, "ab!123"))
        assertFalse(Validaciones.documentoValido(TipoDocumento.PASAPORTE, "a".repeat(21)))
    }
    @Test fun contrasenaRespetaBytesYComplejidad() {
        assertTrue(Validaciones.passwordValida("Contraseña123"))
        assertFalse(Validaciones.passwordValida("abcdefgh"))
        assertFalse(Validaciones.passwordValida("12345678"))
        assertFalse(Validaciones.passwordValida("á".repeat(36) + "1"))
    }
    @Test fun aceptaLimitesDeAgendaYRechazaFueraDeEllos() {
        assertTrue(Validaciones.solicitud(solicitud(ahora.plusHours(2)), ahora).isEmpty())
        assertTrue(Validaciones.solicitud(solicitud(ahora.plusDays(90)), ahora).isEmpty())
        assertTrue("fhAgenda" in Validaciones.solicitud(solicitud(ahora.plusHours(2).minusMinutes(1)), ahora))
        assertTrue("fhAgenda" in Validaciones.solicitud(solicitud(ahora.plusDays(90).plusMinutes(1)), ahora))
    }
    @Test fun exigePasajerosSinDuplicadosYDestinosDistintos() {
        val base = solicitud(ahora.plusHours(3))
        assertTrue("pasajeros" in Validaciones.solicitud(base.copy(pasajeros = emptyList()), ahora))
        assertTrue("pasajeros" in Validaciones.solicitud(base.copy(pasajeros = listOf(pasajero, pasajero.copy(dni = "demo 123"))), ahora))
        assertTrue("destino" in Validaciones.solicitud(base.copy(destino = origen), ahora))
    }
    @Test fun elMotivoTieneDeCincoADoscientosCaracteresYaRecortado() {
        assertNull(Validaciones.motivo("12345"))
        assertNull(Validaciones.motivo("  12345  "))
        assertNull(Validaciones.motivo("a".repeat(200)))
        assertEquals("Debe tener entre 5 y 200 caracteres.", Validaciones.motivo("1234"))
        assertEquals("Debe tener entre 5 y 200 caracteres.", Validaciones.motivo("  1234  "))
        assertEquals("Debe tener entre 5 y 200 caracteres.", Validaciones.motivo("a".repeat(201)))
        assertEquals("Es obligatorio.", Validaciones.motivo(""))
        assertEquals("Es obligatorio.", Validaciones.motivo("      "))
    }
    @Test fun elReembolsoVaDeCeroAlTotalDelPago() {
        assertNull(Validaciones.reembolso("0", 52_000))
        assertNull(Validaciones.reembolso(" 52000 ", 52_000))
        assertNull(Validaciones.reembolso(20_000, 52_000))
        val fueraDeRango = "Debe estar entre \$0 y \$52.000."
        assertEquals(fueraDeRango, Validaciones.reembolso("52001", 52_000))
        assertEquals(fueraDeRango, Validaciones.reembolso("-1", 52_000))
        assertEquals(fueraDeRango, Validaciones.reembolso(-1, 52_000))
        assertEquals("Es obligatorio.", Validaciones.reembolso("  ", 52_000))
        assertEquals("Es obligatorio.", Validaciones.reembolso(null, 52_000))
        assertEquals("Ingresa un monto entero.", Validaciones.reembolso("20.5", 52_000))
        assertEquals("Ingresa un monto entero.", Validaciones.reembolso("99999999999", 52_000))
    }
    @Test fun registroDevuelveErroresPorCampo() {
        val valido = RegistroSolicitante("Ana Pérez", "ana@test.cl", null, "Clave12345", "Empresa", "12.345.678-5")
        assertTrue(Validaciones.registro(valido).isEmpty())
        assertEquals(setOf("nombre", "correo", "telefono", "password", "razonSocial", "rutEmpresa"),
            Validaciones.registro(valido.copy(nombre = "a", correo = "x", telefono = "123", password = "x", razonSocial = "a", rutEmpresa = "123")).keys)
    }
}
