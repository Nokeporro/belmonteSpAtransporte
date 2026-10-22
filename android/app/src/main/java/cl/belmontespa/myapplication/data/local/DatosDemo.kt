package cl.belmontespa.myapplication.data.local

import cl.belmontespa.myapplication.data.local.entity.HabilitacionEntity
import cl.belmontespa.myapplication.data.local.entity.MovilEntity
import cl.belmontespa.myapplication.data.local.entity.PagoEntity
import cl.belmontespa.myapplication.data.local.entity.PasajeroEntity
import cl.belmontespa.myapplication.data.local.entity.UsuarioEntity
import cl.belmontespa.myapplication.data.local.entity.ViajeEntity
import cl.belmontespa.myapplication.data.local.entity.ViajePasajeroEntity
import cl.belmontespa.myapplication.model.Banco
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.util.FechaIso
import java.time.LocalDateTime

/*
 * Datos ficticios de la build demo (RNF09), con los ejemplos de los mockups. SemillaDemo los inserta
 * en Room al crear la base, con fechas relativas a ese primer arranque: el viaje en ventana vence
 * unos 90 minutos después. Antes de cada demostración o grabación se borran los datos de la app:
 *     adb shell pm clear cl.belmontespa.myapplication
 *
 * Contraseña común de todas las cuentas: Belmonte2026
 *
 * Administradores: recepcion@belmontespa.cl (Andrea Castro), cherrera@belmontespa.cl (Camila Herrera)
 * Conductores:     ireyes@belmontespa.cl (Iván Reyes, conductor del viaje en ventana TR-1042)
 *                  pmunoz@belmontespa.cl (Paula Muñoz)
 *                  jlara@belmontespa.cl (Jorge Lara, con TR-1045 en curso)
 *                  msoto@belmontespa.cl (Marta Soto, inactiva: no inicia sesión)
 * Solicitantes:    crios@andesconsult.cl (Constanza Ríos, Andes Consultores SpA)
 *                  rperez@andesconsult.cl (Rodrigo Pérez, por aprobar: no inicia sesión)
 *                  mrojas@pacificoservicios.cl (Matías Rojas, Pacífico Servicios Ltda.)
 *
 * Documentos ficticios de los pasajeros (para "Abordar viaje"):
 *   Constanza Ríos RUT 15.234.567-4 · Tomás Vidal pasaporte P4471023 · Marcela Soto RUT 16.789.012-1
 *   Roberto Aguilar RUT 12.345.678-5 · Paula Lagos RUT 13.579.246-2 · Luis Pino RUT 14.682.035-2
 *   Diego Fuentes RUT 17.246.810-1 · Felipe Mora pasaporte X9087654 · Javiera Núñez RUT 18.364.925-6
 *
 * Vehículos y código de abordaje (QR: BELMONTE-ABORDAJE:{patente}:{código}):
 *   KXJR42 7F3A9C21B04E58D6 · LPHT17 2B8E41D07C95A316 · GZRW90 9D04C6E2A1F7B853
 *   HJKL21 5C1A8F3E0B6D2947 (inactivo, en taller)
 *
 * Viaje en ventana: TR-1042, ASIGNADO a ahora + 30 min, de Iván Reyes en KXJR42, con 3 pasajeros y
 * Constanza Ríos ya abordada; para abordar, usar el documento de Tomás Vidal o de Marcela Soto.
 */
object DatosDemo {
    const val PASSWORD = "Belmonte2026"

    /** Los 14 bancos de los datos iniciales, con su código SBIF. */
    val bancos = listOf(
        Banco("Banco de Chile", 1),
        Banco("Banco Internacional", 9),
        Banco("BancoEstado", 12),
        Banco("Scotiabank Chile", 14),
        Banco("Banco de Crédito e Inversiones", 16),
        Banco("Banco BICE", 28),
        Banco("HSBC Bank Chile", 31),
        Banco("Banco Santander-Chile", 37),
        Banco("Banco Itaú Chile", 39),
        Banco("Banco Security", 49),
        Banco("Banco Falabella", 51),
        Banco("Banco Ripley", 53),
        Banco("Banco Consorcio", 55),
        Banco("Banco BTG Pactual Chile", 59),
    )

