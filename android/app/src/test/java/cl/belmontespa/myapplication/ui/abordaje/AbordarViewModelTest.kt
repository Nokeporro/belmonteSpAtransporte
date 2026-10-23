package cl.belmontespa.myapplication.ui.abordaje

import cl.belmontespa.myapplication.model.CodigoAbordaje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Abordar viaje» (HU29): del QR o de la patente y el código escritos hasta un [CodigoAbordaje] listo para confirmar. */
class AbordarViewModelTest {

    private val vm = AbordarViewModel()
    private val esperado = CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6")

    @Test
    fun unQrDeUnVehiculoBelmonteDejaElCodigoListo() {
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6")

        assertEquals(esperado, vm.estado.value.listo)
        assertNull(vm.estado.value.aviso)
    }

    @Test
    fun unQrAjenoMuestraElAvisoYNoDejaNadaListo() {
        vm.alLeer("https://ejemplo.cl/otro-qr")

        assertEquals(AvisoAbordar.QR_AJENO, vm.estado.value.aviso)
        assertNull(vm.estado.value.listo)
    }

    @Test
    fun unQrConUnCodigoMalFormadoEsAjeno() {
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58")

        assertEquals(AvisoAbordar.QR_AJENO, vm.estado.value.aviso)
    }

    @Test
    fun cancelarElLectorVuelveSinAviso() {
        vm.alLeer(null)

        assertEquals(AbordarUiState(), vm.estado.value)
    }

    @Test
    fun cancelarElLectorNoBorraElAvisoAnteriorPeroUnNuevoEscaneoSi() {
        vm.alLeer("otra cosa")
        vm.alLeer(null)
        assertEquals(AvisoAbordar.QR_AJENO, vm.estado.value.aviso)

        vm.alEscanear()

        assertNull(vm.estado.value.aviso)
    }

    @Test
    fun unQrValidoDespuesDeUnoAjenoQuitaElAviso() {
        vm.alLeer("otra cosa")
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6")

        assertNull(vm.estado.value.aviso)
        assertEquals(esperado, vm.estado.value.listo)
    }

    @Test
    fun consumirLoListoNoDejaElCodigoParaLaVueltaAtras() {
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6")

        vm.consumirListo()

        assertNull(vm.estado.value.listo)
    }

    @Test
    fun lasDosFormasDeNegarLaCamaraSeDistinguen() {
        vm.camaraDenegada(paraSiempre = false)
        assertEquals(AvisoAbordar.CAMARA_NEGADA, vm.estado.value.aviso)
        assertTrue(vm.estado.value.camaraNegada)

        vm.camaraDenegada(paraSiempre = true)
        assertEquals(AvisoAbordar.CAMARA_NEGADA_PARA_SIEMPRE, vm.estado.value.aviso)
        assertTrue(vm.estado.value.camaraNegada)
    }

    @Test
    fun otroAvisoNoEsCamaraNegada() {
        vm.alLeer("otra cosa")

        assertFalse(vm.estado.value.camaraNegada)
    }

    @Test
    fun elIngresoManualSoloSePuedeContinuarConAmbosCamposEscritos() {
        vm.abrirIngreso()
        assertTrue(vm.estado.value.ingresando)
        assertFalse(vm.estado.value.puedeContinuar)

        vm.escribirPatente("KXJR42")
        assertFalse(vm.estado.value.puedeContinuar)

        vm.escribirCodigo("7F3A9C21B04E58D6")
        assertTrue(vm.estado.value.puedeContinuar)
    }

    @Test
    fun continuarSinLosDosCamposNoHaceNada() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR42")

        vm.continuarIngreso()

        assertNull(vm.estado.value.listo)
        assertTrue(vm.estado.value.ingresando)
        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun elIngresoManualConEspaciosYMinusculasDejaElCodigoListoYCierraElDialogo() {
        vm.abrirIngreso()
        vm.escribirPatente("kx jr42")
        vm.escribirCodigo("7f3a 9c21 b04e 58d6")

        vm.continuarIngreso()

        assertEquals(esperado, vm.estado.value.listo)
        assertFalse(vm.estado.value.ingresando)
        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun unIngresoManualMalFormadoMarcaElErrorYSeQuedaEnElDialogo() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR4")
        vm.escribirCodigo("7F3A9C21B04E58D6")

        vm.continuarIngreso()

        assertTrue(vm.estado.value.ingresoInvalido)
        assertTrue(vm.estado.value.ingresando)
        assertNull(vm.estado.value.listo)
    }

    @Test
    fun corregirLoEscritoQuitaElErrorYConservaElOtroCampo() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR4")
        vm.escribirCodigo("7F3A9C21B04E58D6")
        vm.continuarIngreso()

        vm.escribirPatente("KXJR42")

        assertFalse(vm.estado.value.ingresoInvalido)
        assertEquals("7F3A9C21B04E58D6", vm.estado.value.codigo)
    }

    @Test
    fun cerrarElDialogoConservaLoEscritoParaCorregirlo() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR4")
        vm.escribirCodigo("7F3A9C21B04E58D6")
        vm.continuarIngreso()

        vm.cerrarIngreso()
        vm.abrirIngreso()

        assertEquals("KXJR4", vm.estado.value.patente)
        assertEquals("7F3A9C21B04E58D6", vm.estado.value.codigo)
        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun elIngresoManualValidoQuitaElAvisoDelQrAjeno() {
        vm.alLeer("otra cosa")
        vm.abrirIngreso()
        vm.escribirPatente("KXJR42")
        vm.escribirCodigo("7F3A9C21B04E58D6")

        vm.continuarIngreso()

        assertNull(vm.estado.value.aviso)
    }
}
