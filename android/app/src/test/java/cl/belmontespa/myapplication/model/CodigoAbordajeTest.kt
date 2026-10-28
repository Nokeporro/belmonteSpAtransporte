package cl.belmontespa.myapplication.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El código del abordaje (README, Abordaje con código QR): del QR de la puerta o escrito bajo él, siempre normalizado. */
class CodigoAbordajeTest {

    private val esperado = CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6")

    @Test
    fun unQrConPatenteDeDosLetrasYCuatroDigitosEsValido() {
        assertEquals(CodigoAbordaje("AB1234", "7F3A9C21B04E58D6"), CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:AB1234:7F3A9C21B04E58D6"))
    }

    @Test
    fun unQrConPatenteDeCuatroLetrasYDosDigitosEsValido() {
        assertEquals(esperado, CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6"))
    }

    @Test
    fun elQrEnMinusculasSeAceptaPorqueSeLeeEnMayusculas() {
        assertEquals(esperado, CodigoAbordaje.desdeQr("belmonte-abordaje:kxjr42:7f3a9c21b04e58d6"))
    }

    @Test
    fun unQrDeOtroPrefijoNoEsDeUnVehiculoBelmonte() {
        assertNull(CodigoAbordaje.desdeQr("OTRA-APP:KXJR42:7F3A9C21B04E58D6"))
        assertNull(CodigoAbordaje.desdeQr("https://ejemplo.cl/KXJR42"))
        assertNull(CodigoAbordaje.desdeQr(""))
    }

    @Test
    fun unaPatenteMalFormadaEnElQrSeRechaza() {
        for (patente in listOf("KXJR4", "KXJR421", "KXJ421", "K1JR42", "AB12CD", "123456", "ABCDEF", "KX-R42")) {
            assertNull(patente, CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:$patente:7F3A9C21B04E58D6"))
        }
    }

    @Test
    fun unCodigoMalFormadoEnElQrSeRechaza() {
        for (codigo in listOf("7F3A9C21B04E58D", "7F3A9C21B04E58D6A", "7F3A9C21B04E58DG", "7F3A 9C21B04E58D6", "")) {
            assertNull(codigo, CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42:$codigo"))
        }
    }

    @Test
    fun unQrConPartesDeMasOEDeMenosSeRechaza() {
        assertNull(CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42"))
        assertNull(CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6:EXTRA"))
        assertNull(CodigoAbordaje.desdeQr("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6\n"))
    }

    @Test
    fun elIngresoManualAceptaEspaciosGuionYMinusculas() {
        assertEquals(esperado, CodigoAbordaje.manual("kx-jr 42", "7f3a 9c21 b04e 58d6"))
        assertEquals(esperado, CodigoAbordaje.manual("  KXJR42 ", " 7F3A9C21B04E58D6 "))
    }

    @Test
    fun elIngresoManualConFormaInvalidaDaNull() {
        assertNull(CodigoAbordaje.manual("KXJR4", "7F3A9C21B04E58D6"))
        assertNull(CodigoAbordaje.manual("KXJR42", "7F3A9C21B04E58"))
        assertNull(CodigoAbordaje.manual("KXJR42", "7F3A9C21B04E58DZ"))
        assertNull(CodigoAbordaje.manual("", ""))
    }

    @Test
    fun elCodigoViajaPorLaRutaYVuelveIgual() {
        assertEquals("KXJR42:7F3A9C21B04E58D6", esperado.paraRuta())
        assertEquals(esperado, CodigoAbordaje.desdeRuta(esperado.paraRuta()))
    }

    @Test
    fun unArgumentoDeRutaInvalidoNoEsUnCodigo() {
        assertNull(CodigoAbordaje.desdeRuta(""))
        assertNull(CodigoAbordaje.desdeRuta("KXJR42"))
        assertNull(CodigoAbordaje.desdeRuta("KXJR42:ZZZZ"))
        assertNull(CodigoAbordaje.desdeRuta("KXJR42:7F3A9C21B04E58D6:X"))
    }
}
