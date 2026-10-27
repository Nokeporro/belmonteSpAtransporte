package cl.belmontespa.myapplication.view.auth

import android.content.ClipData
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.CampoFormulario
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.auth.RecuperarPasswordViewModel
import kotlinx.coroutines.launch

/**
 * «Recuperar contraseña» (Administrador, desde su Perfil): genera la temporal de otra cuenta y la muestra una
 * sola vez, con «Copiar», para que el Administrador la envíe por su correo.
 */
@Composable
fun RecuperarPasswordScreen(
    onVolver: () -> Unit,
    viewModel: RecuperarPasswordViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val portapapeles = LocalClipboard.current
    val alcance = rememberCoroutineScope()
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.RECUPERAR.titulo),
        cargando = false,
        modificado = estado.modificado,
        onVolver = onVolver,
        error = estado.error,
    ) {
        Aviso(stringResource(R.string.recuperar_explicacion), TonoAviso.INFORMACION)
        CampoFormulario(
            estado.correo, viewModel::escribirCorreo, R.string.campo_correo_cuenta, "correo", estado.error,
            estado.enviando, KeyboardType.Email,
        )
        Accion(R.string.recuperar_generar, viewModel::recuperar, enviando = estado.enviando)
        estado.temporal?.let { temporal ->
            Bloque(stringResource(R.string.recuperar_resultado)) {
                SelectionContainer {
                    Dato(R.string.campo_password_temporal, temporal.password)
                }
                Dato(R.string.campo_vence, Formatos.fechaHoraDetalle(temporal.expira))
                Aviso(stringResource(R.string.recuperar_una_vez), TonoAviso.AVISO)
                Accion(
                    R.string.accion_copiar,
                    {
                        alcance.launch {
                            portapapeles.setClipEntry(ClipEntry(ClipData.newPlainText("Contraseña temporal", temporal.password)))
                        }
                    },
                    TipoBoton.SECUNDARIO,
                )
            }
        }
    }
}
