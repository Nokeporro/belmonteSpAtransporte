package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.asignable
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConductorOpcion(
    val correo: String,
    val nombre: String,
    val estado: EstadoConductor,
    val viajesAsignados: Int,
    val folioEnCurso: String?,
    val habilitado: Boolean,
)

data class VehiculoOpcion(
    val patente: String,
    val capacidad: Int,
    val estado: EstadoMovil,
)

enum class NoAsignable { SIN_PASAJEROS, YA_NO_SE_PUEDE }

data class AsignarUiState(
    val cargando: Boolean = true,
    val viaje: Viaje? = null,
    val noAsignable: NoAsignable? = null,
    val conductores: List<ConductorOpcion> = emptyList(),
    val conductorElegido: String? = null,
    val vehiculos: List<VehiculoOpcion> = emptyList(),
    val cargandoVehiculos: Boolean = false,
    val vehiculoElegido: String? = null,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val asignado: Boolean = false,
) {
    val conductor: ConductorOpcion? get() = conductores.firstOrNull { it.correo == conductorElegido }
    val vehiculo: VehiculoOpcion? get() = vehiculos.firstOrNull { it.patente == vehiculoElegido }

    val modificado: Boolean get() = !asignado && conductorElegido != null
    val puedeConfirmar: Boolean get() = noAsignable == null && conductor != null && vehiculo != null && !enviando
}

class AsignarViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val flota: FlotaRepository = FlotaRepository(application),
    private val clock: Clock = Reloj.santiago,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(AsignarUiState())
    val estado: StateFlow<AsignarUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    private var conductores: List<Conductor> = emptyList()

    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        _estado.value = AsignarUiState()
        conductores = emptyList()
        recargar()
    }

    fun reintentar() {
        _estado.update { it.copy(error = null) }
        recargar()
    }

    fun elegirConductor(correo: String) {
        val estado = _estado.value
        val opcion = estado.conductores.firstOrNull { it.correo == correo } ?: return
        if (!opcion.habilitado || estado.enviando || correo == estado.conductorElegido) return
        _estado.update {
            it.copy(
                conductorElegido = correo, vehiculos = emptyList(), vehiculoElegido = null,
                cargandoVehiculos = true, error = null,
            )
        }
        viewModelScope.launch { cargarVehiculos(correo) }
    }

    fun elegirVehiculo(patente: String) {
        val estado = _estado.value
        if (estado.enviando || estado.vehiculos.none { it.patente == patente }) return
        _estado.update { it.copy(vehiculoElegido = patente, error = null) }
    }

    fun confirmar() {
        val estado = _estado.value
        val folio = folio ?: return
        val correo = estado.conductorElegido
        val patente = estado.vehiculoElegido
        if (!estado.puedeConfirmar || correo == null || patente == null) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = viajes.asignar(folio, correo, patente)) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, asignado = true) }
                is Resultado.Fallo -> {
                    _estado.update { it.copy(enviando = false, error = resultado.error) }
                    if (resultado.error is ErrorApp.Conflicto) recargar(conservarEleccion = true)
                }
                Resultado.Encolado -> _estado.update { it.copy(enviando = false, error = ErrorApp.Inesperado) }
            }
        }
    }

    private fun recargar(conservarEleccion: Boolean = false) {
        val folio = folio ?: return
        observacion?.cancel()
        _estado.update { if (conservarEleccion) it else it.copy(cargando = true) }
        observacion = viewModelScope.launch {
            val refresco = viajes.refrescarViaje(folio)
            if (refresco is Resultado.Fallo && refresco.error != ErrorApp.SinConexion) {
                _estado.update { it.copy(cargando = false, error = it.error ?: refresco.error) }
                return@launch
            }
            when (val lista = flota.listarConductores()) {
                is Resultado.Exito -> {
                    conductores = lista.valor
                    _estado.update { it.copy(conductores = opciones(it.viaje)) }
                }
                is Resultado.Fallo -> {
                    _estado.update { it.copy(cargando = false, error = lista.error) }
                    return@launch
                }
                Resultado.Encolado -> Unit
            }
            _estado.value.conductorElegido?.let { cargarVehiculos(it) }
            observarViaje(folio)
        }
    }

    private suspend fun observarViaje(folio: String) {
        viajes.observarViaje(folio)
            .catch { _estado.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
            .collect { viaje ->
                if (viaje == null) {
                    _estado.update { it.copy(cargando = false, error = ErrorApp.NoEncontrado) }
                } else {
                    _estado.update {
                        it.copy(
                            cargando = false, viaje = viaje, conductores = opciones(viaje),
                            noAsignable = noAsignable(viaje),
                        )
                    }
                }
            }
    }

    private suspend fun cargarVehiculos(correo: String) {
        when (val resultado = flota.listarHabilitaciones(correo = correo)) {
            is Resultado.Exito -> {
                if (_estado.value.conductorElegido != correo) return
                val viaje = _estado.value.viaje
                val elegibles = resultado.valor.mapNotNull { vehiculoElegible(it, viaje) }
                _estado.update {
                    it.copy(
                        vehiculos = elegibles, cargandoVehiculos = false,
                        vehiculoElegido = it.vehiculoElegido?.takeIf { patente -> elegibles.any { v -> v.patente == patente } },
                    )
                }
            }
            is Resultado.Fallo -> _estado.update { it.copy(cargandoVehiculos = false, error = resultado.error) }
            Resultado.Encolado -> _estado.update { it.copy(cargandoVehiculos = false) }
        }
    }

    private fun vehiculoElegible(h: Habilitacion, viaje: Viaje?): VehiculoOpcion? {
        val esElActual = h.patente == viaje?.patenteMovil
        val sirve = h.activoMovil && (esElActual || (h.estadoMovil == EstadoMovil.DISPONIBLE && h.capacidad >= (viaje?.cantidadPasajeros ?: 0)))
        return if (sirve) VehiculoOpcion(h.patente, h.capacidad, h.estadoMovil) else null
    }

    private fun opciones(viaje: Viaje?): List<ConductorOpcion> =
        conductores.filter { it.activo }.map { conductor ->
            ConductorOpcion(
                correo = conductor.correo,
                nombre = conductor.nombre,
                estado = conductor.estado,
                viajesAsignados = conductor.viajesAsignados,
                folioEnCurso = conductor.folioEnCurso,
                habilitado = conductor.estado == EstadoConductor.DISPONIBLE || conductor.correo == viaje?.correoConductor,
            )
        }

    private fun noAsignable(viaje: Viaje): NoAsignable? = when {
        viaje.asignable(clock) -> null
        viaje.cantidadPasajeros == 0 -> NoAsignable.SIN_PASAJEROS
        else -> NoAsignable.YA_NO_SE_PUEDE
    }
}
