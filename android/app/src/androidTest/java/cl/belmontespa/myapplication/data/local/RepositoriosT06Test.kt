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
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    @Test fun habilitacionesMuchosAMuchosConFiltrosNormalizadosYCombinados() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val a = (flota.registrarConductor(NuevaCuenta("Ana HU18", "ana18@demo.cl", null, "Clave12345")) as Resultado.Exito).valor
        val b = (flota.registrarConductor(NuevaCuenta("Beto HU18", "beto18@demo.cl", null, "Clave12345")) as Resultado.Exito).valor
        flota.registrarMovil(NuevoMovil("ABCD18", 8, null)); flota.registrarMovil(NuevoMovil("WXYZ18", 2, null))
        assertTrue(flota.habilitar("ab-cd18", " ANA18@DEMO.CL ") is Resultado.Exito)
        flota.habilitar("ABCD18", b.correo); flota.habilitar("WXYZ18", a.correo)
        val porVehiculo = (flota.listarHabilitaciones(patente = "ab-cd18") as Resultado.Exito).valor
        assertEquals(listOf("Ana HU18", "Beto HU18"), porVehiculo.map { it.nombreConductor })
        assertTrue(porVehiculo.all { it.capacidad == 8 && it.activoMovil && it.activoConductor })
        assertEquals(2, (flota.listarHabilitaciones(correo = " ANA18@DEMO.CL ") as Resultado.Exito).valor.size)
        assertEquals(listOf(b.correo), (flota.listarHabilitaciones("ABCD18", b.correo) as Resultado.Exito).valor.map { it.correoConductor })
        assertTrue((flota.listarHabilitaciones("WXYZ18", b.correo) as Resultado.Exito).valor.isEmpty())
    }
    @Test fun habilitacionesRepetidasYSimultaneasConservanUnaSolaRelacion() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        flota.registrarMovil(NuevoMovil("ABCD18", 3, null))
        coroutineScope {
            val uno = async { flota.habilitar("ABCD18", "ireyes@belmontespa.cl") }
            val dos = async { flota.habilitar("ab-cd18", "IREYES@BELMONTESPA.CL") }
            assertTrue(uno.await() is Resultado.Exito); assertTrue(dos.await() is Resultado.Exito)
        }
        assertTrue(flota.habilitar("ABCD18", "ireyes@belmontespa.cl") is Resultado.Exito)
        assertEquals(1, db.habilitacionDao().observarDelMovil("ABCD18").first().size)
    }
    @Test fun habilitacionConservaEstadosEInactivosSinPermitirNuevasRelaciones() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        flota.registrarMovil(NuevoMovil("ABCD18", 3, null)); flota.habilitar("ABCD18", "ireyes@belmontespa.cl")
        val m = db.movilDao().obtener("ABCD18")!!
        db.movilDao().guardar(m.copy(activo = false, estadoMovil = "EN_USO"))
        val u = local.usuario("ireyes@belmontespa.cl")
        db.usuarioDao().guardar(u.copy(activo = false, estadoConductor = "OCUPADO"))
        val h = (flota.listarHabilitaciones(patente = "ABCD18") as Resultado.Exito).valor.single()
        assertFalse(h.activoMovil); assertFalse(h.activoConductor)
        assertEquals(EstadoMovil.EN_USO, h.estadoMovil); assertEquals(EstadoConductor.OCUPADO, h.estadoConductor)
        assertEquals(Resultado.Exito(h), flota.habilitar("ABCD18", u.correo))
        assertTrue((flota.habilitar("ABCD18", "pmunoz@belmontespa.cl") as Resultado.Fallo).error is ErrorApp.Conflicto)
        assertTrue((flota.habilitar("KXJR42", u.correo)) is Resultado.Exito)
    }
    @Test fun soloAdministradorConsultaYAgregaHabilitaciones() = runBlocking {
        val flota = FlotaLocalRepository(local)
        for (rol in listOf(Rol.SOLICITANTE, Rol.CONDUCTOR)) {
            entrar(rol)
            assertTrue((flota.listarHabilitaciones() as Resultado.Fallo).error is ErrorApp.SinPermiso)
            assertTrue((flota.habilitar("KXJR42", "ireyes@belmontespa.cl") as Resultado.Fallo).error is ErrorApp.SinPermiso)
        }
    }
    @Test fun filtrosInvalidosYCuentasDeOtroRolNoGeneranHabilitaciones() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        assertTrue((flota.listarHabilitaciones(patente = "ABC123") as Resultado.Fallo).error is ErrorApp.Validacion)
        assertTrue((flota.listarHabilitaciones(correo = "invalido") as Resultado.Fallo).error is ErrorApp.Validacion)
        assertTrue((flota.listarHabilitaciones(patente = "ZZ0000") as Resultado.Exito).valor.isEmpty())
        assertTrue((flota.habilitar("KXJR42", "crios@andesconsult.cl") as Resultado.Fallo).error is ErrorApp.NoEncontrado)
        assertTrue((flota.habilitar("ZZ0000", "ireyes@belmontespa.cl") as Resultado.Fallo).error is ErrorApp.NoEncontrado)
    }
    @Test fun altaVehiculoNormalizaPersisteYGeneraCodigosUnicos() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val uno = (flota.registrarMovil(NuevoMovil(" ab-cd 12 ", 20, Int.MAX_VALUE.toLong())) as Resultado.Exito).valor
        val dos = (flota.registrarMovil(NuevoMovil("zz 1234", 1, null)) as Resultado.Exito).valor
        assertEquals("ABCD12", uno.patente); assertTrue(uno.activo)
        assertEquals(EstadoMovil.DISPONIBLE, uno.estado); assertEquals(20, uno.capacidad)
        assertEquals(Int.MAX_VALUE.toLong(), uno.gpsSerial)
        assertTrue(uno.codigoAbordaje.matches(Regex("[0-9A-F]{16}")))
        assertTrue(dos.codigoAbordaje.matches(Regex("[0-9A-F]{16}")))
        assertNotEquals(uno.codigoAbordaje, dos.codigoAbordaje); assertNull(dos.gpsSerial)
        assertEquals(uno, (FlotaLocalRepository(local).obtenerMovil("ABCD12") as Resultado.Exito).valor)
        assertTrue((flota.listarMoviles() as Resultado.Exito).valor.containsAll(listOf(uno, dos)))
    }
    @Test fun patenteDuplicadaNormalizadaNoReemplazaElVehiculo() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val antes = (flota.registrarMovil(NuevoMovil("ABCD12", 20, 123L)) as Resultado.Exito).valor
        val duplicado = flota.registrarMovil(NuevoMovil(" ab-cd 12 ", 1, null))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("La patente ya está registrada.")), duplicado)
        assertEquals(antes, (flota.obtenerMovil("ABCD12") as Resultado.Exito).valor)
    }
    @Test fun vehiculoInvalidoNoSePersiste() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val flota = FlotaLocalRepository(local)
        val antes = db.movilDao().observarTodos().first()
        for (nuevo in listOf(NuevoMovil("ABC123", 20, null), NuevoMovil("ABCD12", 0, null),
            NuevoMovil("ABCD12", 21, null), NuevoMovil("ABCD12", 1, 0), NuevoMovil("ABCD12", 1, -1),
            NuevoMovil("ABCD12", 1, Int.MAX_VALUE.toLong() + 1))) {
            val resultado = flota.registrarMovil(nuevo) as Resultado.Fallo
            assertTrue(resultado.error is ErrorApp.Validacion)
        }
        assertEquals(antes, db.movilDao().observarTodos().first())
    }
    @Test fun soloAdministradorPuedeRegistrarYLeerElCodigoDelVehiculo() = runBlocking {
        val flota = FlotaLocalRepository(local)
        for (rol in listOf(Rol.SOLICITANTE, Rol.CONDUCTOR)) {
            entrar(rol)
            assertTrue((flota.registrarMovil(NuevoMovil("ABCD12", 2, null)) as Resultado.Fallo).error is ErrorApp.SinPermiso)
            assertTrue((flota.listarMoviles() as Resultado.Fallo).error is ErrorApp.SinPermiso)
            assertTrue((flota.obtenerMovil("KXJR42") as Resultado.Fallo).error is ErrorApp.SinPermiso)
        }
        assertNull(db.movilDao().obtener("ABCD12"))
    }
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
    private suspend fun vencer(v: ViajeEntity): ViajeEntity =
        v.copy(fhAgenda = FechaIso.texto(local.ahora().minusHours(1).minusSeconds(1))).also { db.viajeDao().guardar(it) }
    @Test fun elAdministradorCancelaConMotivoYReembolsaTodoSiNoEstaVencido() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val (v, p) = solicitudConPago(50_001)
        // El monto solo cuenta con el viaje vencido: aquí se ignora.
        assertTrue(viajes.cancelar(v.folio, "  Conductor sin disponibilidad  ", 1) is Resultado.Exito)
        val pago = db.pagoDao().obtener(p.hash)!!
        assertEquals("REEMBOLSADO", pago.estadoPago); assertEquals(50_001, pago.montoReembolsado)
        assertEquals(p.fechaPago, pago.fechaPago); assertEquals(50_001, pago.monto)
        assertEquals(v.copy(estado = "CANCELADO", estadoPago = "REEMBOLSADO", motivo = "Conductor sin disponibilidad"), local.viaje(v.folio))
        assertTrue(viajes.cancelar(v.folio, "Otro motivo distinto", null) is Resultado.Fallo)
        assertEquals(pago, db.pagoDao().obtener(p.hash))
    }
    @Test fun elAdministradorFijaElReembolsoDeUnViajeVencidoDeCeroAlTotal() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val (v, p) = solicitudConPago(50_000)
        val vencido = vencer(v)
        val rango = "Debe estar entre \$0 y \$50.000."
        assertEquals(Resultado.Fallo(ErrorApp.Validacion(mapOf("montoReembolso" to "Es obligatorio."))), viajes.cancelar(v.folio, "Nadie lo atendió", null))
        for (monto in listOf(-1, 50_001)) {
            assertEquals(Resultado.Fallo(ErrorApp.Validacion(mapOf("montoReembolso" to rango))), viajes.cancelar(v.folio, "Nadie lo atendió", monto))
        }
        // Un rechazo no deja nada a medias: ni el viaje ni el pago cambian.
        assertEquals(vencido, local.viaje(v.folio)); assertEquals(p, db.pagoDao().obtener(p.hash))
        assertTrue(viajes.cancelar(v.folio, "Nadie lo atendió", 20_000) is Resultado.Exito)
        assertEquals(p.copy(estadoPago = "REEMBOLSADO", montoReembolsado = 20_000), db.pagoDao().obtener(p.hash))
        assertEquals(vencido.copy(estado = "CANCELADO", estadoPago = "REEMBOLSADO", motivo = "Nadie lo atendió"), local.viaje(v.folio))
        val (total, pagoTotal) = solicitudConPago(30_000)
        vencer(total)
        assertTrue(viajes.cancelar(total.folio, "Nadie lo atendió", 30_000) is Resultado.Exito)
        assertEquals(30_000, db.pagoDao().obtener(pagoTotal.hash)!!.montoReembolsado)
    }
    @Test fun conUnReembolsoDeCeroElViajeSeCancelaYElPagoSigueSiendoPagado() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val (v, p) = solicitudConPago(50_000)
        vencer(v)
        assertTrue(viajes.cancelar(v.folio, "Sin reembolso", 0) is Resultado.Exito)
        assertEquals(p, db.pagoDao().obtener(p.hash))
        assertEquals("CANCELADO", local.viaje(v.folio).estado); assertEquals("PAGADO", local.viaje(v.folio).estadoPago)
    }
    @Test fun elAdministradorAnulaUnPagoPendienteYRespetaLosTerminalesSinPedirMonto() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        for (estado in listOf("PENDIENTE", "ANULADO", "RECHAZADO", "REEMBOLSADO")) {
            val (v, p) = solicitudConPago(25_000, estado)
            val original = if (estado == "REEMBOLSADO") p.copy(montoReembolsado = 7_000) else p
            db.pagoDao().guardar(original)
            vencer(v)
            assertTrue(viajes.cancelar(v.folio, "Nadie lo atendió", null) is Resultado.Exito)
            assertEquals(if (estado == "PENDIENTE") original.copy(estadoPago = "ANULADO") else original, db.pagoDao().obtener(p.hash))
        }
        // Sin pago tampoco se pide monto.
        val sinPago = local.viaje("TR-1055")
        assertTrue(viajes.cancelar(sinPago.folio, "Cambio de planes", null) is Resultado.Exito)
        assertEquals(sinPago.copy(estado = "CANCELADO", motivo = "Cambio de planes"), local.viaje(sinPago.folio))
    }
    @Test fun elMotivoDelAdministradorSeValidaAntesQueElViaje() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val original = local.viaje("TR-1051")
        val invalido = Resultado.Fallo(ErrorApp.Validacion(mapOf("motivo" to "Debe tener entre 5 y 200 caracteres.")))
        assertEquals(invalido, viajes.cancelar("TR-1051", "1234", null))
        assertEquals(invalido, viajes.cancelar("TR-1051", "  1234  ", null))
        assertEquals(invalido, viajes.cancelar("TR-1051", "a".repeat(201), null))
        assertEquals(Resultado.Fallo(ErrorApp.Validacion(mapOf("motivo" to "Es obligatorio."))), viajes.cancelar("TR-1051", null, null))
        assertEquals(Resultado.Fallo(ErrorApp.Validacion(mapOf("motivo" to "Es obligatorio."))), viajes.cancelar("TR-1051", "     ", null))
        assertEquals(invalido, viajes.cancelar("TR-9999", "1234", null))
        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), viajes.cancelar("TR-9999", "Motivo válido", null))
        assertEquals(original, local.viaje("TR-1051"))
    }
    @Test fun elAdministradorNoCancelaEstadosCerradosNiLoHaceUnConductor() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val (v, p) = solicitudConPago(25_000)
        for (estado in listOf("EN_CURSO", "FINALIZADO", "CANCELADO")) {
            val cerrado = v.copy(estado = estado)
            db.viajeDao().guardar(cerrado)
            val rechazo = viajes.cancelar(v.folio, "Motivo válido", null)
            assertTrue(rechazo is Resultado.Fallo && rechazo.error is ErrorApp.Conflicto)
            assertEquals(cerrado, local.viaje(v.folio)); assertEquals(p, db.pagoDao().obtener(p.hash))
        }
        entrar(Rol.CONDUCTOR)
        assertEquals(Resultado.Fallo(ErrorApp.SinPermiso()), viajes.cancelar("TR-1042", "Motivo válido", null))
    }
    @Test fun elAdministradorCancelaUnViajeAsignadoYConservaLaAsignacionYLaFlota() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val conductor = local.usuario("ireyes@belmontespa.cl"); val movil = db.movilDao().obtener("KXJR42")!!
        val asignado = local.viaje("TR-1042")
        assertTrue(viajes.cancelar("TR-1042", "Falla del vehículo", null) is Resultado.Exito)
        val cancelado = local.viaje("TR-1042")
        assertEquals("CANCELADO", cancelado.estado); assertEquals("Falla del vehículo", cancelado.motivo)
        assertEquals(asignado.correoConductor, cancelado.correoConductor); assertEquals(asignado.patenteMovil, cancelado.patenteMovil)
        assertEquals(asignado.fhAsignacion, cancelado.fhAsignacion)
        assertEquals(conductor, local.usuario(conductor.correo)); assertEquals(movil, db.movilDao().obtener(movil.patente))
        assertEquals("REEMBOLSADO", db.pagoDao().obtener(asignado.hashPago!!)!!.estadoPago)
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
    // HU23: reasignar es la misma V2 que asignar; solo cambia lo que se pide y el reemplazado pierde el viaje.
    @Test fun reasignarSoloElConductorConservaElAbordajeYLaFlota() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        val antes = local.viaje("TR-1042")
        val marcasAntes = db.viajePasajeroDao().obtenerDelViaje("TR-1042").toSet()
        assertEquals(1, marcasAntes.count { it.fhAbordaje != null })
        assertTrue(viajes.asignar("TR-1042", "pmunoz@belmontespa.cl", "KXJR42") is Resultado.Exito)
        val despues = local.viaje("TR-1042")
        assertEquals("ASIGNADO", despues.estado)
        assertEquals("pmunoz@belmontespa.cl", despues.correoConductor)
        assertEquals("Paula Muñoz", despues.nombreConductor)
        assertEquals("KXJR42", despues.patenteMovil)
        assertEquals(antes.fhAgenda, despues.fhAgenda)
        assertNotEquals(antes.fhAsignacion, despues.fhAsignacion)
        // El mismo vehículo es el mismo QR: nadie vuelve a Por abordar.
        assertEquals(marcasAntes, db.viajePasajeroDao().obtenerDelViaje("TR-1042").toSet())
        // V2 no cambia el estado de nadie.
        assertEquals("DISPONIBLE", local.usuario("ireyes@belmontespa.cl").estadoConductor)
        assertEquals("DISPONIBLE", local.usuario("pmunoz@belmontespa.cl").estadoConductor)
        assertEquals("DISPONIBLE", db.movilDao().obtener("KXJR42")?.estadoMovil)
    }
    @Test fun elViajeReasignadoSaleDeLaAgendaDelReemplazadoYEntraALaDelNuevoConductor() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        assertTrue(viajes.asignar("TR-1042", "pmunoz@belmontespa.cl", "KXJR42") is Resultado.Exito)
        entrar(Rol.CONDUCTOR)
        assertNull(viajes.observarViaje("TR-1042").first())
        assertFalse(viajes.observarViajes(FiltroViajes()).first().any { it.folio == "TR-1042" })
        entrar(Rol.CONDUCTOR, "pmunoz@belmontespa.cl")
        assertEquals("Paula Muñoz", viajes.observarViaje("TR-1042").first()?.nombreConductor)
        assertTrue(viajes.observarViajes(FiltroViajes()).first().any { it.folio == "TR-1042" })
    }
    @Test fun reasignarConductorYVehiculoVuelveAPorAbordarYCambiaLaAgenda() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        // Paula solo está habilitada en KXJR42: se la habilita también en LPHT17, que no choca con TR-1049.
        assertTrue(FlotaLocalRepository(local).habilitar("LPHT17", "pmunoz@belmontespa.cl") is Resultado.Exito)
        assertEquals(1, db.viajePasajeroDao().obtenerDelViaje("TR-1042").count { it.fhAbordaje != null })
        assertTrue(viajes.asignar("TR-1042", "pmunoz@belmontespa.cl", "LPHT17") is Resultado.Exito)
        val guardado = local.viaje("TR-1042")
        assertEquals("ASIGNADO", guardado.estado)
        assertEquals("pmunoz@belmontespa.cl", guardado.correoConductor)
        assertEquals("LPHT17", guardado.patenteMovil)
        assertEquals(3, db.viajePasajeroDao().obtenerDelViaje("TR-1042").size)
        assertTrue(db.viajePasajeroDao().obtenerDelViaje("TR-1042").all { it.fhAbordaje == null })
        // El otro viaje de Iván no se toca.
        assertEquals("ireyes@belmontespa.cl", local.viaje("TR-1049").correoConductor)
        entrar(Rol.CONDUCTOR, "pmunoz@belmontespa.cl")
        assertTrue(viajes.observarViajes(FiltroViajes()).first().any { it.folio == "TR-1042" })
    }
    private suspend fun rechazaLaReasignacion(folio: String, correo: String, patente: String, mensaje: String) {
        val viajeAntes = local.viaje(folio)
        val marcasAntes = db.viajePasajeroDao().obtenerDelViaje(folio).toSet()
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto(mensaje)), viajes.asignar(folio, correo, patente))
        assertEquals(viajeAntes, local.viaje(folio))
        assertEquals(marcasAntes, db.viajePasajeroDao().obtenerDelViaje(folio).toSet())
    }
    @Test fun reasignarAUnConductorOcupadoSeRechazaSinCambiarNada() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        // Jorge Lara está Ocupado con TR-1045: el conductor nuevo debe estar Disponible.
        rechazaLaReasignacion("TR-1042", "jlara@belmontespa.cl", "KXJR42", "El conductor no está disponible")
    }
    @Test fun reasignarAUnConductorConOtroViajeCercanoSeRechazaSinCambiarNada() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        // Paula tiene otro viaje una hora después de TR-1042, que sale en media hora.
        db.viajeDao().guardar(local.viaje("TR-1051").copy(estado = "ASIGNADO", correoConductor = "pmunoz@belmontespa.cl",
            nombreConductor = "Paula Muñoz", patenteMovil = "GZRW90", fhAgenda = FechaIso.texto(local.ahora().plusMinutes(90))))
        rechazaLaReasignacion("TR-1042", "pmunoz@belmontespa.cl", "KXJR42", "El conductor ya tiene un viaje a menos de 2 horas")
    }
    @Test fun reasignarAUnVehiculoConOtroViajeCercanoSeRechazaSinCambiarNada() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        // LPHT17 tiene otro viaje una hora después de TR-1042; Iván sigue siendo el conductor.
        db.viajeDao().guardar(local.viaje("TR-1051").copy(estado = "ASIGNADO", correoConductor = "pmunoz@belmontespa.cl",
            nombreConductor = "Paula Muñoz", patenteMovil = "LPHT17", fhAgenda = FechaIso.texto(local.ahora().plusMinutes(90))))
        rechazaLaReasignacion("TR-1042", "ireyes@belmontespa.cl", "LPHT17", "El móvil ya tiene un viaje a menos de 2 horas")
    }
    @Test fun unViajeEnCursoOVencidoNoSeReasigna() = runBlocking {
        entrar(Rol.ADMINISTRADOR)
        rechazaLaReasignacion("TR-1045", "pmunoz@belmontespa.cl", "KXJR42", "El viaje ya no se puede asignar")
        vencer(local.viaje("TR-1042"))
        rechazaLaReasignacion("TR-1042", "pmunoz@belmontespa.cl", "KXJR42", "El viaje está vencido")
    }
    @Test fun soloElAdministradorReasigna() = runBlocking {
        for (rol in listOf(Rol.SOLICITANTE, Rol.CONDUCTOR)) {
            entrar(rol)
            val antes = local.viaje("TR-1042")
            assertTrue((viajes.asignar("TR-1042", "pmunoz@belmontespa.cl", "KXJR42") as Resultado.Fallo).error is ErrorApp.SinPermiso)
            assertEquals(antes, local.viaje("TR-1042"))
        }
    }
    // HU25: V8. El Conductor asignado devuelve el viaje a Pendiente y dice por qué; no toca el pago ni la flota.
    @Test fun rechazarDevuelveElViajeAPendienteSinNadaDeLaAsignacion() = runBlocking {
        entrar(Rol.CONDUCTOR)
        val antes = local.viaje("TR-1042")
        val hashPago = antes.hashPago!!
        val pagoAntes = db.pagoDao().obtener(hashPago)
        assertEquals(1, db.viajePasajeroDao().obtenerDelViaje("TR-1042").count { it.fhAbordaje != null })
        assertTrue(viajes.rechazar("TR-1042", "  Falla del vehículo  ") is Resultado.Exito)
        // Solo cambia la asignación y el motivo (recortado): todo lo demás del viaje queda igual.
        assertEquals(antes.copy(estado = "PENDIENTE", correoConductor = null, nombreConductor = null, patenteMovil = null,
            fhAsignacion = null, motivo = "Falla del vehículo"), local.viaje("TR-1042"))
        val marcas = db.viajePasajeroDao().obtenerDelViaje("TR-1042")
        assertEquals(3, marcas.size)
        assertTrue(marcas.all { it.fhAbordaje == null })
        assertEquals(pagoAntes, db.pagoDao().obtener(hashPago))
        assertEquals("DISPONIBLE", local.usuario("ireyes@belmontespa.cl").estadoConductor)
        assertEquals("DISPONIBLE", db.movilDao().obtener("KXJR42")?.estadoMovil)
    }
    @Test fun elViajeRechazadoSaleDeMisViajesYElAdministradorLoAsignaDeNuevo() = runBlocking {
        entrar(Rol.CONDUCTOR)
        assertTrue(viajes.rechazar("TR-1042", "Falla del vehículo") is Resultado.Exito)
        assertNull(viajes.observarViaje("TR-1042").first())
        assertFalse(viajes.observarViajes(FiltroViajes()).first().any { it.folio == "TR-1042" })
        entrar(Rol.ADMINISTRADOR)
        val pendiente = viajes.observarViaje("TR-1042").first()!!
        assertEquals(EstadoViaje.PENDIENTE, pendiente.estado)
        assertEquals("Falla del vehículo", pendiente.motivo)
        assertTrue(viajes.asignar("TR-1042", "pmunoz@belmontespa.cl", "KXJR42") is Resultado.Exito)
        // V2 limpia el motivo del rechazo anterior y los pasajeros siguen Por abordar.
        val asignado = local.viaje("TR-1042")
        assertEquals("ASIGNADO", asignado.estado)
        assertEquals("pmunoz@belmontespa.cl", asignado.correoConductor)
        assertNull(asignado.motivo)
        assertTrue(db.viajePasajeroDao().obtenerDelViaje("TR-1042").all { it.fhAbordaje == null })
        entrar(Rol.CONDUCTOR, "pmunoz@belmontespa.cl")
        assertTrue(viajes.observarViajes(FiltroViajes()).first().any { it.folio == "TR-1042" })
    }
    @Test fun unViajeVencidoTambienSePuedeRechazar() = runBlocking {
        entrar(Rol.CONDUCTOR)
        vencer(local.viaje("TR-1042"))
        assertTrue(viajes.rechazar("TR-1042", "Falla del vehículo") is Resultado.Exito)
        assertEquals("PENDIENTE", local.viaje("TR-1042").estado)
    }
    private suspend fun rechazarSinCambiarNada(folio: String, motivo: String): Resultado<Unit> {
        val viajeAntes = local.viaje(folio)
        val marcasAntes = db.viajePasajeroDao().obtenerDelViaje(folio).toSet()
        val resultado = viajes.rechazar(folio, motivo)
        assertEquals(viajeAntes, local.viaje(folio))
        assertEquals(marcasAntes, db.viajePasajeroDao().obtenerDelViaje(folio).toSet())
        return resultado
    }
    @Test fun unMotivoInvalidoSeRechazaConSuErrorDeCampoSinCambiarNada() = runBlocking {
        entrar(Rol.CONDUCTOR)
        for (motivo in listOf("abcd", "  abcd  ", "x".repeat(201))) {
            assertEquals(Resultado.Fallo(ErrorApp.Validacion(mapOf("motivo" to "Debe tener entre 5 y 200 caracteres."))),
                rechazarSinCambiarNada("TR-1042", motivo))
        }
        assertEquals(Resultado.Fallo(ErrorApp.Validacion(mapOf("motivo" to "Es obligatorio."))), rechazarSinCambiarNada("TR-1042", "   "))
    }
    @Test fun soloSeRechazaUnViajeAsignadoDelConductor() = runBlocking {
        entrar(Rol.CONDUCTOR)
        // TR-1045 está En curso con Jorge Lara: se lo presta a Iván.
        db.viajeDao().guardar(local.viaje("TR-1045").copy(correoConductor = "ireyes@belmontespa.cl"))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El viaje ya no se puede rechazar.")), rechazarSinCambiarNada("TR-1045", "Falla del vehículo"))
        // Cancelar conserva la asignación: sigue siendo de Iván, pero ya no hay qué rechazar.
        db.viajeDao().guardar(local.viaje("TR-1042").copy(estado = "CANCELADO"))
        assertEquals(Resultado.Fallo(ErrorApp.Conflicto("El viaje ya no se puede rechazar.")), rechazarSinCambiarNada("TR-1042", "Falla del vehículo"))
        // Un Pendiente no tiene conductor, así que no es de Iván; y un folio que no existe, no existe.
        assertTrue((rechazarSinCambiarNada("TR-1051", "Falla del vehículo") as Resultado.Fallo).error is ErrorApp.SinPermiso)
        assertEquals(Resultado.Fallo(ErrorApp.NoEncontrado), viajes.rechazar("TR-9999", "Falla del vehículo"))
    }
    @Test fun elViajeDeOtroConductorNoSeRechaza() = runBlocking {
        entrar(Rol.CONDUCTOR, "pmunoz@belmontespa.cl")
        assertTrue((rechazarSinCambiarNada("TR-1042", "Falla del vehículo") as Resultado.Fallo).error is ErrorApp.SinPermiso)
    }
    @Test fun soloElConductorRechaza() = runBlocking {
        for (rol in listOf(Rol.ADMINISTRADOR, Rol.SOLICITANTE)) {
            entrar(rol)
            assertTrue((rechazarSinCambiarNada("TR-1042", "Falla del vehículo") as Resultado.Fallo).error is ErrorApp.SinPermiso)
        }
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
