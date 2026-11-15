package cl.belmontespa.myapplication.navigation

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.notificaciones.AvisoPush
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinoDeAvisoTest {

    private fun destino(tipo: String?, folio: String?, rol: Rol) = destinoDeAviso(AvisoPush.desde(tipo, folio), rol)

    @Test
    fun laAsignacionAbreElDetalleDelViajeDelConductor() {
        assertEquals(Destino(PantallaDemo.DETALLE_VIAJE, "TR-1009"), destino("VIAJE", "TR-1009", Rol.CONDUCTOR))
    }

    @Test
    fun elAvisoDeViajeAbreElDetalleQueCorrespondeACadaRol() {
        assertEquals(Destino(PantallaDemo.DETALLE_SOLICITUD, "TR-1009"), destino("VIAJE", "TR-1009", Rol.SOLICITANTE))
        assertEquals(Destino(PantallaDemo.DETALLE_ADMIN, "TR-1009"), destino("VIAJE", "TR-1009", Rol.ADMINISTRADOR))
    }

    @Test
    fun laReasignacionAbreLaAgendaSoloDelConductor() {
        assertEquals(Destino(PantallaDemo.MIS_VIAJES), destino("AGENDA", "TR-1009", Rol.CONDUCTOR))
        assertNull(destino("AGENDA", "TR-1009", Rol.SOLICITANTE))
        assertNull(destino("AGENDA", "TR-1009", Rol.ADMINISTRADOR))
    }

    @Test
    fun lasCuentasPorAprobarSonDelAdministrador() {
        assertEquals(Destino(PantallaDemo.CUENTAS_PENDIENTES), destino("CUENTAS", null, Rol.ADMINISTRADOR))
        assertNull(destino("CUENTAS", null, Rol.CONDUCTOR))
        assertNull(destino("CUENTAS", null, Rol.SOLICITANTE))
    }

    @Test
    fun unViajeSinFolioValidoOUnTipoDesconocidoNoNavegan() {
        Rol.entries.forEach { rol ->
            assertNull(destino("VIAJE", null, rol))
            assertNull(destino("VIAJE", "../admin", rol))
            assertNull(destino("PROMOCION", "TR-1009", rol))
            assertNull(destino(null, "TR-1009", rol))
        }
    }

    @Test
    fun cadaDestinoDeAvisoEsUnaPantallaPermitidaParaSuRol() {
        Rol.entries.forEach { rol ->
            val sesion = EstadoSesion.Autenticada(Sesion("prueba", "cuenta@ejemplo.cl", rol, Long.MAX_VALUE))
            listOf("VIAJE" to "TR-1009", "AGENDA" to "TR-1009", "CUENTAS" to null).forEach { (tipo, folio) ->
                destino(tipo, folio, rol)?.let { assertTrue("$tipo $rol", permitePantalla(it.pantalla, sesion)) }
            }
        }
    }
}
