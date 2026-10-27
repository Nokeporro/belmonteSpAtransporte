package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.cuenta
import cl.belmontespa.myapplication.util.Validaciones
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarAdminUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    /** Contraseña inicial temporal: el nuevo Administrador debe cambiarla al entrar. */
    val password: String = "",
    val cuenta: Cuenta? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
)

/** «Registrar administrador» (HU28, CU-18): solo un Administrador crea a otro, con una contraseña inicial temporal. */
class RegistrarAdminViewModel @JvmOverloads constructor(
    application: Application,
    private val cuentas: CuentaRepository = CuentaRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RegistrarAdminUiState())
    val estado: StateFlow<RegistrarAdminUiState> = _estado.asStateFlow()

    private var cargada = false

    /** Idempotente: la pantalla lo llama en cada composición. */
    fun cargar() {
        if (cargada) return
        cargada = true
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var cuenta: Cuenta? = null
            val error = capturar(ErrorApp.Inesperado) { cuenta = cuentas.obtenerPerfil().valor() }
            _estado.update { it.copy(cuenta = cuenta ?: it.cuenta, cargando = false, error = error) }
        }
    }

    fun reintentar() {
        cargada = false
        cargar()
    }

    private fun editar(cambio: RegistrarAdminUiState.() -> RegistrarAdminUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null, aviso = null) }

    fun escribirNombre(texto: String) = editar { copy(nombre = texto) }
    fun escribirCorreo(texto: String) = editar { copy(correo = texto) }
    fun escribirTelefono(texto: String) = editar { copy(telefono = texto) }
    fun escribirPassword(texto: String) = editar { copy(password = texto) }

    fun guardar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val nueva = NuevaCuenta(datos.nombre, datos.correo, datos.telefono.ifBlank { null }, datos.password)
                if (datos.cuenta?.rol != Rol.ADMINISTRADOR) throw FalloPantalla(ErrorApp.SinPermiso())
                val errores = Validaciones.administrador(nueva)
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                val administrador = cuentas.registrarAdministrador(nueva).valor()
                // La contraseña no sobrevive al envío exitoso; los demás campos quedan con lo guardado.
                _estado.update {
                    it.copy(
                        password = "", nombre = administrador.nombre, correo = administrador.correo,
                        telefono = administrador.telefono.orEmpty(), modificado = false, aviso = R.string.admin_alta_guardada,
                    )
                }
            }
            _estado.update { it.copy(enviando = false, error = error) }
        }
    }
}
