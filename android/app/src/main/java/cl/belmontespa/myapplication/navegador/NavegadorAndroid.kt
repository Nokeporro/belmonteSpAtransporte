package cl.belmontespa.myapplication.navegador

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion

class NavegadorAndroid(private val contexto: Context) : Navegador {

    override fun instaladas(): List<AppNavegacion> =
        AppNavegacion.entries.filter { app -> intent(app, PUNTO_DE_PRUEBA).resolveActivity(contexto.packageManager) != null }

    override fun abrir(app: AppNavegacion, punto: Ubicacion): Boolean = try {
        contexto.startActivity(intent(app, punto))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    internal fun intent(app: AppNavegacion, punto: Ubicacion): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(app.uri(punto)))
            .setPackage(app.paquete)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        val PUNTO_DE_PRUEBA = Ubicacion(direccion = "", comuna = "", region = "", latitud = 0.0, longitud = 0.0)
    }
}
