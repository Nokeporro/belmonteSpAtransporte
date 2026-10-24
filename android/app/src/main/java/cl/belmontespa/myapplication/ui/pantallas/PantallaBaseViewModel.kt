package cl.belmontespa.myapplication.ui.pantallas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.repository.*
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Validaciones
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// El estado de formularios reside en memoria del ViewModel. Los documentos no se serializan en rutas.
data class EstadoPantallaDemo(
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
    val campos: Map<String, String> = emptyMap(),
    val modificado: Boolean = false,
    val viajes: List<Viaje> = emptyList(),
    val viaje: Viaje? = null,
    val cuenta: Cuenta? = null,
    val conductores: List<Conductor> = emptyList(),
    val conductor: Conductor? = null,
    val moviles: List<Movil> = emptyList(),
    val movil: Movil? = null,
    val habilitaciones: List<Habilitacion> = emptyList(),
    val pendientes: List<Solicitante> = emptyList(),
    val pago: Pago? = null,
    val cargandoPago: Boolean = false,
    val errorPago: ErrorApp? = null,
    val catalogos: Catalogos? = null,
    val pasajeros: List<NuevoPasajero> = emptyList(),
    val destino: Pair<PantallaDemo, String>? = null,
    val ubicaciones: Map<String, Ubicacion> = emptyMap(),
)

// La UI recibe solo interfaces: no conoce Room, entidades ni implementaciones demo.
class DependenciasPantallas @Inject constructor(
    val viajes: ViajeRepository,
    val cuentas: CuentaRepository,
    val flota: FlotaRepository,
    val pagos: PagoRepository,
    val auth: AuthRepository,
    val catalogos: CatalogoRepository,
    val clock: Clock,
    @param:javax.inject.Named("ubicaciones_demo") val ubicacionesDemo: List<Ubicacion>,
)

