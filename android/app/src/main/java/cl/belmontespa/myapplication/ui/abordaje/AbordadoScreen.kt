package cl.belmontespa.myapplication.ui.abordaje

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.BarraSuperior
import cl.belmontespa.myapplication.ui.components.BotonAncho
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import java.time.LocalDateTime

/**
 * «Abordaje registrado» (HU29). La hora viene en la ruta (ISO): repetir el escaneo responde la misma, así que esta pantalla
 * no guarda nada. «Volver al inicio» abre el Login, o «Mis solicitudes» si se entró con sesión (lo decide `onNavegar`).
 */
@Composable
fun AbordadoScreen(
    hora: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
) {
    val registrada = remember(hora) { runCatching { LocalDateTime.parse(hora) }.getOrNull() }
    AbordadoContenido(registrada, onVolverAlInicio = { onNavegar(PantallaDemo.LOGIN, "") }, onVolver = onVolver)
}

@Composable
fun AbordadoContenido(
    hora: LocalDateTime?,
    onVolverAlInicio: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(PantallaDemo.ABORDAR.titulo), onVolver = onVolver)
        Column(
            modifier = Modifier
                .testTag("pantalla_contenido")
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hora != null) Aviso(stringResource(R.string.abordaje_registrado, Formatos.hora(hora)), TonoAviso.INFORMACION)
            else Aviso(stringResource(R.string.abordaje_no_encontrado), TonoAviso.AVISO)
            BotonAncho(stringResource(R.string.abordaje_volver_inicio), onVolverAlInicio, tipo = TipoBoton.SECUNDARIO, alto56 = true)
        }
    }
}
