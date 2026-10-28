package cl.belmontespa.myapplication.view.solicitante

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Opcion
import cl.belmontespa.myapplication.view.components.SelectorOpciones
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.solicitante.AgregarPasajeroViewModel

@Composable
fun AgregarPasajeroScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: AgregarPasajeroViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.agregado) { if (estado.agregado) onNavegar(PantallaDemo.DETALLE_SOLICITUD, folio) }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.AGREGAR_PASAJERO.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
    ) {
        val enviando = estado.enviando
        CampoFormulario(estado.nombre, viewModel::escribirNombre, R.string.campo_nombre, "nombre", estado.error, enviando)
        SelectorOpciones(
            estado.tipoDocumento.name, R.string.campo_tipo_documento,
            TipoDocumento.entries.map { Opcion(it.name, stringResource(it.etiqueta)) },
            viewModel::elegirTipoDocumento, !enviando,
        )
        CampoFormulario(estado.documento, viewModel::escribirDocumento, R.string.campo_documento, "documento", estado.error, enviando)
        Accion(R.string.accion_agregar_pasajero, { viewModel.agregar(folio) }, enviando = enviando)
    }
}
