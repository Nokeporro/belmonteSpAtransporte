package cl.belmontespa.myapplication.view.abordaje

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BarraSuperior
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.CampoTexto
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.view.components.texto
import cl.belmontespa.myapplication.viewmodel.abordaje.ConfirmarAbordajeUiState
import cl.belmontespa.myapplication.viewmodel.abordaje.ConfirmarAbordajeViewModel

@Composable
fun ConfirmarAbordajeScreen(
    argumento: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: ConfirmarAbordajeViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(argumento) { viewModel.cargar(argumento) }
    LaunchedEffect(estado.registradoA) {
        estado.registradoA?.let { onNavegar(PantallaDemo.ABORDADO, it.toString()) }
    }
    ConfirmarAbordajeContenido(
        estado = estado,
        onTipo = viewModel::elegirTipo,
        onDocumento = viewModel::escribirDocumento,
        onConfirmar = viewModel::confirmar,
        onVolver = onVolver,
    )
}

@Composable
fun ConfirmarAbordajeContenido(
    estado: ConfirmarAbordajeUiState,
    onTipo: (TipoDocumento) -> Unit,
    onDocumento: (String) -> Unit,
    onConfirmar: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        BarraSuperior(titulo = stringResource(PantallaDemo.CONFIRMAR_ABORDAJE.titulo), onVolver = onVolver)
        Column(
            modifier = Modifier
                .testTag("pantalla_contenido")
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val codigo = estado.codigo
            if (codigo == null) {
                Aviso(stringResource(R.string.abordaje_qr_ajeno), TonoAviso.PELIGRO, Modifier.testTag("pantalla_error"))
                BotonAncho(stringResource(R.string.accion_volver), onVolver, alto56 = true)
                return@Column
            }
            estado.error?.let { error ->
                val texto = if (error == ErrorApp.NoEncontrado) stringResource(R.string.abordaje_no_encontrado) else error.texto()
                Aviso(texto, TonoAviso.PELIGRO, Modifier.testTag("pantalla_error"))
            }
            Column {
                Text(stringResource(R.string.campo_vehiculo), style = EstilosMono.etiqueta)
                Text(codigo.patente, style = EstilosMono.dato)
            }
            Text(stringResource(R.string.campo_tipo_documento), style = EstilosMono.etiqueta)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoDocumento.entries.forEachIndexed { indice, tipo ->
                    SegmentedButton(
                        selected = estado.tipoDocumento == tipo,
                        onClick = { onTipo(tipo) },
                        shape = SegmentedButtonDefaults.itemShape(indice, TipoDocumento.entries.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                        enabled = !estado.enviando,
                    ) { Text(stringResource(tipo.etiqueta), style = MaterialTheme.typography.labelLarge) }
                }
            }
            CampoTexto(
                valor = estado.documento,
                onCambio = onDocumento,
                etiqueta = stringResource(R.string.campo_documento),
                modifier = Modifier.fillMaxWidth(),
                error = if (estado.documentoInvalido) stringResource(R.string.abordaje_documento_invalido) else null,
                habilitado = !estado.enviando,
            )
            BotonAncho(stringResource(R.string.abordaje_confirmar), onConfirmar, habilitado = estado.puedeConfirmar, alto56 = true)
        }
    }
}
