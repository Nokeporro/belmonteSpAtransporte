package cl.belmontespa.myapplication.view.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.ui.theme.EstilosMono

@Composable
fun CampoLugar(
    valor: String,
    @StringRes etiqueta: Int,
    onBuscar: () -> Unit,
    habilitado: Boolean = true,
    error: String? = null,
) {
    OutlinedButton(
        onClick = onBuscar,
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column {
            Text(stringResource(etiqueta), style = EstilosMono.etiqueta)
            Text(valor.ifBlank { stringResource(R.string.aviso_busca_lugar) })
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
fun CorreccionDeComuna(
    regiones: List<Region>,
    onCorregir: (region: String, comuna: String) -> Unit,
    onReintentar: () -> Unit,
    habilitado: Boolean = true,
    error: String? = null,
) {
    var region by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.aviso_comuna_faltante), color = MaterialTheme.colorScheme.error)
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (regiones.isEmpty()) {
        Accion(R.string.accion_reintentar, onReintentar, TipoBoton.SECUNDARIO, habilitada = habilitado)
        return
    }
    SelectorOpciones(region, R.string.campo_region, regiones.map { Opcion(it.nombre, it.nombre) }, { region = it }, habilitado)
    SelectorOpciones(
        valor = "",
        etiqueta = R.string.campo_comuna,
        opciones = regiones.find { it.nombre == region }?.comunas.orEmpty().map { Opcion(it, it) },
        onElegir = { onCorregir(region, it) },
        habilitado = habilitado && region.isNotEmpty(),
    )
}
