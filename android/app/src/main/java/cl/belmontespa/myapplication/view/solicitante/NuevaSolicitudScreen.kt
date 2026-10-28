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
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.CamposFechaHora
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Opcion
import cl.belmontespa.myapplication.view.components.SelectorOpciones
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.deCampo
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_DESTINO
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_DOCUMENTO
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_ORIGEN
import cl.belmontespa.myapplication.viewmodel.solicitante.CAMPO_PASAJERO_NOMBRE
import cl.belmontespa.myapplication.viewmodel.solicitante.NuevaSolicitudViewModel

@Composable
fun NuevaSolicitudScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: NuevaSolicitudViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
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
        SelectorOpciones(estado.origen, R.string.campo_origen, lugares, viewModel::elegirOrigen, !enviando, e.deCampo(CAMPO_ORIGEN))
        SelectorOpciones(estado.destino, R.string.campo_destino, lugares, viewModel::elegirDestino, !enviando, e.deCampo(CAMPO_DESTINO))
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
