package cl.belmontespa.myapplication.view.solicitante

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.CampoLugar
import cl.belmontespa.myapplication.view.components.CamposFechaHora
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.CorreccionDeComuna
import cl.belmontespa.myapplication.view.components.Opcion
import cl.belmontespa.myapplication.view.components.SelectorOpciones
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.deCampo
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_DESTINO
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_DESTINO_COMUNA
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_DOCUMENTO
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_ORIGEN
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_ORIGEN_COMUNA
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_PASAJERO_NOMBRE
import cl.belmontespa.myapplication.viewmodel.solicitante.NuevaSolicitudViewModel

@Composable
fun NuevaSolicitudScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: NuevaSolicitudViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var buscaOrigen by rememberSaveable { mutableStateOf(true) }
    val buscador = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.resultadoDeBusqueda(buscaOrigen, it.resultCode, it.data)
    }
    LaunchedEffect(estado.creada) {
        estado.creada?.let { folio -> viewModel.consumirCreada(); onNavegar(PantallaDemo.DETALLE_SOLICITUD, folio) }
    }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.NUEVA_SOLICITUD.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        val lugares = estado.lugares.map { Opcion(it.direccion, "${it.direccion} · ${it.comuna}") }
        CamposFechaHora(estado.fecha, estado.hora, viewModel::elegirFecha, viewModel::elegirHora, enviando)
        LugarDeSolicitud(
            estado.origen, R.string.campo_origen, lugares, estado.conBusqueda, !enviando, e.deCampo(CAMPO_ORIGEN),
            estado.origenSinComuna, estado.regiones, e.deCampo(CAMPO_ORIGEN_COMUNA),
            viewModel::elegirOrigen,
            { buscaOrigen = true; buscador.launch(viewModel.intentDeBusqueda(contexto)) },
            { region, comuna -> viewModel.corregirComuna(true, region, comuna) },
            viewModel::cargarRegiones,
        )
        LugarDeSolicitud(
            estado.destino, R.string.campo_destino, lugares, estado.conBusqueda, !enviando, e.deCampo(CAMPO_DESTINO),
            estado.destinoSinComuna, estado.regiones, e.deCampo(CAMPO_DESTINO_COMUNA),
            viewModel::elegirDestino,
            { buscaOrigen = false; buscador.launch(viewModel.intentDeBusqueda(contexto)) },
            { region, comuna -> viewModel.corregirComuna(false, region, comuna) },
            viewModel::cargarRegiones,
        )
        CampoFormulario(estado.vuelo, viewModel::escribirVuelo, R.string.campo_vuelo, "numeroVuelo", e, enviando)
        CampoFormulario(estado.pasajeroNombre, viewModel::escribirPasajeroNombre, R.string.campo_nombre, CAMPO_PASAJERO_NOMBRE, e, enviando)
        SelectorOpciones(
            estado.tipoDocumento.name, R.string.campo_tipo_documento,
            TipoDocumento.entries.map { Opcion(it.name, stringResource(it.etiqueta)) },
            viewModel::elegirTipoDocumento, !enviando,
        )
        CampoFormulario(estado.documento, viewModel::escribirDocumento, R.string.campo_documento, CAMPO_DOCUMENTO, e, enviando)
        Accion(R.string.accion_agregar_pasajero, viewModel::agregarPasajero, TipoBoton.SECUNDARIO, enviando = enviando)
        estado.pasajeros.forEachIndexed { indice, pasajero ->
            Bloque(pasajero.nombre) {
                Accion(R.string.accion_editar, { viewModel.editarPasajero(indice) }, TipoBoton.SECUNDARIO, enviando = enviando)
                Accion(R.string.accion_quitar, { viewModel.quitarPasajero(indice) }, TipoBoton.SECUNDARIO, enviando = enviando)
            }
        }
        Accion(R.string.accion_guardar, viewModel::guardar, habilitada = estado.puedeGuardar, enviando = enviando)
    }
}

@Composable
private fun LugarDeSolicitud(
    valor: String,
    @StringRes etiqueta: Int,
    lugares: List<Opcion>,
    conBusqueda: Boolean,
    habilitado: Boolean,
    error: String?,
    sinComuna: Boolean,
    regiones: List<Region>,
    errorComuna: String?,
    onElegir: (String) -> Unit,
    onBuscar: () -> Unit,
    onCorregir: (String, String) -> Unit,
    onReintentar: () -> Unit,
) {
    if (conBusqueda) CampoLugar(valor, etiqueta, onBuscar, habilitado, error)
    else SelectorOpciones(valor, etiqueta, lugares, onElegir, habilitado, error)
    if (sinComuna) CorreccionDeComuna(regiones, onCorregir, onReintentar, habilitado, errorComuna)
}
