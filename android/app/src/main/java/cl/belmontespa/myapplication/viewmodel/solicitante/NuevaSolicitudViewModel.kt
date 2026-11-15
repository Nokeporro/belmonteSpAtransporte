package cl.belmontespa.myapplication.viewmodel.solicitante

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.lugares.LugaresPlaces
import cl.belmontespa.myapplication.lugares.ResultadoDeBusqueda
import cl.belmontespa.myapplication.lugares.ServicioDeLugares
import cl.belmontespa.myapplication.lugares.aUbicacion
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.repository.CatalogoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import cl.belmontespa.myapplication.util.Validaciones
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val CAMPO_ORIGEN = "origen"
internal const val CAMPO_DESTINO = "destino"
internal const val CAMPO_ORIGEN_COMUNA = "origen.comuna"
internal const val CAMPO_DESTINO_COMUNA = "destino.comuna"
internal const val CAMPO_PASAJERO_NOMBRE = "pasajeroNombre"
internal const val CAMPO_DOCUMENTO = "documento"

data class NuevaSolicitudUiState(
    val fecha: LocalDate,
    val hora: LocalTime,
    val origen: String = "",
    val destino: String = "",
    val origenLugar: Ubicacion? = null,
    val destinoLugar: Ubicacion? = null,
    val regiones: List<Region> = emptyList(),
    val conBusqueda: Boolean = false,
    val vuelo: String = "",
    val pasajeroNombre: String = "",
    val tipoDocumento: TipoDocumento = TipoDocumento.RUT,
    val documento: String = "",
    val pasajeros: List<NuevoPasajero> = emptyList(),
    val lugares: List<Ubicacion> = emptyList(),
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
    val creada: String? = null,
) {
    val origenSinComuna: Boolean get() = origenLugar?.comuna?.isBlank() == true
    val destinoSinComuna: Boolean get() = destinoLugar?.comuna?.isBlank() == true
    val puedeGuardar: Boolean
        get() = pasajeros.isNotEmpty() && origen.isNotBlank() && destino.isNotBlank() && !origenSinComuna && !destinoSinComuna
}

class NuevaSolicitudViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val clock: Clock = Reloj.santiago,
    private val lugares: List<Ubicacion> = DatosDemo.lugares,
    private val servicio: ServicioDeLugares = LugaresPlaces.de(application),
    private val catalogos: CatalogoRepository = CatalogoRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(inicial())
    val estado: StateFlow<NuevaSolicitudUiState> = _estado.asStateFlow()

    private var cargaDeRegiones: Job? = null

    private fun inicial(): NuevaSolicitudUiState {
        val propuesta = LocalDateTime.now(clock).plusHours(3).withSecond(0).withNano(0)
        return NuevaSolicitudUiState(
            propuesta.toLocalDate(), propuesta.toLocalTime(), lugares = lugares, conBusqueda = servicio.disponible,
        )
    }

    private fun editar(cambio: NuevaSolicitudUiState.() -> NuevaSolicitudUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null, aviso = null) }

    fun elegirFecha(fecha: LocalDate) = editar { copy(fecha = fecha) }
    fun elegirHora(hora: LocalTime) = editar { copy(hora = hora) }
    fun elegirOrigen(direccion: String) =
        editar { copy(origen = direccion, origenLugar = this.lugares.find { it.direccion == direccion }) }
    fun elegirDestino(direccion: String) =
        editar { copy(destino = direccion, destinoLugar = this.lugares.find { it.direccion == direccion }) }
    fun escribirVuelo(texto: String) = editar { copy(vuelo = texto) }
    fun escribirPasajeroNombre(texto: String) = editar { copy(pasajeroNombre = texto) }
    fun elegirTipoDocumento(tipo: String) = editar { copy(tipoDocumento = TipoDocumento.valueOf(tipo)) }
    fun escribirDocumento(texto: String) = editar { copy(documento = texto) }

    fun intentDeBusqueda(contexto: Context): Intent = servicio.intentDeBusqueda(contexto)

    fun resultadoDeBusqueda(esOrigen: Boolean, codigo: Int, datos: Intent?) {
        when (val resultado = servicio.resultadoDeBusqueda(codigo, datos)) {
            is ResultadoDeBusqueda.Elegido -> elegirLugar(esOrigen, resultado.placeId)
            ResultadoDeBusqueda.Fallido -> _estado.update { it.copy(aviso = R.string.aviso_lugar_no_disponible) }
            ResultadoDeBusqueda.Cancelado -> Unit
        }
    }

    internal fun elegirLugar(esOrigen: Boolean, placeId: String) {
        viewModelScope.launch {
            val lugar = (servicio.obtener(placeId) as? Resultado.Exito)?.valor?.aUbicacion()
            if (lugar == null) {
                _estado.update { it.copy(aviso = R.string.aviso_lugar_no_disponible) }
                return@launch
            }
            editar {
                if (esOrigen) copy(origen = lugar.direccion, origenLugar = lugar)
                else copy(destino = lugar.direccion, destinoLugar = lugar)
            }
            if (lugar.comuna.isBlank()) cargarRegiones()
        }
    }

    fun corregirComuna(esOrigen: Boolean, region: String, comuna: String) {
        if (_estado.value.regiones.none { it.nombre == region && comuna in it.comunas }) return
        editar {
            if (esOrigen) copy(origenLugar = origenLugar?.copy(comuna = comuna, region = region))
            else copy(destinoLugar = destinoLugar?.copy(comuna = comuna, region = region))
        }
    }

    fun cargarRegiones() {
        if (_estado.value.regiones.isNotEmpty() || cargaDeRegiones?.isActive == true) return
        cargaDeRegiones = viewModelScope.launch {
            val error = capturar(ErrorApp.Inesperado) {
                val regiones = catalogos.obtenerCatalogos().valor().regiones
                _estado.update { it.copy(regiones = regiones) }
            }
            if (error != null) _estado.update { it.copy(error = error) }
        }
    }

    fun agregarPasajero() {
        val datos = _estado.value
        if (datos.pasajeroNombre.isBlank() || datos.documento.isBlank()) {
            _estado.update { it.copy(aviso = R.string.aviso_campos_obligatorios) }
            return
        }
        val nuevo = NuevoPasajero(datos.tipoDocumento, datos.documento, datos.pasajeroNombre)
        val errores = Validaciones.pasajero(nuevo)
        if (errores.isNotEmpty()) {
            _estado.update {
                it.copy(error = ErrorApp.Validacion(errores.mapKeys { (k, _) -> if (k == "nombre") CAMPO_PASAJERO_NOMBRE else CAMPO_DOCUMENTO }))
            }
            return
        }
        if (datos.pasajeros.any { Validaciones.documento(it.dni) == Validaciones.documento(nuevo.dni) }) {
            _estado.update { it.copy(error = ErrorApp.Validacion(mapOf(CAMPO_DOCUMENTO to "El pasajero ya está en la solicitud."))) }
            return
        }
        _estado.update {
            it.copy(pasajeros = it.pasajeros + nuevo, pasajeroNombre = "", documento = "", modificado = true, error = null)
        }
    }

    fun quitarPasajero(indice: Int) =
        _estado.update { it.copy(pasajeros = it.pasajeros.filterIndexed { i, _ -> i != indice }, modificado = true) }

    fun editarPasajero(indice: Int) {
        val pasajero = _estado.value.pasajeros.getOrNull(indice) ?: return
        _estado.update {
            it.copy(pasajeroNombre = pasajero.nombre, documento = pasajero.dni, tipoDocumento = pasajero.tipoDocumento)
        }
        quitarPasajero(indice)
    }

    fun guardar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val origen = datos.origenLugar
                    ?: throw FalloPantalla(ErrorApp.Validacion(mapOf(CAMPO_ORIGEN to "Selecciona el origen.")))
                val destino = datos.destinoLugar
                    ?: throw FalloPantalla(ErrorApp.Validacion(mapOf(CAMPO_DESTINO to "Selecciona el destino.")))
                val sinComuna = buildMap {
                    if (origen.comuna.isBlank()) put(CAMPO_ORIGEN_COMUNA, "Falta la comuna.")
                    if (destino.comuna.isBlank()) put(CAMPO_DESTINO_COMUNA, "Falta la comuna.")
                }
                if (sinComuna.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(sinComuna))
                val solicitud = NuevaSolicitud(
                    LocalDateTime.of(datos.fecha, datos.hora), origen, destino, datos.vuelo.ifBlank { null }, datos.pasajeros,
                )
                val folio = viajes.crearSolicitud(solicitud).valor()
                _estado.update { it.copy(creada = folio) }
            }
            val rechazadas = (error as? ErrorApp.Validacion)?.errores?.keys.orEmpty()
            val sinComunaOrigen = CAMPO_ORIGEN_COMUNA in rechazadas
            val sinComunaDestino = CAMPO_DESTINO_COMUNA in rechazadas
            _estado.update {
                it.copy(
                    enviando = false,
                    modificado = error != null && it.modificado,
                    error = error,
                    origenLugar = if (sinComunaOrigen) it.origenLugar?.copy(comuna = "") else it.origenLugar,
                    destinoLugar = if (sinComunaDestino) it.destinoLugar?.copy(comuna = "") else it.destinoLugar,
                )
            }
            if (sinComunaOrigen || sinComunaDestino) cargarRegiones()
        }
    }

    fun consumirCreada() = _estado.update { it.copy(creada = null) }
}
