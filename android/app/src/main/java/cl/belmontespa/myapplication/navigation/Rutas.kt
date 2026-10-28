package cl.belmontespa.myapplication.navigation

import cl.belmontespa.myapplication.model.Rol

object Rutas {
    const val GRAFO_ACCESO = "grafo_acceso"
    const val GRAFO_SOLICITANTE = "grafo_solicitante"
    const val GRAFO_ADMINISTRADOR = "grafo_administrador"
    const val GRAFO_CONDUCTOR = "grafo_conductor"

    fun grafoDe(rol: Rol): String = when (rol) {
        Rol.SOLICITANTE -> GRAFO_SOLICITANTE
        Rol.ADMINISTRADOR -> GRAFO_ADMINISTRADOR
        Rol.CONDUCTOR -> GRAFO_CONDUCTOR
    }
}
