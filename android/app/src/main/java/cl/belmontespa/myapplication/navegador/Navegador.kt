package cl.belmontespa.myapplication.navegador

import cl.belmontespa.myapplication.model.AppNavegacion
import cl.belmontespa.myapplication.model.Ubicacion

interface Navegador {
    fun instaladas(): List<AppNavegacion>

    fun abrir(app: AppNavegacion, punto: Ubicacion): Boolean
}
