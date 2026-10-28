package cl.belmontespa.myapplication.viewmodel.conductor

import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.navegador.Navegador

internal class NavegadorFalso(var disponibles: List<AppNavegacion> = emptyList()) : Navegador {
    val abiertas = mutableListOf<Pair<AppNavegacion, Ubicacion>>()

    var puedeAbrir = true

    override fun instaladas(): List<AppNavegacion> = disponibles

    override fun abrir(app: AppNavegacion, punto: Ubicacion): Boolean {
        abiertas += app to punto
        return puedeAbrir
    }
}
