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
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo

/** «Registrar vehículo»: al guardar abre el detalle del vehículo, con su código de abordaje. */
@Composable
fun RegistrarVehiculoScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistrarVehiculoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.registrado) {
        estado.registrado?.let { patente -> viewModel.consumirRegistrado(); onNavegar(PantallaDemo.DETALLE_VEHICULO, patente) }
    }
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.REGISTRAR_VEHICULO.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
    ) {
        val e = estado.error
        val enviando = estado.enviando
        Text(stringResource(R.string.vehiculo_registro_indicacion))
        CampoFormulario(estado.patente, viewModel::escribirPatente, R.string.campo_patente, "patente", e, enviando)
        CampoFormulario(estado.capacidad, viewModel::escribirCapacidad, R.string.campo_capacidad, "capacidad", e, enviando, KeyboardType.Number)
        CampoFormulario(estado.gps, viewModel::escribirGps, R.string.campo_gps, "gps", e, enviando, KeyboardType.Number)
        Accion(R.string.accion_guardar, viewModel::guardar, enviando = enviando)
    }
}
