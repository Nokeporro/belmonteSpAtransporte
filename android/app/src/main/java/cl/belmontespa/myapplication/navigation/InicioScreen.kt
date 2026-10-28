package cl.belmontespa.myapplication.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.view.components.ContenedorPantalla

@Composable
fun InicioScreen() {
    ContenedorPantalla(
        titulo = stringResource(PantallaDemo.INICIO.titulo),
        cargando = false,
        modificado = false,
        onVolver = null,
    ) {
        Text(stringResource(R.string.inicio_comprobando_sesion))
    }
}
