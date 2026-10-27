package cl.belmontespa.myapplication.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.belmontespa.myapplication.model.HabilitacionEntity
import cl.belmontespa.myapplication.model.MovilEntity
import cl.belmontespa.myapplication.model.PagoEntity
import cl.belmontespa.myapplication.model.PasajeroEntity
import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.model.ViajeEntity
import cl.belmontespa.myapplication.model.ViajePasajeroEntity
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Carga [DatosDemo] al crear la base, en las builds debug y demo. Inserta con SQL directo dentro de
 * `onCreate`, en la misma transacción que crea las tablas, así los datos ya están cuando la app
 * hace su primera consulta (p. ej. el login).
 */
class SemillaDemo(private val clock: Clock = Reloj.santiago) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val semilla = DatosDemo.crear(
            ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES),
            descargadoEn = clock.millis(),
        )
        semilla.usuarios.forEach { db.insertar("usuarios", it.valores()) }
        semilla.moviles.forEach { db.insertar("moviles", it.valores()) }
        semilla.habilitaciones.forEach { db.insertar("habilitaciones", it.valores()) }
        semilla.pasajeros.forEach { db.insertar("pasajeros", it.valores()) }
        semilla.viajes.forEach { db.insertar("viajes", it.valores()) }
        semilla.viajePasajeros.forEach { db.insertar("viaje_pasajeros", it.valores()) }
        semilla.pagos.forEach { db.insertar("pagos", it.valores()) }
    }

    private fun SupportSQLiteDatabase.insertar(tabla: String, valores: ContentValues) {
        insert(tabla, SQLiteDatabase.CONFLICT_ABORT, valores)
    }

    private fun UsuarioEntity.valores() = contentValuesOf(
        "correo" to correo,
        "nombre" to nombre,
        "telefono" to telefono,
        "rol" to rol,
        "activo" to activo,
        "estado_conductor" to estadoConductor,
        "estado_solicitante" to estadoSolicitante,
        "razon_social" to razonSocial,
        "rut_empresa" to rutEmpresa,
        "fecha_registro" to fechaRegistro,
        "password_sha256" to passwordSha256,
        "password_temporal_sha256" to passwordTemporalSha256,
        "password_temporal_expira" to passwordTemporalExpira,
    )

    private fun MovilEntity.valores() = contentValuesOf(
        "patente" to patente,
        "capacidad" to capacidad,
        "gps_serial" to gpsSerial,
        "activo" to activo,
        "estado_movil" to estadoMovil,
        "codigo_abordaje" to codigoAbordaje,
    )

    private fun HabilitacionEntity.valores() = contentValuesOf(
        "patente" to patente,
        "correo_conductor" to correoConductor,
    )

    private fun PasajeroEntity.valores() = contentValuesOf(
        "dni_hash" to dniHash,
        "nombre" to nombre,
    )

    private fun ViajeEntity.valores() = contentValuesOf(
        "folio" to folio,
        "estado" to estado,
        "correo_solicitante" to correoSolicitante,
        "nombre_solicitante" to nombreSolicitante,
        "telefono_solicitante" to telefonoSolicitante,
        "razon_social" to razonSocial,
        "origen_direccion" to origenDireccion,
        "origen_comuna" to origenComuna,
        "origen_region" to origenRegion,
        "origen_latitud" to origenLatitud,
        "origen_longitud" to origenLongitud,
        "destino_direccion" to destinoDireccion,
        "destino_comuna" to destinoComuna,
        "destino_region" to destinoRegion,
        "destino_latitud" to destinoLatitud,
        "destino_longitud" to destinoLongitud,
        "fh_creacion" to fhCreacion,
        "fh_agenda" to fhAgenda,
        "fh_asignacion" to fhAsignacion,
        "fh_inicio" to fhInicio,
        "fh_fin" to fhFin,
        "numero_vuelo" to numeroVuelo,
        "cantidad_pasajeros" to cantidadPasajeros,
        "correo_conductor" to correoConductor,
        "nombre_conductor" to nombreConductor,
        "patente_movil" to patenteMovil,
        "hash_pago" to hashPago,
        "estado_pago" to estadoPago,
        "motivo" to motivo,
        "folio_correlativo" to folioCorrelativo,
        "descargado_en" to descargadoEn,
    )

    private fun ViajePasajeroEntity.valores() = contentValuesOf(
        "folio" to folio,
        "dni_hash" to dniHash,
        "fh_abordaje" to fhAbordaje,
    )

    private fun PagoEntity.valores() = contentValuesOf(
        "hash" to hash,
        "folio" to folio,
        "monto" to monto,
        "estado_pago" to estadoPago,
        "banco" to banco,
        "cod_sbif" to codSbif,
        "rut_empresa" to rutEmpresa,
        "medio_pago" to medioPago,
        "tipo_recibo" to tipoRecibo,
        "fecha_pago" to fechaPago,
        "monto_reembolsado" to montoReembolsado,
    )
}
