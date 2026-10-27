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
    /** Motivo del error de validación bajo el campo del diálogo (`motivo`). */
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    /** Verdadero cuando el rechazo quedó confirmado: el viaje ya no es suyo y la pantalla vuelve a «Mis viajes». */
    val rechazado: Boolean = false,
    /** Aplicaciones de la hoja «Abrir con»; `null` con la hoja cerrada. */
    val opcionesDeNavegacion: List<AppNavegacion>? = null,
    /** Verdadero cuando no hay a qué aplicación pasar la ruta: «Instala Google Maps o Waze para navegar.» */
    val sinAppDeNavegacion: Boolean = false,
) {
    private val estado: EstadoViaje? get() = viaje?.estado

    /** Los botones de estado solo existen mientras el viaje está `ASIGNADO` o `EN_CURSO`. */
    val conControles: Boolean get() = estado == EstadoViaje.ASIGNADO || estado == EstadoViaje.EN_CURSO
    val puedeIniciar: Boolean get() = inicio == Inicio.HABILITADO && !enviando
    val puedeFinalizar: Boolean get() = estado == EstadoViaje.EN_CURSO && !enviando
    val puedeRechazar: Boolean get() = estado == EstadoViaje.ASIGNADO && !enviando
    /** Siempre que se pueda rechazar: el motivo se valida al confirmar y su error sale bajo el campo. */
    val puedeConfirmarRechazo: Boolean get() = puedeRechazar

    /** «Disponible desde las {hora}»: solo antes de la ventana. */
    val disponibleDesde: LocalDateTime? get() = viaje?.takeIf { inicio == Inicio.ANTES_DE_LA_VENTANA }?.disponibleDesde()
}

