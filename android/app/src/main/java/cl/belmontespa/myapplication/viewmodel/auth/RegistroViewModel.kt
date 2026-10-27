package cl.belmontespa.myapplication.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val password: String = "",
    val razonSocial: String = "",
    val rutEmpresa: String = "",
    val enviando: Boolean = false,
    /** Hay datos escritos que se perderían al salir. */
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    /** Cuenta aprobada o por aprobar, según la empresa ya existiera. */
    val aviso: Int? = null,
) {
    val registrado: Boolean
        get() = aviso == R.string.aviso_registro_aprobado || aviso == R.string.aviso_registro_pendiente
}

/** «Crear cuenta solicitante» (RF01): el autorregistro, sin sesión. Las reglas las aplica el repositorio. */
class RegistroViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: AuthRepository = AuthRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    private fun editar(cambio: RegistroUiState.() -> RegistroUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null, aviso = null) }

    fun escribirNombre(texto: String) = editar { copy(nombre = texto) }
    fun escribirCorreo(texto: String) = editar { copy(correo = texto) }
    fun escribirTelefono(texto: String) = editar { copy(telefono = texto) }
    fun escribirPassword(texto: String) = editar { copy(password = texto) }
    fun escribirRazonSocial(texto: String) = editar { copy(razonSocial = texto) }
    fun escribirRutEmpresa(texto: String) = editar { copy(rutEmpresa = texto) }

    fun registrar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val registro = RegistroSolicitante(
                    datos.nombre, datos.correo, datos.telefono.ifBlank { null }, datos.password,
                    datos.razonSocial, datos.rutEmpresa,
                )
                val solicitante = auth.registrarSolicitante(registro).valor()
                _estado.update {
                    it.copy(
                        aviso = if (solicitante.estado == EstadoSolicitante.PENDIENTE) R.string.aviso_registro_pendiente
                        else R.string.aviso_registro_aprobado,
                    )
                }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }
}
