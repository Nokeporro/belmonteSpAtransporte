package cl.belmontespa.myapplication.notificaciones

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.navigation.Destino
import cl.belmontespa.myapplication.navigation.PantallaDemo
import org.junit.Assert.*
import org.junit.Test

class AvisoPushTest {

    @Test
    fun leeElTipoYElFolioDelPushDeAsignacion() {
        val aviso = AvisoPush.desde("VIAJE", "TR-1009", "Nuevo viaje asignado", "Viaje TR-1009 para 24-09-2026 · 14:30")

        assertEquals(TipoAviso.VIAJE, aviso.tipo)
        assertEquals("TR-1009", aviso.folio)
        assertEquals("Nuevo viaje asignado", aviso.titulo)
        assertEquals("TR-1009", aviso.etiqueta)
    }

    @Test
    fun reconoceLosTresTiposAunqueVenganEnMinusculasOConEspacios() {
        assertEquals(TipoAviso.AGENDA, AvisoPush.desde(" agenda ", "TR-1009").tipo)
        assertEquals(TipoAviso.CUENTAS, AvisoPush.desde("Cuentas", null).tipo)
        assertEquals(TipoAviso.VIAJE, AvisoPush.desde("viaje", "TR-1").tipo)
    }

    @Test
    fun unTipoDesconocidoOAusenteQuedaSinTipoPeroConservaElTexto() {
        val desconocido = AvisoPush.desde("PROMOCION", "TR-1009", "Título", "Texto")
        val ausente = AvisoPush.desde(null, null, "Título", "Texto")

        assertNull(desconocido.tipo)
        assertNull(ausente.tipo)
        assertEquals("Texto", ausente.mensaje)
        assertEquals("AVISO", ausente.etiqueta)
    }

    @Test
    fun descartaLosFoliosQueNoTienenElFormatoDelServidor() {
        listOf("", "  ", "1009", "TR-", "TR-10a9", "tr-1009", "TR-1009/../x", "TR-123456789012345678").forEach {
            assertNull(it, AvisoPush.desde("VIAJE", it).folio)
        }
        assertNull(AvisoPush.desde("VIAJE", null).folio)
    }

    @Test
    fun ignoraTituloYMensajeEnBlanco() {
        val aviso = AvisoPush.desde("AGENDA", "TR-1009", " ", "")

        assertNull(aviso.titulo)
        assertNull(aviso.mensaje)
    }

    @Test
    fun sinFolioLaEtiquetaEsElTipo() {
        assertEquals("CUENTAS", AvisoPush.desde("CUENTAS", null).etiqueta)

    }
    private fun sesion(rol: Rol) = EstadoSesion.Autenticada(Sesion("jwt", "cuenta@test.cl", rol, Long.MAX_VALUE))
    private fun viaje() = AvisoPush.deDatos(mapOf("tipo" to "VIAJE", "folio" to "TR-1001"))!!

    @Test fun elViajeAbreElDetalleCorrespondienteAlRolDeLaSesion() {
        val pantallas = mapOf(Rol.SOLICITANTE to PantallaDemo.DETALLE_SOLICITUD,
            Rol.ADMINISTRADOR to PantallaDemo.DETALLE_ADMIN, Rol.CONDUCTOR to PantallaDemo.DETALLE_VIAJE)
        pantallas.forEach { (rol, pantalla) ->
            assertEquals(Destino(pantalla, "TR-1001"), viaje().destino(sesion(rol)))
        }
    }

    @Test fun unaSesionTemporalOSinSesionNoAbreElViaje() {
        assertNull(viaje().destino(null))
        assertNull(viaje().destino(EstadoSesion.SinSesion))
        assertNull(viaje().destino(EstadoSesion.Temporal(sesion(Rol.SOLICITANTE).sesion)))
    }

    @Test fun ignoraLosTiposDesconocidosYLosFoliosMalFormados() {
        assertNull(AvisoPush.deDatos(emptyMap()))
        assertNull(AvisoPush.deDatos(mapOf("tipo" to "detalle_admin", "folio" to "TR-1001")))
        listOf("", "TR-0001", "TR-123", "TR-1001?id=otro", "TR-1001/../cuentas", "TR-100000000000000000").forEach {
            assertNull(it, AvisoPush.deDatos(mapOf("tipo" to "VIAJE", "folio" to it)))
        }
    }

    @Test fun aceptaElLimiteDeFolioDelContratoYDescartaLosExtras() {
        val aviso = AvisoPush.deDatos(mapOf("tipo" to "VIAJE", "folio" to "TR-12345678901234567",
            "rol" to "ADMINISTRADOR", "ruta" to "cuentas_pendientes"))!!
        assertEquals(PantallaDemo.DETALLE_SOLICITUD, aviso.destino(sesion(Rol.SOLICITANTE))!!.pantalla)
    }

    @Test fun soloElConductorAbreLaAgendaYNoElViajeDelQueFueReemplazado() {
        val aviso = AvisoPush.deDatos(mapOf("tipo" to "AGENDA", "folio" to "TR-1001"))!!
        assertEquals(Destino(PantallaDemo.MIS_VIAJES), aviso.destino(sesion(Rol.CONDUCTOR)))
        assertEquals("", aviso.destino(sesion(Rol.CONDUCTOR))!!.id)
        assertNull(aviso.destino(sesion(Rol.SOLICITANTE)))
        assertNull(aviso.destino(sesion(Rol.ADMINISTRADOR)))
    }

    @Test fun soloElAdministradorAbreLaRevisionDeCuentas() {
        val aviso = AvisoPush.deDatos(mapOf("tipo" to "CUENTAS"))!!
        assertEquals(Destino(PantallaDemo.CUENTAS_PENDIENTES), aviso.destino(sesion(Rol.ADMINISTRADOR)))
        assertNull(aviso.destino(sesion(Rol.SOLICITANTE)))
        assertNull(aviso.destino(sesion(Rol.CONDUCTOR)))
    }
}
