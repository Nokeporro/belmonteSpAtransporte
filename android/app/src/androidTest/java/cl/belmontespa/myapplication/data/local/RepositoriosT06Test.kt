package cl.belmontespa.myapplication.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.belmontespa.myapplication.data.local.database.*
import cl.belmontespa.myapplication.data.local.repository.*
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.util.FechaIso
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
    @Test fun altaDeConductorNormalizaPersisteYPermiteLoginInmediato() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val cuenta = NuevaCuenta("  Ana Conductora  ", "  ANA@demo.cl  ", "9 5555-0101", "Definitiva123")
        val alta = (flota.registrarConductor(cuenta) as Resultado.Exito).valor
        assertEquals("ana@demo.cl", alta.correo); assertEquals("Ana Conductora", alta.nombre)
        assertEquals("+56955550101", alta.telefono); assertTrue(alta.activo)
        assertEquals(EstadoConductor.DISPONIBLE, alta.estado)
        assertEquals(0, alta.viajesAsignados); assertNull(alta.folioEnCurso)
        assertEquals(alta, (FlotaLocalRepository(local).obtenerConductor(alta.correo) as Resultado.Exito).valor)
        assertNotEquals(cuenta.password, db.usuarioDao().obtener(alta.correo)!!.passwordSha256)
        assertTrue(AuthLocalRepository(db.usuarioDao(), sesiones, reloj, local).iniciarSesion(alta.correo, cuenta.password) is Resultado.Exito)
    }
    @Test fun unCorreoOcupadoPorOtroRolNoSeReemplazaAlRegistrarConductor() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val antes = local.usuario("crios@andesconsult.cl")
        val resultado = FlotaLocalRepository(local).registrarConductor(NuevaCuenta("Conductor", " CRIOS@ANDESCONSULT.CL ", null, "Clave12345"))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El correo ya está registrado.")), resultado)
        assertEquals(antes, local.usuario(antes.correo))
    }
    @Test fun datosInvalidosNoCreanLaCuentaDelConductor() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val cuenta = NuevaCuenta("A", "alta@demo.cl", "123", "abcdefgh")
        val resultado = flota.registrarConductor(cuenta) as Resultado.Fallo
        assertEquals(setOf("nombre", "telefono", "password"), (resultado.error as ErrorApp.Validacion).errores.keys)
        assertNull(db.usuarioDao().obtener(cuenta.correo))
    }
    @Test fun editarConductorConservaOCambiaLaClaveSinAlterarElCorreo() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val correo = "ireyes@belmontespa.cl"
        val hash = local.usuario(correo).passwordSha256
        assertTrue(flota.actualizarConductor(correo, "  Nombre Editado  ", "9 5555-0101", null) is Resultado.Exito)
        assertEquals(hash, local.usuario(correo).passwordSha256)
        assertEquals("+56955550101", local.usuario(correo).telefono)
        assertTrue(flota.actualizarConductor(correo, "Nombre Editado", null, "12345678") is Resultado.Fallo)
        assertEquals(hash, local.usuario(correo).passwordSha256)
        assertTrue(flota.actualizarConductor(correo, "Nombre Editado", null, "NuevaClave123") is Resultado.Exito)
        val auth = AuthLocalRepository(db.usuarioDao(), sesiones, reloj, local)
        assertTrue(auth.iniciarSesion(correo, "Belmonte2026") is Resultado.Fallo)
        assertTrue(auth.iniciarSesion(correo, "NuevaClave123") is Resultado.Exito)
    }
    @Test fun unConductorRecienCreadoSeAsignaConVehiculoHabilitado() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val nuevo = (flota.registrarConductor(NuevaCuenta("Nuevo Conductor", "nuevo@demo.cl", null, "Clave12345")) as Resultado.Exito).valor
        assertTrue(viajes.asignar("TR-1051", nuevo.correo, "KXJR42") is Resultado.Fallo)
        assertTrue(flota.habilitar("KXJR42", nuevo.correo) is Resultado.Exito)
        assertTrue(viajes.asignar("TR-1051", nuevo.correo, "KXJR42") is Resultado.Exito)
        val asignado = (flota.obtenerConductor(nuevo.correo) as Resultado.Exito).valor
        assertEquals(1, asignado.viajesAsignados); assertEquals(EstadoConductor.DISPONIBLE, asignado.estado)
    }
    @Test fun soloElAdministradorRegistraYEditaConductoresLocales() = runBlocking {
        val flota = FlotaLocalRepository(local)
        for (rol in listOf(Rol.CONDUCTOR, Rol.SOLICITANTE)) {
            entrar(rol)
            assertEquals(Resultado.Fallo(ErrorApp.SinPermiso()), flota.registrarConductor(NuevaCuenta("Sin permiso", "rechazado@demo.cl", null, "Clave12345")))
            assertEquals(Resultado.Fallo(ErrorApp.SinPermiso()), flota.actualizarConductor("ireyes@belmontespa.cl", "Sin permiso", null, "Clave12345"))
        }
        assertNull(db.usuarioDao().obtener("rechazado@demo.cl"))
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
    private suspend fun solicitudConPago(monto: Int, estado: String = "PAGADO"): Pair<ViajeEntity, PagoEntity> {
        val numero = local.viajes().maxOf { it.folioCorrelativo } + 1
        val folio = "TR-$numero"
        val v = local.viaje("TR-1051").copy(folio = folio, folioCorrelativo = numero, estado = "PENDIENTE",
            fhAgenda = FechaIso.texto(local.ahora().plusHours(3)), fhFin = null, hashPago = "pago-$numero", estadoPago = estado)
        val pago = PagoEntity("pago-$numero", folio, monto, estado, "Banco de prueba", 12,
            "12345678-5", "TRANSFERENCIA", "BOLETA", FechaIso.texto(local.ahora().minusDays(1)), null)
        db.viajeDao().guardar(v); db.pagoDao().insertar(pago)
        return v to pago
    }
    @Test fun cancelarReembolsaLaMitadRedondeadaYNoLaAplicaDosVeces() = runBlocking {
        entrar(Rol.SOLICITANTE)
        for (monto in listOf(50_001, 1)) {
            val (v, p) = solicitudConPago(monto)
            assertTrue(viajes.cancelar(v.folio) is Resultado.Exito)
            val pago = db.pagoDao().obtener(p.hash)!!
            assertEquals("REEMBOLSADO", pago.estadoPago); assertEquals(monto / 2, pago.montoReembolsado)
            assertEquals(p.fechaPago, pago.fechaPago); assertEquals(monto, pago.monto)
            assertEquals("REEMBOLSADO", local.viaje(v.folio).estadoPago)
            assertEquals(v.copy(estado = "CANCELADO", estadoPago = "REEMBOLSADO"), local.viaje(v.folio))
            assertTrue(viajes.cancelar(v.folio) is Resultado.Fallo)
            assertEquals(pago, db.pagoDao().obtener(p.hash))
        }
    }
    @Test fun cancelarAnulaUnPagoPendienteYRespetaLosTerminales() = runBlocking {
        entrar(Rol.SOLICITANTE)
        for (estado in listOf("PENDIENTE", "ANULADO", "RECHAZADO", "REEMBOLSADO")) {
            val (v, p) = solicitudConPago(25_000, estado)
            val original = if (estado == "REEMBOLSADO") p.copy(montoReembolsado = 7_000) else p
            db.pagoDao().guardar(original)
            assertTrue(viajes.cancelar(v.folio) is Resultado.Exito)
            assertEquals(if (estado == "PENDIENTE") original.copy(estadoPago = "ANULADO") else original,
                db.pagoDao().obtener(p.hash))
        }
    }
    @Test fun cancelarIncluyeElLimiteYDespuesNoTocaViajeNiPago() = runBlocking {
        entrar(Rol.SOLICITANTE)
        val (limite, _) = solicitudConPago(25_000)
        db.viajeDao().guardar(limite.copy(fhAgenda = FechaIso.texto(local.ahora().minusHours(1))))
        assertTrue(viajes.cancelar(limite.folio) is Resultado.Exito)
        val (v, pago) = solicitudConPago(50_001)
        val vencido = v.copy(fhAgenda = FechaIso.texto(local.ahora().minusHours(1).minusSeconds(1)))
        db.viajeDao().guardar(vencido)
        assertTrue(viajes.cancelar(v.folio) is Resultado.Fallo)
        assertEquals(vencido, local.viaje(v.folio)); assertEquals(pago, db.pagoDao().obtener(pago.hash))
    }
    @Test fun cancelarNoModificaUnaFlotaOcupadaPorOtroViaje() = runBlocking {
        entrar(Rol.SOLICITANTE)
        val (v, _) = solicitudConPago(25_000)
        val conductor = local.usuario("ireyes@belmontespa.cl").copy(estadoConductor = "OCUPADO")
        val movil = db.movilDao().obtener("KXJR42")!!.copy(estadoMovil = "EN_USO")
        db.usuarioDao().guardar(conductor); db.movilDao().guardar(movil)
        val asignado = v.copy(estado = "ASIGNADO", correoConductor = conductor.correo, patenteMovil = movil.patente)
        db.viajeDao().guardar(asignado)
        assertTrue(viajes.cancelar(v.folio) is Resultado.Exito)
        assertEquals(conductor, local.usuario(conductor.correo)); assertEquals(movil, db.movilDao().obtener(movil.patente))
        assertEquals(asignado.copy(estado = "CANCELADO", estadoPago = "REEMBOLSADO"), local.viaje(v.folio))
    }
    @Test fun cancelarNoAdmiteOtroDuenoConductorNiEstadosCerrados() = runBlocking {
        entrar(Rol.SOLICITANTE)
        val (v, p) = solicitudConPago(25_000)
        entrar(Rol.SOLICITANTE, "otra@andesconsult.cl")
        assertTrue(viajes.cancelar(v.folio) is Resultado.Fallo)
        entrar(Rol.CONDUCTOR)
        assertEquals(Resultado.Fallo(ErrorApp.SinPermiso()), viajes.cancelar(v.folio))
        entrar(Rol.SOLICITANTE)
        for (estado in listOf("EN_CURSO", "FINALIZADO", "CANCELADO")) {
            val cerrado = v.copy(estado = estado)
            db.viajeDao().guardar(cerrado)
            assertTrue(viajes.cancelar(v.folio) is Resultado.Fallo)
            assertEquals(cerrado, local.viaje(v.folio)); assertEquals(p, db.pagoDao().obtener(p.hash))
        }
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
