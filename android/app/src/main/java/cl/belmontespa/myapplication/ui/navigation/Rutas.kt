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

// Formulario de una nueva solicitud.
// Sus campos viven en el ViewModel, no en la ruta.
@Serializable
data object NuevaSolicitud

// El detalle obtiene los datos mediante el repositorio.
// La navegación transporta únicamente el identificador del viaje.
@Serializable
data class DetalleSolicitud(val folio: String)

// El formulario necesita identificar a qué solicitud agrega el pasajero.
@Serializable
data class AgregarPasajero(val folio: String)

// La confirmación vuelve a consultar el viaje antes de permitir cancelar.
@Serializable
data class ConfirmarCancelacionSolicitud(val folio: String)

// Solo se muestra después de que el repositorio confirme la cancelación.
@Serializable
data class SolicitudCancelada(val folio: String)

// La cuenta se obtiene de la sesión; no se envían contraseñas por navegación.
@Serializable
data object CambiarPassword
