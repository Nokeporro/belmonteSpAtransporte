package cl.belmontespa.myapplication.ui.abordaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.data.repository.AbordajeRepository
import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.util.Validaciones
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfirmarAbordajeUiState(
    /** El vehículo de la ruta; `null` si el argumento no era de un código válido. */
    val codigo: CodigoAbordaje? = null,
    val tipoDocumento: TipoDocumento = TipoDocumento.RUT,
    val documento: String = "",
    val documentoInvalido: Boolean = false,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    /** La marca guardada: la pantalla navega a «Abordaje registrado» y deja de mostrar el formulario. */
    val registradoA: LocalDateTime? = null,
) {
    val puedeConfirmar: Boolean get() = codigo != null && documento.isNotBlank() && !enviando && registradoA == null

    // El documento vive solo en la memoria del ViewModel (README): tampoco sale en un log por un toString().
    override fun toString(): String =
        "ConfirmarAbordajeUiState(codigo=$codigo, tipoDocumento=$tipoDocumento, enviando=$enviando, error=$error, registradoA=$registradoA)"
}

/**
 * «Confirmar abordaje» (HU29): tipo y número de documento para `POST /abordajes`. Los errores conservan el formulario,
 * así que reintentar es seguro; el éxito borra el documento antes de salir.
 */
@HiltViewModel
class ConfirmarAbordajeViewModel @Inject constructor(
    private val abordaje: AbordajeRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(ConfirmarAbordajeUiState())
    val estado: StateFlow<ConfirmarAbordajeUiState> = _estado.asStateFlow()

    private var argumento: String? = null

    /** Idempotente: la pantalla lo llama en cada composición con el mismo argumento de la ruta. */
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
        // `enviando` se marca antes de lanzar nada: un segundo toque no encuentra el botón libre.
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val resultado = abordaje.registrarAbordaje(codigo, actual.tipoDocumento, actual.documento)
            _estado.update {
                when (resultado) {
                    is Resultado.Exito -> it.copy(enviando = false, documento = "", registradoA = resultado.valor)
                    is Resultado.Fallo -> it.copy(enviando = false, error = resultado.error)
                    // El abordaje no usa la cola: no debería ocurrir.
                    Resultado.Encolado -> it.copy(enviando = false, error = ErrorApp.Inesperado)
                }
            }
        }
    }
}
