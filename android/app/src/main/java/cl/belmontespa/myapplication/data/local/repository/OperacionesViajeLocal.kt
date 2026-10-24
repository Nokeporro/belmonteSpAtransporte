package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Validaciones
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// Operaciones locales para que los formularios navegables de T06 persistan en la demo.
// No realizan llamadas al backend ni simulan notificaciones remotas.
class OperacionesViajeLocal @Inject constructor(private val local: SoporteLocal) {
    suspend fun crear(s: NuevaSolicitud): Resultado<String> = local.ejecutar {
        val sesion = local.sesion(Rol.SOLICITANTE)
        val u = local.usuario(sesion.correo)
        exigir(u.estadoSolicitante == "APROBADO", "La cuenta debe estar aprobada.")
        validar(cl.belmontespa.myapplication.util.Validaciones.solicitud(s, local.ahora()))
        val origen = normalizarLugar(s.origen, "origen")
        val destino = normalizarLugar(s.destino, "destino")
        exigir(s.pasajeros.map { Hashes.documento(it.dni) }.distinct().size == s.pasajeros.size, "Hay pasajeros repetidos.")
        val numero = (local.viajes().maxOfOrNull { it.folioCorrelativo } ?: 1000) + 1
        val folio = "TR-$numero"
        val v = ViajeEntity(
            folio = folio, estado = "PENDIENTE", correoSolicitante = u.correo, nombreSolicitante = u.nombre,
            telefonoSolicitante = u.telefono, razonSocial = u.razonSocial!!,
            origenDireccion = origen.direccion, origenComuna = origen.comuna, origenRegion = origen.region,
            origenLatitud = origen.latitud, origenLongitud = origen.longitud,
            destinoDireccion = destino.direccion, destinoComuna = destino.comuna, destinoRegion = destino.region,
            destinoLatitud = destino.latitud, destinoLongitud = destino.longitud,
            fhCreacion = FechaIso.texto(local.ahora()), fhAgenda = FechaIso.texto(s.fhAgenda.withSecond(0).withNano(0)),
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = cl.belmontespa.myapplication.util.Validaciones.vuelo(s.numeroVuelo),
            cantidadPasajeros = s.pasajeros.size, correoConductor = null, nombreConductor = null,
            patenteMovil = null, hashPago = null, estadoPago = null, motivo = null,
            folioCorrelativo = numero, descargadoEn = local.clock.millis(),
        )
        local.db.viajeDao().guardar(v)
        s.pasajeros.forEach { insertarPasajero(folio, it) }
        folio
    }
    private fun normalizarLugar(lugar: Ubicacion, campo: String): Ubicacion {
        val nombre = cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(lugar.comuna)
        val region = cl.belmontespa.myapplication.data.local.DatosDemo.regiones.firstOrNull { r -> r.comunas.any {
            cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(it) == nombre
        } } ?: falla(ErrorApp.Validacion(mapOf("$campo.comuna" to "No existe en el catálogo.")))
        val comuna = region.comunas.first { cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(it) == nombre }
        return lugar.copy(direccion = lugar.direccion.trim(), comuna = comuna, region = region.nombre,
            latitud = lugar.latitud.toBigDecimal().setScale(6, java.math.RoundingMode.HALF_UP).toDouble(),
            longitud = lugar.longitud.toBigDecimal().setScale(6, java.math.RoundingMode.HALF_UP).toDouble())
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
        validar(cl.belmontespa.myapplication.util.Validaciones.pasajero(p))
        val anteriores = local.db.viajePasajeroDao().obtenerDelViaje(folio)
        if (anteriores.any { it.dniHash == Hashes.documento(p.dni) }) return@ejecutar Unit
        val capacidad = v.patenteMovil?.let { local.db.movilDao().obtener(it)?.capacidad }
        exigir(capacidad == null || anteriores.size < capacidad, "El vehículo no tiene capacidad disponible.")
        insertarPasajero(folio, p)
        local.db.viajeDao().guardar(v.copy(cantidadPasajeros = anteriores.size + 1))
    }
    suspend fun asignar(folio: String, correo: String, patente: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val v = local.viaje(folio)
        val u = local.usuario(correo)
        exigir(u.rol == "CONDUCTOR", "La cuenta no es de un conductor.")
        val m = local.db.movilDao().obtener(patente) ?: falla(ErrorApp.NoEncontrado)
        val ocupados = local.viajes().filter { it.folio != folio && it.estado in listOf("ASIGNADO", "EN_CURSO") }
        val conductor = ParticipanteAsignacion(u.activo, u.estadoConductor == "DISPONIBLE",
            ocupados.filter { it.correoConductor == correo }.map { FechaIso.leer(it.fhAgenda) }, v.correoConductor == correo)
        val movil = ParticipanteAsignacion(m.activo, m.estadoMovil == "DISPONIBLE",
            ocupados.filter { it.patenteMovil == patente }.map { FechaIso.leer(it.fhAgenda) }, v.patenteMovil == patente)
        val habilitado = local.db.habilitacionDao().observarDelConductor(correo).first().any { it.patente == patente }
        val conflicto = conflictoDeAsignacion(EstadoViaje.valueOf(v.estado), FechaIso.leer(v.fhAgenda), v.cantidadPasajeros,
            local.ahora(), conductor, movil, m.capacidad, habilitado)
        exigir(conflicto == null, conflicto.orEmpty())
        // Repetir la misma asignación no cambia nada: ni fecha de asignación ni abordaje.
        if (conductor.yaAsignado && movil.yaAsignado) return@ejecutar
        // Otro vehículo, otro QR: los pasajeros vuelven a Por abordar.
        if (!movil.yaAsignado) local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(v.copy(estado = "ASIGNADO", correoConductor = correo, nombreConductor = u.nombre,
            patenteMovil = patente, fhAsignacion = FechaIso.texto(local.ahora()), motivo = null))
    }
    suspend fun cancelar(folio: String, motivo: String?, monto: Int?): Resultado<Unit> = local.ejecutar {
        val s = local.sesion()
        if (s.rol == Rol.CONDUCTOR) falla(ErrorApp.SinPermiso())
        val admin = s.rol == Rol.ADMINISTRADOR
        // V7: el motivo se valida antes que el viaje, como el 400 del backend; el Solicitante no lo envía.
        val motivoLimpio = motivo.orEmpty().trim()
        if (admin) validar(listOfNotNull(Validaciones.motivo(motivoLimpio)?.let { "motivo" to it }).toMap())
        val v = local.alcance(folio)
        exigir(v.estado in listOf("PENDIENTE", "ASIGNADO"), "El viaje ya no se puede cancelar.")
        if (!admin) exigir(!v.vencido(local), "El viaje está vencido. Contacta al Administrador.")
        local.cancelarConPago(v, if (admin) motivoLimpio else v.motivo, admin, monto)
    }
    // V8: el motivo se valida antes que el viaje, como el 400 del backend. Solo el conductor del viaje (uno ajeno o
    // un PENDIENTE sin conductor es SinPermiso) y solo mientras está ASIGNADO. No toca el pago ni la flota.
    suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR)
        validar(listOfNotNull(Validaciones.motivo(motivo)?.let { "motivo" to it }).toMap())
        val v = local.alcance(folio)
        exigir(v.estado == "ASIGNADO", "El viaje ya no se puede rechazar.")
        local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(v.copy(estado = "PENDIENTE", correoConductor = null, nombreConductor = null,
            patenteMovil = null, fhAsignacion = null, motivo = motivo.trim()))
    }
    suspend fun estado(folio: String, estado: EstadoViaje): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR)
        val v = local.alcance(folio)
        val otrosEnCurso = local.viajes().filter { it.folio != folio && it.estado == "EN_CURSO" }
        val conflicto = conflictoDeEstado(EstadoViaje.valueOf(v.estado), estado, FechaIso.leer(v.fhAgenda), local.ahora(),
            conductorConOtroEnCurso = otrosEnCurso.any { it.correoConductor == v.correoConductor },
            movilConOtroEnCurso = otrosEnCurso.any { it.patenteMovil == v.patenteMovil })
        exigir(conflicto == null, conflicto.orEmpty())
        val inicio = estado == EstadoViaje.EN_CURSO
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
        val vencido = v.vencido(this)
        // V7: con el viaje vencido y el pago PAGADO el Administrador fija el monto, de 0 al total; si no, se ignora.
        if (admin && vencido && p.estadoPago == "PAGADO") {
            validar(listOfNotNull(Validaciones.reembolso(monto, p.monto)?.let { "montoReembolso" to it }).toMap())
        }
        val nuevo = p.alCancelarElViaje(admin, vencido, monto)
        db.pagoDao().guardar(nuevo)
        estadoPago = nuevo.estadoPago
    }
    db.viajeDao().guardar(v.copy(estado = "CANCELADO", motivo = motivo, estadoPago = estadoPago))
}
