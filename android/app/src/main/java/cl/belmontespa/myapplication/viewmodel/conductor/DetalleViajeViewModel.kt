package cl.belmontespa.myapplication.viewmodel.conductor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Arqueo
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Inicio
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.arqueo
import cl.belmontespa.myapplication.model.disponibleDesde
import cl.belmontespa.myapplication.model.puntoDeNavegacion
import cl.belmontespa.myapplication.model.retraso
import cl.belmontespa.myapplication.model.ventanaDeInicio
import cl.belmontespa.myapplication.navegador.Navegador
import cl.belmontespa.myapplication.navegador.NavegadorAndroid
import cl.belmontespa.myapplication.repository.ViajeRepository
import cl.belmontespa.myapplication.util.Reloj
import cl.belmontespa.myapplication.util.Validaciones
import cl.belmontespa.myapplication.viewmodel.administrador.CAMPO_MOTIVO
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleViajeUiState(
    val cargando: Boolean = true,
    val viaje: Viaje? = null,
    val retraso: Retraso? = null,
    val arqueo: Arqueo? = null,
    val inicio: Inicio = Inicio.NO_APLICA,
    val confirmandoFin: Boolean = false,
    val rechazando: Boolean = false,
    val motivo: String = "",
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val rechazado: Boolean = false,
    val opcionesDeNavegacion: List<AppNavegacion>? = null,
    val sinAppDeNavegacion: Boolean = false,
) {
    private val estado: EstadoViaje? get() = viaje?.estado

    val conControles: Boolean get() = estado == EstadoViaje.ASIGNADO || estado == EstadoViaje.EN_CURSO
    val puedeIniciar: Boolean get() = inicio == Inicio.HABILITADO && !enviando
    val puedeFinalizar: Boolean get() = estado == EstadoViaje.EN_CURSO && !enviando
    val puedeRechazar: Boolean get() = estado == EstadoViaje.ASIGNADO && !enviando
    val puedeConfirmarRechazo: Boolean get() = puedeRechazar

    val disponibleDesde: LocalDateTime? get() = viaje?.takeIf { inicio == Inicio.ANTES_DE_LA_VENTANA }?.disponibleDesde()
}

