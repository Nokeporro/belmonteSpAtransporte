package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.PasajeroEntity
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.ViajeEntity
import cl.belmontespa.myapplication.model.ViajePasajeroEntity
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Hashes
import cl.belmontespa.myapplication.util.Validaciones
import java.math.RoundingMode
import kotlinx.coroutines.flow.first

// Operaciones locales para que los formularios navegables de T06 persistan en la demo.
// No realizan llamadas al backend ni simulan notificaciones remotas.
//
// Cómo se lee cada operación: `local.ejecutar { ... }` corre el bloque en una transacción de Room. Dentro,
// `exigir(condicion, mensaje)` corta con un 409 si no se cumple, `validar(errores)` con un 400 y `falla(error)`
// con el error indicado; `ejecutar` los convierte en `Resultado.Fallo` y deshace los cambios. Si el bloque llega
// al final, devuelve `Resultado.Exito` con el último valor.
class OperacionesViajeLocal(private val local: SoporteLocal) {
    suspend fun crear(solicitud: NuevaSolicitud): Resultado<String> = local.ejecutar {
        val sesion = local.sesion(Rol.SOLICITANTE)
        val usuario = local.usuario(sesion.correo)
        exigir(usuario.estadoSolicitante == "APROBADO", "La cuenta debe estar aprobada.")
        validar(Validaciones.solicitud(solicitud, local.ahora()))
        val origen = normalizarLugar(solicitud.origen, "origen")
        val destino = normalizarLugar(solicitud.destino, "destino")
        exigir(solicitud.pasajeros.map { Hashes.documento(it.dni) }.distinct().size == solicitud.pasajeros.size, "Hay pasajeros repetidos.")
        val numero = (local.viajes().maxOfOrNull { it.folioCorrelativo } ?: 1000) + 1
        val folio = "TR-$numero"
        val viaje = ViajeEntity(
            folio = folio, estado = "PENDIENTE", correoSolicitante = usuario.correo, nombreSolicitante = usuario.nombre,
            telefonoSolicitante = usuario.telefono, razonSocial = usuario.razonSocial!!,
            origenDireccion = origen.direccion, origenComuna = origen.comuna, origenRegion = origen.region,
            origenLatitud = origen.latitud, origenLongitud = origen.longitud,
            destinoDireccion = destino.direccion, destinoComuna = destino.comuna, destinoRegion = destino.region,
            destinoLatitud = destino.latitud, destinoLongitud = destino.longitud,
            fhCreacion = FechaIso.texto(local.ahora()), fhAgenda = FechaIso.texto(solicitud.fhAgenda.withSecond(0).withNano(0)),
            fhAsignacion = null, fhInicio = null, fhFin = null,
            numeroVuelo = Validaciones.vuelo(solicitud.numeroVuelo),
            cantidadPasajeros = solicitud.pasajeros.size, correoConductor = null, nombreConductor = null,
            patenteMovil = null, hashPago = null, estadoPago = null, motivo = null,
            folioCorrelativo = numero, descargadoEn = local.clock.millis(),
        )
        local.db.viajeDao().guardar(viaje)
        solicitud.pasajeros.forEach { insertarPasajero(folio, it) }
        folio
    }
    private fun normalizarLugar(lugar: Ubicacion, campo: String): Ubicacion {
        val nombre = Validaciones.nombreCatalogo(lugar.comuna)
        val region = DatosDemo.regiones.firstOrNull { candidata ->
            candidata.comunas.any { Validaciones.nombreCatalogo(it) == nombre }
        } ?: falla(ErrorApp.Validacion(mapOf("$campo.comuna" to "No existe en el catálogo.")))
        val comuna = region.comunas.first { Validaciones.nombreCatalogo(it) == nombre }
        return lugar.copy(direccion = lugar.direccion.trim(), comuna = comuna, region = region.nombre,
            latitud = lugar.latitud.toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble(),
            longitud = lugar.longitud.toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble())
    }
    private suspend fun insertarPasajero(folio: String, pasajero: NuevoPasajero) {
        val hash = Hashes.documento(pasajero.dni)
        // Un documento ya registrado conserva su nombre y no se duplica.
        local.db.pasajeroDao().insertar(PasajeroEntity(hash, pasajero.nombre.trim()))
        local.db.viajePasajeroDao().insertar(ViajePasajeroEntity(folio, hash, null))
    }
    suspend fun agregar(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.SOLICITANTE)
        val viaje = local.alcance(folio)
        exigir(viaje.estado in listOf("PENDIENTE", "ASIGNADO"), "El viaje ya no admite pasajeros.")
        validar(Validaciones.pasajero(pasajero))
        val anteriores = local.db.viajePasajeroDao().obtenerDelViaje(folio)
        if (anteriores.any { it.dniHash == Hashes.documento(pasajero.dni) }) return@ejecutar Unit
        val capacidad = viaje.patenteMovil?.let { local.db.movilDao().obtener(it)?.capacidad }
        exigir(capacidad == null || anteriores.size < capacidad, "El vehículo no tiene capacidad disponible.")
        insertarPasajero(folio, pasajero)
        local.db.viajeDao().guardar(viaje.copy(cantidadPasajeros = anteriores.size + 1))
    }
    suspend fun asignar(folio: String, correo: String, patente: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR)
        val viaje = local.viaje(folio)
        val cuenta = local.usuario(correo)
        exigir(cuenta.rol == "CONDUCTOR", "La cuenta no es de un conductor.")
        val vehiculo = local.db.movilDao().obtener(patente) ?: falla(ErrorApp.NoEncontrado)
        val ocupados = local.viajes().filter { it.folio != folio && it.estado in listOf("ASIGNADO", "EN_CURSO") }
        val conductor = ParticipanteAsignacion(cuenta.activo, cuenta.estadoConductor == "DISPONIBLE",
            ocupados.filter { it.correoConductor == correo }.map { FechaIso.leer(it.fhAgenda) }, viaje.correoConductor == correo)
        val movil = ParticipanteAsignacion(vehiculo.activo, vehiculo.estadoMovil == "DISPONIBLE",
            ocupados.filter { it.patenteMovil == patente }.map { FechaIso.leer(it.fhAgenda) }, viaje.patenteMovil == patente)
        val habilitado = local.db.habilitacionDao().observarDelConductor(correo).first().any { it.patente == patente }
        val conflicto = conflictoDeAsignacion(EstadoViaje.valueOf(viaje.estado), FechaIso.leer(viaje.fhAgenda), viaje.cantidadPasajeros,
            local.ahora(), conductor, movil, vehiculo.capacidad, habilitado)
        exigir(conflicto == null, conflicto.orEmpty())
        // Repetir la misma asignación no cambia nada: ni fecha de asignación ni abordaje.
        if (conductor.yaAsignado && movil.yaAsignado) return@ejecutar
        // Otro vehículo, otro QR: los pasajeros vuelven a Por abordar.
        if (!movil.yaAsignado) local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(viaje.copy(estado = "ASIGNADO", correoConductor = correo, nombreConductor = cuenta.nombre,
            patenteMovil = patente, fhAsignacion = FechaIso.texto(local.ahora()), motivo = null))
    }
    suspend fun cancelar(folio: String, motivo: String?, monto: Int?): Resultado<Unit> = local.ejecutar {
        val sesion = local.sesion()
        if (sesion.rol == Rol.CONDUCTOR) falla(ErrorApp.SinPermiso())
        val admin = sesion.rol == Rol.ADMINISTRADOR
        // V7: el motivo se valida antes que el viaje, como el 400 del backend; el Solicitante no lo envía.
        val motivoLimpio = motivo.orEmpty().trim()
        if (admin) validar(listOfNotNull(Validaciones.motivo(motivoLimpio)?.let { "motivo" to it }).toMap())
        val viaje = local.alcance(folio)
        exigir(viaje.estado in listOf("PENDIENTE", "ASIGNADO"), "El viaje ya no se puede cancelar.")
        if (!admin) exigir(!viaje.vencido(local), "El viaje está vencido. Contacta al Administrador.")
        local.cancelarConPago(viaje, if (admin) motivoLimpio else viaje.motivo, admin, monto)
    }
    // V8: el motivo se valida antes que el viaje, como el 400 del backend. Solo el conductor del viaje (uno ajeno o
    // un PENDIENTE sin conductor es SinPermiso) y solo mientras está ASIGNADO. No toca el pago ni la flota.
    suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR)
        validar(listOfNotNull(Validaciones.motivo(motivo)?.let { "motivo" to it }).toMap())
        val viaje = local.alcance(folio)
        exigir(viaje.estado == "ASIGNADO", "El viaje ya no se puede rechazar.")
        local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(viaje.copy(estado = "PENDIENTE", correoConductor = null, nombreConductor = null,
            patenteMovil = null, fhAsignacion = null, motivo = motivo.trim()))
    }
    suspend fun estado(folio: String, estado: EstadoViaje): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR)
        val viaje = local.alcance(folio)
        val otrosEnCurso = local.viajes().filter { it.folio != folio && it.estado == "EN_CURSO" }
        val conflicto = conflictoDeEstado(EstadoViaje.valueOf(viaje.estado), estado, FechaIso.leer(viaje.fhAgenda), local.ahora(),
            conductorConOtroEnCurso = otrosEnCurso.any { it.correoConductor == viaje.correoConductor },
            movilConOtroEnCurso = otrosEnCurso.any { it.patenteMovil == viaje.patenteMovil })
        exigir(conflicto == null, conflicto.orEmpty())
        val inicio = estado == EstadoViaje.EN_CURSO
        val usuario = local.usuario(viaje.correoConductor!!)
        val vehiculo = local.db.movilDao().obtener(viaje.patenteMovil!!) ?: falla(ErrorApp.NoEncontrado)
        local.db.usuarioDao().guardar(usuario.copy(estadoConductor = if (inicio) "OCUPADO" else "DISPONIBLE"))
        local.db.movilDao().guardar(vehiculo.copy(estadoMovil = if (inicio) "EN_USO" else "DISPONIBLE"))
        local.db.viajeDao().guardar(if (inicio) viaje.copy(estado = estado.name, fhInicio = FechaIso.texto(local.ahora()))
            else viaje.copy(estado = estado.name, fhFin = FechaIso.texto(local.ahora())))
    }
}
internal fun ViajeEntity.vencido(local: SoporteLocal) = local.ahora().isAfter(FechaIso.leer(fhAgenda).plusHours(1))
// La cancelación actualiza el pago y la copia del estado en el viaje en una sola transacción.
internal suspend fun SoporteLocal.cancelarConPago(viaje: ViajeEntity, motivo: String?, admin: Boolean, monto: Int?) {
    var estadoPago = viaje.estadoPago
    val pago = viaje.hashPago?.let { db.pagoDao().obtener(it) }
    if (pago != null) {
        val vencido = viaje.vencido(this)
        // V7: con el viaje vencido y el pago PAGADO el Administrador fija el monto, de 0 al total; si no, se ignora.
        if (admin && vencido && pago.estadoPago == "PAGADO") {
            validar(listOfNotNull(Validaciones.reembolso(monto, pago.monto)?.let { "montoReembolso" to it }).toMap())
        }
        val nuevo = pago.alCancelarElViaje(admin, vencido, monto)
        db.pagoDao().guardar(nuevo)
        estadoPago = nuevo.estadoPago
    }
    db.viajeDao().guardar(viaje.copy(estado = "CANCELADO", motivo = motivo, estadoPago = estadoPago))
}