    /** Regiones Metropolitana y de Valparaíso, con los nombres oficiales del catálogo del backend. */
    val regiones = listOf(
        Region(
            "Metropolitana de Santiago",
            listOf(
                "Santiago", "Cerrillos", "Cerro Navia", "Conchalí", "El Bosque", "Estación Central",
                "Huechuraba", "Independencia", "La Cisterna", "La Florida", "La Granja", "La Pintana",
                "La Reina", "Las Condes", "Lo Barnechea", "Lo Espejo", "Lo Prado", "Macul", "Maipú",
                "Ñuñoa", "Pedro Aguirre Cerda", "Peñalolén", "Providencia", "Pudahuel", "Quilicura",
                "Quinta Normal", "Recoleta", "Renca", "San Joaquín", "San Miguel", "San Ramón",
                "Vitacura", "Puente Alto", "Pirque", "San José de Maipo", "Colina", "Lampa", "Tiltil",
                "San Bernardo", "Buin", "Calera de Tango", "Paine", "Melipilla", "Alhué", "Curacaví",
                "María Pinto", "San Pedro", "Talagante", "El Monte", "Isla de Maipo", "Padre Hurtado",
                "Peñaflor",
            ),
        ),
        Region(
            "Valparaíso",
            listOf(
                "Valparaíso", "Casablanca", "Concón", "Juan Fernández", "Puchuncaví", "Quintero",
                "Viña del Mar", "Isla de Pascua", "Los Andes", "Calle Larga", "Rinconada", "San Esteban",
                "La Ligua", "Cabildo", "Papudo", "Petorca", "Zapallar", "Quillota", "La Calera",
                "Hijuelas", "La Cruz", "Nogales", "San Antonio", "Algarrobo", "Cartagena", "El Quisco",
                "El Tabo", "Santo Domingo", "San Felipe", "Catemu", "Llaillay", "Panquehue", "Putaendo",
                "Santa María", "Quilpué", "Limache", "Olmué", "Villa Alemana",
            ),
        ),
    )

    private const val RM = "Metropolitana de Santiago"
    private val AEROPUERTO = Ubicacion("Aeropuerto Arturo Merino Benítez", "Pudahuel", RM, -33.392975, -70.785803)
    private val APOQUINDO = Ubicacion("Av. Apoquindo 5400", "Las Condes", RM, -33.411520, -70.575384)
    private val ISIDORA_GOYENECHEA = Ubicacion("Av. Isidora Goyenechea 3000", "Las Condes", RM, -33.414710, -70.602530)
    private val VITACURA_2939 = Ubicacion("Av. Vitacura 2939", "Las Condes", RM, -33.413820, -70.601170)
    private val EL_BOSQUE_NORTE = Ubicacion("Av. El Bosque Norte 500", "Las Condes", RM, -33.414400, -70.598300)
    private val VITACURA = Ubicacion("Av. Vitacura 5250", "Vitacura", RM, -33.399010, -70.590230)
    private val NUNOA = Ubicacion("Av. Irarrázaval 3400", "Ñuñoa", RM, -33.454600, -70.596700)
    private val PROVIDENCIA = Ubicacion("Av. Providencia 1760", "Providencia", RM, -33.425480, -70.611990)
    private val VINA_DEL_MAR = Ubicacion("Av. San Martín 199", "Viña del Mar", "Valparaíso", -33.019800, -71.552100)
    private val VALPARAISO = Ubicacion("Muelle Prat", "Valparaíso", "Valparaíso", -33.037900, -71.627600)

    // Catálogo demo independiente del historial: también sirve a una cuenta recién creada.
    val ubicaciones: List<Ubicacion> = listOf(AEROPUERTO, APOQUINDO, ISIDORA_GOYENECHEA, VITACURA_2939, EL_BOSQUE_NORTE, VITACURA, NUNOA, PROVIDENCIA, VINA_DEL_MAR, VALPARAISO)

    private data class Empresa(val razonSocial: String, val rut: String)

    private val ANDES = Empresa("Andes Consultores SpA", "76543210-3")
    private val PACIFICO = Empresa("Pacífico Servicios Ltda.", "77123456-9")

    /** Pasajero ficticio con su documento en claro, que solo existe en este archivo y en las pruebas. */
    internal data class PasajeroDemo(val nombre: String, val documento: String) {
        val dniHash = Hashes.documento(documento)
    }

    private val CONSTANZA_RIOS = PasajeroDemo("Constanza Ríos", "15.234.567-4")
    private val TOMAS_VIDAL = PasajeroDemo("Tomás Vidal", "P4471023")
    private val MARCELA_SOTO = PasajeroDemo("Marcela Soto", "16.789.012-1")
    private val ROBERTO_AGUILAR = PasajeroDemo("Roberto Aguilar", "12.345.678-5")
    private val PAULA_LAGOS = PasajeroDemo("Paula Lagos", "13.579.246-2")
    private val LUIS_PINO = PasajeroDemo("Luis Pino", "14.682.035-2")
    private val DIEGO_FUENTES = PasajeroDemo("Diego Fuentes", "17.246.810-1")
    private val FELIPE_MORA = PasajeroDemo("Felipe Mora", "X9087654")
    private val JAVIERA_NUNEZ = PasajeroDemo("Javiera Núñez", "18.364.925-6")

