package cl.belmontespa.myapplication.ui.pantallas

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.ui.components.*
import cl.belmontespa.myapplication.util.Formatos
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.FileOutputStream

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
