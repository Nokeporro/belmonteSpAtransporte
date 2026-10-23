package cl.belmontespa.myapplication.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.belmontespa.myapplication.data.local.database.*
import cl.belmontespa.myapplication.data.local.repository.*
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

// Pruebas contra Room real en memoria; ninguna escribe en la base instalada del usuario.
@RunWith(AndroidJUnit4::class)
class RepositoriosT06Test {
    private lateinit var db: BelmonteDatabase
    private lateinit var sesiones: SesionStore
    private lateinit var local: SoporteLocal
    private lateinit var viajes: ViajeLocalRepository
    private lateinit var operaciones: OperacionesViajeLocal
    private val reloj = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), ZoneId.of("America/Santiago"))
    @Before fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BelmonteDatabase::class.java).addCallback(SemillaDemo(reloj)).build()
        sesiones = SesionStore(PreferenciasRepositorio(), reloj)
        local = SoporteLocal(db, sesiones, reloj)
        operaciones = OperacionesViajeLocal(local)
        viajes = ViajeLocalRepository(db.viajeDao(), sesiones, reloj, operaciones)
    }
    @After fun cerrar() { db.close() }
    private suspend fun entrar(rol: Rol, correo: String = when(rol) {
        Rol.ADMINISTRADOR -> "recepcion@belmontespa.cl"
        Rol.SOLICITANTE -> "crios@andesconsult.cl"
        Rol.CONDUCTOR -> "ireyes@belmontespa.cl"
    }) { sesiones.guardar(Sesion("prueba", correo, rol, reloj.millis() + 3_600_000)) }
    @Test fun noEntregaViajesSinSesion() = runBlocking {
        assertTrue(viajes.observarViajes(FiltroViajes()).first().isEmpty())
        assertNull(viajes.observarViaje("TR-1042").first())
    }
    @Test fun unaTemporalNoEntregaViajes() = runBlocking {
        sesiones.guardarTemporal(Sesion("temporal", "recepcion@belmontespa.cl", Rol.ADMINISTRADOR, reloj.millis() + 3_600_000))
        assertTrue(viajes.observarViajes(FiltroViajes()).first().isEmpty())
    }
    @Test fun elSolicitanteSoloVeSusPropiosViajes() = runBlocking {
        entrar(Rol.SOLICITANTE)
        val original = local.viaje("TR-1042")
        db.viajeDao().guardar(original.copy(folio = "TR-2000", folioCorrelativo = 2000, correoSolicitante = "otra@andesconsult.cl"))
        val lista = viajes.observarViajes(FiltroViajes()).first()
        assertTrue(lista.isNotEmpty())
        assertTrue(lista.all { it.correoSolicitante == "crios@andesconsult.cl" })
        assertNull(viajes.observarViaje("TR-2000").first())
    }
    @Test fun elAdministradorVeTodoElAlcance() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        assertEquals(local.viajes().size, viajes.observarViajes(FiltroViajes()).first().size)
    }
    @Test fun elConductorNoRecibeInformacionDePago() = runBlocking {
        entrar(Rol.CONDUCTOR)
        val original = local.viaje("TR-1042")
        db.viajeDao().guardar(original.copy(hashPago = "pago-privado", estadoPago = "PAGADO"))
        val detalle = viajes.observarViaje(original.folio).first()!!
        assertNull(detalle.hashPago)
        assertNull(detalle.estadoPago)
        assertTrue(viajes.observarViajes(FiltroViajes()).first().all { it.correoConductor == "ireyes@belmontespa.cl" })
        assertNull(viajes.observarViaje("TR-1045").first())
    }
    @Test fun crearSolicitudPersisteElViajeYSusPasajeros() = runBlocking {
        entrar(Rol.SOLICITANTE)
        val base = viajes.observarViaje("TR-1042").first()!!
        val r = viajes.crearSolicitud(NuevaSolicitud(local.ahora().plusHours(3), base.origen, base.destino, null,
            listOf(NuevoPasajero(TipoDocumento.PASAPORTE, "DEMO123", "Pasajero de prueba"))))
        assertTrue(r is Resultado.Exito)
        val guardado = viajes.observarViaje((r as Resultado.Exito).valor).first()!!
        assertEquals(1, guardado.cantidadPasajeros)
        assertEquals("Pasajero de prueba", guardado.pasajeros.single().nombre)
        assertEquals(EstadoViaje.PENDIENTE, guardado.estado)
    }
    @Test fun cancelarUnaSolicitudSeReflejaEnRoom() = runBlocking {
        entrar(Rol.SOLICITANTE)
        assertTrue(viajes.cancelar("TR-1051") is Resultado.Exito)
        assertEquals("CANCELADO", local.viaje("TR-1051").estado)
    }
    @Test fun registroNuevoApruebaYEmpresaExistenteDejaLaCuentaPendiente() = runBlocking {
        val auth = AuthLocalRepository(db.usuarioDao(), sesiones, reloj, local)
        val primera = RegistroSolicitante("Ana Demo", " ANA@nuevo.cl ", "9 5555-0101", "Clave12345", "Empresa Nueva", "12.345.678-5")
        val aprobada = auth.registrarSolicitante(primera) as Resultado.Exito
        assertEquals(EstadoSolicitante.APROBADO, aprobada.valor.estado)
        assertEquals("ana@nuevo.cl", aprobada.valor.correo)
        assertEquals("+56955550101", aprobada.valor.telefono)
        assertTrue(auth.registrarSolicitante(primera) is Resultado.Fallo)
        val segunda = auth.registrarSolicitante(primera.copy(correo = "otra@nuevo.cl", razonSocial = "No reemplaza la empresa")) as Resultado.Exito
        assertEquals(EstadoSolicitante.PENDIENTE, segunda.valor.estado)
        assertEquals("Empresa Nueva", segunda.valor.razonSocial)
        assertEquals(Resultado.Fallo(ErrorApp.SinPermiso(MotivoSinPermiso.CUENTA_PENDIENTE)), auth.iniciarSesion("otra@nuevo.cl", "Clave12345"))
        assertTrue(auth.iniciarSesion("ana@nuevo.cl", "Clave12345") is Resultado.Exito)
    }
    @Test fun elLoginDeUnConductorInactivoEsGenerico() = runBlocking {
        val auth = AuthLocalRepository(db.usuarioDao(), sesiones, reloj, local)
        val conductor = local.usuario("ireyes@belmontespa.cl")
        db.usuarioDao().guardar(conductor.copy(activo = false))
        assertEquals(Resultado.Fallo(ErrorApp.SesionExpirada), auth.iniciarSesion(conductor.correo, "Belmonte2026"))
        assertEquals(Resultado.Fallo(ErrorApp.SesionExpirada), auth.iniciarSesion("inexistente@demo.cl", "Belmonte2026"))
    }
    @Test fun unaSolicitudInvalidaNoPersisteNiElViajeNiSusPasajeros() = runBlocking {
        entrar(Rol.SOLICITANTE)
        val base = viajes.observarViaje("TR-1042").first()!!
        val antes = local.viajes().size
        val s = NuevaSolicitud(local.ahora().plusHours(3), base.origen, base.destino, null,
            listOf(NuevoPasajero(TipoDocumento.RUT, "12345678-4", "Pasajero")))
        assertTrue(viajes.crearSolicitud(s) is Resultado.Fallo)
        assertEquals(antes, local.viajes().size)
        assertTrue(viajes.crearSolicitud(s.copy(pasajeros = listOf(NuevoPasajero(TipoDocumento.PASAPORTE, "DEMO123", "Pasajero")), destino = base.origen)) is Resultado.Fallo)
        assertEquals(antes, local.viajes().size)
    }
    @Test fun repetirElAbordajeConservaLaMarca() = runBlocking {
        val abordaje = AbordajeLocalRepository(local)
        val codigo = CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6")
        val primera = abordaje.registrarAbordaje(codigo, TipoDocumento.PASAPORTE, "P4471023")
        assertTrue(primera is Resultado.Exito)
        val segunda = abordaje.registrarAbordaje(codigo, TipoDocumento.PASAPORTE, "P4471023")
        assertEquals(primera, segunda)
    }
    @Test fun unAbordajeAjenoNoModificaElViaje() = runBlocking {
        val antes = db.viajePasajeroDao().obtenerDelViaje("TR-1042")
        val resultado = AbordajeLocalRepository(local).registrarAbordaje(CodigoAbordaje("KXJR42", "0000000000000000"), TipoDocumento.PASAPORTE, "P4471023")
        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), resultado)
        assertEquals(antes, db.viajePasajeroDao().obtenerDelViaje("TR-1042"))
    }
    // Abordaje (HU29): las mismas reglas que `AbordajeService` del backend, con la semilla demo y el reloj fijo (12:00 en Chile).
    private val kxjr42 = CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6")
    @Test fun unPasajeroYaAbordadoRecibeSuMarcaOriginal() = runBlocking {
        val resultado = AbordajeLocalRepository(local).registrarAbordaje(kxjr42, TipoDocumento.RUT, "15.234.567-4")
        assertEquals(Resultado.Exito(LocalDateTime.of(2026, 10, 21, 11, 55)), resultado)
    }
    @Test fun unRutSinPuntosYUnCodigoConEspaciosCalzanIgual() = runBlocking {
        val resultado = AbordajeLocalRepository(local).registrarAbordaje(CodigoAbordaje("KXJR42", "7f3a 9c21 b04e 58d6"), TipoDocumento.RUT, "16789012-1")
        assertEquals(Resultado.Exito(LocalDateTime.of(2026, 10, 21, 12, 0)), resultado)
    }
    @Test fun unDocumentoQueNoViajaEnElVehiculoEsNoEncontradoYNoCambiaNada() = runBlocking {
        val antes = db.viajePasajeroDao().obtenerDelViaje("TR-1042")
        // Felipe Mora viaja en LPHT17 (TR-1049), no en KXJR42.
        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), AbordajeLocalRepository(local).registrarAbordaje(kxjr42, TipoDocumento.PASAPORTE, "X9087654"))
        assertEquals(antes, db.viajePasajeroDao().obtenerDelViaje("TR-1042"))
    }
    @Test fun unViajeAsignadoSoloSeAbordaDentroDeLaVentanaDeUnaHora() = runBlocking {
        val abordaje = AbordajeLocalRepository(local)
        val lphT17 = CodigoAbordaje("LPHT17", "2B8E41D07C95A316")
        // TR-1049 parte en 4 h 15 min: aún no es candidato.
        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), abordaje.registrarAbordaje(lphT17, TipoDocumento.PASAPORTE, "X9087654"))
        val ahora = local.ahora()
        db.viajeDao().guardar(local.viaje("TR-1049").copy(fhAgenda = FechaIso.texto(ahora.plusHours(1).plusSeconds(1))))
        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), abordaje.registrarAbordaje(lphT17, TipoDocumento.PASAPORTE, "X9087654"))
        db.viajeDao().guardar(local.viaje("TR-1049").copy(fhAgenda = FechaIso.texto(ahora.plusHours(1))))
        assertEquals(Resultado.Exito(ahora), abordaje.registrarAbordaje(lphT17, TipoDocumento.PASAPORTE, "X9087654"))
    }
    @Test fun unViajeEnCursoSeAbordaAunqueSuAgendaEsteLejos() = runBlocking {
        val gzrw90 = CodigoAbordaje("GZRW90", "9D04C6E2A1F7B853")
        db.viajeDao().guardar(local.viaje("TR-1045").copy(fhAgenda = FechaIso.texto(local.ahora().minusHours(6))))
        // Javiera Núñez ya estaba abordada en TR-1045: recibe la misma marca.
        assertEquals(Resultado.Exito(LocalDateTime.of(2026, 10, 21, 11, 30)), AbordajeLocalRepository(local).registrarAbordaje(gzrw90, TipoDocumento.RUT, "18.364.925-6"))
    }
    @Test fun unDocumentoInvalidoEsValidacionAunqueElCodigoSeaAjeno() = runBlocking {
        val resultado = AbordajeLocalRepository(local).registrarAbordaje(CodigoAbordaje("KXJR42", "0000000000000000"), TipoDocumento.RUT, "15.234.567-0")
        assertTrue(resultado is Resultado.Fallo && resultado.error is ErrorApp.Validacion)
    }
    // Arqueo (HU30): el viaje que observa el Conductor refleja el abordaje apenas se registra.
    @Test fun alAbordarUnPasajeroElArqueoDelViajeSeActualiza() = runBlocking {
        entrar(Rol.CONDUCTOR)
        val antes = viajes.observarViaje("TR-1042").first()!!.arqueo()!!
        assertEquals(1, antes.abordaron)
        assertEquals(3, antes.total)
        assertEquals(setOf("Marcela Soto", "Tomás Vidal"), antes.faltan.toSet())
        AbordajeLocalRepository(local).registrarAbordaje(kxjr42, TipoDocumento.PASAPORTE, "P4471023")
        val despues = viajes.observarViaje("TR-1042").first()!!.arqueo()!!
        assertEquals(2, despues.abordaron)
        assertEquals(3, despues.total)
        assertEquals(setOf("Marcela Soto"), despues.faltan.toSet())
    }
    @Test fun repetirElAbordajeNoCambiaElArqueo() = runBlocking {
        entrar(Rol.CONDUCTOR)
        val abordaje = AbordajeLocalRepository(local)
        abordaje.registrarAbordaje(kxjr42, TipoDocumento.PASAPORTE, "P4471023")
        val unaVez = viajes.observarViaje("TR-1042").first()!!
        abordaje.registrarAbordaje(kxjr42, TipoDocumento.PASAPORTE, "P4471023")
        val dosVeces = viajes.observarViaje("TR-1042").first()!!
        assertEquals(unaVez.pasajeros.toSet(), dosVeces.pasajeros.toSet())
        assertEquals(unaVez.arqueo(), dosVeces.arqueo())
    }
    // V2 no cambia el estado de nadie: Ocupado y En uso los marca V3, al iniciar el viaje.
    @Test fun asignarNoCambiaElEstadoDelConductorNiDelVehiculo() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        assertTrue(viajes.asignar("TR-1051", "pmunoz@belmontespa.cl", "KXJR42") is Resultado.Exito)
        val guardado = local.viaje("TR-1051")
        assertEquals("ASIGNADO", guardado.estado)
        assertEquals("pmunoz@belmontespa.cl", guardado.correoConductor)
        assertNotNull(guardado.fhAsignacion)
        assertEquals("DISPONIBLE", local.usuario("pmunoz@belmontespa.cl").estadoConductor)
        assertEquals("DISPONIBLE", db.movilDao().obtener("KXJR42")?.estadoMovil)
    }
    @Test fun unConductorOcupadoNoSePuedeAsignar() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El conductor no está disponible")),
            viajes.asignar("TR-1051", "jlara@belmontespa.cl", "GZRW90"))
        assertEquals("PENDIENTE", local.viaje("TR-1051").estado)
    }
    @Test fun otroViajeDelConductorAMenosDeDosHorasEsConflicto() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        // TR-1047 es una hora antes que TR-1042, el viaje de Iván Reyes.
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El conductor ya tiene un viaje a menos de 2 horas")),
            viajes.asignar("TR-1047", "ireyes@belmontespa.cl", "KXJR42"))
        assertEquals("PENDIENTE", local.viaje("TR-1047").estado)
    }
    @Test fun reasignarSoloElVehiculoVuelveAPorAbordar() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        // LPHT17 tiene TR-1049 casi cuatro horas después: no choca.
        assertTrue(viajes.asignar("TR-1042", "ireyes@belmontespa.cl", "LPHT17") is Resultado.Exito)
        assertEquals("LPHT17", local.viaje("TR-1042").patenteMovil)
        assertTrue(db.viajePasajeroDao().obtenerDelViaje("TR-1042").all { it.fhAbordaje == null })
    }
    // V3 y V4 (HU09): los mismos mensajes que el backend.
    @Test fun iniciarYFinalizarMarcanYLiberanAlConductorYAlVehiculo() = runBlocking {
        entrar(Rol.CONDUCTOR)
        assertTrue(viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO) is Resultado.Exito)
        assertEquals("EN_CURSO", local.viaje("TR-1042").estado)
        assertNotNull(local.viaje("TR-1042").fhInicio)
        assertEquals("OCUPADO", local.usuario("ireyes@belmontespa.cl").estadoConductor)
        assertEquals("EN_USO", db.movilDao().obtener("KXJR42")?.estadoMovil)
        assertTrue(viajes.cambiarEstado("TR-1042", EstadoViaje.FINALIZADO) is Resultado.Exito)
        assertEquals("FINALIZADO", local.viaje("TR-1042").estado)
        assertNotNull(local.viaje("TR-1042").fhFin)
        assertEquals("DISPONIBLE", local.usuario("ireyes@belmontespa.cl").estadoConductor)
        assertEquals("DISPONIBLE", db.movilDao().obtener("KXJR42")?.estadoMovil)
    }
    @Test fun iniciarAntesDeLaVentanaEsConflictoYNoCambiaNada() = runBlocking {
        entrar(Rol.CONDUCTOR)
        // TR-1049 es cuatro horas y cuarto después: la ventana abre una hora antes.
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El viaje está fuera de la ventana de inicio")),
            viajes.cambiarEstado("TR-1049", EstadoViaje.EN_CURSO))
        assertEquals("ASIGNADO", local.viaje("TR-1049").estado)
        assertNull(local.viaje("TR-1049").fhInicio)
        assertEquals("DISPONIBLE", local.usuario("ireyes@belmontespa.cl").estadoConductor)
    }
    @Test fun otroViajeEnCursoDelConductorOdelVehiculoImpideIniciar() = runBlocking {
        entrar(Rol.CONDUCTOR)
        // TR-1045 está En curso con Jorge Lara en GZRW90: se lo presta a Iván, y luego solo el vehículo.
        val enCurso = local.viaje("TR-1045")
        db.viajeDao().guardar(enCurso.copy(correoConductor = "ireyes@belmontespa.cl"))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El conductor ya tiene un viaje en curso")),
            viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO))
        db.viajeDao().guardar(enCurso.copy(patenteMovil = "KXJR42"))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El móvil ya tiene un viaje en curso")),
            viajes.cambiarEstado("TR-1042", EstadoViaje.EN_CURSO))
        assertEquals("ASIGNADO", local.viaje("TR-1042").estado)
    }
    @Test fun unaTransicionInvalidaDiceDeQueEstadoANingunLado() = runBlocking {
        entrar(Rol.CONDUCTOR)
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("No se puede pasar de ASIGNADO a FINALIZADO")),
            viajes.cambiarEstado("TR-1042", EstadoViaje.FINALIZADO))
    }
    @Test fun repetirLaAsignacionNoCambiaNada() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val viajeAntes = local.viaje("TR-1042")
        val pasajerosAntes = db.viajePasajeroDao().obtenerDelViaje("TR-1042")
        assertTrue(viajes.asignar("TR-1042", "ireyes@belmontespa.cl", "KXJR42") is Resultado.Exito)
        assertEquals(viajeAntes, local.viaje("TR-1042"))
        assertEquals(pasajerosAntes, db.viajePasajeroDao().obtenerDelViaje("TR-1042"))
    }
}
private class PreferenciasRepositorio : DataStore<Preferences> {
    private val estado = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = estado
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(estado.value).also { estado.value = it }
}
