package cl.belmontespa.myapplication.data.navegacion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion

/**
 * `ACTION_VIEW` a Google Maps o a Waze, fijado a su paquete para que no lo tome un navegador. Desde Android 11 la app solo
 * ve esos paquetes porque el manifest los declara en `<queries>`.
 */
class NavegadorAndroid(private val contexto: Context) : Navegador {

    override fun instaladas(): List<AppNavegacion> =
        AppNavegacion.entries.filter { app -> intent(app, PUNTO_DE_PRUEBA).resolveActivity(contexto.packageManager) != null }

    override fun abrir(app: AppNavegacion, punto: Ubicacion): Boolean = try {
        contexto.startActivity(intent(app, punto))
        true
    } catch (e: ActivityNotFoundException) {
        // Se desinstaló entre que se listó y que se tocó.
        false
    }

    // `NEW_TASK` porque se lanza desde el contexto de la aplicación, no desde la actividad.
    internal fun intent(app: AppNavegacion, punto: Ubicacion): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(app.uri(punto)))
            .setPackage(app.paquete)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        // Para saber si la app está instalada basta con que resuelva el esquema; las coordenadas no importan.
        val PUNTO_DE_PRUEBA = Ubicacion(direccion = "", comuna = "", region = "", latitud = 0.0, longitud = 0.0)
    }
}