    internal val pasajerosDemo = listOf(
        CONSTANZA_RIOS, TOMAS_VIDAL, MARCELA_SOTO, ROBERTO_AGUILAR, PAULA_LAGOS, LUIS_PINO,
        DIEGO_FUENTES, FELIPE_MORA, JAVIERA_NUNEZ,
    )

    /** Filas listas para insertar en Room. */
    data class Semilla(
        val usuarios: List<UsuarioEntity>,
        val moviles: List<MovilEntity>,
        val habilitaciones: List<HabilitacionEntity>,
        val pasajeros: List<PasajeroEntity>,
        val viajes: List<ViajeEntity>,
        val viajePasajeros: List<ViajePasajeroEntity>,
        val pagos: List<PagoEntity>,
    )

    /** Crea la semilla con fechas relativas a [ahora] (hora de Chile, truncada al minuto). */
    fun crear(ahora: LocalDateTime, descargadoEn: Long = 0): Semilla {
        val passwordSha256 = Hashes.password(PASSWORD)
        val registro = FechaIso.texto(ahora.minusDays(90))

        fun cuenta(
            correo: String,
            nombre: String,
            telefono: String?,
            rol: Rol,
            activo: Boolean = true,
            estadoConductor: EstadoConductor? = null,
            estadoSolicitante: EstadoSolicitante? = null,
            empresa: Empresa? = null,
        ) = UsuarioEntity(
            correo = correo,
            nombre = nombre,
            telefono = telefono,
            rol = rol.name,
            activo = activo,
            estadoConductor = estadoConductor?.name,
            estadoSolicitante = estadoSolicitante?.name,
            razonSocial = empresa?.razonSocial,
            rutEmpresa = empresa?.rut,
            fechaRegistro = registro,
            passwordSha256 = passwordSha256,
        )

        val andrea = cuenta("recepcion@belmontespa.cl", "Andrea Castro", null, Rol.ADMINISTRADOR)
        val camila = cuenta("cherrera@belmontespa.cl", "Camila Herrera", "+56955550505", Rol.ADMINISTRADOR)
        val ivan = cuenta("ireyes@belmontespa.cl", "Iván Reyes", "+56955550303", Rol.CONDUCTOR, estadoConductor = EstadoConductor.DISPONIBLE)
        val paula = cuenta("pmunoz@belmontespa.cl", "Paula Muñoz", "+56955550202", Rol.CONDUCTOR, estadoConductor = EstadoConductor.DISPONIBLE)
        val jorge = cuenta("jlara@belmontespa.cl", "Jorge Lara", null, Rol.CONDUCTOR, estadoConductor = EstadoConductor.OCUPADO)
        val marta = cuenta("msoto@belmontespa.cl", "Marta Soto", null, Rol.CONDUCTOR, activo = false, estadoConductor = EstadoConductor.DISPONIBLE)
        val constanza = cuenta("crios@andesconsult.cl", "Constanza Ríos", "+56955550101", Rol.SOLICITANTE, estadoSolicitante = EstadoSolicitante.APROBADO, empresa = ANDES)
        val rodrigo = cuenta("rperez@andesconsult.cl", "Rodrigo Pérez", null, Rol.SOLICITANTE, estadoSolicitante = EstadoSolicitante.PENDIENTE, empresa = ANDES)
        val matias = cuenta("mrojas@pacificoservicios.cl", "Matías Rojas", "+56955550404", Rol.SOLICITANTE, estadoSolicitante = EstadoSolicitante.APROBADO, empresa = PACIFICO)

        val moviles = listOf(
            MovilEntity("KXJR42", 4, 50231, true, EstadoMovil.DISPONIBLE.name, "7F3A9C21B04E58D6"),
            MovilEntity("LPHT17", 6, null, true, EstadoMovil.DISPONIBLE.name, "2B8E41D07C95A316"),
            MovilEntity("GZRW90", 4, 50477, true, EstadoMovil.EN_USO.name, "9D04C6E2A1F7B853"),
            MovilEntity("HJKL21", 4, null, false, EstadoMovil.DISPONIBLE.name, "5C1A8F3E0B6D2947"),
        )

        val habilitaciones = listOf(
            HabilitacionEntity("KXJR42", ivan.correo),
            HabilitacionEntity("LPHT17", ivan.correo),
            HabilitacionEntity("KXJR42", paula.correo),
            HabilitacionEntity("GZRW90", jorge.correo),
            HabilitacionEntity("HJKL21", marta.correo),
        )

        fun pago(
            hash: String,
            folio: Int,
            monto: Int,
            estado: EstadoPago,
            codSbif: Int,
            medioPago: MedioPago,
            tipoRecibo: TipoRecibo,
            empresa: Empresa,
            fechaPago: LocalDateTime? = null,
            montoReembolsado: Int? = null,
        ) = PagoEntity(
            hash = hash,
            folio = "TR-$folio",
            monto = monto,
            estadoPago = estado.name,
            banco = bancos.first { it.codSbif == codSbif }.nombre,
            codSbif = codSbif,
            rutEmpresa = empresa.rut,
            medioPago = medioPago.name,
            tipoRecibo = tipoRecibo.name,
            fechaPago = fechaPago?.let(FechaIso::texto),
            montoReembolsado = montoReembolsado,
        )

        val pagos = listOf(
            // V5 con el pago Pagado: reembolso del 50 % (P4).
            pago("9a4c2e7b1d5f3086c2a9e4b7d1f5c3e2", 1033, 40000, EstadoPago.REEMBOLSADO, 12, MedioPago.TRANSFERENCIA, TipoRecibo.FACTURA, ANDES, ahora.minusDays(4), 20000),
            // V7 con el pago Pendiente: anulado (P5).
            pago("5d3b1f8e2a7c4096d1e5b3a8c7f2e014", 1036, 35000, EstadoPago.ANULADO, 1, MedioPago.TARJETA_CREDITO, TipoRecibo.BOLETA, PACIFICO),
            pago("b71e4d2a9c3f5086a1d7e2c4b9f03a58", 1038, 52000, EstadoPago.PAGADO, 37, MedioPago.TRANSFERENCIA, TipoRecibo.FACTURA, ANDES, ahora.minusDays(2)),
            pago("0c5e8a3d7f1b4926b8e2d5a7c1f0e963", 1039, 45000, EstadoPago.RECHAZADO, 16, MedioPago.TARJETA_DEBITO, TipoRecibo.FACTURA, ANDES),
            pago("3f9a1c7e5b2d4a608e1f7c3b9d2a5e41", 1042, 48000, EstadoPago.PAGADO, 12, MedioPago.TRANSFERENCIA, TipoRecibo.FACTURA, ANDES, ahora.minusDays(1)),
            pago("e2a7c9f1d4b6083a5c7e1b9d2f4a6c80", 1051, 38000, EstadoPago.PENDIENTE, 39, MedioPago.EFECTIVO, TipoRecibo.BOLETA, ANDES),
        )

        val viajes = mutableListOf<ViajeEntity>()
        val viajePasajeros = mutableListOf<ViajePasajeroEntity>()

        fun viaje(
            folio: Int,
            estado: EstadoViaje,
            solicitante: UsuarioEntity,
            origen: Ubicacion,
            destino: Ubicacion,
            fhAgenda: LocalDateTime,
            pasajeros: List<Pair<PasajeroDemo, LocalDateTime?>>,
            conductor: UsuarioEntity? = null,
            patente: String? = null,
            numeroVuelo: String? = null,
            motivo: String? = null,
            fhAsignacion: LocalDateTime? = null,
            fhInicio: LocalDateTime? = null,
            fhFin: LocalDateTime? = null,
        ) {
            val pago = pagos.find { it.folio == "TR-$folio" }
            viajes += ViajeEntity(
                folio = "TR-$folio",
                estado = estado.name,
                correoSolicitante = solicitante.correo,
                nombreSolicitante = solicitante.nombre,
                telefonoSolicitante = solicitante.telefono,
                razonSocial = solicitante.razonSocial.orEmpty(),
                origenDireccion = origen.direccion,
                origenComuna = origen.comuna,
                origenRegion = origen.region,
                origenLatitud = origen.latitud,
                origenLongitud = origen.longitud,
                destinoDireccion = destino.direccion,
                destinoComuna = destino.comuna,
                destinoRegion = destino.region,
                destinoLatitud = destino.latitud,
                destinoLongitud = destino.longitud,
                fhCreacion = FechaIso.texto(minOf(fhAgenda, ahora).minusDays(2)),
                fhAgenda = FechaIso.texto(fhAgenda),
                fhAsignacion = fhAsignacion?.let(FechaIso::texto),
                fhInicio = fhInicio?.let(FechaIso::texto),
                fhFin = fhFin?.let(FechaIso::texto),
                numeroVuelo = numeroVuelo,
                cantidadPasajeros = pasajeros.size,
                correoConductor = conductor?.correo,
                nombreConductor = conductor?.nombre,
                patenteMovil = patente,
                hashPago = pago?.hash,
                estadoPago = pago?.estadoPago,
                motivo = motivo,
                folioCorrelativo = folio.toLong(),
                descargadoEn = descargadoEn,
            )
            pasajeros.forEach { (pasajero, fhAbordaje) ->
                viajePasajeros += ViajePasajeroEntity("TR-$folio", pasajero.dniHash, fhAbordaje?.let(FechaIso::texto))
            }
        }

        viaje(
            1033, EstadoViaje.CANCELADO, constanza, EL_BOSQUE_NORTE, AEROPUERTO, ahora.minusDays(1),
            listOf(PAULA_LAGOS to null),
        )
        viaje(
            1036, EstadoViaje.CANCELADO, matias, VINA_DEL_MAR, VALPARAISO, ahora.plusDays(2),
            listOf(JAVIERA_NUNEZ to null),
            motivo = "Cliente reprogramó el traslado",
        )
        // Vencido (más de 1 hora después de su hora agendada), con el pago Pagado.
        viaje(
            1038, EstadoViaje.PENDIENTE, constanza, NUNOA, AEROPUERTO, ahora.minusHours(3),
            listOf(LUIS_PINO to null),
        )
        viaje(
            1039, EstadoViaje.FINALIZADO, constanza, APOQUINDO, AEROPUERTO, ahora.minusHours(5),
            listOf(ROBERTO_AGUILAR to ahora.minusHours(5).minusMinutes(10)),
            conductor = ivan, patente = "LPHT17",
            fhAsignacion = ahora.minusDays(2), fhInicio = ahora.minusHours(5).minusMinutes(5), fhFin = ahora.minusHours(4),
        )
        // Viaje en ventana de inicio: 3 pasajeros, 1 ya abordado.
        viaje(
            1042, EstadoViaje.ASIGNADO, constanza, AEROPUERTO, APOQUINDO, ahora.plusMinutes(30),
            listOf(CONSTANZA_RIOS to ahora.minusMinutes(5), TOMAS_VIDAL to null, MARCELA_SOTO to null),
            conductor = ivan, patente = "KXJR42", numeroVuelo = "LA1502", fhAsignacion = ahora.minusDays(1),
        )
        viaje(
            1045, EstadoViaje.EN_CURSO, matias, PROVIDENCIA, AEROPUERTO, ahora.minusMinutes(20),
            listOf(JAVIERA_NUNEZ to ahora.minusMinutes(30)),
            conductor = jorge, patente = "GZRW90", fhAsignacion = ahora.minusDays(1), fhInicio = ahora.minusMinutes(25),
        )
        // Atrasado: pasó su hora agendada, pero aún puede iniciarse.
        viaje(
            1047, EstadoViaje.PENDIENTE, constanza, PROVIDENCIA, AEROPUERTO, ahora.minusMinutes(30),
            listOf(DIEGO_FUENTES to null),
        )
        viaje(
            1049, EstadoViaje.ASIGNADO, constanza, ISIDORA_GOYENECHEA, AEROPUERTO, ahora.plusHours(4).plusMinutes(15),
            listOf(FELIPE_MORA to null),
            conductor = ivan, patente = "LPHT17", fhAsignacion = ahora.minusDays(1),
        )
        viaje(
            1051, EstadoViaje.PENDIENTE, constanza, VITACURA, AEROPUERTO, ahora.plusDays(1),
            listOf(MARCELA_SOTO to null, TOMAS_VIDAL to null),
        )
        viaje(
            1053, EstadoViaje.PENDIENTE, matias, VINA_DEL_MAR, AEROPUERTO, ahora.plusDays(3),
            listOf(JAVIERA_NUNEZ to null),
        )
        // Sin pasajeros: no se puede asignar hasta agregar al menos uno.
        viaje(
            1055, EstadoViaje.PENDIENTE, constanza, VITACURA_2939, AEROPUERTO, ahora.plusDays(2),
            emptyList(),
        )

        val pasajeros = pasajerosDemo.map { PasajeroEntity(it.dniHash, it.nombre) }

        return Semilla(
            usuarios = listOf(andrea, camila, ivan, paula, jorge, marta, constanza, rodrigo, matias),
            moviles = moviles,
            habilitaciones = habilitaciones,
            pasajeros = pasajeros,
            viajes = viajes,
            viajePasajeros = viajePasajeros,
            pagos = pagos,
        )
    }
}
