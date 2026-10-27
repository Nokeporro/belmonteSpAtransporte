package cl.belmontespa.myapplication.navigation

import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RutasSesionTest {

    private fun sesion(rol: Rol, correo: String = "usuario@belmontespa.cl") = Sesion("token", correo, rol, 1_000L)
    private fun autenticada(rol: Rol, correo: String = "usuario@belmontespa.cl") =
        EstadoSesion.Autenticada(sesion(rol, correo))

    // ---- Una ruta por pantalla ----

    @Test
    fun cadaPantallaTieneSuPropiaRuta() {
        val rutas = PantallaDemo.entries.map { it.ruta }
        assertEquals(46, rutas.toSet().size)
    }

    @Test
    fun soloLasPantallasConDatoLlevanElArgumentoId() {
        PantallaDemo.entries.forEach { assertEquals(it.name, it.ruta.contains("{id}"), it.conId) }
        assertTrue(PantallaDemo.DETALLE_ADMIN.conId)
        assertFalse(PantallaDemo.REGISTRO.conId)
    }

    @Test
    fun sinDatoLaRutaEsLaBase() {
        assertEquals("detalle_admin", PantallaDemo.DETALLE_ADMIN.hacia())
        assertEquals("registro", PantallaDemo.REGISTRO.hacia("ignorado"))
        assertEquals("mis_solicitudes", PantallaDemo.MIS_SOLICITUDES.hacia())
    }

    @Test
    fun laRutaDeUnDestinoVuelveASuPantalla() {
        PantallaDemo.entries.forEach { assertEquals(it, PantallaDemo.deRuta(it.ruta)) }
        assertNull(PantallaDemo.deRuta("grafo_acceso"))
        assertNull(PantallaDemo.deRuta(null))
    }

    @Test
    fun laBarraInferiorUsaLasRutasDeSusPantallas() {
        assertEquals(
            listOf("mis_solicitudes", "perfil_solicitante"),
            itemsBarra(Rol.SOLICITANTE).map { it.ruta },
        )
        assertEquals(PantallaDemo.SOLICITUDES.ruta, itemsBarra(Rol.ADMINISTRADOR).first().ruta)
        assertEquals(PantallaDemo.MIS_VIAJES.ruta, itemsBarra(Rol.CONDUCTOR).first().ruta)
        Rol.entries.forEach { assertEquals(inicioDe(it).ruta, itemsBarra(it).first().ruta) }
    }

    // ---- A dónde ir cuando cambia la sesión ----

    @Test
    fun alEntrarVaAlInicioDeSuRol() {
        Rol.entries.forEach { rol ->
            assertEquals(
                Destino(inicioDe(rol)),
                destinoPorSesion(EstadoSesion.SinSesion, autenticada(rol), cierreManual = false),
            )
        }
    }

    @Test
    fun conUnaContrasenaTemporalVaAlCambioObligatorio() {
        val temporal = EstadoSesion.Temporal(sesion(Rol.ADMINISTRADOR))
        assertEquals(Destino(PantallaDemo.CAMBIO_OBLIGATORIO), destinoPorSesion(EstadoSesion.SinSesion, temporal, false))
    }

    @Test
    fun perderLaSesionMuestraSesionExpiradaPeroCerrarlaVaAlLogin() {
        val anterior = autenticada(Rol.CONDUCTOR)
        assertEquals(Destino(PantallaDemo.SESION_EXPIRADA), destinoPorSesion(anterior, EstadoSesion.SinSesion, false))
        assertEquals(Destino(PantallaDemo.LOGIN), destinoPorSesion(anterior, EstadoSesion.SinSesion, true))
    }

    @Test
    fun sinSesionPrevia_elDestinoEsElLogin() {
        assertEquals(Destino(PantallaDemo.LOGIN), destinoPorSesion(null, EstadoSesion.SinSesion, false))
        assertEquals(Destino(PantallaDemo.LOGIN), destinoPorSesion(EstadoSesion.SinSesion, EstadoSesion.SinSesion, false))
    }

    @Test
    fun elLoginYElInicioDelRolVanAlGrafoDeSuFlujo() {
        assertEquals(Rutas.GRAFO_ACCESO, grafoDeSesion(Destino(PantallaDemo.LOGIN), EstadoSesion.SinSesion))
        val estado = autenticada(Rol.ADMINISTRADOR)
        assertEquals(Rutas.GRAFO_ADMINISTRADOR, grafoDeSesion(Destino(PantallaDemo.SOLICITUDES), estado))
        assertNull(grafoDeSesion(Destino(PantallaDemo.SESION_EXPIRADA), EstadoSesion.SinSesion))
        assertNull(grafoDeSesion(Destino(PantallaDemo.CAMBIO_OBLIGATORIO), EstadoSesion.Temporal(sesion(Rol.SOLICITANTE))))
    }

    // ---- Conservar la pantalla abierta (p. ej. tras una rotación) ----

    @Test
    fun unaRotacionConservaElDetalleAutorizado() {
        val detalle = Destino(PantallaDemo.DETALLE_ADMIN, "TR-1042")
        assertTrue(conservarPantalla(null, autenticada(Rol.ADMINISTRADOR), detalle))
        assertTrue(conservarPantalla(autenticada(Rol.ADMINISTRADOR), autenticada(Rol.ADMINISTRADOR), detalle))
    }

    @Test
    fun noSeConservaSinPantallaAbiertaNiAlEntrarNiAlCambiarDeCuenta() {
        val detalle = Destino(PantallaDemo.DETALLE_ADMIN, "TR-1042")
        val admin = autenticada(Rol.ADMINISTRADOR)
        assertFalse(conservarPantalla(null, admin, null))
        assertFalse(conservarPantalla(EstadoSesion.SinSesion, admin, detalle))
        assertFalse(conservarPantalla(EstadoSesion.Temporal(sesion(Rol.ADMINISTRADOR)), admin, detalle))
        assertFalse(conservarPantalla(autenticada(Rol.ADMINISTRADOR, "a@belmontespa.cl"), autenticada(Rol.ADMINISTRADOR, "b@belmontespa.cl"), detalle))
    }

    @Test
    fun noSeConservaUnaPantallaQueLaSesionNoPermite() {
        val detalleAdmin = Destino(PantallaDemo.DETALLE_ADMIN, "TR-1042")
        assertFalse(conservarPantalla(null, autenticada(Rol.CONDUCTOR), detalleAdmin))
        assertFalse(conservarPantalla(null, EstadoSesion.SinSesion, detalleAdmin))
    }

    @Test
    fun alPerderLaSesionNoSeConservaNiLaPantallaPublica() {
        val registro = Destino(PantallaDemo.REGISTRO)
        assertTrue(conservarPantalla(null, EstadoSesion.SinSesion, registro))
        assertFalse(conservarPantalla(autenticada(Rol.SOLICITANTE), EstadoSesion.SinSesion, registro))
    }

    @Test
    fun elCambioObligatorioSeAbandonaAlTenerSesionCompleta() {
        val cambio = Destino(PantallaDemo.CAMBIO_OBLIGATORIO)
        assertTrue(conservarPantalla(null, EstadoSesion.Temporal(sesion(Rol.ADMINISTRADOR)), cambio))
        assertFalse(conservarPantalla(null, autenticada(Rol.ADMINISTRADOR), cambio))
    }
}
