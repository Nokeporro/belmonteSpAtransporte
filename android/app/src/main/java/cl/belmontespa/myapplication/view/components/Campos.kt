package cl.belmontespa.myapplication.view.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.ui.theme.EstilosMono

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true,
) {
    val esPassword = tipoTeclado == KeyboardType.Password ||
        tipoTeclado == KeyboardType.NumberPassword
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = {
            Text(
                text = etiqueta,
                style = EstilosMono.etiqueta,
            )
        },
        isError = error != null,
        supportingText = error?.let { mensaje ->
            { Text(text = mensaje) }
        },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        visualTransformation = if (esPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (error != null) error(error)
            },
    )
}

enum class TipoBoton { PRINCIPAL, SECUNDARIO, PELIGRO }

@Composable
fun BotonAncho(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tipo: TipoBoton = TipoBoton.PRINCIPAL,
    habilitado: Boolean = true,
    alto56: Boolean = false,
) {
    val tamano = modifier
        .fillMaxWidth()
        .heightIn(min = if (alto56) 56.dp else 48.dp)
    when (tipo) {
        TipoBoton.PRINCIPAL -> Button(
            onClick = onClick,
            enabled = habilitado,
            modifier = tamano,
        ) {
            Text(text = texto, textAlign = TextAlign.Center)
        }
        TipoBoton.SECUNDARIO -> OutlinedButton(
            onClick = onClick,
            enabled = habilitado,
            modifier = tamano,
        ) {
            Text(text = texto, textAlign = TextAlign.Center)
        }
        TipoBoton.PELIGRO -> Button(
            onClick = onClick,
            enabled = habilitado,
            modifier = tamano,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(text = texto, textAlign = TextAlign.Center)
        }
    }
}
