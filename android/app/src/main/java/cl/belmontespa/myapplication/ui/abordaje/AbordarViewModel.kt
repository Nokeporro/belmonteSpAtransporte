package cl.belmontespa.myapplication.ui.abordaje

import androidx.lifecycle.ViewModel
import cl.belmontespa.myapplication.model.CodigoAbordaje
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lo que la pantalla «Abordar viaje» le avisa al Pasajero (README, Textos). */
enum class AvisoAbordar {
    /** El QR no es de un vehículo Belmonte. */
    QR_AJENO,

    /** Se negó la cámara y todavía se puede volver a pedir. */
    CAMARA_NEGADA,

    /** Se negó la cámara y Android ya no la vuelve a pedir: solo queda «Abrir ajustes». */
    CAMARA_NEGADA_PARA_SIEMPRE,
}

data class AbordarUiState(
    val aviso: AvisoAbordar? = null,
    /** Diálogo «Ingresar código» abierto. */
    val ingresando: Boolean = false,
    val patente: String = "",
    val codigo: String = "",
    /** La patente o el código escritos no tienen la forma de los de un vehículo. */
    val ingresoInvalido: Boolean = false,
    /** Código listo para confirmar: la pantalla navega a «Confirmar abordaje» y lo consume. */
    val listo: CodigoAbordaje? = null,
) {
    val puedeContinuar: Boolean get() = patente.isNotBlank() && codigo.isNotBlank()
    val camaraNegada: Boolean get() = aviso == AvisoAbordar.CAMARA_NEGADA || aviso == AvisoAbordar.CAMARA_NEGADA_PARA_SIEMPRE
}

/**
 * «Abordar viaje» (HU29): el Pasajero llega con el QR de la puerta, o escribiendo la patente y el código, y sigue a
 * «Confirmar abordaje». No usa la sesión ni el repositorio: solo produce un [CodigoAbordaje].
 */
@HiltViewModel
class AbordarViewModel @Inject constructor() : ViewModel() {

    private val _estado = MutableStateFlow(AbordarUiState())
    val estado: StateFlow<AbordarUiState> = _estado.asStateFlow()

    /** Cada intento de escanear parte sin el aviso del anterior. */
    fun alEscanear() {
        _estado.update { it.copy(aviso = null) }
    }

    /** `null` es el lector cancelado con atrás: vuelve sin aviso. */
    fun alLeer(contenido: String?) {
        if (contenido == null) return
        val codigo = CodigoAbordaje.desdeQr(contenido)
        _estado.update { if (codigo != null) it.copy(aviso = null, listo = codigo) else it.copy(aviso = AvisoAbordar.QR_AJENO) }
    }

    fun camaraDenegada(paraSiempre: Boolean) {
        _estado.update { it.copy(aviso = if (paraSiempre) AvisoAbordar.CAMARA_NEGADA_PARA_SIEMPRE else AvisoAbordar.CAMARA_NEGADA) }
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

    fun escribirCodigo(texto: String) {
        _estado.update { it.copy(codigo = texto, ingresoInvalido = false) }
    }

    /** Conserva lo escrito: si «Confirmar abordaje» no encuentra el viaje, al volver se puede corregir. */
    fun continuarIngreso() {
        val actual = _estado.value
        if (!actual.puedeContinuar) return
        val codigo = CodigoAbordaje.manual(actual.patente, actual.codigo)
        _estado.update {
            if (codigo != null) it.copy(ingresando = false, ingresoInvalido = false, aviso = null, listo = codigo)
            else it.copy(ingresoInvalido = true)
        }
    }

    fun consumirListo() {
        _estado.update { it.copy(listo = null) }
    }
}
