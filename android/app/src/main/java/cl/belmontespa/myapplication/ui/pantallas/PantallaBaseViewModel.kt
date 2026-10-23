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
    val catalogos: Catalogos? = null,
    val pasajeros: List<NuevoPasajero> = emptyList(),
    val destino: Pair<PantallaDemo, String>? = null,
    val horaAbordaje: LocalDateTime? = null,
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
    val abordaje: AbordajeRepository,
    val clock: Clock,
    val borrador: BorradorAbordaje,
    @param:javax.inject.Named("ubicaciones_demo") val ubicacionesDemo: List<Ubicacion>,
)

open class PantallaBaseViewModel(private val repos: DependenciasPantallas) : ViewModel() {
    private val mutable = MutableStateFlow(EstadoPantallaDemo())
    val estado: StateFlow<EstadoPantallaDemo> = mutable.asStateFlow()
    private var pantalla: PantallaDemo? = null
    private var referencia = ""
    private var consulta: Job? = null
    fun campo(clave: String, valor: String) { mutable.update { it.copy(campos = it.campos + (clave to valor), modificado = pantalla !in listOf(PantallaDemo.MIS_SOLICITUDES, PantallaDemo.SOLICITUDES), error = null, aviso = null) } }
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
            mutable.update { it.copy(cargando = true, error = null) }
            try {
                val actual = mutable.value
                val fecha = LocalDateTime.now(repos.clock).plusHours(3)
                if (actual.campos.isEmpty()) mutable.update { it.copy(campos = mapOf("fecha" to fecha.toLocalDate().toString(), "hora" to fecha.toLocalTime().withSecond(0).withNano(0).toString(), "tipoDocumento" to "RUT", "medioPago" to "TRANSFERENCIA", "tipoRecibo" to "BOLETA", "estado" to "TODOS")) }
                if (p == PantallaDemo.CONFIRMAR_ABORDAJE) repos.borrador.codigo?.let { codigo ->
                    mutable.update { it.copy(campos = it.campos + mapOf("patente" to codigo.patente, "codigo" to codigo.codigo)) }
                }
                if (p == PantallaDemo.ABORDADO) mutable.update { it.copy(horaAbordaje = repos.borrador.hora) }
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
                if (p == PantallaDemo.HABILITACIONES) mutable.update { it.copy(habilitaciones = repos.flota.listarHabilitaciones().valor()) }
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
                    repos.viajes.observarViaje(id).collect { viaje ->
                        val reembolso = if (p == PantallaDemo.CANCELAR_VIAJE && viaje?.hashPago != null && "reembolso" !in mutable.value.campos) repos.pagos.obtenerPago(viaje.hashPago).valor().monto.toString() else null
                        mutable.update { it.copy(viaje = viaje, cargando = false,
                            error = if (viaje == null) ErrorApp.NoEncontrado else null,
                            campos = if ("reembolso" in it.campos || viaje == null) it.campos else if (reembolso != null) it.campos + ("reembolso" to reembolso) else it.campos) }
                    }
                } else mutable.update { it.copy(cargando = false) }
            } catch (e: CancellationException) { throw e
            } catch (e: FalloPantalla) { mutable.update { it.copy(cargando = false, error = e.error) }
            } catch (e: Exception) { mutable.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
        }
    }
    fun actualizar() { pantalla?.let { cargar(it, referencia, true) } }
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
    fun seleccionarQr(texto: String): Boolean {
        val partes = texto.split(':')
        if (partes.size != 3 || partes[0] != "BELMONTE-ABORDAJE" || !partes[2].matches(Regex("[A-Fa-f0-9]{16}"))) { aviso(R.string.abordaje_qr_ajeno); return false }
        campo("patente", partes[1].uppercase()); campo("codigo", partes[2].uppercase())
        repos.borrador.codigo = CodigoAbordaje(partes[1].uppercase(), partes[2].uppercase())
        return true
    }
    fun ejecutar(accion: String, dato: String = "") {
        if (mutable.value.enviando) return
        mutable.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            try {
                val p = pantalla ?: return@launch
                var destino: Pair<PantallaDemo, String>? = null
                when (accion) {
                    "aprobar", "rechazarCuenta" -> { if (accion == "aprobar") repos.cuentas.aprobarSolicitante(dato).valor() else repos.cuentas.rechazarSolicitante(dato).valor(); actualizar() }
                    "activoConductor" -> { repos.flota.cambiarActivoConductor(referencia, mutable.value.conductor?.activo != true).valor(); actualizar() }
                    "activoMovil" -> { repos.flota.cambiarActivoMovil(referencia, mutable.value.movil?.activo != true).valor(); actualizar() }
                    "pago" -> { repos.pagos.cambiarEstadoPago(referencia, EstadoPago.valueOf(dato)).valor(); actualizar() }
                    "finalizar" -> { repos.viajes.cambiarEstado(referencia, EstadoViaje.FINALIZADO).valor(); destino = PantallaDemo.DETALLE_VIAJE to referencia }
                    "rechazarViaje" -> { repos.viajes.rechazar(referencia, valor("motivo")).valor(); destino = PantallaDemo.MIS_VIAJES to "" }
                    else -> when (p) {
                        PantallaDemo.REGISTRO -> {
                            val r = repos.auth.registrarSolicitante(RegistroSolicitante(valor("nombre"), valor("correo"), valor("telefono").ifBlank { null }, valor("password"), valor("razonSocial"), valor("rutEmpresa"))).valor()
                            mutable.update { it.copy(aviso = if (r.estado == EstadoSolicitante.PENDIENTE) R.string.aviso_registro_pendiente else R.string.aviso_registro_aprobado) }
                        }
                        PantallaDemo.RECUPERAR -> { repos.auth.recuperarPassword(valor("correo")).valor(); aviso(R.string.aviso_recuperacion_enviada) }
                        PantallaDemo.CAMBIAR_PASSWORD, PantallaDemo.CAMBIO_OBLIGATORIO -> {
                            if (valor("passwordNueva") != valor("confirmacion")) throw FalloPantalla(ErrorApp.Validacion(mapOf("confirmacion" to "Las contraseñas no coinciden.")))
                            repos.cuentas.cambiarPassword(valor("passwordActual"), valor("passwordNueva")).valor(); aviso(R.string.aviso_password_guardada)
                        }
                        PantallaDemo.NUEVA_SOLICITUD -> {
                            val origen = mutable.value.ubicaciones["origen"] ?: throw FalloPantalla(ErrorApp.Validacion(mapOf("origen" to "Selecciona el origen desde Google Places.")))
                            val destinoLugar = mutable.value.ubicaciones["destino"] ?: throw FalloPantalla(ErrorApp.Validacion(mapOf("destino" to "Selecciona el destino desde Google Places.")))
                            val folio = repos.viajes.crearSolicitud(NuevaSolicitud(LocalDateTime.of(LocalDate.parse(valor("fecha")), LocalTime.parse(valor("hora"))), origen, destinoLugar, valor("vuelo").ifBlank { null }, mutable.value.pasajeros)).valor()
                            destino = PantallaDemo.DETALLE_SOLICITUD to folio
                        }
                        PantallaDemo.AGREGAR_PASAJERO -> { repos.viajes.agregarPasajero(referencia, NuevoPasajero(TipoDocumento.valueOf(valor("tipoDocumento")), valor("documento"), valor("nombre"))).valor(); destino = PantallaDemo.DETALLE_SOLICITUD to referencia }
                        PantallaDemo.CONFIRMAR_CANCELACION, PantallaDemo.CANCELAR_VIAJE -> { repos.viajes.cancelar(referencia, valor("motivo").ifBlank { null }, valor("reembolso").toIntOrNull()).valor(); destino = (if (p == PantallaDemo.CONFIRMAR_CANCELACION) PantallaDemo.SOLICITUD_CANCELADA else PantallaDemo.DETALLE_ADMIN) to referencia }
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
                        PantallaDemo.REGISTRAR_VEHICULO -> { val m = repos.flota.registrarMovil(NuevoMovil(valor("patente"), valor("capacidad").toInt(), valor("gps").toLongOrNull())).valor(); destino = PantallaDemo.DETALLE_VEHICULO to m.patente }
                        PantallaDemo.HABILITACIONES -> { repos.flota.habilitar(valor("vehiculo"), valor("conductor")).valor(); actualizar() }
                        PantallaDemo.REGISTRAR_PAGO -> { val pago = repos.pagos.registrarPago(NuevoPago(referencia, valor("monto").toInt(), valor("banco").toInt(), MedioPago.valueOf(valor("medioPago")), TipoRecibo.valueOf(valor("tipoRecibo")))).valor(); destino = PantallaDemo.DETALLE_PAGO to pago.hash }
                        PantallaDemo.CONFIRMAR_INICIO -> { repos.viajes.cambiarEstado(referencia, EstadoViaje.EN_CURSO).valor(); destino = PantallaDemo.DETALLE_VIAJE to referencia }
                        PantallaDemo.CONFIRMAR_ABORDAJE -> { val hora = repos.abordaje.registrarAbordaje(CodigoAbordaje(valor("patente"), valor("codigo")), TipoDocumento.valueOf(valor("tipoDocumento")), valor("documento")).valor(); repos.borrador.hora = hora; mutable.update { it.copy(horaAbordaje = hora) }; destino = PantallaDemo.ABORDADO to "" }
                        else -> Unit
                    }
                }
                mutable.update { it.copy(enviando = false, modificado = false, destino = destino) }
            } catch (e: CancellationException) { throw e
            } catch (e: FalloPantalla) { mutable.update { it.copy(enviando = false, error = e.error) }
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
        val pantallasDeViaje = setOf(PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS, PantallaDemo.AGREGAR_PASAJERO, PantallaDemo.CONFIRMAR_CANCELACION, PantallaDemo.SOLICITUD_CANCELADA, PantallaDemo.DETALLE_ADMIN, PantallaDemo.SIN_PASAJEROS_ADMIN, PantallaDemo.CANCELAR_VIAJE, PantallaDemo.REGISTRAR_PAGO, PantallaDemo.DETALLE_VIAJE, PantallaDemo.ANTES_VENTANA, PantallaDemo.CONFIRMAR_INICIO, PantallaDemo.SIN_CONEXION, PantallaDemo.CONFLICTO, PantallaDemo.NAVEGAR)
    }
}
private class FalloPantalla(val error: ErrorApp) : RuntimeException()
private fun <T> Resultado<T>.valor(): T = when(this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> throw FalloPantalla(error)
    Resultado.Encolado -> throw FalloPantalla(ErrorApp.Conflicto("El cambio está pendiente de sincronizar."))
}

// Intercambio efímero entre escaneo, confirmación y resultado; nunca almacena el documento.
@javax.inject.Singleton
class BorradorAbordaje @Inject constructor() {
    var codigo: CodigoAbordaje? = null
    var hora: LocalDateTime? = null
}
