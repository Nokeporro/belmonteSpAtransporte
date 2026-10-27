package cl.belmontespa.myapplication.viewmodel.conductor

import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.navegador.Navegador

/** Navegador de las pruebas JVM: dice qué aplicaciones "hay instaladas" y guarda lo que se le pidió abrir. */
internal class NavegadorFalso(var disponibles: List<AppNavegacion> = emptyList()) : Navegador {
    val abiertas = mutableListOf<Pair<AppNavegacion, Ubicacion>>()

    /** Falso simula una aplicación que ya no está o que no pudo abrirse. */
    var puedeAbrir = true

    override fun instaladas(): List<AppNavegacion> = disponibles

    override fun abrir(app: AppNavegacion, punto: Ubicacion): Boolean {
        abiertas += app to punto
        return puedeAbrir
    }
}
