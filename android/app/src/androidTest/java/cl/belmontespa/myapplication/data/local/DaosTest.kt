package cl.belmontespa.myapplication.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.belmontespa.myapplication.data.local.database.BelmonteDatabase
import cl.belmontespa.myapplication.data.local.entity.HabilitacionEntity
import cl.belmontespa.myapplication.data.local.entity.MovilEntity
import cl.belmontespa.myapplication.data.local.entity.PasajeroEntity
import cl.belmontespa.myapplication.data.local.entity.UsuarioEntity
import cl.belmontespa.myapplication.data.local.entity.ViajeEntity
import cl.belmontespa.myapplication.data.local.entity.ViajePasajeroEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** PK, UQ, FK en cascada, consultas `Flow` y transacciones de Room v1 (README, Base de datos local). */
@RunWith(AndroidJUnit4::class)
class DaosTest {

    private lateinit var db: BelmonteDatabase

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BelmonteDatabase::class.java,
        ).build()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun laPkRechazaUnCorreoRepetido() = runBlocking {
        db.usuarioDao().insertar(usuario("ireyes@belmontespa.cl"))
        val error = runCatching { db.usuarioDao().insertar(usuario("ireyes@belmontespa.cl")) }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
    }

    @Test
    fun laUqRechazaUnCodigoDeAbordajeRepetido() = runBlocking {
        db.movilDao().insertar(movil("KXJR42", "7F3A9C21B04E58D6"))
        val error = runCatching { db.movilDao().insertar(movil("LPHT17", "7F3A9C21B04E58D6")) }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
    }

    @Test
    fun laFkRechazaUnaHabilitacionDeUnVehiculoInexistente() = runBlocking {
        db.usuarioDao().insertar(usuario("ireyes@belmontespa.cl"))
        val error = runCatching {
            db.habilitacionDao().insertar(HabilitacionEntity("ZZZZ99", "ireyes@belmontespa.cl"))
        }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
    }

    @Test
    fun unaHabilitacionRepetidaSeIgnoraSinDuplicar() = runBlocking {
        db.usuarioDao().insertar(usuario("ireyes@belmontespa.cl"))
        db.movilDao().insertar(movil("KXJR42", "7F3A9C21B04E58D6"))
        db.habilitacionDao().insertar(HabilitacionEntity("KXJR42", "ireyes@belmontespa.cl"))
        assertEquals(-1L, db.habilitacionDao().insertar(HabilitacionEntity("KXJR42", "ireyes@belmontespa.cl")))
        assertEquals(1, db.habilitacionDao().observarDelMovil("KXJR42").first().size)
    }

    @Test
    fun borrarUnViajeBorraSusPasajerosEnCascada() = runBlocking {
        guardarViaje(1042, pasajeros = listOf("Constanza Ríos", "Tomás Vidal"))
        db.viajeDao().borrar("TR-1042")
        assertTrue(db.viajePasajeroDao().obtenerDelViaje("TR-1042").isEmpty())
        // Los pasajeros siguen registrados: solo se borra su asociación con el viaje.
        assertEquals(2, db.pasajeroDao().observarTodos().first().size)
    }

    @Test
    fun observarEmiteElViajeConSusPasajerosAlGuardarlo() = runBlocking {
        val siguiente = async { withTimeout(5_000) { db.viajeDao().observar("TR-1042").first { it != null } } }
        guardarViaje(1042, pasajeros = listOf("Constanza Ríos", "Tomás Vidal"))
        val viaje = siguiente.await()!!
        assertEquals("TR-1042", viaje.viaje.folio)
        assertEquals(setOf("Constanza Ríos", "Tomás Vidal"), viaje.pasajeros.map { it.pasajero.nombre }.toSet())
    }

    @Test
    fun unaTransaccionQueFallaNoGuardaNada() = runBlocking {
        // El detalle apunta a un pasajero que no se guarda: la FK falla y se revierte todo el viaje.
        val error = runCatching {
            db.viajeDao().guardarConPasajeros(
                viaje = viaje(1042),
                pasajeros = emptyList(),
                detalles = listOf(ViajePasajeroEntity("TR-1042", "sin-pasajero", null)),
            )
        }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
        assertNull(db.viajeDao().obtener("TR-1042"))
    }

    @Test
    fun laListaOrdenaSegunElEstado() = runBlocking {
        guardarViaje(1051, fhAgenda = "2026-09-25T09:00:00")
        guardarViaje(1047, fhAgenda = "2026-09-24T11:45:00")
        guardarViaje(1048, fhAgenda = "2026-09-24T11:45:00")
        val proximos = db.viajeDao().observarLista(null, null, "PENDIENTE", null, ascendente = true).first()
        assertEquals(listOf("TR-1047", "TR-1048", "TR-1051"), proximos.map { it.viaje.folio })
        val recientes = db.viajeDao().observarLista(null, null, null, null, ascendente = false).first()
        assertEquals(listOf("TR-1051", "TR-1048", "TR-1047"), recientes.map { it.viaje.folio })
    }

    @Test
    fun laAgendaDelConductorSumaSuEnCursoAnteriorYExcluyeLosCancelados() = runBlocking {
        val ivan = "ireyes@belmontespa.cl"
        guardarViaje(1049, estado = "ASIGNADO", fhAgenda = "2026-09-24T18:45:00", conductor = ivan)
        guardarViaje(1042, estado = "ASIGNADO", fhAgenda = "2026-09-24T14:30:00", conductor = ivan)
        guardarViaje(1040, estado = "EN_CURSO", fhAgenda = "2026-09-23T23:30:00", conductor = ivan)
        guardarViaje(1041, estado = "CANCELADO", fhAgenda = "2026-09-24T10:00:00", conductor = ivan)
        guardarViaje(1043, estado = "ASIGNADO", fhAgenda = "2026-09-24T16:00:00", conductor = "pmunoz@belmontespa.cl")

        val hoy = db.viajeDao().observarAgendaConductor(ivan, "2026-09-24", incluirEnCursoAnterior = true).first()
        assertEquals(listOf("TR-1040", "TR-1042", "TR-1049"), hoy.map { it.viaje.folio })

        val otroDia = db.viajeDao().observarAgendaConductor(ivan, "2026-09-24", incluirEnCursoAnterior = false).first()
        assertEquals(listOf("TR-1042", "TR-1049"), otroDia.map { it.viaje.folio })
    }

    @Test
    fun repetirElAbordajeNoCambiaLaHoraGuardada() = runBlocking {
        guardarViaje(1042, pasajeros = listOf("Tomás Vidal"))
        val dniHash = Hashes.documento("Tomás Vidal")
        assertEquals(1, db.viajePasajeroDao().marcarAbordaje("TR-1042", dniHash, "2026-09-24T14:05:00"))
        assertEquals(0, db.viajePasajeroDao().marcarAbordaje("TR-1042", dniHash, "2026-09-24T14:20:00"))
        assertEquals("2026-09-24T14:05:00", db.viajePasajeroDao().obtenerDelViaje("TR-1042").single().fhAbordaje)
    }

    private suspend fun guardarViaje(
        folio: Int,
        estado: String = "PENDIENTE",
        fhAgenda: String = "2026-09-24T14:30:00",
        conductor: String? = null,
        pasajeros: List<String> = emptyList(),
    ) {
        // En estas pruebas el "documento" es el nombre: basta con que el hash sea distinto por pasajero.
        val filas = pasajeros.map { PasajeroEntity(Hashes.documento(it), it) }
        db.viajeDao().guardarConPasajeros(
            viaje = viaje(folio, estado, fhAgenda, conductor, filas.size),
            pasajeros = filas,
            detalles = filas.map { ViajePasajeroEntity("TR-$folio", it.dniHash, null) },
        )
    }

    private fun usuario(correo: String) = UsuarioEntity(
        correo = correo,
        nombre = "Iván Reyes",
        telefono = null,
        rol = "CONDUCTOR",
        activo = true,
        estadoConductor = "DISPONIBLE",
        estadoSolicitante = null,
        razonSocial = null,
        rutEmpresa = null,
        fechaRegistro = "2026-06-01T09:00:00",
        passwordSha256 = null,
    )

    private fun movil(patente: String, codigo: String) =
        MovilEntity(patente, 4, null, true, "DISPONIBLE", codigo)

    private fun viaje(
        folio: Int,
        estado: String = "PENDIENTE",
        fhAgenda: String = "2026-09-24T14:30:00",
        conductor: String? = null,
        cantidadPasajeros: Int = 0,
    ) = ViajeEntity(
        folio = "TR-$folio",
        estado = estado,
        correoSolicitante = "crios@andesconsult.cl",
        nombreSolicitante = "Constanza Ríos",
        telefonoSolicitante = null,
        razonSocial = "Andes Consultores SpA",
        origenDireccion = "Aeropuerto Arturo Merino Benítez",
        origenComuna = "Pudahuel",
        origenRegion = "Metropolitana de Santiago",
        origenLatitud = -33.392975,
        origenLongitud = -70.785803,
        destinoDireccion = "Av. Apoquindo 5400",
        destinoComuna = "Las Condes",
        destinoRegion = "Metropolitana de Santiago",
        destinoLatitud = -33.411520,
        destinoLongitud = -70.575384,
        fhCreacion = "2026-09-20T10:05:00",
        fhAgenda = fhAgenda,
        fhAsignacion = null,
        fhInicio = null,
        fhFin = null,
        numeroVuelo = null,
        cantidadPasajeros = cantidadPasajeros,
        correoConductor = conductor,
        nombreConductor = null,
        patenteMovil = null,
        hashPago = null,
        estadoPago = null,
        motivo = null,
        folioCorrelativo = folio.toLong(),
        descargadoEn = 0,
    )
}
