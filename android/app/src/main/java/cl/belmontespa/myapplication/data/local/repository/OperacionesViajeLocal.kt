package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// Operaciones locales para que los formularios navegables de T06 persistan en la demo.
// No realizan llamadas al backend ni simulan notificaciones remotas.
class OperacionesViajeLocal @Inject constructor(private val local: SoporteLocal) {
    suspend fun crear(s: NuevaSolicitud): Resultado<String> = local.ejecutar {
        val sesion = local.sesion(Rol.SOLICITANTE)
        val u = local.usuario(sesion.correo)
        exigir(u.estadoSolicitante == "APROBADO", "La cuenta debe estar aprobada.")
        exigir(!s.fhAgenda.isBefore(local.ahora().plusHours(2)) && !s.fhAgenda.isAfter(local.ahora().plusDays(90)), "La fecha debe estar entre 2 horas y 90 días desde ahora.")
        exigir(s.pasajeros.isNotEmpty() && s.pasajeros.all { it.nombre.isNotBlank() && it.dni.isNotBlank() }, "Agrega al menos un pasajero con nombre y documento.")
        exigir(s.pasajeros.map { Hashes.documento(it.dni) }.distinct().size == s.pasajeros.size, "Hay pasajeros repetidos.")
        val numero = (local.viajes().maxOfOrNull { it.folioCorrelativo } ?: 1000) + 1
        val folio = "TR-$numero"
        val v = ViajeEntity(
            folio = folio, estado = "PENDIENTE", correoSolicitante = u.correo, nombreSolicitante = u.nombre,
            telefonoSolicitante = u.telefono, razonSocial = u.razonSocial!!,
            origenDireccion = s.origen.direccion, origenComuna = s.origen.comuna, origenRegion = s.origen.region,
            origenLatitud = s.origen.latitud, origenLongitud = s.origen.longitud,
            destinoDireccion = s.destino.direccion, destinoComuna = s.destino.comuna, destinoRegion = s.destino.region,
            destinoLatitud = s.destino.latitud, destinoLongitud = s.destino.longitud,
            fhCreacion = FechaIso.texto(local.ahora()), fhAgenda = FechaIso.texto(s.fhAgenda),
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = s.numeroVuelo,
            cantidadPasajeros = s.pasajeros.size, correoConductor = null, nombreConductor = null,
            patenteMovil = null, hashPago = null, estadoPago = null, motivo = null,
            folioCorrelativo = numero, descargadoEn = local.clock.millis(),
        )
        local.db.viajeDao().guardar(v)
        s.pasajeros.forEach { insertarPasajero(folio, it) }
        folio
    }
    private suspend fun insertarPasajero(folio: String, p: NuevoPasajero) {
        val hash = Hashes.documento(p.dni)
        // Un documento ya registrado conserva su nombre y no se duplica.
        local.db.pasajeroDao().insertar(PasajeroEntity(hash, p.nombre.trim()))
        local.db.viajePasajeroDao().insertar(ViajePasajeroEntity(folio, hash, null))
    }
    suspend fun agregar(folio: String, p: NuevoPasajero): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.SOLICITANTE)
        val v = local.alcance(folio)
        exigir(v.estado in listOf("PENDIENTE", "ASIGNADO"), "El viaje ya no admite pasajeros.")
        exigir(p.nombre.isNotBlank() && p.dni.isNotBlank(), "Ingresa nombre y documento.")
        val anteriores = local.db.viajePasajeroDao().obtenerDelViaje(folio)
        exigir(anteriores.none { it.dniHash == Hashes.documento(p.dni) }, "El pasajero ya está en este viaje.")
        val capacidad = v.patenteMovil?.let { local.db.movilDao().obtener(it)?.capacidad }
        exigir(capacidad == null || anteriores.size < capacidad, "El vehículo no tiene capacidad disponible.")
        insertarPasajero(folio, p)
        local.db.viajeDao().guardar(v.copy(cantidadPasajeros = anteriores.size + 1))
    }
    suspend fun asignar(folio: String, correo: String, patente: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val v = local.viaje(folio)
        exigir(v.estado in listOf("PENDIENTE", "ASIGNADO") && !v.vencido(local), "El viaje ya no se puede asignar.")
        val u = local.usuario(correo)
        val m = local.db.movilDao().obtener(patente) ?: falla(ErrorApp.NoEncontrado)
        exigir(u.rol == "CONDUCTOR" && u.activo && m.activo && u.estadoConductor == "DISPONIBLE" && m.estadoMovil == "DISPONIBLE", "Selecciona conductor y vehículo disponibles.")
        exigir(v.cantidadPasajeros in 1..m.capacidad, "Revisa la cantidad de pasajeros y la capacidad.")
        exigir(local.db.habilitacionDao().observarDelConductor(correo).first().any { it.patente == patente }, "El vehículo no está habilitado para el conductor.")
        val agenda = FechaIso.leer(v.fhAgenda)
        exigir(local.viajes().none { otro -> otro.folio != folio && otro.estado in listOf("ASIGNADO", "EN_CURSO") &&
            (otro.correoConductor == correo || otro.patenteMovil == patente) &&
            kotlin.math.abs(Duration.between(agenda, FechaIso.leer(otro.fhAgenda)).toMinutes()) < 120 }, "Hay otro viaje a menos de dos horas.")
        if (v.patenteMovil != patente) local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(v.copy(estado = "ASIGNADO", correoConductor = correo, nombreConductor = u.nombre,
            patenteMovil = patente, fhAsignacion = FechaIso.texto(local.ahora()), motivo = null))
    }
    suspend fun cancelar(folio: String, motivo: String?, monto: Int?): Resultado<Unit> = local.ejecutar {
        val s = local.sesion()
        exigir(s.rol != Rol.CONDUCTOR, "No tienes permiso para cancelar este viaje.")
        val v = local.alcance(folio)
        exigir(v.estado in listOf("PENDIENTE", "ASIGNADO"), "El viaje ya no se puede cancelar.")
        if (s.rol == Rol.SOLICITANTE) exigir(!v.vencido(local), "El viaje está vencido. Contacta al Administrador.")
        else exigir(!motivo.isNullOrBlank(), "Ingresa el motivo de cancelación.")
        local.cancelarConPago(v, motivo, s.rol == Rol.ADMINISTRADOR, monto)
    }
    suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR)
        val v = local.alcance(folio)
        exigir(v.estado == "ASIGNADO" && motivo.isNotBlank(), "Ingresa un motivo para rechazar la asignación.")
        local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(v.copy(estado = "PENDIENTE", correoConductor = null, nombreConductor = null,
            patenteMovil = null, fhAsignacion = null, motivo = motivo.trim()))
    }
    suspend fun estado(folio: String, estado: EstadoViaje): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR)
        val v = local.alcance(folio)
        val inicio = estado == EstadoViaje.EN_CURSO
        exigir(inicio && v.estado == "ASIGNADO" || estado == EstadoViaje.FINALIZADO && v.estado == "EN_CURSO", "El viaje cambió de estado. Actualiza el detalle.")
        if (inicio) {
            exigir(!local.ahora().isBefore(FechaIso.leer(v.fhAgenda).minusHours(1)) && !v.vencido(local), "El viaje está fuera del horario de inicio.")
            exigir(local.viajes().none { it.folio != folio && it.estado == "EN_CURSO" && (it.correoConductor == v.correoConductor || it.patenteMovil == v.patenteMovil) }, "El conductor o vehículo ya tiene un viaje en curso.")
        }
        val u = local.usuario(v.correoConductor!!)
        val m = local.db.movilDao().obtener(v.patenteMovil!!) ?: falla(ErrorApp.NoEncontrado)
        local.db.usuarioDao().guardar(u.copy(estadoConductor = if (inicio) "OCUPADO" else "DISPONIBLE"))
        local.db.movilDao().guardar(m.copy(estadoMovil = if (inicio) "EN_USO" else "DISPONIBLE"))
        local.db.viajeDao().guardar(if (inicio) v.copy(estado = estado.name, fhInicio = FechaIso.texto(local.ahora()))
            else v.copy(estado = estado.name, fhFin = FechaIso.texto(local.ahora())))
    }
}
internal fun ViajeEntity.vencido(local: SoporteLocal) = local.ahora().isAfter(FechaIso.leer(fhAgenda).plusHours(1))
// La cancelación actualiza el pago y la copia del estado en el viaje en una sola transacción.
internal suspend fun SoporteLocal.cancelarConPago(v: ViajeEntity, motivo: String?, admin: Boolean, monto: Int?) {
    var estadoPago = v.estadoPago
    val p = v.hashPago?.let { db.pagoDao().obtener(it) }
    if (p != null) {
        val nuevo = when (p.estadoPago) {
            "PENDIENTE" -> p.copy(estadoPago = "ANULADO")
            "PAGADO" -> {
                val reembolso = if (!admin) p.monto / 2 else if (v.vencido(this)) monto ?: p.monto else p.monto
                exigir(reembolso in 0..p.monto, "El reembolso debe estar entre cero y el total pagado.")
                if (reembolso == 0) p else p.copy(estadoPago = "REEMBOLSADO", montoReembolsado = reembolso)
            }
            else -> p
        }
        db.pagoDao().guardar(nuevo)
        estadoPago = nuevo.estadoPago
    }
    db.viajeDao().guardar(v.copy(estado = "CANCELADO", motivo = motivo, estadoPago = estadoPago))
}
