package cl.belmontespa.myapplication.ui.pantallas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.ui.components.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.FileOutputStream

@Composable
internal fun ContextoPantalla.PantallaAbordaje() {
    val contexto = LocalContext.current
    val indicacion = stringResource(R.string.abordaje_lector_indicacion)
    val scanner = rememberLauncherForActivityResult(ScanContract()) { resultado ->
        resultado.getContents()?.let { contenido ->
            if (vm.seleccionarQr(contenido)) navegar(PantallaDemo.CONFIRMAR_ABORDAJE, "")
        }
    }
    fun escanear() {
        scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt(indicacion).setBeepEnabled(false).setOrientationLocked(false))
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        if (permitido) escanear() else vm.aviso(R.string.abordaje_camara_negada)
    }
    if (pantalla == PantallaDemo.ABORDADO) {
        val hora = estado.horaAbordaje
        if (hora != null) Aviso(stringResource(R.string.abordaje_registrado, Formatos.hora(hora)), TonoAviso.INFORMACION)
        else Aviso(stringResource(R.string.error_no_encontrado), TonoAviso.AVISO)
        Accion(R.string.abordaje_volver_inicio, { navegar(PantallaDemo.LOGIN, "") }, TipoBoton.SECUNDARIO)
        return
    }
    if (pantalla in listOf(PantallaDemo.ABORDAR, PantallaDemo.ESCANEAR)) {
        Text(stringResource(if (pantalla == PantallaDemo.ESCANEAR) R.string.abordaje_lector_indicacion else R.string.abordaje_indicacion))
        Accion(R.string.abordaje_escanear, {
            if (pantalla != PantallaDemo.ESCANEAR) navegar(PantallaDemo.ESCANEAR, "")
            else if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) escanear()
            else permiso.launch(Manifest.permission.CAMERA)
        })
        if (estado.aviso == R.string.abordaje_camara_negada) Accion(R.string.abordaje_abrir_ajustes, {
            contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}")))
        }, TipoBoton.SECUNDARIO)
    }
    // Ingreso manual disponible siempre: no depende del permiso de cámara.
    if (pantalla == PantallaDemo.CONFIRMAR_ABORDAJE) Dato(R.string.campo_vehiculo, vm.valor("patente"))
    else {
        Campo("patente", R.string.campo_patente)
        Campo("codigo", R.string.campo_codigo)
    }
    if (pantalla == PantallaDemo.CONFIRMAR_ABORDAJE) {
        Selector("tipoDocumento", R.string.campo_tipo_documento, cl.belmontespa.myapplication.model.TipoDocumento.entries.map { Opcion(it.name, stringResource(it.etiqueta)) })
        Campo("documento", R.string.campo_documento)
        Accion(R.string.abordaje_confirmar)
    } else Accion(R.string.abordaje_ingresar_codigo, {
        if (vm.seleccionarQr("BELMONTE-ABORDAJE:${vm.valor("patente")}:${vm.valor("codigo").filterNot(Char::isWhitespace)}")) navegar(PantallaDemo.CONFIRMAR_ABORDAJE, "")
    }, TipoBoton.SECUNDARIO)
}

@Composable
internal fun QrVehiculo(movil: Movil) {
    val contexto = LocalContext.current
    val contenido = "BELMONTE-ABORDAJE:${movil.patente}:${movil.codigoAbordaje}"
    val bitmap = remember(contenido) { BarcodeEncoder().encodeBitmap(contenido, BarcodeFormat.QR_CODE, 512, 512) }
    Image(bitmap.asImageBitmap(), stringResource(R.string.qr_vehiculo_descripcion, movil.patente), modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 320.dp))
    Text(Formatos.codigoAbordaje(movil.codigoAbordaje))
    Text(stringResource(R.string.qr_vehiculo_indicacion))
    val titulo = stringResource(R.string.qr_vehiculo_descripcion, movil.patente)
    BotonAncho(stringResource(R.string.accion_imprimir), {
        (contexto.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager).print(titulo, AdaptadorQr(bitmap, movil.patente), null)
    }, tipo = TipoBoton.SECUNDARIO)
}

// PDF generado a demanda en el servicio de impresión; no guarda datos personales en archivos.
private class AdaptadorQr(private val bitmap: Bitmap, private val patente: String) : PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
        if (cancellationSignal.isCanceled()) callback.onLayoutCancelled()
        else callback.onLayoutFinished(PrintDocumentInfo.Builder("$patente.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(), true)
    }
    override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
        if (cancellationSignal.isCanceled()) { callback.onWriteCancelled(); return }
        try {
            val documento = PdfDocument()
            try {
                val pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                pagina.canvas.drawBitmap(bitmap, null, android.graphics.Rect(50, 100, 545, 595), null)
                pagina.canvas.drawText(patente, 50f, 650f, android.graphics.Paint().apply { textSize = 28f })
                documento.finishPage(pagina)
                FileOutputStream(destination.fileDescriptor).use { documento.writeTo(it) }
            }
            finally { documento.close() }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) { callback.onWriteFailed(null) }
    }
}
