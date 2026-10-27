package cl.belmontespa.myapplication.viewmodel.administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.repository.CatalogoRepository
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.viewmodel.DATOS_INVALIDOS
import cl.belmontespa.myapplication.viewmodel.FalloPantalla
import cl.belmontespa.myapplication.viewmodel.capturar
import cl.belmontespa.myapplication.viewmodel.valor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarPagoUiState(
    val viaje: Viaje? = null,
    val catalogos: Catalogos? = null,
    val monto: String = "",
    /** Código SBIF del banco elegido, como texto. */
    val banco: String = "",
    val medioPago: String = "TRANSFERENCIA",
    val tipoRecibo: String = "BOLETA",
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val modificado: Boolean = false,
    val error: ErrorApp? = null,
    /** Hash del pago guardado: la pantalla navega a su detalle y lo consume. */
    val registrado: String? = null,
) {
    /** El viaje admite un pago solo si no está cancelado ni tiene ya uno. */
    val viajeAdmitePago: Boolean get() = viaje?.let { it.estado != EstadoViaje.CANCELADO && it.hashPago == null } == true
}

/** «Registrar pago» (HU19, P1): monto, banco, medio de pago y tipo de recibo de un viaje, con datos de los catálogos. */
class RegistrarPagoViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val catalogos: CatalogoRepository = CatalogoRepository(),
    private val pagos: PagoRepository = PagoRepository(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(RegistrarPagoUiState())
    val estado: StateFlow<RegistrarPagoUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var consulta: Job? = null

    /** Idempotente con el mismo folio; [forzar] vuelve a leer los catálogos y el viaje sin perder lo escrito. */
    fun cargar(folio: String, forzar: Boolean = false) {
        if (folio == this.folio && !forzar) return
        this.folio = folio
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val error = capturar(ErrorApp.Inesperado) {
                val datos = catalogos.obtenerCatalogos().valor()
                _estado.update { it.copy(catalogos = datos) }
                _estado.update { it.copy(cargando = false) }
                viajes.observarViaje(folio).collectLatest { viaje ->
                    _estado.update {
                        it.copy(viaje = viaje, cargando = false, error = if (viaje == null) ErrorApp.NoEncontrado else null)
                    }
                }
            }
            _estado.update { it.copy(cargando = false, error = error ?: it.error) }
        }
    }

    fun reintentar() {
        folio?.let { cargar(it, forzar = true) }
    }

    private fun editar(cambio: RegistrarPagoUiState.() -> RegistrarPagoUiState) =
        _estado.update { it.cambio().copy(modificado = true, error = null) }

    fun escribirMonto(texto: String) = editar { copy(monto = texto) }
    fun elegirBanco(codigo: String) = editar { copy(banco = codigo) }
    fun elegirMedioPago(medio: String) = editar { copy(medioPago = medio) }
    fun elegirTipoRecibo(tipo: String) = editar { copy(tipoRecibo = tipo) }

    fun guardar() {
        val folio = folio ?: return
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                val viaje = datos.viaje ?: throw FalloPantalla(ErrorApp.NoEncontrado)
                if (viaje.estado == EstadoViaje.CANCELADO || viaje.hashPago != null) {
                    throw FalloPantalla(ErrorApp.Conflicto("El viaje ya tiene un pago o está cancelado."))
                }
                val catalogos = datos.catalogos
                    ?: throw FalloPantalla(ErrorApp.Conflicto("Carga los catálogos antes de registrar el pago."))
                val monto = datos.monto.trim().toIntOrNull()
                val banco = datos.banco.toIntOrNull()
                val medio = catalogos.mediosPago.find { it.name == datos.medioPago }
                val recibo = catalogos.tiposRecibo.find { it.name == datos.tipoRecibo }
                val errores = buildMap {
                    if (monto == null || monto <= 0) put("monto", "Ingresa un monto entero entre 1 y 2147483647.")
                    if (banco == null || catalogos.bancos.none { it.codSbif == banco }) put("banco", "Selecciona un banco del catálogo.")
                    if (medio == null) put("medioPago", "Selecciona un medio de pago del catálogo.")
                    if (recibo == null) put("tipoRecibo", "Selecciona un tipo de recibo del catálogo.")
                }
                if (errores.isNotEmpty()) throw FalloPantalla(ErrorApp.Validacion(errores))
                val pago = pagos.registrarPago(NuevoPago(folio, monto!!, banco!!, medio!!, recibo!!)).valor()
                _estado.update { it.copy(registrado = pago.hash) }
            }
            _estado.update { it.copy(enviando = false, modificado = error != null && it.modificado, error = error) }
        }
    }

    fun consumirRegistrado() = _estado.update { it.copy(registrado = null) }
}
