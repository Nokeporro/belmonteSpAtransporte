package cl.belmontespa.myapplication.navigation

import cl.belmontespa.myapplication.model.Rol

// Rutas de Navigation Compose, como `Routes` del template del curso. Hay un flujo de acceso y uno por rol
// (README, Navegación y pantallas); la ruta de cada pantalla está en PantallaDemo.
// Las rutas solo llevan identificadores, nunca datos personales ni contraseñas.
object Rutas {
    const val GRAFO_ACCESO = "grafo_acceso"
    const val GRAFO_SOLICITANTE = "grafo_solicitante"
    const val GRAFO_ADMINISTRADOR = "grafo_administrador"
    const val GRAFO_CONDUCTOR = "grafo_conductor"

    /** Grafo de navegación del rol que entrega el login. */
    fun grafoDe(rol: Rol): String = when (rol) {
        Rol.SOLICITANTE -> GRAFO_SOLICITANTE
        Rol.ADMINISTRADOR -> GRAFO_ADMINISTRADOR
        Rol.CONDUCTOR -> GRAFO_CONDUCTOR
    }
}