class DetalleViajeViewModel @JvmOverloads constructor(
    application: Application,
    private val viajes: ViajeRepository = ViajeRepository(application),
    private val clock: Clock = Reloj.santiago,
    private val navegador: Navegador = NavegadorAndroid(application),
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(DetalleViajeUiState())
    val estado: StateFlow<DetalleViajeUiState> = _estado.asStateFlow()

    private var folio: String? = null
    private var observacion: Job? = null

    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        _estado.value = DetalleViajeUiState()
        iniciar()
    }

    fun actualizar() {
        _estado.update { it.copy(error = null) }
        iniciar()
    }

    fun alVolverAPrimerPlano() {
        _estado.update { actual -> actual.viaje?.let { actual.derivado(it) } ?: actual }
    }

    fun navegar() {
        val punto = _estado.value.viaje?.puntoDeNavegacion() ?: return
        val apps = navegador.instaladas()
        when (apps.size) {
            0 -> _estado.update { it.copy(opcionesDeNavegacion = null, sinAppDeNavegacion = true) }
            1 -> abrir(apps.single(), punto)
            else -> _estado.update { it.copy(opcionesDeNavegacion = apps, sinAppDeNavegacion = false) }
        }
    }

    fun mostrarOpciones() {
        if (_estado.value.viaje?.puntoDeNavegacion() == null) return
        val apps = navegador.instaladas()
        _estado.update {
            if (apps.isEmpty()) it.copy(opcionesDeNavegacion = null, sinAppDeNavegacion = true)
            else it.copy(opcionesDeNavegacion = apps, sinAppDeNavegacion = false)
        }
    }

    fun elegir(app: AppNavegacion) {
        val actual = _estado.value
        if (actual.opcionesDeNavegacion?.contains(app) != true) return
        val punto = actual.viaje?.puntoDeNavegacion()
        _estado.update { it.copy(opcionesDeNavegacion = null) }
        if (punto != null) abrir(app, punto)
    }

    fun cerrarOpciones() {
        _estado.update { it.copy(opcionesDeNavegacion = null) }
    }

    private fun abrir(app: AppNavegacion, punto: Ubicacion) {
        val abierta = navegador.abrir(app, punto)
        _estado.update { it.copy(opcionesDeNavegacion = null, sinAppDeNavegacion = !abierta) }
    }

    fun pedirFinalizar() {
        if (_estado.value.puedeFinalizar) _estado.update { it.copy(confirmandoFin = true) }
    }

    fun descartarFinalizar() {
        _estado.update { it.copy(confirmandoFin = false) }
    }

    fun confirmarFinalizar() {
        val folio = folio ?: return
        if (!_estado.value.puedeFinalizar) return
        _estado.update { it.copy(enviando = true, confirmandoFin = false, error = null) }
        viewModelScope.launch { terminarEnvio(viajes.cambiarEstado(folio, EstadoViaje.FINALIZADO)) }
    }

    fun pedirRechazo() {
        if (_estado.value.puedeRechazar) _estado.update { it.copy(rechazando = true) }
    }

    fun escribirMotivo(texto: String) {
        _estado.update { it.copy(motivo = texto, errores = it.errores - CAMPO_MOTIVO) }
    }

    fun descartarRechazo() {
        _estado.update { it.copy(rechazando = false, errores = emptyMap()) }
    }

    fun confirmarRechazo() {
        val folio = folio ?: return
        val actual = _estado.value
        if (!actual.puedeConfirmarRechazo) return
        Validaciones.motivo(actual.motivo)?.let { error ->
            _estado.update { it.copy(errores = mapOf(CAMPO_MOTIVO to error)) }
            return
        }
        _estado.update { it.copy(enviando = true, rechazando = false, errores = emptyMap(), error = null) }
        viewModelScope.launch {
            val resultado = viajes.rechazar(folio, actual.motivo.trim())
            if (resultado is Resultado.Exito) _estado.update { it.copy(motivo = "", rechazado = true) }
            terminarRechazo(resultado)
        }
    }

    private fun terminarRechazo(resultado: Resultado<Unit>) {
        val error = (resultado as? Resultado.Fallo)?.error
        val deMotivo = (error as? ErrorApp.Validacion)?.errores?.get(CAMPO_MOTIVO)
        if (error is ErrorApp.Validacion && deMotivo != null) {
            val resto = error.errores - CAMPO_MOTIVO
            _estado.update {
                it.copy(
                    enviando = false, rechazando = true, errores = mapOf(CAMPO_MOTIVO to deMotivo),
                    error = ErrorApp.Validacion(resto).takeIf { resto.isNotEmpty() },
                )
            }
        } else {
            terminarEnvio(resultado)
        }
    }

    private fun terminarEnvio(resultado: Resultado<Unit>) {
        when (resultado) {
            is Resultado.Exito -> _estado.update { it.copy(enviando = false) }
            is Resultado.Fallo -> {
                _estado.update { it.copy(enviando = false, error = resultado.error) }
                if (resultado.error is ErrorApp.Conflicto) refrescar()
            }
            Resultado.Encolado -> _estado.update { it.copy(enviando = false) }
        }
    }

    private fun iniciar() {
        val folio = folio ?: return
        observacion?.cancel()
        observacion = viewModelScope.launch {
            viajes.observarViaje(folio)
                .catch { _estado.update { it.copy(cargando = false, error = ErrorApp.Inesperado) } }
                .collect { viaje ->
                    if (viaje == null) {
                        _estado.update {
                            it.copy(cargando = false, viaje = null, retraso = null, arqueo = null,
                                inicio = Inicio.NO_APLICA, error = it.error ?: ErrorApp.NoEncontrado,
                                opcionesDeNavegacion = null, sinAppDeNavegacion = false)
                        }
                    } else {
                        _estado.update { it.copy(cargando = false).derivado(viaje) }
                    }
                }
        }
        refrescar()
    }

    private fun refrescar() {
        val folio = folio ?: return
        viewModelScope.launch {
            val resultado = viajes.refrescarViaje(folio)
            if (resultado is Resultado.Fallo) _estado.update { it.copy(cargando = false, error = resultado.error) }
        }
    }

    private fun DetalleViajeUiState.derivado(viaje: Viaje): DetalleViajeUiState {
        val navegable = viaje.puntoDeNavegacion() != null
        return copy(
            viaje = viaje,
            retraso = viaje.retraso(clock),
            arqueo = viaje.arqueo(),
            inicio = viaje.ventanaDeInicio(clock),
            opcionesDeNavegacion = opcionesDeNavegacion.takeIf { navegable },
            sinAppDeNavegacion = sinAppDeNavegacion && navegable,
            error = error.takeUnless { it == ErrorApp.NoEncontrado },
        )
    }
}
