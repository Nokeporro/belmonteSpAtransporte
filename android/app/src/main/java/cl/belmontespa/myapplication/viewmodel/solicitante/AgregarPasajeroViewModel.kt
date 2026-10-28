package cl.belmontespa.myapplication.viewmodel.solicitante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgregarPasajeroUiState(
    val nombre: String = "",
    val tipoDocumento: TipoDocumento = TipoDocumento.RUT,
    val documento: String = "",
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    val agregado: Boolean = false,
)

class AgregarPasajeroViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(AgregarPasajeroUiState())
    val estado: StateFlow<AgregarPasajeroUiState> = _estado.asStateFlow()

    private fun editar(cambio: AgregarPasajeroUiState.() -> AgregarPasajeroUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null) }

    fun escribirNombre(texto: String) = editar { copy(nombre = texto) }
    fun elegirTipoDocumento(tipo: String) = editar { copy(tipoDocumento = TipoDocumento.valueOf(tipo)) }
    fun escribirDocumento(texto: String) = editar { copy(documento = texto) }

    fun agregar(folio: String) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                viajes.agregarPasajero(folio, NuevoPasajero(datos.tipoDocumento, datos.documento, datos.nombre)).valor()
                _estado.update { it.copy(agregado = true) }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }
}
