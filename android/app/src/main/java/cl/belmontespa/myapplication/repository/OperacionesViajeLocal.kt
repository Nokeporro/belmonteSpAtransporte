package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.database.DatosDemo
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

class OperacionesViajeLocal(private val local: SoporteLocal) {
    suspend fun crear(solicitud: NuevaSolicitud): Resultado<String> = local.ejecutar {
        val sesion = local.sesion(Rol.SOLICITANTE).obtener { return@ejecutar it }
        val usuario = local.buscarUsuario(sesion.correo) ?: return@ejecutar noEncontrado
        if (usuario.estadoSolicitante != "APROBADO") return@ejecutar conflicto("La cuenta debe estar aprobada.")
        val errores = Validaciones.solicitud(solicitud, local.ahora())
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val origen = normalizarLugar(solicitud.origen, "origen").obtener { return@ejecutar it }
        val destino = normalizarLugar(solicitud.destino, "destino").obtener { return@ejecutar it }
        if (solicitud.pasajeros.map { Hashes.documento(it.dni) }.distinct().size != solicitud.pasajeros.size) {
            return@ejecutar conflicto("Hay pasajeros repetidos.")
        }
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
        Resultado.Exito(folio)
    }
    private fun normalizarLugar(lugar: Ubicacion, campo: String): Resultado<Ubicacion> {
        val nombre = Validaciones.nombreCatalogo(lugar.comuna)
        val region = DatosDemo.regiones.firstOrNull { candidata ->
            candidata.comunas.any { Validaciones.nombreCatalogo(it) == nombre }
        } ?: return invalido(mapOf("$campo.comuna" to "No existe en el catálogo."))
        val comuna = region.comunas.first { Validaciones.nombreCatalogo(it) == nombre }
        return Resultado.Exito(lugar.copy(direccion = lugar.direccion.trim(), comuna = comuna, region = region.nombre,
            latitud = lugar.latitud.toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble(),
            longitud = lugar.longitud.toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()))
    }
    private suspend fun insertarPasajero(folio: String, pasajero: NuevoPasajero) {
        val hash = Hashes.documento(pasajero.dni)
        local.db.pasajeroDao().insertar(PasajeroEntity(hash, pasajero.nombre.trim()))
        local.db.viajePasajeroDao().insertar(ViajePasajeroEntity(folio, hash, null))
    }
    suspend fun agregar(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.SOLICITANTE).obtener { return@ejecutar it }
        val viaje = local.alcance(folio).obtener { return@ejecutar it }
        if (viaje.estado !in listOf("PENDIENTE", "ASIGNADO")) return@ejecutar conflicto("El viaje ya no admite pasajeros.")
        val errores = Validaciones.pasajero(pasajero)
        if (errores.isNotEmpty()) return@ejecutar invalido(errores)
        val anteriores = local.db.viajePasajeroDao().obtenerDelViaje(folio)
        if (anteriores.any { it.dniHash == Hashes.documento(pasajero.dni) }) return@ejecutar Resultado.Exito(Unit)
        val capacidad = viaje.patenteMovil?.let { local.db.movilDao().obtener(it)?.capacidad }
        if (capacidad != null && anteriores.size >= capacidad) return@ejecutar conflicto("El vehículo no tiene capacidad disponible.")
        insertarPasajero(folio, pasajero)
        local.db.viajeDao().guardar(viaje.copy(cantidadPasajeros = anteriores.size + 1))
        Resultado.Exito(Unit)
    }
    suspend fun asignar(folio: String, correo: String, patente: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.ADMINISTRADOR).obtener { return@ejecutar it }
        val viaje = local.buscarViaje(folio) ?: return@ejecutar noEncontrado
        val cuenta = local.buscarUsuario(correo) ?: return@ejecutar noEncontrado
        if (cuenta.rol != "CONDUCTOR") return@ejecutar conflicto("La cuenta no es de un conductor.")
        val vehiculo = local.db.movilDao().obtener(patente) ?: return@ejecutar noEncontrado
        val ocupados = local.viajes().filter { it.folio != folio && it.estado in listOf("ASIGNADO", "EN_CURSO") }
        val conductor = ParticipanteAsignacion(cuenta.activo, cuenta.estadoConductor == "DISPONIBLE",
            ocupados.filter { it.correoConductor == correo }.map { FechaIso.leer(it.fhAgenda) }, viaje.correoConductor == correo)
        val movil = ParticipanteAsignacion(vehiculo.activo, vehiculo.estadoMovil == "DISPONIBLE",
            ocupados.filter { it.patenteMovil == patente }.map { FechaIso.leer(it.fhAgenda) }, viaje.patenteMovil == patente)
        val habilitado = local.db.habilitacionDao().observarDelConductor(correo).first().any { it.patente == patente }
        val conflicto = conflictoDeAsignacion(EstadoViaje.valueOf(viaje.estado), FechaIso.leer(viaje.fhAgenda), viaje.cantidadPasajeros,
            local.ahora(), conductor, movil, vehiculo.capacidad, habilitado)
        if (conflicto != null) return@ejecutar conflicto(conflicto)
        if (conductor.yaAsignado && movil.yaAsignado) return@ejecutar Resultado.Exito(Unit)
        if (!movil.yaAsignado) local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(viaje.copy(estado = "ASIGNADO", correoConductor = correo, nombreConductor = cuenta.nombre,
            patenteMovil = patente, fhAsignacion = FechaIso.texto(local.ahora()), motivo = null))
        Resultado.Exito(Unit)
    }
    suspend fun cancelar(folio: String, motivo: String?, monto: Int?): Resultado<Unit> = local.ejecutar {
        val sesion = local.sesion().obtener { return@ejecutar it }
        if (sesion.rol == Rol.CONDUCTOR) return@ejecutar sinPermiso()
        val admin = sesion.rol == Rol.ADMINISTRADOR
        val motivoLimpio = motivo.orEmpty().trim()
        if (admin) {
            val errorMotivo = Validaciones.motivo(motivoLimpio)
            if (errorMotivo != null) return@ejecutar invalido(mapOf("motivo" to errorMotivo))
        }
        val viaje = local.alcance(folio).obtener { return@ejecutar it }
        if (viaje.estado !in listOf("PENDIENTE", "ASIGNADO")) return@ejecutar conflicto("El viaje ya no se puede cancelar.")
        if (!admin && viaje.vencido(local)) return@ejecutar conflicto("El viaje está vencido. Contacta al Administrador.")
        local.cancelarConPago(viaje, if (admin) motivoLimpio else viaje.motivo, admin, monto)
    }
    suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR).obtener { return@ejecutar it }
        val errorMotivo = Validaciones.motivo(motivo)
        if (errorMotivo != null) return@ejecutar invalido(mapOf("motivo" to errorMotivo))
        val viaje = local.alcance(folio).obtener { return@ejecutar it }
        if (viaje.estado != "ASIGNADO") return@ejecutar conflicto("El viaje ya no se puede rechazar.")
        local.db.viajePasajeroDao().limpiarAbordaje(folio)
        local.db.viajeDao().guardar(viaje.copy(estado = "PENDIENTE", correoConductor = null, nombreConductor = null,
            patenteMovil = null, fhAsignacion = null, motivo = motivo.trim()))
        Resultado.Exito(Unit)
    }
    suspend fun estado(folio: String, estado: EstadoViaje): Resultado<Unit> = local.ejecutar {
        local.sesion(Rol.CONDUCTOR).obtener { return@ejecutar it }
        val viaje = local.alcance(folio).obtener { return@ejecutar it }
        val otrosEnCurso = local.viajes().filter { it.folio != folio && it.estado == "EN_CURSO" }
        val conflicto = conflictoDeEstado(EstadoViaje.valueOf(viaje.estado), estado, FechaIso.leer(viaje.fhAgenda), local.ahora(),
            conductorConOtroEnCurso = otrosEnCurso.any { it.correoConductor == viaje.correoConductor },
            movilConOtroEnCurso = otrosEnCurso.any { it.patenteMovil == viaje.patenteMovil })
        if (conflicto != null) return@ejecutar conflicto(conflicto)
        val inicio = estado == EstadoViaje.EN_CURSO
        val usuario = local.buscarUsuario(viaje.correoConductor!!) ?: return@ejecutar noEncontrado
        val vehiculo = local.db.movilDao().obtener(viaje.patenteMovil!!) ?: return@ejecutar noEncontrado
        local.db.usuarioDao().guardar(usuario.copy(estadoConductor = if (inicio) "OCUPADO" else "DISPONIBLE"))
        local.db.movilDao().guardar(vehiculo.copy(estadoMovil = if (inicio) "EN_USO" else "DISPONIBLE"))
        local.db.viajeDao().guardar(if (inicio) viaje.copy(estado = estado.name, fhInicio = FechaIso.texto(local.ahora()))
            else viaje.copy(estado = estado.name, fhFin = FechaIso.texto(local.ahora())))
        Resultado.Exito(Unit)
    }
}
internal fun ViajeEntity.vencido(local: SoporteLocal) = local.ahora().isAfter(FechaIso.leer(fhAgenda).plusHours(1))
internal suspend fun SoporteLocal.cancelarConPago(viaje: ViajeEntity, motivo: String?, admin: Boolean, monto: Int?): Resultado<Unit> {
    var estadoPago = viaje.estadoPago
    val pago = viaje.hashPago?.let { db.pagoDao().obtener(it) }
    if (pago != null) {
        val vencido = viaje.vencido(this)
        if (admin && vencido && pago.estadoPago == "PAGADO") {
            val errorMonto = Validaciones.reembolso(monto, pago.monto)
            if (errorMonto != null) return invalido(mapOf("montoReembolso" to errorMonto))
        }
        val nuevo = pago.alCancelarElViaje(admin, vencido, monto)
        db.pagoDao().guardar(nuevo)
        estadoPago = nuevo.estadoPago
    }
    db.viajeDao().guardar(viaje.copy(estado = "CANCELADO", motivo = motivo, estadoPago = estadoPago))
    return Resultado.Exito(Unit)
}
