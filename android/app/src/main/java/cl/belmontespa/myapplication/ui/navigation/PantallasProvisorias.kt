package cl.belmontespa.myapplication.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.BarraSuperior
import cl.belmontespa.myapplication.ui.components.BotonAncho
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso

// Destinos de la navegación base (T03). T06 reemplaza cada uno por su pantalla real.

@Composable
fun PantallaProvisoria(
    @StringRes titulo: Int,
    onVolver: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        BarraSuperior(titulo = stringResource(titulo), onVolver = onVolver)
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.pantalla_en_construccion),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            contenido()
        }
    }
}

/** Perfil de cualquier rol: por ahora solo el cierre de sesión local. */
@Composable
fun PerfilProvisorio(onCerrarSesion: () -> Unit) {
    PantallaProvisoria(R.string.nav_perfil) {
        BotonAncho(stringResource(R.string.accion_cerrar_sesion), onCerrarSesion, tipo = TipoBoton.SECUNDARIO)
    }
}

/** Entró con la contraseña temporal: debe cambiarla antes de usar la app. */
@Composable
fun CambioPasswordObligatorioProvisorio(onCerrarSesion: () -> Unit) {
    PantallaProvisoria(R.string.titulo_cambiar_password) {
        Aviso(stringResource(R.string.aviso_cambio_password_obligatorio), TonoAviso.AVISO)
        BotonAncho(stringResource(R.string.accion_cerrar_sesion), onCerrarSesion, tipo = TipoBoton.SECUNDARIO)
    }
}
