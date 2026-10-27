package cl.belmontespa.myapplication.view.solicitante

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.Opcion
import cl.belmontespa.myapplication.view.components.SelectorOpciones
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.viewmodel.solicitante.MisSolicitudesViewModel

private const val TODOS = "TODOS"

/** «Mis solicitudes»: raíz de la barra inferior del Solicitante, con «Nueva solicitud» y el filtro por estado. */
@Composable
fun MisSolicitudesScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    viewModel: MisSolicitudesViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.MIS_SOLICITUDES.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = null,
        error = estado.error,
        onReintentar = viewModel::actualizar,
    ) {
        Accion(R.string.accion_nueva_solicitud, { onNavegar(PantallaDemo.NUEVA_SOLICITUD, "") }, TipoBoton.SECUNDARIO)
        SelectorOpciones(
            valor = estado.estado?.name ?: TODOS,
            etiqueta = R.string.campo_estado,
            opciones = listOf(Opcion(TODOS, stringResource(R.string.filtro_todos))) +
                EstadoViaje.entries.map { Opcion(it.name, stringResource(it.etiqueta)) },
            onElegir = { viewModel.filtrar(if (it == TODOS) null else EstadoViaje.valueOf(it)) },
        )
        Accion(R.string.accion_actualizar, viewModel::actualizar, TipoBoton.SECUNDARIO)
        if (estado.viajes.isEmpty()) Text(stringResource(R.string.vacio_mis_solicitudes))
        estado.viajes.forEach { viaje ->
            Bloque(viaje.folio) {
                EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip)
                Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraLista(viaje.fhAgenda))
                Text(viaje.origen.direccion + " → " + viaje.destino.direccion)
                Text(pluralStringResource(R.plurals.solicitud_cantidad_pasajeros, viaje.cantidadPasajeros, viaje.cantidadPasajeros))
                if (viaje.pasajeros.isNotEmpty()) Text(viaje.pasajeros.joinToString { it.nombre })
                val detalle = if (viaje.cantidadPasajeros == 0) PantallaDemo.SOLICITUD_SIN_PASAJEROS else PantallaDemo.DETALLE_SOLICITUD
                Accion(R.string.accion_ver_detalle, { onNavegar(detalle, viaje.folio) }, TipoBoton.SECUNDARIO)
            }
        }
    }
}
