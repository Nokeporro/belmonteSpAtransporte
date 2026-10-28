package cl.belmontespa.myapplication.view.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.CampoTexto
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.auth.LoginUiState
import cl.belmontespa.myapplication.viewmodel.auth.LoginViewModel

@Composable
fun LoginScreen(
    onCrearCuenta: () -> Unit,
    onAbordarViaje: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
    correoInicial: String = "",
    mostrarSesionExpirada: Boolean = false,
    modoDemoForzado: Boolean? = null,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(correoInicial) {
        if (correoInicial.isNotBlank()) viewModel.cambiarCorreo(correoInicial)
    }
    LoginContenido(
        estado = if (mostrarSesionExpirada && estado.error == null) estado.copy(error = R.string.error_sesion_terminada) else estado,
        modoDemo = modoDemoForzado ?: viewModel.modoDemo,
        onCorreo = viewModel::cambiarCorreo,
        onPassword = viewModel::cambiarPassword,
        onIniciarSesion = viewModel::iniciarSesion,
        onCrearCuenta = onCrearCuenta,
        onAbordarViaje = onAbordarViaje,
    )
}

@Composable
fun LoginContenido(
    estado: LoginUiState,
    modoDemo: Boolean,
    onCorreo: (String) -> Unit,
    onPassword: (String) -> Unit,
    onIniciarSesion: () -> Unit,
    onCrearCuenta: () -> Unit,
    onAbordarViaje: () -> Unit,
) {
    var pedirTemporal by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.marca),
                style = EstilosMono.marca,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.marca_lema),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (modoDemo) {
            Aviso(stringResource(R.string.aviso_modo_demo), TonoAviso.INFORMACION)
        }
        CampoTexto(
            valor = estado.correo,
            onCambio = onCorreo,
            etiqueta = stringResource(R.string.login_correo),
            tipoTeclado = KeyboardType.Email,
            habilitado = !estado.enviando,
        )
        CampoTexto(
            valor = estado.password,
            onCambio = onPassword,
            etiqueta = stringResource(R.string.login_password),
            tipoTeclado = KeyboardType.Password,
            habilitado = !estado.enviando,
        )
        if (estado.error != null) {
            Text(
                stringResource(estado.error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        BotonAncho(
            texto = stringResource(R.string.login_iniciar_sesion),
            onClick = onIniciarSesion,
            habilitado = !estado.enviando,
        )
        BotonAncho(
            texto = stringResource(R.string.login_crear_cuenta),
            onClick = onCrearCuenta,
            tipo = TipoBoton.SECUNDARIO,
        )
        TextButton(onClick = { pedirTemporal = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.login_recuperar_password))
        }
        if (pedirTemporal) {
            Aviso(stringResource(R.string.aviso_pedir_temporal_admin), TonoAviso.INFORMACION)
        }
        OutlinedButton(onClick = onAbordarViaje, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(
                painterResource(R.drawable.ic_qr),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(stringResource(R.string.login_abordar_viaje), modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            stringResource(R.string.login_nota_conductores),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginDemoPreview() {
    BelmonteTheme {
        LoginContenido(
            estado = LoginUiState(correo = "crios@andesconsult.cl", error = R.string.error_credenciales),
            modoDemo = true,
            onCorreo = {},
            onPassword = {},
            onIniciarSesion = {},
            onCrearCuenta = {},
            onAbordarViaje = {},
        )
    }
}
