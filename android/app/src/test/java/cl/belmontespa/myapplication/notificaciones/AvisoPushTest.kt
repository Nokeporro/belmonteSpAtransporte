package cl.belmontespa.myapplication.notificaciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        listOf("", "  ", "1009", "TR-", "TR-10a9", "tr-1009", "TR-1009/../x", "TR-12345678901").forEach {
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
}
