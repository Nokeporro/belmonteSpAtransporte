package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleVehiculoUiState(
    val movil: Movil? = null,
    val habilitaciones: List<Habilitacion> = emptyList(),
    val cuenta: Cuenta? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
)

/** «Detalle de vehículo» (HU17, HU26): sus datos y su código de abordaje, sus conductores habilitados y su baja. */
class DetalleVehiculoViewModel @JvmOverloads constructor(
    application: Application,
    private val flota: FlotaRepository = FlotaRepository(application),
    private val cuentas: CuentaRepository = CuentaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(DetalleVehiculoUiState())
    val estado: StateFlow<DetalleVehiculoUiState> = _estado.asStateFlow()

    private var patente: String? = null
    private var consulta: Job? = null

    /** Vuelve a leer al volver del alta o de editar. */
    fun cargar(patente: String, forzar: Boolean = false) {
        if (patente == this.patente && !forzar) return
        this.patente = patente
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            var cuenta: Cuenta? = null
            var movil: Movil? = null
            var habilitaciones: List<Habilitacion> = emptyList()
            val error = capturar(ErrorApp.Inesperado) {
                cuenta = cuentas.obtenerPerfil().valor()
                movil = flota.obtenerMovil(patente).valor()
                habilitaciones = flota.listarHabilitaciones(patente = patente).valor()
            }
            _estado.update {
                it.copy(
                    cuenta = cuenta ?: it.cuenta,
                    movil = movil ?: it.movil,
                    habilitaciones = if (movil != null) habilitaciones else it.habilitaciones,
                    cargando = false, error = error,
                )
            }
        }
    }

    fun reintentar() {
        patente?.let { cargar(it, forzar = true) }
    }

    /** Desactiva o reactiva (HU26); solo el Administrador, y el estado mostrado es el de la respuesta guardada. */
    fun cambiarActivo() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                if (datos.cuenta?.rol != Rol.ADMINISTRADOR) throw FalloPantalla(ErrorApp.SinPermiso())
                val movil = datos.movil ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                val guardado = flota.cambiarActivoMovil(movil.patente, !movil.activo).valor()
                _estado.update {
                    it.copy(
                        movil = guardado, aviso = R.string.flota_activo_guardado,
                        habilitaciones = it.habilitaciones.map { h -> h.copy(activoMovil = guardado.activo) },
                    )
                }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }
}