/**
 * Detalle del viaje del Conductor (HU08): datos, arqueo de abordaje y los controles de estado con su
 * regla de visibilidad y habilitación (README, «Detalle del Conductor»). El viaje es un `Flow` de Room,
 * así que el arqueo y el estado se actualizan sin recargar.
 */
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

    /** Idempotente: la pantalla lo llama en cada composición con el mismo folio. */
    fun cargar(folio: String) {
        if (folio == this.folio) return
        this.folio = folio
        _estado.value = DetalleViajeUiState()
        iniciar()
    }

    /** «Actualizar» y «Reintentar»: vuelve a leer el viaje y a pedirlo. */
    fun actualizar() {
        _estado.update { it.copy(error = null) }
        iniciar()
    }

    /** La ventana de inicio y el retraso dependen del reloj: se recalculan al volver a primer plano. */
    fun alVolverAPrimerPlano() {
        _estado.update { actual -> actual.viaje?.let { actual.derivado(it) } ?: actual }
    }

    /**
     * «Navegar al origen/destino» (HU10): sin aplicación de navegación, aviso; con una, la abre directo; con varias,
     * pregunta con cuál en la hoja «Abrir con».
     */
    fun navegar() {
        val punto = _estado.value.viaje?.puntoDeNavegacion() ?: return
        val apps = navegador.instaladas()
        when (apps.size) {
            0 -> _estado.update { it.copy(opcionesDeNavegacion = null, sinAppDeNavegacion = true) }
            1 -> abrir(apps.single(), punto)
            else -> _estado.update { it.copy(opcionesDeNavegacion = apps, sinAppDeNavegacion = false) }
        }
    }

    /** Variante «Abrir navegación»: la hoja con las instaladas, aunque sea una, sin abrir nada por su cuenta. */
    fun mostrarOpciones() {
        if (_estado.value.viaje?.puntoDeNavegacion() == null) return
        val apps = navegador.instaladas()
        _estado.update {
            if (apps.isEmpty()) it.copy(opcionesDeNavegacion = null, sinAppDeNavegacion = true)
            else it.copy(opcionesDeNavegacion = apps, sinAppDeNavegacion = false)
        }
    }

    /** El punto se toma del viaje de ahora, no del de cuando se abrió la hoja: pudo pasar de `ASIGNADO` a `EN_CURSO`. */
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

    // Cada intento reemplaza el aviso del anterior: si la aplicación no abrió, se avisa; si abrió, se quita.
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
        // `enviando` se marca antes de lanzar nada: un segundo toque no encuentra el botón libre.
        _estado.update { it.copy(enviando = true, confirmandoFin = false, error = null) }
        viewModelScope.launch { terminarEnvio(viajes.cambiarEstado(folio, EstadoViaje.FINALIZADO)) }
    }

    fun pedirRechazo() {
        if (_estado.value.puedeRechazar) _estado.update { it.copy(rechazando = true) }
    }

    fun escribirMotivo(texto: String) {
        _estado.update { it.copy(motivo = texto, errores = it.errores - CAMPO_MOTIVO) }
    }

    /** Cierra el diálogo y conserva lo escrito: al reabrirlo, el motivo sigue ahí. */
    fun descartarRechazo() {
        _estado.update { it.copy(rechazando = false, errores = emptyMap()) }
    }

    fun confirmarRechazo() {
        val folio = folio ?: return
        val actual = _estado.value
        if (!actual.puedeConfirmarRechazo) return
        // El mismo motivo de 5 a 200 caracteres que valida el backend (400): el error queda bajo el campo, sin enviar.
        Validaciones.motivo(actual.motivo)?.let { error ->
            _estado.update { it.copy(errores = mapOf(CAMPO_MOTIVO to error)) }
            return
        }
        // `enviando` se marca antes de lanzar nada: un segundo toque no encuentra el botón libre.
        _estado.update { it.copy(enviando = true, rechazando = false, errores = emptyMap(), error = null) }
        viewModelScope.launch {
            val resultado = viajes.rechazar(folio, actual.motivo.trim())
            if (resultado is Resultado.Exito) _estado.update { it.copy(motivo = "", rechazado = true) }
            terminarRechazo(resultado)
        }
    }

    // Un 400 con `motivo` vuelve al diálogo con ese error; lo demás se muestra como cualquier otro envío.
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

    // Un cambio se muestra confirmado solo con la respuesta del servidor (README, Comunes).
    private fun terminarEnvio(resultado: Resultado<Unit>) {
        when (resultado) {
            is Resultado.Exito -> _estado.update { it.copy(enviando = false) }
            is Resultado.Fallo -> {
                _estado.update { it.copy(enviando = false, error = resultado.error) }
                // Un 409 casi siempre es que algo cambió: se vuelve a pedir el viaje.
                if (resultado.error is ErrorApp.Conflicto) refrescar()
            }
            // Sin conexión, Iniciar y Finalizar quedan en cola (HU11 los mostrará «Pendiente de sincronizar»).
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
                        // Un viaje ajeno, reasignado o que no existe se ve igual (README, 403 y 404).
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
            // Un fallo del refresco se avisa, pero no borra lo ya cargado.
            if (resultado is Resultado.Fallo) _estado.update { it.copy(cargando = false, error = resultado.error) }
        }
    }

    private fun DetalleViajeUiState.derivado(viaje: Viaje): DetalleViajeUiState {
        // Sin ruta que trazar (finalizado, cancelado) la hoja y su aviso no tienen a qué referirse.
        val navegable = viaje.puntoDeNavegacion() != null
        return copy(
            viaje = viaje,
            retraso = viaje.retraso(clock),
            arqueo = viaje.arqueo(),
            inicio = viaje.ventanaDeInicio(clock),
            opcionesDeNavegacion = opcionesDeNavegacion.takeIf { navegable },
            sinAppDeNavegacion = sinAppDeNavegacion && navegable,
            // Si el viaje volvió a aparecer, el «no encontrado» anterior ya no vale.
            error = error.takeUnless { it == ErrorApp.NoEncontrado },
        )
    }
}
