package cl.belmontespa.myapplication.navigation

import android.net.Uri
import androidx.annotation.StringRes
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol

enum class PantallaDemo(@param:StringRes val titulo: Int, val rol: Rol?, val ruta: String) {
    INICIO(R.string.t06_inicio, null, "inicio"),
    LOGIN(R.string.t06_login, null, "login?id={id}"),
    LOGIN_DEMO(R.string.t06_login_demo, null, "login_demo"),
    REGISTRO(R.string.t06_registro, null, "registro"),
    RECUPERAR(R.string.t06_recuperar, Rol.ADMINISTRADOR, "recuperar"),
    CAMBIO_OBLIGATORIO(R.string.t06_cambio_obligatorio, null, "cambio_obligatorio"),
    SESION_EXPIRADA(R.string.t06_sesion_expirada, null, "sesion_expirada"),
    ABORDAR(R.string.t06_abordar, null, "abordar"),
    ESCANEAR(R.string.t06_escanear, null, "escanear"),
    CONFIRMAR_ABORDAJE(R.string.t06_confirmar_abordaje, null, "confirmar_abordaje?id={id}"),
    ABORDADO(R.string.t06_abordado, null, "abordado?id={id}"),
    MIS_SOLICITUDES(R.string.t06_mis_solicitudes, Rol.SOLICITANTE, "mis_solicitudes"),
    NUEVA_SOLICITUD(R.string.t06_nueva_solicitud, Rol.SOLICITANTE, "nueva_solicitud"),
    DETALLE_SOLICITUD(R.string.t06_detalle_solicitud, Rol.SOLICITANTE, "detalle_solicitud?id={id}"),
    SOLICITUD_SIN_PASAJEROS(R.string.t06_solicitud_sin_pasajeros, Rol.SOLICITANTE, "solicitud_sin_pasajeros?id={id}"),
    AGREGAR_PASAJERO(R.string.t06_agregar_pasajero, Rol.SOLICITANTE, "agregar_pasajero?id={id}"),
    CONFIRMAR_CANCELACION(R.string.t06_confirmar_cancelacion, Rol.SOLICITANTE, "confirmar_cancelacion?id={id}"),
    SOLICITUD_CANCELADA(R.string.t06_solicitud_cancelada, Rol.SOLICITANTE, "solicitud_cancelada?id={id}"),
    PERFIL_SOLICITANTE(R.string.t06_perfil_solicitante, Rol.SOLICITANTE, "perfil_solicitante"),
    CAMBIAR_PASSWORD(R.string.t06_cambiar_password, null, "cambiar_password"),
    SOLICITUDES(R.string.t06_solicitudes, Rol.ADMINISTRADOR, "solicitudes"),
    CUENTAS_PENDIENTES(R.string.t06_cuentas_pendientes, Rol.ADMINISTRADOR, "cuentas_pendientes"),
    DETALLE_ADMIN(R.string.t06_detalle_admin, Rol.ADMINISTRADOR, "detalle_admin?id={id}"),
    SIN_PASAJEROS_ADMIN(R.string.t06_sin_pasajeros_admin, Rol.ADMINISTRADOR, "sin_pasajeros_admin?id={id}"),
    CANCELAR_VIAJE(R.string.t06_cancelar_viaje, Rol.ADMINISTRADOR, "cancelar_viaje?id={id}"),
    ASIGNAR(R.string.t06_asignar, Rol.ADMINISTRADOR, "asignar?id={id}"),
    CONDUCTORES(R.string.t06_conductores, Rol.ADMINISTRADOR, "conductores"),
    REGISTRAR_CONDUCTOR(R.string.t06_registrar_conductor, Rol.ADMINISTRADOR, "registrar_conductor"),
    DETALLE_CONDUCTOR(R.string.t06_detalle_conductor, Rol.ADMINISTRADOR, "detalle_conductor?id={id}"),
    VEHICULOS(R.string.t06_vehiculos, Rol.ADMINISTRADOR, "vehiculos"),
    REGISTRAR_VEHICULO(R.string.t06_registrar_vehiculo, Rol.ADMINISTRADOR, "registrar_vehiculo"),
    DETALLE_VEHICULO(R.string.t06_detalle_vehiculo, Rol.ADMINISTRADOR, "detalle_vehiculo?id={id}"),
    HABILITACIONES(R.string.t06_habilitaciones, Rol.ADMINISTRADOR, "habilitaciones?id={id}"),
    REGISTRAR_PAGO(R.string.t06_registrar_pago, Rol.ADMINISTRADOR, "registrar_pago?id={id}"),
    DETALLE_PAGO(R.string.t06_detalle_pago, null, "detalle_pago?id={id}"),
    PERFIL_ADMIN(R.string.t06_perfil_admin, Rol.ADMINISTRADOR, "perfil_admin"),
    REGISTRAR_ADMIN(R.string.t06_registrar_admin, Rol.ADMINISTRADOR, "registrar_admin"),
    NOTIFICACION(R.string.t06_notificacion, null, "notificacion"),
    MIS_VIAJES(R.string.t06_mis_viajes, Rol.CONDUCTOR, "mis_viajes"),
    DETALLE_VIAJE(R.string.t06_detalle_viaje, Rol.CONDUCTOR, "detalle_viaje?id={id}"),
    ANTES_VENTANA(R.string.t06_antes_ventana, Rol.CONDUCTOR, "antes_ventana?id={id}"),
    CONFIRMAR_INICIO(R.string.t06_confirmar_inicio, Rol.CONDUCTOR, "confirmar_inicio?id={id}"),
    SIN_CONEXION(R.string.t06_sin_conexion, Rol.CONDUCTOR, "sin_conexion?id={id}"),
    CONFLICTO(R.string.t06_conflicto, Rol.CONDUCTOR, "conflicto?id={id}"),
    NAVEGAR(R.string.t06_navegar, Rol.CONDUCTOR, "navegar?id={id}"),
    PERFIL_CONDUCTOR(R.string.t06_perfil_conductor, Rol.CONDUCTOR, "perfil_conductor");

    val publica: Boolean get() = ordinal <= ABORDADO.ordinal && this != CAMBIO_OBLIGATORIO && this != RECUPERAR

    val conId: Boolean get() = ruta.endsWith(SUFIJO_ID)

    val base: String get() = ruta.removeSuffix(SUFIJO_ID)

    fun hacia(id: String = ""): String = if (conId && id.isNotEmpty()) "$base?id=${Uri.encode(id)}" else base

    companion object {
        const val ARGUMENTO_ID = "id"
        private const val SUFIJO_ID = "?id={id}"

        fun deRuta(ruta: String?): PantallaDemo? = entries.firstOrNull { it.ruta == ruta }
    }
}

data class Destino(val pantalla: PantallaDemo, val id: String = "") {
    fun ruta(): String = pantalla.hacia(id)
}
