package cl.belmontespa.myapplication.viewmodel.abordaje

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.repository.AbordajeRepository
import cl.belmontespa.myapplication.util.Validaciones
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfirmarAbordajeUiState(
    val codigo: CodigoAbordaje? = null,
    val tipoDocumento: TipoDocumento = TipoDocumento.RUT,
    val documento: String = "",
    val documentoInvalido: Boolean = false,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val registradoA: LocalDateTime? = null,
) {
    val puedeConfirmar: Boolean get() = codigo != null && documento.isNotBlank() && !enviando && registradoA == null

    override fun toString(): String =
        "ConfirmarAbordajeUiState(codigo=$codigo, tipoDocumento=$tipoDocumento, enviando=$enviando, error=$error, registradoA=$registradoA)"
}

class ConfirmarAbordajeViewModel @JvmOverloads constructor(
    application: Application,
    private val abordaje: AbordajeRepository = AbordajeRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(ConfirmarAbordajeUiState())
    val estado: StateFlow<ConfirmarAbordajeUiState> = _estado.asStateFlow()

    private var argumento: String? = null

    fun cargar(argumento: String) {
        if (argumento == this.argumento) return
        this.argumento = argumento
        _estado.value = ConfirmarAbordajeUiState(codigo = CodigoAbordaje.desdeRuta(argumento))
    }

    fun elegirTipo(tipo: TipoDocumento) {
        _estado.update { it.copy(tipoDocumento = tipo, documentoInvalido = false, error = null) }
    }

    fun escribirDocumento(texto: String) {
        _estado.update { it.copy(documento = texto, documentoInvalido = false, error = null) }
    }

    fun confirmar() {
        val actual = _estado.value
        val codigo = actual.codigo ?: return
        if (!actual.puedeConfirmar) return
        if (!Validaciones.documentoValido(actual.tipoDocumento, actual.documento)) {
            _estado.update { it.copy(documentoInvalido = true) }
            return
        }
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val resultado = abordaje.registrarAbordaje(codigo, actual.tipoDocumento, actual.documento)
            _estado.update {
                when (resultado) {
                    is Resultado.Exito -> it.copy(enviando = false, documento = "", registradoA = resultado.valor)
                    is Resultado.Fallo -> it.copy(enviando = false, error = resultado.error)
                    Resultado.Encolado -> it.copy(enviando = false, error = ErrorApp.Inesperado)
                }
            }
        }
    }
}
