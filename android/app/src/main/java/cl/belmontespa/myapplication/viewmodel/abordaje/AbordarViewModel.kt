package cl.belmontespa.myapplication.viewmodel.abordaje

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.belmontespa.myapplication.model.CodigoAbordaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lo que la pantalla «Abordar viaje» le avisa al Pasajero (README, Textos). */
enum class AvisoAbordar {
    /** El QR no es de un vehículo Belmonte. */
    QR_AJENO,

    /** El lector de Google no se pudo abrir: solo queda «Ingresar patente». */
    LECTOR_NO_DISPONIBLE,
}

data class AbordarUiState(
    val aviso: AvisoAbordar? = null,
    /** Diálogo «Ingresar patente» abierto. */
    val ingresando: Boolean = false,
    val patente: String = "",
    /** La patente escrita no tiene la forma de la de un vehículo. */
    val ingresoInvalido: Boolean = false,
    /** Vehículo listo para confirmar: la pantalla navega a «Confirmar abordaje» y lo consume. */
    val listo: CodigoAbordaje? = null,
) {
    val puedeContinuar: Boolean get() = patente.isNotBlank()
}

/**
 * «Abordar viaje» (HU29): el Pasajero llega con el QR de la puerta, o escribiendo la patente, y sigue a
 * «Confirmar abordaje». No usa la sesión ni el repositorio: solo produce un [CodigoAbordaje].
 */
class AbordarViewModel @JvmOverloads constructor(
    application: Application,
) : AndroidViewModel(application) {

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

    /** Conserva lo escrito: si «Confirmar abordaje» no encuentra el viaje, al volver se puede corregir. */
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
