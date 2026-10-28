package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.util.Validaciones
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

data class DetalleConductorUiState(
    val conductor: Conductor? = null,
    val habilitaciones: List<Habilitacion> = emptyList(),
    val cuenta: Cuenta? = null,
    val nombre: String = "",
    val telefono: String = "",
    val password: String = "",
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
)

class DetalleConductorViewModel @JvmOverloads constructor(
    application: Application,
    private val flota: FlotaRepository = FlotaRepository(application),
    private val cuentas: CuentaRepository = CuentaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(DetalleConductorUiState())
    val estado: StateFlow<DetalleConductorUiState> = _estado.asStateFlow()

    private var correo: String? = null
    private var consulta: Job? = null

    fun cargar(correo: String, forzar: Boolean = false) {
        if (correo == this.correo && !forzar) return
        this.correo = correo
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            var cuenta: Cuenta? = null
            var conductor: Conductor? = null
            var habilitaciones: List<Habilitacion> = emptyList()
            val error = capturar(ErrorApp.Inesperado) {
                cuenta = cuentas.obtenerPerfil().valor()
                conductor = flota.obtenerConductor(correo).valor()
                habilitaciones = flota.listarHabilitaciones(correo = correo).valor()
            }
            _estado.update {
                val leido = conductor
                it.copy(
                    cuenta = cuenta ?: it.cuenta,
                    conductor = leido ?: it.conductor,
                    habilitaciones = if (leido != null) habilitaciones else it.habilitaciones,
                    nombre = if (leido != null && !it.modificado) leido.nombre else it.nombre,
                    telefono = if (leido != null && !it.modificado) leido.telefono.orEmpty() else it.telefono,
                    cargando = false, error = error,
                )
            }
        }
    }

    fun reintentar() {
        correo?.let { cargar(it, forzar = true) }
    }

    private fun editar(cambio: DetalleConductorUiState.() -> DetalleConductorUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null, aviso = null) }

    fun escribirNombre(texto: String) = editar { copy(nombre = texto) }
    fun escribirTelefono(texto: String) = editar { copy(telefono = texto) }
    fun escribirPassword(texto: String) = editar { copy(password = texto) }

    fun guardar() {
        val correo = correo ?: return
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val cuenta = NuevaCuenta(datos.nombre, correo, datos.telefono.ifBlank { null }, datos.password)
                val errores = Validaciones.conductor(cuenta, passwordRequerida = false)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                val guardado = flota.actualizarConductor(correo, cuenta.nombre, cuenta.telefono, cuenta.password.ifEmpty { null }).valor()
                _estado.update {
                    it.copy(
                        conductor = guardado, password = "", nombre = guardado.nombre,
                        telefono = guardado.telefono.orEmpty(), aviso = R.string.aviso_guardado, modificado = false,
                    )
                }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }

    fun cambiarActivo() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                if (datos.cuenta?.rol != Rol.ADMINISTRADOR) throw FalloPantalla(ErrorApp.SinPermiso())
                val conductor = datos.conductor ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                val guardado = flota.cambiarActivoConductor(conductor.correo, !conductor.activo).valor()
                _estado.update {
                    it.copy(
                        conductor = guardado, aviso = R.string.flota_activo_guardado,
                        habilitaciones = it.habilitaciones.map { h -> h.copy(activoConductor = guardado.activo) },
                    )
                }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }
}
