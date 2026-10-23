package cl.belmontespa.myapplication.ui.abordaje

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.ui.components.Aviso
import cl.belmontespa.myapplication.ui.components.BarraSuperior
import cl.belmontespa.myapplication.ui.components.BotonAncho
import cl.belmontespa.myapplication.ui.components.CampoTexto
import cl.belmontespa.myapplication.ui.components.TipoBoton
import cl.belmontespa.myapplication.ui.components.TonoAviso
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * «Abordar viaje» (HU29). El lector de ZXing es la variante «Escanear código QR» (README): se abre desde aquí, a pantalla
 * completa. Como la variante del catálogo `ESCANEAR` no puede llevar la cámara adentro, es esta misma pantalla con el
 * título del lector y no abre nada por su cuenta. No necesita sesión.
 */
@Composable
fun AbordarScreen(
    pantalla: PantallaDemo,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: AbordarViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val indicacion = stringResource(R.string.abordaje_lector_indicacion)

    LaunchedEffect(estado.listo) {
        estado.listo?.let { codigo ->
            viewModel.consumirListo()
            onNavegar(PantallaDemo.CONFIRMAR_ABORDAJE, codigo.paraRuta())
        }
    }

    // Cancelar con atrás devuelve `null`: vuelve sin aviso.
    val lector = rememberLauncherForActivityResult(ScanContract()) { viewModel.alLeer(it.contents) }
    fun escanear() {
        lector.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(indicacion)
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) escanear() else viewModel.camaraDenegada(paraSiempre = !contexto.sePuedePedirLaCamara())
    }

    AbordarContenido(
        pantalla = pantalla,
        estado = estado,
        onEscanear = {
            viewModel.alEscanear()
            if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) escanear()
            else permiso.launch(Manifest.permission.CAMERA)
        },
        onIngresar = viewModel::abrirIngreso,
        onAbrirAjustes = {
            contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${contexto.packageName}".toUri()))
        },
        onVolver = onVolver,
        onPatente = viewModel::escribirPatente,
        onCodigo = viewModel::escribirCodigo,
        onContinuar = viewModel::continuarIngreso,
        onCerrarIngreso = viewModel::cerrarIngreso,
    )
}

// Tras negar el permiso, Android deja de explicar (y de pedir) cuando el usuario marcó «no volver a preguntar».
private fun Context.sePuedePedirLaCamara(): Boolean {
    val actividad = generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
    return actividad != null && ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.CAMERA)
}

@Composable
fun AbordarContenido(
    pantalla: PantallaDemo,
    estado: AbordarUiState,
    onEscanear: () -> Unit,
    onIngresar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onVolver: () -> Unit,
    onPatente: (String) -> Unit,
    onCodigo: (String) -> Unit,
    onContinuar: () -> Unit,
    onCerrarIngreso: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(pantalla.titulo), onVolver = onVolver)
        Column(
            modifier = Modifier
                .testTag("t06_contenido")
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (pantalla == PantallaDemo.ESCANEAR) R.string.abordaje_lector_indicacion else R.string.abordaje_indicacion),
                style = MaterialTheme.typography.bodyLarge,
            )
            when (estado.aviso) {
                AvisoAbordar.QR_AJENO -> Aviso(stringResource(R.string.abordaje_qr_ajeno), TonoAviso.AVISO, Modifier.testTag("t06_aviso_abordaje"))
                AvisoAbordar.CAMARA_NEGADA, AvisoAbordar.CAMARA_NEGADA_PARA_SIEMPRE ->
                    Aviso(stringResource(R.string.abordaje_camara_negada), TonoAviso.AVISO, Modifier.testTag("t06_aviso_abordaje"))
                null -> Unit
            }
            BotonAncho(stringResource(R.string.abordaje_escanear), onEscanear, alto56 = true)
            // El ingreso manual no depende de la cámara: está siempre disponible.
            BotonAncho(stringResource(R.string.abordaje_ingresar_codigo), onIngresar, tipo = TipoBoton.SECUNDARIO, alto56 = true)
            if (estado.aviso == AvisoAbordar.CAMARA_NEGADA_PARA_SIEMPRE) {
                BotonAncho(stringResource(R.string.abordaje_abrir_ajustes), onAbrirAjustes, tipo = TipoBoton.SECUNDARIO, alto56 = true)
            }
        }
    }
    if (estado.ingresando) {
        AlertDialog(
            onDismissRequest = onCerrarIngreso,
            title = { Text(stringResource(R.string.abordaje_ingresar_codigo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto(
                        valor = estado.patente,
                        onCambio = onPatente,
                        etiqueta = stringResource(R.string.campo_patente),
                        modifier = Modifier.fillMaxWidth(),
                        error = if (estado.ingresoInvalido) stringResource(R.string.abordaje_codigo_invalido) else null,
                    )
                    CampoTexto(
                        valor = estado.codigo,
                        onCambio = onCodigo,
                        etiqueta = stringResource(R.string.campo_codigo),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onContinuar, enabled = estado.puedeContinuar) { Text(stringResource(R.string.accion_continuar)) }
            },
            dismissButton = { TextButton(onClick = onCerrarIngreso) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}
