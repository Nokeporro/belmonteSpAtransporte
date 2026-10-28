package cl.belmontespa.myapplication.viewmodel.abordaje

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.belmontespa.myapplication.model.CodigoAbordaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AvisoAbordar {
    QR_AJENO,

    LECTOR_NO_DISPONIBLE,
}

data class AbordarUiState(
    val aviso: AvisoAbordar? = null,
    val ingresando: Boolean = false,
    val patente: String = "",
    val ingresoInvalido: Boolean = false,
    val listo: CodigoAbordaje? = null,
) {
    val puedeContinuar: Boolean get() = patente.isNotBlank()
}

class AbordarViewModel @JvmOverloads constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(AbordarUiState())
    val estado: StateFlow<AbordarUiState> = _estado.asStateFlow()

    fun alEscanear() {
        _estado.update { it.copy(aviso = null) }
    }

    fun alLeer(contenido: String?) {
        if (contenido == null) return
        val codigo = CodigoAbordaje.desdeQr(contenido)
        _estado.update { if (codigo != null) it.copy(aviso = null, listo = codigo) else it.copy(aviso = AvisoAbordar.QR_AJENO) }
    }

    fun lectorNoDisponible() {
        _estado.update { it.copy(aviso = AvisoAbordar.LECTOR_NO_DISPONIBLE) }
    }

    fun abrirIngreso() {
        _estado.update { it.copy(ingresando = true, ingresoInvalido = false) }
    }

    fun cerrarIngreso() {
        _estado.update { it.copy(ingresando = false, ingresoInvalido = false) }
    }

    fun escribirPatente(texto: String) {
        _estado.update { it.copy(patente = texto, ingresoInvalido = false) }
    }

    fun continuarIngreso() {
        val actual = _estado.value
        if (!actual.puedeContinuar) return
        val codigo = CodigoAbordaje.manual(actual.patente)
        _estado.update {
            if (codigo != null) it.copy(ingresando = false, ingresoInvalido = false, aviso = null, listo = codigo)
            else it.copy(ingresoInvalido = true)
        }
    }

    fun consumirListo() {
        _estado.update { it.copy(listo = null) }
    }
}
