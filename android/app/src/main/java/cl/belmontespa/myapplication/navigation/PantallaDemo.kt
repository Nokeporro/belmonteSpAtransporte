package cl.belmontespa.myapplication.navigation

import androidx.annotation.StringRes
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import kotlinx.serialization.Serializable

// Catálogo de las 46 pantallas/variantes de los wireframes del README.
// El rol se comprueba también al abrir una ruta; ocultar botones no autoriza una operación.
@Serializable
enum class PantallaDemo(@param:StringRes val titulo: Int, val rol: Rol?) {
    INICIO(R.string.t06_inicio, null),
    LOGIN(R.string.t06_login, null),
    LOGIN_DEMO(R.string.t06_login_demo, null),
    REGISTRO(R.string.t06_registro, null),
    RECUPERAR(R.string.t06_recuperar, Rol.ADMINISTRADOR),
    CAMBIO_OBLIGATORIO(R.string.t06_cambio_obligatorio, null),
    SESION_EXPIRADA(R.string.t06_sesion_expirada, null),
    ABORDAR(R.string.t06_abordar, null),
    ESCANEAR(R.string.t06_escanear, null),
    CONFIRMAR_ABORDAJE(R.string.t06_confirmar_abordaje, null),
    ABORDADO(R.string.t06_abordado, null),
    MIS_SOLICITUDES(R.string.t06_mis_solicitudes, Rol.SOLICITANTE),
    NUEVA_SOLICITUD(R.string.t06_nueva_solicitud, Rol.SOLICITANTE),
    DETALLE_SOLICITUD(R.string.t06_detalle_solicitud, Rol.SOLICITANTE),
    SOLICITUD_SIN_PASAJEROS(R.string.t06_solicitud_sin_pasajeros, Rol.SOLICITANTE),
    AGREGAR_PASAJERO(R.string.t06_agregar_pasajero, Rol.SOLICITANTE),
    CONFIRMAR_CANCELACION(R.string.t06_confirmar_cancelacion, Rol.SOLICITANTE),
    SOLICITUD_CANCELADA(R.string.t06_solicitud_cancelada, Rol.SOLICITANTE),
    PERFIL_SOLICITANTE(R.string.t06_perfil_solicitante, Rol.SOLICITANTE),
    CAMBIAR_PASSWORD(R.string.t06_cambiar_password, null),
    SOLICITUDES(R.string.t06_solicitudes, Rol.ADMINISTRADOR),
    CUENTAS_PENDIENTES(R.string.t06_cuentas_pendientes, Rol.ADMINISTRADOR),
    DETALLE_ADMIN(R.string.t06_detalle_admin, Rol.ADMINISTRADOR),
    SIN_PASAJEROS_ADMIN(R.string.t06_sin_pasajeros_admin, Rol.ADMINISTRADOR),
    CANCELAR_VIAJE(R.string.t06_cancelar_viaje, Rol.ADMINISTRADOR),
    ASIGNAR(R.string.t06_asignar, Rol.ADMINISTRADOR),
    CONDUCTORES(R.string.t06_conductores, Rol.ADMINISTRADOR),
    REGISTRAR_CONDUCTOR(R.string.t06_registrar_conductor, Rol.ADMINISTRADOR),
    DETALLE_CONDUCTOR(R.string.t06_detalle_conductor, Rol.ADMINISTRADOR),
    VEHICULOS(R.string.t06_vehiculos, Rol.ADMINISTRADOR),
    REGISTRAR_VEHICULO(R.string.t06_registrar_vehiculo, Rol.ADMINISTRADOR),
    DETALLE_VEHICULO(R.string.t06_detalle_vehiculo, Rol.ADMINISTRADOR),
    HABILITACIONES(R.string.t06_habilitaciones, Rol.ADMINISTRADOR),
    REGISTRAR_PAGO(R.string.t06_registrar_pago, Rol.ADMINISTRADOR),
    DETALLE_PAGO(R.string.t06_detalle_pago, null),
    PERFIL_ADMIN(R.string.t06_perfil_admin, Rol.ADMINISTRADOR),
    REGISTRAR_ADMIN(R.string.t06_registrar_admin, Rol.ADMINISTRADOR),
    NOTIFICACION(R.string.t06_notificacion, null),
    MIS_VIAJES(R.string.t06_mis_viajes, Rol.CONDUCTOR),
    DETALLE_VIAJE(R.string.t06_detalle_viaje, Rol.CONDUCTOR),
    ANTES_VENTANA(R.string.t06_antes_ventana, Rol.CONDUCTOR),
    CONFIRMAR_INICIO(R.string.t06_confirmar_inicio, Rol.CONDUCTOR),
    SIN_CONEXION(R.string.t06_sin_conexion, Rol.CONDUCTOR),
    CONFLICTO(R.string.t06_conflicto, Rol.CONDUCTOR),
    NAVEGAR(R.string.t06_navegar, Rol.CONDUCTOR),
    PERFIL_CONDUCTOR(R.string.t06_perfil_conductor, Rol.CONDUCTOR);

    // Solo estas pantallas se pueden recorrer sin una cuenta autenticada.
    val publica: Boolean get() = ordinal <= ABORDADO.ordinal && this != CAMBIO_OBLIGATORIO && this != RECUPERAR
}

// Solo identificadores: nunca se serializan formularios, documentos ni contraseñas.
@Serializable
data class DestinoDemo(val pantalla: PantallaDemo, val id: String = "")