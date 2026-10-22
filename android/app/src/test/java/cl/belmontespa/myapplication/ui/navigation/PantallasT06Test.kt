package cl.belmontespa.myapplication.ui.navigation

import cl.belmontespa.myapplication.model.*
import org.junit.Assert.*
import org.junit.Test

// Comprueba cobertura y permisos; el recorrido visual completo se ejecuta en androidTest.
class PantallasT06Test {
    @Test fun defineLas46VariantesDeLosMockups() { assertEquals(46, PantallaDemo.entries.size) }
    @Test fun sinSesionNoAbrePantallasPrivadas() {
        PantallaDemo.entries.filter { !it.publica }.forEach { assertFalse(permitePantalla(it, EstadoSesion.SinSesion)) }
    }
    @Test fun cadaRolRechazaLasPantallasDeLosOtrosRoles() {
        Rol.entries.forEach { rol ->
            val sesion = EstadoSesion.Autenticada(Sesion("prueba", "cuenta@ejemplo.cl", rol, Long.MAX_VALUE))
            PantallaDemo.entries.filter { it.rol != null && it.rol != rol }.forEach { assertFalse(permitePantalla(it, sesion)) }
        }
    }
    @Test fun elConductorNoAccedeAlCambioDePasswordNiAlPago() {
        val sesion = EstadoSesion.Autenticada(Sesion("prueba", "conductor@ejemplo.cl", Rol.CONDUCTOR, Long.MAX_VALUE))
        assertFalse(permitePantalla(PantallaDemo.CAMBIAR_PASSWORD, sesion))
        assertFalse(permitePantalla(PantallaDemo.DETALLE_PAGO, sesion))
    }
    @Test fun laTemporalSoloAbreElCambioObligatorio() {
        val estado = EstadoSesion.Temporal(Sesion("temporal", "cuenta@ejemplo.cl", Rol.ADMINISTRADOR, Long.MAX_VALUE))
        PantallaDemo.entries.forEach { assertEquals(it == PantallaDemo.CAMBIO_OBLIGATORIO, permitePantalla(it, estado)) }
    }
}