package cl.belmontespa.myapplication.viewmodel.solicitante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.model.Ubicacion
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val CAMPO_ORIGEN = "origen"
internal const val CAMPO_DESTINO = "destino"
internal const val CAMPO_PASAJERO_NOMBRE = "pasajeroNombre"
internal const val CAMPO_DOCUMENTO = "documento"

data class NuevaSolicitudUiState(
    val fecha: LocalDate,
    val hora: LocalTime,
    val origen: String = "",
    val destino: String = "",
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
    val puedeGuardar: Boolean get() = pasajeros.isNotEmpty() && origen.isNotBlank() && destino.isNotBlank()
}

class NuevaSolicitudViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val clock: Clock = Reloj.santiago,
    private val lugares: List<Ubicacion> = DatosDemo.lugares,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(inicial())
    val estado: StateFlow<NuevaSolicitudUiState> = _estado.asStateFlow()

    private fun inicial(): NuevaSolicitudUiState {
        val propuesta = LocalDateTime.now(clock).plusHours(3).withSecond(0).withNano(0)
        return NuevaSolicitudUiState(propuesta.toLocalDate(), propuesta.toLocalTime(), lugares = lugares)
    }

    private fun editar(cambio: NuevaSolicitudUiState.() -> NuevaSolicitudUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null, aviso = null) }

    fun elegirFecha(fecha: LocalDate) = editar { copy(fecha = fecha) }
    fun elegirHora(hora: LocalTime) = editar { copy(hora = hora) }
    fun elegirOrigen(direccion: String) = editar { copy(origen = direccion) }
    fun elegirDestino(direccion: String) = editar { copy(destino = direccion) }
    fun escribirVuelo(texto: String) = editar { copy(vuelo = texto) }
    fun escribirPasajeroNombre(texto: String) = editar { copy(pasajeroNombre = texto) }
    fun elegirTipoDocumento(tipo: String) = editar { copy(tipoDocumento = TipoDocumento.valueOf(tipo)) }
    fun escribirDocumento(texto: String) = editar { copy(documento = texto) }

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
                val origen = datos.lugares.find { it.direccion == datos.origen }
                    ?: throw FalloPantalla(ErrorApp.Validacion(mapOf(CAMPO_ORIGEN to "Selecciona el origen.")))
                val destino = datos.lugares.find { it.direccion == datos.destino }
                    ?: throw FalloPantalla(ErrorApp.Validacion(mapOf(CAMPO_DESTINO to "Selecciona el destino.")))
                val solicitud = NuevaSolicitud(
                    LocalDateTime.of(datos.fecha, datos.hora), origen, destino, datos.vuelo.ifBlank { null }, datos.pasajeros,
                )
                val folio = viajes.crearSolicitud(solicitud).valor()
                _estado.update { it.copy(creada = folio) }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }

    fun consumirCreada() = _estado.update { it.copy(creada = null) }
}
