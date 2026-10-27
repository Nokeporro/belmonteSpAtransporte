package cl.belmontespa.myapplication.ui.administrador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.ui.DATOS_INVALIDOS
import cl.belmontespa.myapplication.ui.FalloPantalla
import cl.belmontespa.myapplication.ui.capturar
import cl.belmontespa.myapplication.ui.valor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CuentasPendientesUiState(
    val pendientes: List<Solicitante> = emptyList(),
    val cuenta: Cuenta? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
    val aviso: Int? = null,
)

/** «Cuentas por aprobar» (HU27, CU-17): el Administrador aprueba o rechaza a los Solicitantes de empresas existentes. */
@HiltViewModel
class CuentasPendientesViewModel @Inject constructor(
    private val cuentas: CuentaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(CuentasPendientesUiState())
    val estado: StateFlow<CuentasPendientesUiState> = _estado.asStateFlow()

    /** Se llama al abrir y al volver a la pantalla, para leer lo que otro Administrador haya revisado. */
    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            var cuenta: Cuenta? = null
            var pendientes: List<Solicitante> = emptyList()
            val error = capturar(ErrorApp.Inesperado) {
                cuenta = cuentas.obtenerPerfil().valor()
                pendientes = cuentas.listarSolicitantesPendientes().valor()
            }
            _estado.update {
                it.copy(
                    cuenta = cuenta ?: it.cuenta,
                    pendientes = if (error == null) pendientes else it.pendientes,
                    cargando = false, error = error,
                )
            }
        }
    }

    fun aprobar(correo: String) = revisar(correo, aprobar = true)
    fun rechazar(correo: String) = revisar(correo, aprobar = false)

    private fun revisar(correo: String, aprobar: Boolean) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null, aviso = null) }
        viewModelScope.launch {
            val datos = _estado.value
            val error = capturar(DATOS_INVALIDOS) {
                if (datos.cuenta?.rol != Rol.ADMINISTRADOR) throw FalloPantalla(ErrorApp.SinPermiso())
                val pendiente = datos.pendientes.find { it.correo == correo && it.estado == EstadoSolicitante.PENDIENTE }
                    ?: throw FalloPantalla(ErrorApp.Conflicto("La cuenta ya no está pendiente. Actualiza la lista."))
                if (aprobar) cuentas.aprobarSolicitante(pendiente.correo).valor()
                else cuentas.rechazarSolicitante(pendiente.correo).valor()
                _estado.update {
                    it.copy(
                        pendientes = it.pendientes.filterNot { c -> c.correo == pendiente.correo },
                        aviso = if (aprobar) R.string.cuenta_revision_aprobada else R.string.cuenta_revision_rechazada,
                    )
                }
            }
            // Ante un conflicto otro Administrador ya la revisó: se lee la lista vigente, sin afirmar que se guardó.
            val vigentes = if (error is ErrorApp.Conflicto) {
                (cuentas.listarSolicitantesPendientes() as? Resultado.Exito)?.valor
            } else null
            _estado.update { it.copy(enviando = false, error = error, pendientes = vigentes ?: it.pendientes) }
        }
    }
}
