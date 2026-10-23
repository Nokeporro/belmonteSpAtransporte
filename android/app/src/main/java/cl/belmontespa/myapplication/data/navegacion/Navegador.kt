package cl.belmontespa.myapplication.data.navegacion

import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion

/**
 * Pasa una ruta a otra aplicación del teléfono (CU-03, RF10). Es lo único de la navegación que toca Android: sin mapa
 * embebido ni permiso de ubicación. Los ViewModel lo reciben por esta interfaz para probarse en JVM.
 */
interface Navegador {
    /** Las aplicaciones de [AppNavegacion] que el teléfono tiene instaladas, en el orden de la enumeración. */
    fun instaladas(): List<AppNavegacion>

    /** Abre [app] con la ruta hasta [punto]; `false` si no se pudo abrir (por ejemplo, ya no está instalada). */
    fun abrir(app: AppNavegacion, punto: Ubicacion): Boolean
}
