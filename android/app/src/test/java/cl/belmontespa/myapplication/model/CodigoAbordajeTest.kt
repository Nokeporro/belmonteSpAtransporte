package cl.belmontespa.myapplication.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodigoAbordajeTest {

    private val esperado = CodigoAbordaje("KXJR42")

    @Test
    fun unQrConPatenteDeDosLetrasYCuatroDigitosEsValido() {
        assertEquals(CodigoAbordaje("AB1234"), CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:AB1234"))
    }

    @Test
    fun unQrConPatenteDeCuatroLetrasYDosDigitosEsValido() {
        assertEquals(esperado, CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42"))
    }

    @Test
    fun elQrEnMinusculasSeAceptaPorqueSeLeeEnMayusculas() {
        assertEquals(esperado, CodigoAbordaje.desdeQr("belmonte-abordaje:kxjr42"))
    }

    @Test
    fun unQrDeOtroPrefijoNoEsDeUnVehiculoBelmonte() {
        assertNull(CodigoAbordaje.desdeQr("OTRA-APP:KXJR42"))
        assertNull(CodigoAbordaje.desdeQr("https://ejemplo.cl/KXJR42"))
        assertNull(CodigoAbordaje.desdeQr(""))
    }

    @Test
    fun unaPatenteMalFormadaEnElQrSeRechaza() {
        for (patente in listOf("KXJR4", "KXJR421", "KXJ421", "K1JR42", "AB12CD", "123456", "ABCDEF", "KX-R42", "")) {
            assertNull(patente, CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:$patente"))
        }
    }

    @Test
    fun unQrConPartesDeMasOEDeMenosSeRechaza() {
        assertNull(CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE"))
        assertNull(CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6"))
        assertNull(CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42\n"))
    }

    @Test
    fun elIngresoManualAceptaEspaciosGuionYMinusculas() {
        assertEquals(esperado, CodigoAbordaje.manual("kx-jr 42"))
        assertEquals(esperado, CodigoAbordaje.manual("  KXJR42 "))
    }

    @Test
    fun elIngresoManualConFormaInvalidaDaNull() {
        assertNull(CodigoAbordaje.manual("KXJR4"))
        assertNull(CodigoAbordaje.manual("KXJR421"))
        assertNull(CodigoAbordaje.manual(""))
    }

    @Test
    fun laPatenteViajaPorLaRutaYVuelveIgual() {
        assertEquals("KXJR42", esperado.paraRuta())
        assertEquals(esperado, CodigoAbordaje.desdeRuta(esperado.paraRuta()))
    }

    @Test
    fun unArgumentoDeRutaInvalidoNoEsUnaPatente() {
        assertNull(CodigoAbordaje.desdeRuta(""))
        assertNull(CodigoAbordaje.desdeRuta("KXJR4"))
        assertNull(CodigoAbordaje.desdeRuta("KXJR42:7F3A9C21B04E58D6"))
    }
}
