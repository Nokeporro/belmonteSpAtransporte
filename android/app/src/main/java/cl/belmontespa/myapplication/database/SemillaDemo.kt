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
        semilla.usuarios.forEach { db.insertar("usuarios", valoresUsuario(it)) }
        semilla.moviles.forEach { db.insertar("moviles", valoresMovil(it)) }
        semilla.habilitaciones.forEach { db.insertar("habilitaciones", valoresHabilitacion(it)) }
        semilla.pasajeros.forEach { db.insertar("pasajeros", valoresPasajero(it)) }
        semilla.viajes.forEach { db.insertar("viajes", valoresViaje(it)) }
        semilla.viajePasajeros.forEach { db.insertar("viaje_pasajeros", valoresViajePasajero(it)) }
        semilla.pagos.forEach { db.insertar("pagos", valoresPago(it)) }
    }

    private fun SupportSQLiteDatabase.insertar(tabla: String, valores: ContentValues) {
        insert(tabla, SQLiteDatabase.CONFLICT_ABORT, valores)
    }

    private fun valoresUsuario(e: UsuarioEntity) = contentValuesOf(
        "correo" to e.correo,
        "nombre" to e.nombre,
        "telefono" to e.telefono,
        "rol" to e.rol,
        "activo" to e.activo,
        "estado_conductor" to e.estadoConductor,
        "estado_solicitante" to e.estadoSolicitante,
        "razon_social" to e.razonSocial,
        "rut_empresa" to e.rutEmpresa,
        "fecha_registro" to e.fechaRegistro,
        "password_sha256" to e.passwordSha256,
        "password_temporal_sha256" to e.passwordTemporalSha256,
        "password_temporal_expira" to e.passwordTemporalExpira,
    )

    private fun valoresMovil(e: MovilEntity) = contentValuesOf(
        "patente" to e.patente,
        "capacidad" to e.capacidad,
        "gps_serial" to e.gpsSerial,
        "activo" to e.activo,
        "estado_movil" to e.estadoMovil,
        "codigo_abordaje" to e.codigoAbordaje,
    )

    private fun valoresHabilitacion(e: HabilitacionEntity) = contentValuesOf(
        "patente" to e.patente,
        "correo_conductor" to e.correoConductor,
    )

    private fun valoresPasajero(e: PasajeroEntity) = contentValuesOf(
        "dni_hash" to e.dniHash,
        "nombre" to e.nombre,
    )

    private fun valoresViaje(e: ViajeEntity) = contentValuesOf(
        "folio" to e.folio,
        "estado" to e.estado,
        "correo_solicitante" to e.correoSolicitante,
        "nombre_solicitante" to e.nombreSolicitante,
        "telefono_solicitante" to e.telefonoSolicitante,
        "razon_social" to e.razonSocial,
        "origen_direccion" to e.origenDireccion,
        "origen_comuna" to e.origenComuna,
        "origen_region" to e.origenRegion,
        "origen_latitud" to e.origenLatitud,
        "origen_longitud" to e.origenLongitud,
        "destino_direccion" to e.destinoDireccion,
        "destino_comuna" to e.destinoComuna,
        "destino_region" to e.destinoRegion,
        "destino_latitud" to e.destinoLatitud,
        "destino_longitud" to e.destinoLongitud,
        "fh_creacion" to e.fhCreacion,
        "fh_agenda" to e.fhAgenda,
        "fh_asignacion" to e.fhAsignacion,
        "fh_inicio" to e.fhInicio,
        "fh_fin" to e.fhFin,
        "numero_vuelo" to e.numeroVuelo,
        "cantidad_pasajeros" to e.cantidadPasajeros,
        "correo_conductor" to e.correoConductor,
        "nombre_conductor" to e.nombreConductor,
        "patente_movil" to e.patenteMovil,
        "hash_pago" to e.hashPago,
        "estado_pago" to e.estadoPago,
        "motivo" to e.motivo,
        "folio_correlativo" to e.folioCorrelativo,
        "descargado_en" to e.descargadoEn,
    )

    private fun valoresViajePasajero(e: ViajePasajeroEntity) = contentValuesOf(
        "folio" to e.folio,
        "dni_hash" to e.dniHash,
        "fh_abordaje" to e.fhAbordaje,
    )

    private fun valoresPago(e: PagoEntity) = contentValuesOf(
        "hash" to e.hash,
        "folio" to e.folio,
        "monto" to e.monto,
        "estado_pago" to e.estadoPago,
        "banco" to e.banco,
        "cod_sbif" to e.codSbif,
        "rut_empresa" to e.rutEmpresa,
        "medio_pago" to e.medioPago,
        "tipo_recibo" to e.tipoRecibo,
        "fecha_pago" to e.fechaPago,
        "monto_reembolsado" to e.montoReembolsado,
    )
}
