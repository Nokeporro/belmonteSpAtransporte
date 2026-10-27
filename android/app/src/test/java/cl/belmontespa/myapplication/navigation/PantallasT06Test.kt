package cl.belmontespa.myapplication.navigation

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
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
    @Test fun guardarDesdeElDetalleDelMismoViajeLoReemplaza() {
        fun d(p: PantallaDemo, id: String = "TR-1051") = DestinoDemo(p, id)
        assertTrue(reemplazaDetalleAnterior(d(PantallaDemo.ASIGNAR), d(PantallaDemo.DETALLE_ADMIN), d(PantallaDemo.DETALLE_ADMIN)))
        assertTrue(reemplazaDetalleAnterior(d(PantallaDemo.CANCELAR_VIAJE), d(PantallaDemo.DETALLE_ADMIN), d(PantallaDemo.DETALLE_ADMIN)))
        assertTrue(reemplazaDetalleAnterior(d(PantallaDemo.AGREGAR_PASAJERO), d(PantallaDemo.DETALLE_SOLICITUD), d(PantallaDemo.DETALLE_SOLICITUD)))
        assertTrue(reemplazaDetalleAnterior(d(PantallaDemo.AGREGAR_PASAJERO), d(PantallaDemo.SOLICITUD_SIN_PASAJEROS), d(PantallaDemo.DETALLE_SOLICITUD)))
    }
    @Test fun otrosDestinosOtroViajeOSinFormularioApilanComoAntes() {
        fun d(p: PantallaDemo, id: String = "TR-1051") = DestinoDemo(p, id)
        assertFalse(reemplazaDetalleAnterior(d(PantallaDemo.ASIGNAR), d(PantallaDemo.DETALLE_ADMIN, "TR-1053"), d(PantallaDemo.DETALLE_ADMIN)))
        assertFalse(reemplazaDetalleAnterior(d(PantallaDemo.REGISTRAR_PAGO), d(PantallaDemo.DETALLE_ADMIN), d(PantallaDemo.DETALLE_PAGO, "hash")))
        assertFalse(reemplazaDetalleAnterior(d(PantallaDemo.CONFIRMAR_CANCELACION), d(PantallaDemo.DETALLE_SOLICITUD), d(PantallaDemo.SOLICITUD_CANCELADA)))
        assertFalse(reemplazaDetalleAnterior(d(PantallaDemo.ASIGNAR), null, d(PantallaDemo.DETALLE_ADMIN)))
        assertFalse(reemplazaDetalleAnterior(d(PantallaDemo.DETALLE_ADMIN), d(PantallaDemo.DETALLE_ADMIN), d(PantallaDemo.DETALLE_ADMIN)))
        assertFalse(reemplazaDetalleAnterior(null, d(PantallaDemo.DETALLE_ADMIN), d(PantallaDemo.DETALLE_ADMIN)))
    }
    @Test fun laTemporalSoloAbreElCambioObligatorio() {
        val estado = EstadoSesion.Temporal(Sesion("temporal", "cuenta@ejemplo.cl", Rol.ADMINISTRADOR, Long.MAX_VALUE))
        PantallaDemo.entries.forEach { assertEquals(it == PantallaDemo.CAMBIO_OBLIGATORIO, permitePantalla(it, estado)) }
    }
}