package cl.belmontespa.myapplication.ui.navigation

import kotlinx.serialization.Serializable

// Destinos de Navigation Compose: un flujo de acceso y uno por rol (README, Navegación y pantallas).
// Los grafos agrupan los destinos de cada flujo; las rutas solo llevan identificadores, nunca datos
// personales ni contraseñas.

@Serializable
data object Inicio

@Serializable
data object GrafoAcceso

@Serializable
data object Login

@Serializable
data object RegistroSolicitante

@Serializable
data object RecuperarPassword

@Serializable
data object AbordarViaje

@Serializable
data object CambioPasswordObligatorio

@Serializable
data object GrafoSolicitante

@Serializable
data object MisSolicitudes

@Serializable
data object PerfilSolicitante

@Serializable
data object GrafoAdministrador

@Serializable
data object Solicitudes

@Serializable
data object Conductores

@Serializable
data object Vehiculos

@Serializable
data object PerfilAdministrador

@Serializable
data object GrafoConductor

@Serializable
data object MisViajes

@Serializable
data object PerfilConductor
