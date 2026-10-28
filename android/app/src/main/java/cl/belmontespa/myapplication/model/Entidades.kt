package cl.belmontespa.myapplication.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val correo: String,
    val nombre: String,
    val telefono: String?,
    val rol: String,
    val activo: Boolean,
    @ColumnInfo(name = "estado_conductor") val estadoConductor: String?,
    @ColumnInfo(name = "estado_solicitante") val estadoSolicitante: String?,
    @ColumnInfo(name = "razon_social") val razonSocial: String?,
    @ColumnInfo(name = "rut_empresa") val rutEmpresa: String?,
    @ColumnInfo(name = "fecha_registro") val fechaRegistro: String,
    @ColumnInfo(name = "password_sha256") val passwordSha256: String?,
    @ColumnInfo(name = "password_temporal_sha256") val passwordTemporalSha256: String? = null,
    @ColumnInfo(name = "password_temporal_expira") val passwordTemporalExpira: Long? = null,
)

@Entity(
    tableName = "moviles",
    indices = [Index(value = ["codigo_abordaje"], unique = true)],
)
data class MovilEntity(
    @PrimaryKey val patente: String,
    val capacidad: Int,
    @ColumnInfo(name = "gps_serial") val gpsSerial: Long?,
    val activo: Boolean,
    @ColumnInfo(name = "estado_movil") val estadoMovil: String,
    @ColumnInfo(name = "codigo_abordaje") val codigoAbordaje: String,
)

@Entity(
    tableName = "habilitaciones",
    primaryKeys = ["patente", "correo_conductor"],
    foreignKeys = [
        ForeignKey(entity = MovilEntity::class, parentColumns = ["patente"], childColumns = ["patente"]),
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["correo"], childColumns = ["correo_conductor"]),
    ],
    indices = [Index("correo_conductor")],
)
data class HabilitacionEntity(
    val patente: String,
    @ColumnInfo(name = "correo_conductor") val correoConductor: String,
)

@Entity(tableName = "viajes")
data class ViajeEntity(
    @PrimaryKey val folio: String,
    val estado: String,
    @ColumnInfo(name = "correo_solicitante") val correoSolicitante: String,
    @ColumnInfo(name = "nombre_solicitante") val nombreSolicitante: String,
    @ColumnInfo(name = "telefono_solicitante") val telefonoSolicitante: String?,
    @ColumnInfo(name = "razon_social") val razonSocial: String,
    @ColumnInfo(name = "origen_direccion") val origenDireccion: String,
    @ColumnInfo(name = "origen_comuna") val origenComuna: String,
    @ColumnInfo(name = "origen_region") val origenRegion: String,
    @ColumnInfo(name = "origen_latitud") val origenLatitud: Double,
    @ColumnInfo(name = "origen_longitud") val origenLongitud: Double,
    @ColumnInfo(name = "destino_direccion") val destinoDireccion: String,
    @ColumnInfo(name = "destino_comuna") val destinoComuna: String,
    @ColumnInfo(name = "destino_region") val destinoRegion: String,
    @ColumnInfo(name = "destino_latitud") val destinoLatitud: Double,
    @ColumnInfo(name = "destino_longitud") val destinoLongitud: Double,
    @ColumnInfo(name = "fh_creacion") val fhCreacion: String,
    @ColumnInfo(name = "fh_agenda") val fhAgenda: String,
    @ColumnInfo(name = "fh_asignacion") val fhAsignacion: String?,
    @ColumnInfo(name = "fh_inicio") val fhInicio: String?,
    @ColumnInfo(name = "fh_fin") val fhFin: String?,
    @ColumnInfo(name = "numero_vuelo") val numeroVuelo: String?,
    @ColumnInfo(name = "cantidad_pasajeros") val cantidadPasajeros: Int,
    @ColumnInfo(name = "correo_conductor") val correoConductor: String?,
    @ColumnInfo(name = "nombre_conductor") val nombreConductor: String?,
    @ColumnInfo(name = "patente_movil") val patenteMovil: String?,
    @ColumnInfo(name = "hash_pago") val hashPago: String?,
    @ColumnInfo(name = "estado_pago") val estadoPago: String?,
    val motivo: String?,
    @ColumnInfo(name = "folio_correlativo") val folioCorrelativo: Long,
    @ColumnInfo(name = "descargado_en") val descargadoEn: Long,
)

@Entity(tableName = "pasajeros")
data class PasajeroEntity(
    @PrimaryKey @ColumnInfo(name = "dni_hash") val dniHash: String,
    val nombre: String,
)

@Entity(
    tableName = "viaje_pasajeros",
    primaryKeys = ["folio", "dni_hash"],
    foreignKeys = [
        ForeignKey(
            entity = ViajeEntity::class,
            parentColumns = ["folio"],
            childColumns = ["folio"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = PasajeroEntity::class, parentColumns = ["dni_hash"], childColumns = ["dni_hash"]),
    ],
    indices = [Index("dni_hash")],
)
data class ViajePasajeroEntity(
    val folio: String,
    @ColumnInfo(name = "dni_hash") val dniHash: String,
    @ColumnInfo(name = "fh_abordaje") val fhAbordaje: String?,
)

@Entity(
    tableName = "pagos",
    indices = [Index(value = ["folio"], unique = true)],
)
data class PagoEntity(
    @PrimaryKey val hash: String,
    val folio: String,
    val monto: Int,
    @ColumnInfo(name = "estado_pago") val estadoPago: String,
    val banco: String,
    @ColumnInfo(name = "cod_sbif") val codSbif: Int,
    @ColumnInfo(name = "rut_empresa") val rutEmpresa: String,
    @ColumnInfo(name = "medio_pago") val medioPago: String,
    @ColumnInfo(name = "tipo_recibo") val tipoRecibo: String,
    @ColumnInfo(name = "fecha_pago") val fechaPago: String?,
    @ColumnInfo(name = "monto_reembolsado") val montoReembolsado: Int?,
)

@Entity(
    tableName = "operaciones_pendientes",
    foreignKeys = [ForeignKey(entity = ViajeEntity::class, parentColumns = ["folio"], childColumns = ["folio"])],
    indices = [Index("folio")],
)
data class OperacionPendienteEntity(
    @PrimaryKey val id: String,
    val cuenta: String,
    val folio: String,
    @ColumnInfo(name = "estado_destino") val estadoDestino: String,
    @ColumnInfo(name = "creado_en") val creadoEn: Long,
    @ColumnInfo(name = "estado_envio") val estadoEnvio: String,
    @ColumnInfo(name = "mensaje_error") val mensajeError: String?,
)