open class PantallaBaseViewModel(private val repos: DependenciasPantallas) : ViewModel() {
    private val mutable = MutableStateFlow(EstadoPantallaDemo())
    val estado: StateFlow<EstadoPantallaDemo> = mutable.asStateFlow()
    private var pantalla: PantallaDemo? = null
    private var referencia = ""
    private var consulta: Job? = null
    fun campo(clave: String, valor: String) { mutable.update { it.copy(campos = it.campos + (clave to valor), modificado =
        if (pantalla == PantallaDemo.HABILITACIONES && clave.startsWith("filtro")) it.modificado
        else pantalla !in listOf(PantallaDemo.MIS_SOLICITUDES, PantallaDemo.SOLICITUDES), error = null, aviso = null) } }
    fun seleccionarLugar(campo: String, lugar: Ubicacion) {
        val catalogos = mutable.value.catalogos
        val region = catalogos?.regiones?.firstOrNull { r -> r.comunas.any {
            cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(it) == cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(lugar.comuna)
        } }
        val comuna = region?.comunas?.firstOrNull {
            cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(it) == cl.belmontespa.myapplication.util.Validaciones.nombreCatalogo(lugar.comuna)
        }
        val seleccionado = lugar.copy(comuna = comuna.orEmpty(), region = region?.nombre ?: lugar.region)
        mutable.update { it.copy(ubicaciones = it.ubicaciones + (campo to seleccionado),
            campos = it.campos + mapOf(campo to lugar.direccion, "$campo.region" to seleccionado.region, "$campo.comuna" to seleccionado.comuna), modificado = true, error = null) }
    }
    fun corregirComuna(campo: String, comuna: String) {
        val lugar = mutable.value.ubicaciones[campo] ?: return
        val region = mutable.value.catalogos?.regiones?.find { comuna in it.comunas } ?: return
        seleccionarLugar(campo, lugar.copy(comuna = comuna, region = region.nombre))
    }
    fun seleccionarRegion(campo: String) {
        val lugar = mutable.value.ubicaciones[campo] ?: return
        mutable.update { it.copy(ubicaciones = it.ubicaciones + (campo to lugar.copy(region = valor("$campo.region"), comuna = "")),
            campos = it.campos + ("$campo.comuna" to ""), modificado = true) }
    }
    fun consumirDestino() { mutable.update { it.copy(destino = null) } }
    fun aviso(id: Int) { mutable.update { it.copy(aviso = id) } }
    fun error(error: ErrorApp) { mutable.update { it.copy(error = error) } }
    fun valor(clave: String): String = mutable.value.campos[clave].orEmpty()
    fun cargar(p: PantallaDemo, id: String, forzar: Boolean = false) {
        if (pantalla == p && referencia == id && !forzar) return
        pantalla = p; referencia = id
        consulta?.cancel()
        consulta = viewModelScope.launch {
            mutable.update { it.copy(cargando = true, error = null,
                pago = if (p == PantallaDemo.DETALLE_PAGO) null else it.pago) }
            try {
                val actual = mutable.value
                val fecha = LocalDateTime.now(repos.clock).plusHours(3)
                if (actual.campos.isEmpty()) mutable.update { it.copy(campos = mapOf("fecha" to fecha.toLocalDate().toString(), "hora" to fecha.toLocalTime().withSecond(0).withNano(0).toString(), "tipoDocumento" to "RUT", "medioPago" to "TRANSFERENCIA", "tipoRecibo" to "BOLETA", "estado" to "TODOS")) }
                val publico = p.publica || p == PantallaDemo.CAMBIO_OBLIGATORIO
                if (!publico || p == PantallaDemo.CAMBIO_OBLIGATORIO) {
                    val cuenta = repos.cuentas.obtenerPerfil().valor()
                    mutable.update { it.copy(cuenta = cuenta) }
                    if (p in listOf(PantallaDemo.PERFIL_SOLICITANTE, PantallaDemo.PERFIL_ADMIN, PantallaDemo.PERFIL_CONDUCTOR)) {
                        mutable.update { it.copy(campos = it.campos + mapOf("nombre" to cuenta.nombre, "telefono" to cuenta.telefono.orEmpty())) }
                    }
                }
                if (p in listOf(PantallaDemo.REGISTRAR_PAGO, PantallaDemo.NUEVA_SOLICITUD)) {
                    mutable.update { it.copy(catalogos = repos.catalogos.obtenerCatalogos().valor()) }
                }
                if (p == PantallaDemo.CUENTAS_PENDIENTES) mutable.update { it.copy(pendientes = repos.cuentas.listarSolicitantesPendientes().valor()) }
                if (p in listOf(PantallaDemo.CONDUCTORES, PantallaDemo.HABILITACIONES)) mutable.update { it.copy(conductores = repos.flota.listarConductores().valor()) }
                if (p in listOf(PantallaDemo.VEHICULOS, PantallaDemo.HABILITACIONES)) mutable.update { it.copy(moviles = repos.flota.listarMoviles().valor()) }
                if (p == PantallaDemo.HABILITACIONES) {
                    if ("filtroVehiculo" !in mutable.value.campos) {
                        val vehiculo = id.removePrefix("vehiculo:").takeIf { id.startsWith("vehiculo:") }.orEmpty()
                        val conductor = id.removePrefix("conductor:").takeIf { id.startsWith("conductor:") }.orEmpty()
                        mutable.update { it.copy(campos = it.campos + mapOf("filtroVehiculo" to vehiculo, "filtroConductor" to conductor,
                            "vehiculo" to vehiculo, "conductor" to conductor)) }
                    }
                    mutable.update { it.copy(habilitaciones = repos.flota.listarHabilitaciones(
                        patente = valor("filtroVehiculo").ifBlank { null }, correo = valor("filtroConductor").ifBlank { null }).valor()) }
                }
                if (p == PantallaDemo.DETALLE_CONDUCTOR) {
                    val conductor = repos.flota.obtenerConductor(id).valor()
                    val habilitaciones = repos.flota.listarHabilitaciones(correo = id).valor()
                    mutable.update { it.copy(conductor = conductor, habilitaciones = habilitaciones,
                        campos = if (it.modificado) it.campos else it.campos + mapOf("nombre" to conductor.nombre, "telefono" to conductor.telefono.orEmpty())) }
                }
                if (p == PantallaDemo.DETALLE_VEHICULO) mutable.update { it.copy(movil = repos.flota.obtenerMovil(id).valor(), habilitaciones = repos.flota.listarHabilitaciones(patente = id).valor()) }
                if (p == PantallaDemo.DETALLE_PAGO) mutable.update { it.copy(pago = repos.pagos.obtenerPago(id).valor()) }
                val listado = p in listOf(PantallaDemo.MIS_SOLICITUDES, PantallaDemo.NOTIFICACION)
                if (listado) {
                    val filtro = FiltroViajes(estado = valor("estado").takeIf { it.isNotBlank() && it != "TODOS" }?.let(EstadoViaje::valueOf))
                    mutable.update { it.copy(cargando = false) }
                    repos.viajes.observarViajes(filtro).collect { datos -> mutable.update { it.copy(viajes = datos, cargando = false) } }
                } else if (p in pantallasDeViaje && id.isNotBlank()) {
                    mutable.update { it.copy(cargando = false) }
                    repos.viajes.observarViaje(id).collectLatest { viaje ->
                        val consultaSolicitante = p in listOf(PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS)
                        // El viaje permanece visible mientras se consulta el pago, incluso si este falla.
                        mutable.update { it.copy(viaje = viaje, cargando = false, pago = null, errorPago = null,
                            cargandoPago = consultaSolicitante && viaje?.hashPago != null,
                            error = if (viaje == null) ErrorApp.NoEncontrado else null) }
                        if (consultaSolicitante) {
                            val resultado = viaje?.hashPago?.let { hash ->
                                try { repos.pagos.obtenerPago(hash) }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { Resultado.Fallo(ErrorApp.Inesperado) }
                            }
                            mutable.update { it.copy(pago = (resultado as? Resultado.Exito)?.valor, cargandoPago = false,
                                errorPago = when (resultado) {
                                    is Resultado.Fallo -> resultado.error
                                    Resultado.Encolado -> ErrorApp.Inesperado
                                    else -> null
                                }) }
                            return@collectLatest
                        }
                        // La cancelación del Solicitante necesita el pago (HU16); el detalle del Administrador lo muestra,
                        // sin que un pago ilegible le oculte el viaje (HU24).
                        val pagoDelViaje = viaje?.hashPago?.let { hash -> when (p) {
                            PantallaDemo.CONFIRMAR_CANCELACION, PantallaDemo.SOLICITUD_CANCELADA -> repos.pagos.obtenerPago(hash).valor()
                            PantallaDemo.DETALLE_ADMIN, PantallaDemo.SIN_PASAJEROS_ADMIN -> (repos.pagos.obtenerPago(hash) as? Resultado.Exito)?.valor
                            else -> null
                        } }
                        mutable.update { it.copy(viaje = viaje, cargando = false, pago = pagoDelViaje,
                            error = if (viaje == null) ErrorApp.NoEncontrado else null) }
                    }
                } else mutable.update { it.copy(cargando = false) }
            } catch (e: CancellationException) { throw e
            } catch (e: FalloPantalla) { mutable.update { it.copy(cargando = false, error = e.error) }
            } catch (e: Exception) { mutable.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
        }
    }
    fun actualizar() { pantalla?.let { cargar(it, referencia, true) } }
    fun alVolverAPrimerPlano() {
        if (!mutable.value.cargando && pantalla in listOf(PantallaDemo.DETALLE_SOLICITUD,
                PantallaDemo.SOLICITUD_SIN_PASAJEROS, PantallaDemo.DETALLE_PAGO)) actualizar()
    }
    fun agregarPasajero() {
        if (valor("pasajeroNombre").isBlank() || valor("documento").isBlank()) { aviso(R.string.aviso_campos_obligatorios); return }
        val nuevo = NuevoPasajero(TipoDocumento.valueOf(valor("tipoDocumento")), valor("documento"), valor("pasajeroNombre"))
        val errores = cl.belmontespa.myapplication.util.Validaciones.pasajero(nuevo)
        if (errores.isNotEmpty()) { error(ErrorApp.Validacion(errores.mapKeys { if (it.key == "nombre") "pasajeroNombre" else "documento" })); return }
        if (mutable.value.pasajeros.any { cl.belmontespa.myapplication.util.Validaciones.documento(it.dni) == cl.belmontespa.myapplication.util.Validaciones.documento(nuevo.dni) }) {
            error(ErrorApp.Validacion(mapOf("documento" to "El pasajero ya está en la solicitud."))); return
        }
        mutable.update { it.copy(pasajeros = it.pasajeros + nuevo, campos = it.campos + mapOf("pasajeroNombre" to "", "documento" to ""), modificado = true) }
    }
    fun quitarPasajero(indice: Int) { mutable.update { it.copy(pasajeros = it.pasajeros.filterIndexed { i, _ -> i != indice }, modificado = true) } }
    fun editarPasajero(indice: Int) {
        val p = mutable.value.pasajeros.getOrNull(indice) ?: return
        mutable.update { it.copy(campos = it.campos + mapOf("pasajeroNombre" to p.nombre, "documento" to p.dni, "tipoDocumento" to p.tipoDocumento.name)) }
        quitarPasajero(indice)
    }
    fun ejecutar(accion: String, dato: String = "") {
        if (mutable.value.enviando) return
        mutable.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            try {
                val p = pantalla ?: return@launch
                var destino: Pair<PantallaDemo, String>? = null
                when (accion) {
                    "aprobar", "rechazarCuenta" -> {
                        if (p != PantallaDemo.CUENTAS_PENDIENTES || mutable.value.cuenta?.rol != Rol.ADMINISTRADOR) {
                            throw FalloPantalla(ErrorApp.SinPermiso())
                        }
                        val cuenta = mutable.value.pendientes.find { it.correo == dato && it.estado == EstadoSolicitante.PENDIENTE }
                            ?: throw FalloPantalla(ErrorApp.Conflicto("La cuenta ya no está pendiente. Actualiza la lista."))
                        if (accion == "aprobar") repos.cuentas.aprobarSolicitante(cuenta.correo).valor()
                        else repos.cuentas.rechazarSolicitante(cuenta.correo).valor()
                        mutable.update { it.copy(pendientes = it.pendientes.filterNot { c -> c.correo == cuenta.correo },
                            aviso = if (accion == "aprobar") R.string.cuenta_revision_aprobada else R.string.cuenta_revision_rechazada) }
                    }
                    "activoConductor" -> {
                        if (p != PantallaDemo.DETALLE_CONDUCTOR || mutable.value.cuenta?.rol != Rol.ADMINISTRADOR)
                            throw FalloPantalla(ErrorApp.SinPermiso())
                        val conductor = mutable.value.conductor ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                        val guardado = repos.flota.cambiarActivoConductor(conductor.correo, !conductor.activo).valor()
                        mutable.update { it.copy(conductor = guardado, aviso = R.string.flota_activo_guardado,
                            habilitaciones = it.habilitaciones.map { h -> h.copy(activoConductor = guardado.activo) }) }
                    }
                    "activoMovil" -> {
                        if (p != PantallaDemo.DETALLE_VEHICULO || mutable.value.cuenta?.rol != Rol.ADMINISTRADOR)
                            throw FalloPantalla(ErrorApp.SinPermiso())
                        val movil = mutable.value.movil ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                        val guardado = repos.flota.cambiarActivoMovil(movil.patente, !movil.activo).valor()
                        mutable.update { it.copy(movil = guardado, aviso = R.string.flota_activo_guardado,
                            habilitaciones = it.habilitaciones.map { h -> h.copy(activoMovil = guardado.activo) }) }
                    }
                    "pago" -> {
                        val pago = mutable.value.pago ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                        val nuevo = EstadoPago.entries.find { it.name == dato }
                        if (p != PantallaDemo.DETALLE_PAGO || mutable.value.cuenta?.rol != Rol.ADMINISTRADOR) {
                            throw FalloPantalla(ErrorApp.SinPermiso())
                        }
                        if (nuevo == null || !pago.estado.permiteCambioA(nuevo)) {
                            throw FalloPantalla(ErrorApp.Conflicto("El cambio de pago no está permitido. Actualiza su información."))
                        }
                        val guardado = repos.pagos.cambiarEstadoPago(referencia, nuevo).valor()
                        // La respuesta confirmada evita ofrecer de nuevo las acciones del estado anterior.
                        mutable.update { it.copy(pago = guardado, aviso = R.string.pago_estado_guardado) }
                    }
                    "finalizar" -> { repos.viajes.cambiarEstado(referencia, EstadoViaje.FINALIZADO).valor(); destino = PantallaDemo.DETALLE_VIAJE to referencia }
                    "rechazarViaje" -> { repos.viajes.rechazar(referencia, valor("motivo")).valor(); destino = PantallaDemo.MIS_VIAJES to "" }
                    else -> when (p) {
                        PantallaDemo.REGISTRO -> {
                            val r = repos.auth.registrarSolicitante(RegistroSolicitante(valor("nombre"), valor("correo"), valor("telefono").ifBlank { null }, valor("password"), valor("razonSocial"), valor("rutEmpresa"))).valor()
                            mutable.update { it.copy(aviso = if (r.estado == EstadoSolicitante.PENDIENTE) R.string.aviso_registro_pendiente else R.string.aviso_registro_aprobado) }
                        }
                        PantallaDemo.RECUPERAR -> {
                            val errores = Validaciones.recuperarPassword(valor("correo"))
                            if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                            repos.auth.recuperarPassword(Validaciones.correo(valor("correo"))).valor()
                            aviso(R.string.aviso_recuperacion_enviada)
                        }
                        PantallaDemo.CAMBIAR_PASSWORD, PantallaDemo.CAMBIO_OBLIGATORIO -> {
                            val errores = Validaciones.cambioPassword(valor("passwordActual"), valor("passwordNueva"), valor("confirmacion"))
                            if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                            if (mutable.value.cuenta?.rol == Rol.CONDUCTOR) throw FalloPantalla(ErrorApp.SinPermiso())
                            repos.cuentas.cambiarPassword(valor("passwordActual"), valor("passwordNueva")).valor()
                            mutable.update { it.copy(campos = it.campos - setOf("passwordActual", "passwordNueva", "confirmacion")) }
                            aviso(R.string.aviso_password_guardada)
                        }
                        PantallaDemo.NUEVA_SOLICITUD -> {
                            val origen = mutable.value.ubicaciones["origen"] ?: throw FalloPantalla(ErrorApp.Validacion(mapOf("origen" to "Selecciona el origen desde Google Places.")))
                            val destinoLugar = mutable.value.ubicaciones["destino"] ?: throw FalloPantalla(ErrorApp.Validacion(mapOf("destino" to "Selecciona el destino desde Google Places.")))
                            val folio = repos.viajes.crearSolicitud(NuevaSolicitud(LocalDateTime.of(LocalDate.parse(valor("fecha")), LocalTime.parse(valor("hora"))), origen, destinoLugar, valor("vuelo").ifBlank { null }, mutable.value.pasajeros)).valor()
                            destino = PantallaDemo.DETALLE_SOLICITUD to folio
                        }
                        PantallaDemo.AGREGAR_PASAJERO -> { repos.viajes.agregarPasajero(referencia, NuevoPasajero(TipoDocumento.valueOf(valor("tipoDocumento")), valor("documento"), valor("nombre"))).valor(); destino = PantallaDemo.DETALLE_SOLICITUD to referencia }
                        PantallaDemo.CONFIRMAR_CANCELACION -> {
                            val viaje = mutable.value.viaje ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                            if (!viaje.cancelablePorSolicitante(ahora())) throw FalloPantalla(ErrorApp.Conflicto("La solicitud ya no se puede cancelar. Actualiza su información."))
                            repos.viajes.cancelar(referencia).valor()
                            destino = PantallaDemo.SOLICITUD_CANCELADA to referencia
                        }
                        PantallaDemo.REGISTRAR_CONDUCTOR, PantallaDemo.REGISTRAR_ADMIN -> {
                            val c = NuevaCuenta(valor("nombre"), valor("correo"), valor("telefono").ifBlank { null }, valor("password"))
                            if (p == PantallaDemo.REGISTRAR_CONDUCTOR) {
                                validarConductor(c)
                                val conductor = repos.flota.registrarConductor(c).valor()
                                mutable.update { it.copy(campos = it.campos - "password") }
                                destino = PantallaDemo.DETALLE_CONDUCTOR to conductor.correo
                            }
                            else { repos.cuentas.registrarAdministrador(c).valor(); aviso(R.string.aviso_guardado) }
                        }
                        PantallaDemo.DETALLE_CONDUCTOR -> {
                            val cuenta = NuevaCuenta(valor("nombre"), referencia, valor("telefono").ifBlank { null }, valor("password"))
                            validarConductor(cuenta, passwordRequerida = false)
                            val conductor = repos.flota.actualizarConductor(referencia, cuenta.nombre, cuenta.telefono, cuenta.password.ifEmpty { null }).valor()
                            mutable.update { it.copy(conductor = conductor, campos = (it.campos - "password") +
                                mapOf("nombre" to conductor.nombre, "telefono" to conductor.telefono.orEmpty()), aviso = R.string.aviso_guardado) }
                        }
                        PantallaDemo.REGISTRAR_VEHICULO -> {
                            val gps = valor("gps").trim()
                            val nuevo = NuevoMovil(Validaciones.patente(valor("patente")), valor("capacidad").trim().toIntOrNull() ?: 0,
                                if (gps.isEmpty()) null else gps.toLongOrNull() ?: 0L)
                            val errores = Validaciones.movil(nuevo)
                            if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                            val movil = repos.flota.registrarMovil(nuevo).valor()
                            destino = PantallaDemo.DETALLE_VEHICULO to movil.patente
                        }
                        PantallaDemo.HABILITACIONES -> {
                            val errores = buildMap {
                                if (mutable.value.moviles.none { it.patente == valor("vehiculo") && it.activo }) put("vehiculo", "Selecciona un vehículo activo.")
                                if (mutable.value.conductores.none { it.correo == valor("conductor") && it.activo }) put("conductor", "Selecciona un conductor activo.")
                            }
                            if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                            repos.flota.habilitar(valor("vehiculo"), valor("conductor")).valor()
                            actualizar(); aviso(R.string.habilitacion_guardada)
                        }
                        PantallaDemo.REGISTRAR_PAGO -> {
                            val viaje = mutable.value.viaje ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                            if (viaje.estado == EstadoViaje.CANCELADO || viaje.hashPago != null) throw FalloPantalla(ErrorApp.Conflicto("El viaje ya tiene un pago o está cancelado."))
                            val catalogos = mutable.value.catalogos ?: throw FalloPantalla(ErrorApp.Conflicto("Carga los catálogos antes de registrar el pago."))
                            val monto = valor("monto").trim().toIntOrNull()
                            val banco = valor("banco").toIntOrNull()
                            val medio = catalogos.mediosPago.find { it.name == valor("medioPago") }
                            val recibo = catalogos.tiposRecibo.find { it.name == valor("tipoRecibo") }
                            val errores = buildMap {
                                if (monto == null || monto <= 0) put("monto", "Ingresa un monto entero entre 1 y 2147483647.")
                                if (banco == null || catalogos.bancos.none { it.codSbif == banco }) put("banco", "Selecciona un banco del catálogo.")
                                if (medio == null) put("medioPago", "Selecciona un medio de pago del catálogo.")
                                if (recibo == null) put("tipoRecibo", "Selecciona un tipo de recibo del catálogo.")
                            }
                            if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                            val pago = repos.pagos.registrarPago(NuevoPago(referencia, monto!!, banco!!, medio!!, recibo!!)).valor()
                            destino = PantallaDemo.DETALLE_PAGO to pago.hash
                        }
                        else -> Unit
                    }
                }
                mutable.update { it.copy(enviando = false, modificado = false, destino = destino) }
            } catch (e: CancellationException) { throw e
            } catch (e: FalloPantalla) {
                val pendientes = if (accion in listOf("aprobar", "rechazarCuenta") && e.error is ErrorApp.Conflicto)
                    (repos.cuentas.listarSolicitantesPendientes() as? Resultado.Exito)?.valor else null
                mutable.update { it.copy(enviando = false, error = e.error, pendientes = pendientes ?: it.pendientes) }
            } catch (e: Exception) { mutable.update { it.copy(enviando = false, error = ErrorApp.Validacion(mapOf("datos" to "Revisa los datos ingresados."))) } }
        }
    }
    suspend fun lugares(): List<Ubicacion> = repos.ubicacionesDemo
    private fun validarConductor(cuenta: NuevaCuenta, passwordRequerida: Boolean = true) {
        val errores = Validaciones.conductor(cuenta, passwordRequerida)
        if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
    }
    fun ahora(): LocalDateTime = LocalDateTime.now(repos.clock)
    private companion object {
        val pantallasDeViaje = setOf(PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS, PantallaDemo.AGREGAR_PASAJERO, PantallaDemo.CONFIRMAR_CANCELACION, PantallaDemo.SOLICITUD_CANCELADA, PantallaDemo.DETALLE_ADMIN, PantallaDemo.SIN_PASAJEROS_ADMIN, PantallaDemo.REGISTRAR_PAGO, PantallaDemo.SIN_CONEXION, PantallaDemo.CONFLICTO)
    }
}
private class FalloPantalla(val error: ErrorApp) : RuntimeException()
private fun <T> Resultado<T>.valor(): T = when(this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> throw FalloPantalla(error)
    Resultado.Encolado -> throw FalloPantalla(ErrorApp.Conflicto("El cambio está pendiente de sincronizar."))
}
