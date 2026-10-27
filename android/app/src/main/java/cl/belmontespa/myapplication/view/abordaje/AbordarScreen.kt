package cl.belmontespa.myapplication.view.abordaje

import android.content.Context
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.BarraSuperior
import cl.belmontespa.myapplication.view.components.BotonAncho
import cl.belmontespa.myapplication.view.components.CampoTexto
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.viewmodel.abordaje.AbordarUiState
import cl.belmontespa.myapplication.viewmodel.abordaje.AbordarViewModel
import cl.belmontespa.myapplication.viewmodel.abordaje.AvisoAbordar
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * «Abordar viaje» (HU29). El lector de Google Code Scanner es la variante «Escanear código QR» (README): se abre desde
 * aquí, a pantalla completa, en Google Play services. Como la variante del catálogo `ESCANEAR` no puede llevar la cámara
 * adentro, es esta misma pantalla con el título del lector y no abre nada por su cuenta. No necesita sesión.
 */
@Composable
fun AbordarScreen(
    pantalla: PantallaDemo,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: AbordarViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    LaunchedEffect(estado.listo) {
        estado.listo?.let { codigo ->
            viewModel.consumirListo()
            onNavegar(PantallaDemo.CONFIRMAR_ABORDAJE, codigo.paraRuta())
        }
    }

    AbordarContenido(
        pantalla = pantalla,
        estado = estado,
        onEscanear = {
            viewModel.alEscanear()
            contexto.escanearQr(alLeer = viewModel::alLeer, alFallar = viewModel::lectorNoDisponible)
        },
        onIngresar = viewModel::abrirIngreso,
        onVolver = onVolver,
        onPatente = viewModel::escribirPatente,
        onCodigo = viewModel::escribirCodigo,
        onContinuar = viewModel::continuarIngreso,
        onCerrarIngreso = viewModel::cerrarIngreso,
    )
}

// El lector corre en Google Play services: la app no pide `CAMERA`. Cancelar con atrás llega como `null` (vuelve sin
// aviso) y un QR sin texto como "" (cae en «QR ajeno»); cualquier fallo (sin Play services, módulo sin descargar o
// permiso negado en la pantalla de Google) deja «Ingresar código» como salida.
private fun Context.escanearQr(alLeer: (String?) -> Unit, alFallar: () -> Unit) {
    val opciones = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(this, opciones).startScan()
        .addOnSuccessListener { alLeer(it.rawValue.orEmpty()) }
        .addOnCanceledListener { alLeer(null) }
        .addOnFailureListener { alFallar() }
}

@Composable
fun AbordarContenido(
    pantalla: PantallaDemo,
    estado: AbordarUiState,
    onEscanear: () -> Unit,
    onIngresar: () -> Unit,
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
                .testTag("pantalla_contenido")
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
                AvisoAbordar.QR_AJENO -> Aviso(stringResource(R.string.abordaje_qr_ajeno), TonoAviso.AVISO, Modifier.testTag("pantalla_aviso_abordaje"))
                AvisoAbordar.LECTOR_NO_DISPONIBLE ->
                    Aviso(stringResource(R.string.abordaje_lector_no_disponible), TonoAviso.AVISO, Modifier.testTag("pantalla_aviso_abordaje"))
                null -> Unit
            }
            BotonAncho(stringResource(R.string.abordaje_escanear), onEscanear, alto56 = true)
            // El ingreso manual no depende del lector: está siempre disponible.
            BotonAncho(stringResource(R.string.abordaje_ingresar_codigo), onIngresar, tipo = TipoBoton.SECUNDARIO, alto56 = true)
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
