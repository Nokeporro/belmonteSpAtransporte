package cl.belmontespa.myapplication.view.administrador

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.administrador.CuentasPendientesViewModel

/** «Cuentas por aprobar»: cada decisión pide una confirmación con la identidad y la empresa. */
@Composable
fun CuentasPendientesScreen(
    onVolver: () -> Unit,
    viewModel: CuentasPendientesViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver a la pantalla se lee de nuevo la lista de pendientes.
    LaunchedEffect(Unit) { viewModel.cargar() }
    var revision by remember { mutableStateOf<Pair<Solicitante, Boolean>?>(null) }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.CUENTAS_PENDIENTES.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::cargar,
    ) {
        val sinError = estado.error == null
        if (BuildConfig.MODO_LOCAL) Aviso(stringResource(R.string.cuenta_revision_demo), TonoAviso.INFORMACION)
        if (estado.pendientes.isEmpty()) Text(stringResource(R.string.vacio_cuentas_por_aprobar))
        estado.pendientes.forEach { cuenta ->
            Bloque(cuenta.nombre) {
                Dato(R.string.campo_correo, cuenta.correo)
                Dato(R.string.campo_empresa, cuenta.razonSocial)
                Dato(R.string.campo_rut_empresa, Formatos.rut(cuenta.rutEmpresa))
                Accion(R.string.accion_aprobar, { revision = cuenta to true }, habilitada = sinError, enviando = estado.enviando)
                Accion(R.string.accion_rechazar, { revision = cuenta to false }, TipoBoton.PELIGRO, sinError, estado.enviando)
            }
        }
        revision?.let { (cuenta, aprobar) ->
            AlertDialog(
                onDismissRequest = { revision = null },
                title = { Text(stringResource(if (aprobar) R.string.cuenta_aprobar_titulo else R.string.cuenta_rechazar_titulo)) },
                text = {
                    Text(
                        stringResource(
                            if (aprobar) R.string.cuenta_aprobar_confirmacion else R.string.cuenta_rechazar_confirmacion,
                            cuenta.nombre, cuenta.correo, cuenta.razonSocial,
                        ),
                        Modifier.verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            revision = null
                            if (aprobar) viewModel.aprobar(cuenta.correo) else viewModel.rechazar(cuenta.correo)
                        },
                        enabled = !estado.enviando && sinError,
                    ) { Text(stringResource(R.string.accion_confirmar)) }
                },
                dismissButton = { TextButton(onClick = { revision = null }) { Text(stringResource(R.string.accion_cancelar)) } },
            )
        }
    }
}
