package cl.belmontespa.myapplication.view.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.viewmodel.administrador.RegistrarVehiculoViewModel

/** «Registrar vehículo»: al guardar abre el detalle del vehículo, con su código de abordaje. */
@Composable
fun RegistrarVehiculoScreen(
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistrarVehiculoViewModel = viewModel(),
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
