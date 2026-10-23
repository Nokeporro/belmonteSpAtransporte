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
import java.time.Clock
import java.time.Instant
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
