package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.CampoFormulario
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.Dato
import cl.belmontespa.myapplication.ui.components.Opcion
import cl.belmontespa.myapplication.ui.components.SelectorOpciones
import cl.belmontespa.myapplication.ui.components.deCampo
import cl.belmontespa.myapplication.ui.components.etiqueta
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Registrar pago»: al guardar abre el detalle del pago, que nace `PENDIENTE`. */
@Composable
fun RegistrarPagoScreen(
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistrarPagoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LaunchedEffect(estado.registrado) {
        estado.registrado?.let { hash -> viewModel.consumirRegistrado(); onNavegar(PantallaDemo.DETALLE_PAGO, hash) }
    }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.REGISTRAR_PAGO.titulo),
        cargando = estado.cargando,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        onReintentar = viewModel::reintentar,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        val catalogos = estado.catalogos
        Dato(R.string.campo_folio, estado.viaje?.folio)
        CampoFormulario(estado.monto, viewModel::escribirMonto, R.string.campo_monto, "monto", e, enviando, KeyboardType.Number)
        SelectorOpciones(
            estado.banco, R.string.campo_banco,
            catalogos?.bancos.orEmpty().map { Opcion(it.codSbif.toString(), it.nombre) },
            viewModel::elegirBanco, !enviando, e.deCampo("banco"),
        )
        SelectorOpciones(
            estado.medioPago, R.string.campo_medio_pago,
            catalogos?.mediosPago.orEmpty().map { Opcion(it.name, stringResource(it.etiqueta)) },
            viewModel::elegirMedioPago, !enviando, e.deCampo("medioPago"),
        )
        SelectorOpciones(
            estado.tipoRecibo, R.string.campo_tipo_recibo,
            catalogos?.tiposRecibo.orEmpty().map { Opcion(it.name, stringResource(it.etiqueta)) },
            viewModel::elegirTipoRecibo, !enviando, e.deCampo("tipoRecibo"),
        )
        if (estado.viaje != null && !estado.viajeAdmitePago) Text(stringResource(R.string.pago_registro_no_disponible))
        Accion(
            R.string.accion_registrar_pago, viewModel::guardar,
            habilitada = estado.error == null && catalogos != null && estado.viajeAdmitePago,
            enviando = enviando,
        )
    }
}
