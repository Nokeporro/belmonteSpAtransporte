package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Accion
import cl.belmontespa.myapplication.ui.components.ContenedorPantalla
import cl.belmontespa.myapplication.ui.components.Opcion
import cl.belmontespa.myapplication.ui.components.SelectorOpciones
import cl.belmontespa.myapplication.ui.components.deCampo
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Gestionar habilitaciones»: `referencia` es `vehiculo:{patente}`, `conductor:{correo}` o vacía. */
@Composable
fun HabilitacionesScreen(
    referencia: String,
    onVolver: () -> Unit,
    viewModel: HabilitacionesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver del alta o de editar, las listas deben leer lo guardado.
    LaunchedEffect(referencia) { viewModel.cargar(referencia, forzar = true) }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.HABILITACIONES.titulo),
        cargando = estado.cargando,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
        aviso = estado.aviso,
        onReintentar = viewModel::reintentar,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        val conductores = estado.conductores.map { Opcion(it.correo, "${it.nombre} (${it.correo})") }
        val todos = Opcion("", stringResource(R.string.filtro_todos))
        Text(stringResource(R.string.habilitaciones_ayuda))
        SelectorOpciones(
            estado.conductor, R.string.campo_conductor, estado.conductores.filter { it.activo }
                .map { Opcion(it.correo, "${it.nombre} (${it.correo})") },
            viewModel::elegirConductor, !enviando, e.deCampo("conductor"),
        )
        SelectorOpciones(
            estado.vehiculo, R.string.campo_vehiculo, estado.moviles.filter { it.activo }.map { Opcion(it.patente, it.patente) },
            viewModel::elegirVehiculo, !enviando, e.deCampo("vehiculo"),
        )
        Accion(
            R.string.accion_habilitar, viewModel::habilitar,
            habilitada = estado.conductor.isNotBlank() && estado.vehiculo.isNotBlank(), enviando = enviando,
        )
        SelectorOpciones(
            estado.filtroVehiculo, R.string.habilitaciones_filtro_vehiculo,
            listOf(todos) + estado.moviles.map { Opcion(it.patente, it.patente) },
            viewModel::filtrarPorVehiculo, !enviando,
        )
        SelectorOpciones(
            estado.filtroConductor, R.string.habilitaciones_filtro_conductor, listOf(todos) + conductores,
            viewModel::filtrarPorConductor, !enviando,
        )
        if (estado.habilitaciones.isEmpty()) Text(stringResource(R.string.habilitaciones_sin_resultados))
        ResumenHabilitaciones(estado.habilitaciones)
    }
}
