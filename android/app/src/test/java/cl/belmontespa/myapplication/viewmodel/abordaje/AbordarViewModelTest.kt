package cl.belmontespa.myapplication.viewmodel.abordaje

import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AbordarViewModelTest {

    private val vm = AbordarViewModel(aplicacionFalsa())
    private val esperado = CodigoAbordaje("KXJR42")

    @Test
    fun unQrDeUnVehiculoBelmonteDejaElCodigoListo() {
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42")

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
    fun unQrConUnaPatenteMalFormadaOConPartesDeMasEsAjeno() {
        vm.alLeer("BELMONTE-ABORDAJE:KXJR4")
        assertEquals(AvisoAbordar.QR_AJENO, vm.estado.value.aviso)

        vm.alEscanear()
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42:7F3A9C21B04E58D6")

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
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42")

        assertNull(vm.estado.value.aviso)
        assertEquals(esperado, vm.estado.value.listo)
    }

    @Test
    fun consumirLoListoNoDejaElCodigoParaLaVueltaAtras() {
        vm.alLeer("BELMONTE-ABORDAJE:KXJR42")

        vm.consumirListo()

        assertNull(vm.estado.value.listo)
    }

    @Test
    fun unLectorQueNoSePuedeAbrirAvisaYNoDejaNadaListo() {
        vm.lectorNoDisponible()

        assertEquals(AvisoAbordar.LECTOR_NO_DISPONIBLE, vm.estado.value.aviso)
        assertNull(vm.estado.value.listo)
    }

    @Test
    fun unNuevoEscaneoBorraElAvisoDelLectorNoDisponible() {
        vm.lectorNoDisponible()

        vm.alEscanear()

        assertNull(vm.estado.value.aviso)
    }

    @Test
    fun cancelarElLectorNoBorraElAvisoDelLectorNoDisponible() {
        vm.lectorNoDisponible()

        vm.alLeer(null)

        assertEquals(AvisoAbordar.LECTOR_NO_DISPONIBLE, vm.estado.value.aviso)
    }

    @Test
    fun unQrValidoDespuesDeUnLectorNoDisponibleQuitaElAviso() {
        vm.lectorNoDisponible()

        vm.alLeer("BELMONTE-ABORDAJE:KXJR42")

        assertNull(vm.estado.value.aviso)
        assertEquals(esperado, vm.estado.value.listo)
    }

    @Test
    fun elIngresoManualValidoQuitaElAvisoDelLectorNoDisponible() {
        vm.lectorNoDisponible()
        vm.abrirIngreso()
        vm.escribirPatente("KXJR42")

        vm.continuarIngreso()

        assertNull(vm.estado.value.aviso)
        assertEquals(esperado, vm.estado.value.listo)
    }

    @Test
    fun unQrAjenoDespuesDeUnLectorNoDisponibleCambiaElAviso() {
        vm.lectorNoDisponible()

        vm.alLeer("otra cosa")

        assertEquals(AvisoAbordar.QR_AJENO, vm.estado.value.aviso)
    }

    @Test
    fun elIngresoManualSoloSePuedeContinuarConLaPatenteEscrita() {
        vm.abrirIngreso()
        assertTrue(vm.estado.value.ingresando)
        assertFalse(vm.estado.value.puedeContinuar)

        vm.escribirPatente("KXJR42")
        assertTrue(vm.estado.value.puedeContinuar)
    }

    @Test
    fun continuarSinPatenteNoHaceNada() {
        vm.abrirIngreso()

        vm.continuarIngreso()

        assertNull(vm.estado.value.listo)
        assertTrue(vm.estado.value.ingresando)
        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun elIngresoManualConEspaciosYMinusculasDejaElCodigoListoYCierraElDialogo() {
        vm.abrirIngreso()
        vm.escribirPatente("kx jr42")

        vm.continuarIngreso()

        assertEquals(esperado, vm.estado.value.listo)
        assertFalse(vm.estado.value.ingresando)
        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun unIngresoManualMalFormadoMarcaElErrorYSeQuedaEnElDialogo() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR4")

        vm.continuarIngreso()

        assertTrue(vm.estado.value.ingresoInvalido)
        assertTrue(vm.estado.value.ingresando)
        assertNull(vm.estado.value.listo)
    }

    @Test
    fun corregirLoEscritoQuitaElError() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR4")
        vm.continuarIngreso()

        vm.escribirPatente("KXJR42")

        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun cerrarElDialogoConservaLoEscritoParaCorregirlo() {
        vm.abrirIngreso()
        vm.escribirPatente("KXJR4")
        vm.continuarIngreso()

        vm.cerrarIngreso()
        vm.abrirIngreso()

        assertEquals("KXJR4", vm.estado.value.patente)
        assertFalse(vm.estado.value.ingresoInvalido)
    }

    @Test
    fun elIngresoManualValidoQuitaElAvisoDelQrAjeno() {
        vm.alLeer("otra cosa")
        vm.abrirIngreso()
        vm.escribirPatente("KXJR42")

        vm.continuarIngreso()

        assertNull(vm.estado.value.aviso)
    }
}
