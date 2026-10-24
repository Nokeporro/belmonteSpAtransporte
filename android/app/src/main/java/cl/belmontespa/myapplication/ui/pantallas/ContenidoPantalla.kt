package cl.belmontespa.myapplication.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.ui.components.*
import cl.belmontespa.myapplication.ui.navigation.PantallaDemo
import cl.belmontespa.myapplication.ui.theme.EstilosMono
import cl.belmontespa.myapplication.util.Formatos
import java.time.LocalDateTime
import kotlinx.coroutines.delay

// Las variantes comparten datos y controles, pero conservan sus rutas y títulos de los mockups.
@Composable
internal fun ContenidoPantalla(c: ContextoPantalla) = with(c) {
    when (pantalla) {
        PantallaDemo.REGISTRO -> {
            Campo("nombre", R.string.campo_nombre); Campo("correo", R.string.campo_correo, KeyboardType.Email)
            Campo("telefono", R.string.campo_telefono, KeyboardType.Phone); Campo("password", R.string.campo_password, KeyboardType.Password)
            Campo("razonSocial", R.string.campo_razon_social); Campo("rutEmpresa", R.string.campo_rut_empresa)
            Accion(R.string.login_crear_cuenta)
            if (estado.aviso in listOf(R.string.aviso_registro_aprobado, R.string.aviso_registro_pendiente)) Accion(R.string.login_iniciar_sesion, { navegar(PantallaDemo.LOGIN, vm.valor("correo")) }, TipoBoton.SECUNDARIO)
        }
        PantallaDemo.RECUPERAR -> { Campo("correo", R.string.campo_correo, KeyboardType.Email); Accion(R.string.login_recuperar_password) }
        PantallaDemo.CAMBIO_OBLIGATORIO, PantallaDemo.CAMBIAR_PASSWORD -> {
            if (pantalla == PantallaDemo.CAMBIO_OBLIGATORIO) Aviso(stringResource(R.string.aviso_cambio_password_obligatorio), TonoAviso.AVISO)
            Campo("passwordActual", R.string.campo_password_actual, KeyboardType.Password)
            Campo("passwordNueva", R.string.campo_password_nueva, KeyboardType.Password)
            Campo("confirmacion", R.string.campo_confirmacion, KeyboardType.Password)
            Accion(R.string.accion_guardar)
            if (pantalla == PantallaDemo.CAMBIO_OBLIGATORIO) Accion(R.string.accion_cerrar_sesion, cerrarSesion, TipoBoton.SECUNDARIO)
        }
        PantallaDemo.SESION_EXPIRADA -> { Aviso(stringResource(R.string.error_sesion_terminada), TonoAviso.AVISO); Enlace(R.string.login_iniciar_sesion, PantallaDemo.LOGIN, "") }
        PantallaDemo.MIS_SOLICITUDES -> ListaViajes()
        // «Mis viajes» la dibuja MisViajesScreen, desde PantallaT06 (HU07).
        PantallaDemo.MIS_VIAJES -> Unit
        // «Solicitudes» del Administrador la dibuja SolicitudesScreen, desde PantallaT06 (HU04).
        PantallaDemo.SOLICITUDES -> Unit
        // «Asignar conductor y vehículo» la dibuja AsignarScreen, desde PantallaT06 (HU05).
        PantallaDemo.ASIGNAR -> Unit
        PantallaDemo.DETALLE_SOLICITUD, PantallaDemo.SOLICITUD_SIN_PASAJEROS, PantallaDemo.DETALLE_ADMIN,
        PantallaDemo.SIN_PASAJEROS_ADMIN, PantallaDemo.SIN_CONEXION, PantallaDemo.CONFLICTO -> DetalleViaje()
        // El detalle del Conductor y «Antes de la ventana» los dibuja DetalleViajeScreen, desde PantallaT06 (HU08).
        PantallaDemo.DETALLE_VIAJE, PantallaDemo.ANTES_VENTANA -> Unit
        // «Confirmar inicio» la dibuja ConfirmarInicioScreen, desde PantallaT06 (HU09).
        PantallaDemo.CONFIRMAR_INICIO -> Unit
        // «Abrir navegación» es el detalle con la hoja «Abrir con»: lo dibuja DetalleViajeScreen, desde PantallaT06 (HU10).
        PantallaDemo.NAVEGAR -> Unit
        PantallaDemo.NUEVA_SOLICITUD -> NuevaSolicitudFormulario()
        PantallaDemo.AGREGAR_PASAJERO -> { FormularioPasajero(false); Accion(R.string.accion_agregar_pasajero) }
        PantallaDemo.CONFIRMAR_CANCELACION -> ConfirmarCancelacionSolicitud()
        // «Cancelar viaje» del Administrador la dibuja CancelarViajeScreen, desde PantallaT06 (HU24).
        PantallaDemo.CANCELAR_VIAJE -> Unit
        PantallaDemo.SOLICITUD_CANCELADA -> {
            if (estado.viaje?.estado == EstadoViaje.CANCELADO) {
                Aviso(stringResource(R.string.aviso_solicitud_cancelada), TonoAviso.INFORMACION)
                PagoDeCancelacion(confirmando = false)
            }
            else Aviso(stringResource(R.string.error_no_encontrado), TonoAviso.AVISO)
            Dato(R.string.campo_folio, estado.viaje?.folio)
            Enlace(R.string.accion_volver_solicitudes, PantallaDemo.MIS_SOLICITUDES, "")
        }
        PantallaDemo.PERFIL_SOLICITANTE, PantallaDemo.PERFIL_ADMIN, PantallaDemo.PERFIL_CONDUCTOR -> Perfil()
        PantallaDemo.CUENTAS_PENDIENTES -> {
            if (estado.pendientes.isEmpty()) Text(stringResource(R.string.vacio_cuentas_por_aprobar))
            estado.pendientes.forEach { cuenta -> Bloque(cuenta.nombre) {
                Dato(R.string.campo_correo, cuenta.correo); Dato(R.string.campo_empresa, cuenta.razonSocial)
                Dato(R.string.campo_rut_empresa, Formatos.rut(cuenta.rutEmpresa))
                Accion(R.string.accion_aprobar, { vm.ejecutar("aprobar", cuenta.correo) })
                Accion(R.string.accion_rechazar, { vm.ejecutar("rechazarCuenta", cuenta.correo) }, TipoBoton.PELIGRO)
            } }
        }
        PantallaDemo.CONDUCTORES -> {
            Enlace(R.string.t06_registrar_conductor, PantallaDemo.REGISTRAR_CONDUCTOR, "")
            if (estado.conductores.isEmpty()) Text(stringResource(R.string.vacio_conductores_disponibles))
            estado.conductores.forEach { conductor -> Bloque(conductor.nombre) {
                Dato(R.string.campo_correo, conductor.correo)
                EstadoChip(stringResource(conductor.estado.etiqueta(conductor.activo)), conductor.estado.estiloChip(conductor.activo))
                ResumenAgendaConductor(conductor)
                Enlace(R.string.accion_ver_detalle, PantallaDemo.DETALLE_CONDUCTOR, conductor.correo)
            } }
        }
        PantallaDemo.REGISTRAR_CONDUCTOR, PantallaDemo.REGISTRAR_ADMIN -> {
            Campo("nombre", R.string.campo_nombre); Campo("correo", R.string.campo_correo, KeyboardType.Email)
            Campo("telefono", R.string.campo_telefono, KeyboardType.Phone); Campo("password", R.string.campo_password, KeyboardType.Password)
            if (pantalla == PantallaDemo.REGISTRAR_CONDUCTOR) Text(stringResource(R.string.conductor_alta_ayuda))
            Accion(R.string.accion_guardar)
        }
        PantallaDemo.DETALLE_CONDUCTOR -> estado.conductor?.let { conductor ->
            Dato(R.string.campo_correo, conductor.correo)
            EstadoChip(stringResource(conductor.estado.etiqueta(conductor.activo)), conductor.estado.estiloChip(conductor.activo))
            ResumenAgendaConductor(conductor)
            Dato(R.string.campo_fecha_registro, Formatos.fechaHoraDetalle(conductor.fechaRegistro))
            Campo("nombre", R.string.campo_nombre); Campo("telefono", R.string.campo_telefono, KeyboardType.Phone)
            Campo("password", R.string.campo_password_nueva, KeyboardType.Password)
            Text(stringResource(R.string.conductor_password_opcional))
            Accion(R.string.accion_guardar)
            if (estado.habilitaciones.isEmpty()) Text(stringResource(R.string.conductor_sin_habilitaciones))
            HabilitacionesResumen(); Enlace(R.string.accion_habilitaciones, PantallaDemo.HABILITACIONES, "conductor:${conductor.correo}")
            Accion(if (conductor.activo) R.string.accion_desactivar else R.string.accion_reactivar, { vm.ejecutar("activoConductor") }, TipoBoton.SECUNDARIO)
        }
        PantallaDemo.VEHICULOS -> {
            Enlace(R.string.t06_registrar_vehiculo, PantallaDemo.REGISTRAR_VEHICULO, "")
            if (estado.moviles.isEmpty()) Text(stringResource(R.string.aviso_sin_resultados))
            estado.moviles.forEach { movil -> Bloque(movil.patente) {
                Dato(R.string.campo_capacidad, movil.capacidad.toString())
                EstadoChip(stringResource(movil.estado.etiqueta(movil.activo)), movil.estado.estiloChip(movil.activo))
                Enlace(R.string.accion_ver_detalle, PantallaDemo.DETALLE_VEHICULO, movil.patente)
            } }
        }
        PantallaDemo.REGISTRAR_VEHICULO -> {
            Text(stringResource(R.string.vehiculo_registro_indicacion))
            Campo("patente", R.string.campo_patente)
            Campo("capacidad", R.string.campo_capacidad, KeyboardType.Number)
            Campo("gps", R.string.campo_gps, KeyboardType.Number)
            Accion(R.string.accion_guardar)
        }
        PantallaDemo.DETALLE_VEHICULO -> estado.movil?.let { movil ->
            Dato(R.string.campo_patente, movil.patente); Dato(R.string.campo_capacidad, movil.capacidad.toString())
            Dato(R.string.campo_gps, movil.gpsSerial?.toString() ?: stringResource(R.string.vehiculo_sin_gps))
            EstadoChip(stringResource(movil.estado.etiqueta(movil.activo)), movil.estado.estiloChip(movil.activo))
            HabilitacionesResumen(); Enlace(R.string.accion_habilitaciones, PantallaDemo.HABILITACIONES, "vehiculo:${movil.patente}")
            QrVehiculo(movil)
            Accion(if (movil.activo) R.string.accion_desactivar else R.string.accion_reactivar, { vm.ejecutar("activoMovil") }, TipoBoton.SECUNDARIO)
        }
        PantallaDemo.HABILITACIONES -> {
            Text(stringResource(R.string.habilitaciones_ayuda))
            Selector("conductor", R.string.campo_conductor, estado.conductores.filter { it.activo }.map { Opcion(it.correo, "${it.nombre} (${it.correo})") })
            Selector("vehiculo", R.string.campo_vehiculo, estado.moviles.filter { it.activo }.map { Opcion(it.patente, it.patente) })
            Accion(R.string.accion_habilitar, habilitada = vm.valor("conductor").isNotBlank() && vm.valor("vehiculo").isNotBlank())
            Selector("filtroVehiculo", R.string.habilitaciones_filtro_vehiculo,
                listOf(Opcion("", stringResource(R.string.filtro_todos))) + estado.moviles.map { Opcion(it.patente, it.patente) }, vm::actualizar)
            Selector("filtroConductor", R.string.habilitaciones_filtro_conductor,
                listOf(Opcion("", stringResource(R.string.filtro_todos))) + estado.conductores.map { Opcion(it.correo, "${it.nombre} (${it.correo})") }, vm::actualizar)
            if (estado.habilitaciones.isEmpty()) Text(stringResource(R.string.habilitaciones_sin_resultados))
            HabilitacionesResumen()
        }
        PantallaDemo.REGISTRAR_PAGO -> {
            Dato(R.string.campo_folio, estado.viaje?.folio)
            Campo("monto", R.string.campo_monto, KeyboardType.Number)
            Selector("banco", R.string.campo_banco, estado.catalogos?.bancos.orEmpty().map { Opcion(it.codSbif.toString(), it.nombre) })
            Selector("medioPago", R.string.campo_medio_pago, estado.catalogos?.mediosPago.orEmpty().map { Opcion(it.name, stringResource(it.etiqueta)) })
            Selector("tipoRecibo", R.string.campo_tipo_recibo, estado.catalogos?.tiposRecibo.orEmpty().map { Opcion(it.name, stringResource(it.etiqueta)) })
            if (estado.viaje?.let { it.estado == EstadoViaje.CANCELADO || it.hashPago != null } == true) Text(stringResource(R.string.pago_registro_no_disponible))
            Accion(R.string.accion_registrar_pago, habilitada = estado.error == null && estado.catalogos != null &&
                estado.viaje?.let { it.estado != EstadoViaje.CANCELADO && it.hashPago == null } == true)
        }
        PantallaDemo.DETALLE_PAGO -> estado.pago?.let { pago ->
            Dato(R.string.campo_folio, pago.folio); Dato(R.string.campo_monto, Formatos.monto(pago.monto))
            EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
            Dato(R.string.campo_banco, pago.banco); Dato(R.string.campo_medio_pago, stringResource(pago.medioPago.etiqueta)); Dato(R.string.campo_tipo_recibo, stringResource(pago.tipoRecibo.etiqueta))
            Dato(R.string.pago_fecha_label, pago.fechaPago?.let(Formatos::fechaHoraDetalle) ?: stringResource(R.string.pago_fecha_pendiente))
            pago.montoReembolsado?.let { Dato(R.string.campo_reembolso, Formatos.monto(it)) }
            if (estado.cuenta?.rol == Rol.ADMINISTRADOR) {
                if (pago.estado == EstadoPago.PENDIENTE) {
                    Accion(R.string.accion_pagar, { vm.ejecutar("pago", "PAGADO") })
                    Accion(R.string.accion_rechazar, { vm.ejecutar("pago", "RECHAZADO") }, TipoBoton.SECUNDARIO)
                }
                if (pago.estado in listOf(EstadoPago.PENDIENTE, EstadoPago.PAGADO)) Accion(R.string.accion_anular, { vm.ejecutar("pago", "ANULADO") }, TipoBoton.PELIGRO)
            }
        }
        // El abordaje del Pasajero lo dibujan AbordarScreen, ConfirmarAbordajeScreen y AbordadoScreen, desde PantallaT06 (HU29).
        PantallaDemo.ABORDAR, PantallaDemo.ESCANEAR, PantallaDemo.CONFIRMAR_ABORDAJE, PantallaDemo.ABORDADO -> Unit
        PantallaDemo.NOTIFICACION -> {
            Aviso(stringResource(R.string.aviso_notificacion), TonoAviso.INFORMACION)
            estado.viajes.forEach { viaje -> Bloque(stringResource(R.string.notificacion_nuevo_viaje)) { Dato(R.string.campo_folio, viaje.folio); Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraDetalle(viaje.fhAgenda)); Enlace(R.string.accion_ver_viaje, detallePara(estado.cuenta?.rol), viaje.folio) } }
            // Los escenarios son explícitos y solo existen en la build demo.
            // No se presentan como notificaciones push recibidas del servidor.
            if (BuildConfig.MODO_LOCAL && estado.cuenta?.rol == Rol.CONDUCTOR) {
                val viaje = estado.viajes.firstOrNull()
                if (viaje != null) {
                    Enlace(R.string.t06_sin_conexion, PantallaDemo.SIN_CONEXION, viaje.folio)
                    Enlace(R.string.t06_conflicto, PantallaDemo.CONFLICTO, viaje.folio)
                }
            }
        }
        PantallaDemo.INICIO -> Text(stringResource(R.string.inicio_comprobando_sesion))
        PantallaDemo.LOGIN, PantallaDemo.LOGIN_DEMO -> Enlace(R.string.login_iniciar_sesion, PantallaDemo.LOGIN, "")
    }
}

internal fun detallePara(rol: Rol?) = when(rol) {
    Rol.ADMINISTRADOR -> PantallaDemo.DETALLE_ADMIN
    Rol.CONDUCTOR -> PantallaDemo.DETALLE_VIAJE
    else -> PantallaDemo.DETALLE_SOLICITUD
}

@Composable
private fun ContextoPantalla.ListaViajes() {
    Enlace(R.string.accion_nueva_solicitud, PantallaDemo.NUEVA_SOLICITUD, "")
    Selector("estado", R.string.campo_estado, listOf(Opcion("TODOS", stringResource(R.string.filtro_todos))) + EstadoViaje.entries.map { Opcion(it.name, stringResource(it.etiqueta)) }, alElegir = vm::actualizar)
    Accion(R.string.accion_actualizar, vm::actualizar, TipoBoton.SECUNDARIO)
    if (estado.viajes.isEmpty()) Text(stringResource(R.string.vacio_mis_solicitudes))
    estado.viajes.forEach { viaje -> Bloque(viaje.folio) {
        EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip)
        Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraLista(viaje.fhAgenda))
        Text(viaje.origen.direccion + " → " + viaje.destino.direccion)
        Text(pluralStringResource(R.plurals.solicitud_cantidad_pasajeros, viaje.cantidadPasajeros, viaje.cantidadPasajeros))
        if (pantalla == PantallaDemo.MIS_SOLICITUDES && viaje.pasajeros.isNotEmpty()) Text(viaje.pasajeros.joinToString { it.nombre })
        val detalle = when {
            viaje.cantidadPasajeros == 0 && estado.cuenta?.rol == Rol.ADMINISTRADOR -> PantallaDemo.SIN_PASAJEROS_ADMIN
            viaje.cantidadPasajeros == 0 && estado.cuenta?.rol == Rol.SOLICITANTE -> PantallaDemo.SOLICITUD_SIN_PASAJEROS
            else -> detallePara(estado.cuenta?.rol)
        }
        Enlace(R.string.accion_ver_detalle, detalle, viaje.folio)
    } }
}

@Composable
private fun ContextoPantalla.DetalleViaje() {
    val viaje = estado.viaje ?: return
    val ahora = HoraActual()
    val editable = viaje.estado in listOf(EstadoViaje.PENDIENTE, EstadoViaje.ASIGNADO)
    val vencido = editable && ahora.isAfter(viaje.fhAgenda.plusHours(1))
    if (pantalla == PantallaDemo.SIN_CONEXION) { BannerSinConexion(); EstadoChip(stringResource(R.string.etiqueta_pendiente_sincronizar), EstiloChip.AVISO); Aviso(stringResource(R.string.aviso_sincronizacion_demo), TonoAviso.INFORMACION) }
    if (pantalla == PantallaDemo.CONFLICTO) {
        Aviso(stringResource(R.string.aviso_escenario_conflicto), TonoAviso.INFORMACION)
        Enlace(R.string.accion_descartar_cambio, PantallaDemo.DETALLE_VIAJE)
        Aviso(stringResource(R.string.aviso_cambio_conflicto), TonoAviso.PELIGRO); Accion(R.string.accion_actualizar, vm::actualizar, TipoBoton.SECUNDARIO) }
    Dato(R.string.campo_folio, viaje.folio)
    EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip)
    if (editable && ahora.isAfter(viaje.fhAgenda)) EstadoChip(stringResource(if (vencido) R.string.etiqueta_vencido else R.string.etiqueta_atrasado), if (vencido) EstiloChip.PELIGRO_BORDE else EstiloChip.AVISO)
    Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraDetalle(viaje.fhAgenda))
    Dato(R.string.campo_origen, viaje.origen.direccion); Dato(R.string.campo_destino, viaje.destino.direccion)
    Dato(R.string.campo_nombre, viaje.nombreSolicitante); Dato(R.string.campo_empresa, viaje.razonSocial)
    Dato(R.string.campo_telefono, viaje.telefonoSolicitante?.let(Formatos::telefono))
    Dato(R.string.campo_vuelo, viaje.numeroVuelo); Dato(R.string.campo_conductor, viaje.nombreConductor); Dato(R.string.campo_patente, viaje.patenteMovil)
    // El Administrador y el Conductor ven a los pasajeros como arqueo (README, RF29), solo en los estados que lo tienen.
    val veArqueo = estado.cuenta?.rol == Rol.ADMINISTRADOR || estado.cuenta?.rol == Rol.CONDUCTOR
    val arqueo = viaje.arqueo().takeIf { veArqueo }
    if (viaje.pasajeros.isEmpty()) Aviso(stringResource(R.string.aviso_faltan_pasajeros), TonoAviso.AVISO)
    else if (arqueo != null) ArqueoDeAbordaje(arqueo, viaje.pasajeros)
    else Bloque(stringResource(R.string.campo_pasajeros)) { viaje.pasajeros.forEach { pasajero ->
        Text(pasajero.nombre)
        if (!veArqueo) EstadoChip(stringResource(if (pasajero.fhAbordaje != null) R.string.etiqueta_abordado else R.string.etiqueta_por_abordar), if (pasajero.fhAbordaje != null) EstiloChip.CORRECTO else EstiloChip.NEUTRO)
    } }
    // El Administrador ve el pago del viaje, también su resultado tras cancelar (README: motivo, pago y arqueo).
    if (estado.cuenta?.rol == Rol.ADMINISTRADOR) estado.pago?.let { PagoDelViaje(it) }
    when (estado.cuenta?.rol) {
        Rol.SOLICITANTE -> {
            if (editable) Enlace(R.string.accion_agregar_pasajero, PantallaDemo.AGREGAR_PASAJERO)
            if (viaje.cancelablePorSolicitante(ahora)) Enlace(R.string.accion_cancelar_solicitud, PantallaDemo.CONFIRMAR_CANCELACION)
            viaje.hashPago?.let { Enlace(R.string.campo_pago, PantallaDemo.DETALLE_PAGO, it) }
        }
        Rol.ADMINISTRADOR -> {
            if (editable && !vencido && viaje.cantidadPasajeros > 0) Enlace(if (viaje.estado == EstadoViaje.ASIGNADO) R.string.accion_reasignar else R.string.accion_asignar, PantallaDemo.ASIGNAR)
            if (editable) Enlace(R.string.t06_cancelar_viaje, PantallaDemo.CANCELAR_VIAJE)
            if (viaje.hashPago == null && viaje.estado != EstadoViaje.CANCELADO) Enlace(R.string.accion_registrar_pago, PantallaDemo.REGISTRAR_PAGO)
            viaje.hashPago?.let { Enlace(R.string.campo_pago, PantallaDemo.DETALLE_PAGO, it) }
        }
        Rol.CONDUCTOR -> {
            if (viaje.estado in listOf(EstadoViaje.ASIGNADO, EstadoViaje.EN_CURSO)) {
                Enlace(R.string.accion_navegar, PantallaDemo.NAVEGAR)
                if (ahora.isBefore(viaje.fhAgenda.minusHours(1))) Aviso(stringResource(R.string.iniciar_disponible_desde, Formatos.hora(viaje.fhAgenda.minusHours(1))), TonoAviso.INFORMACION)
                if (vencido) Aviso(stringResource(R.string.iniciar_vencido), TonoAviso.AVISO)
                Accion(R.string.accion_iniciar, { navegar(PantallaDemo.CONFIRMAR_INICIO, id) }, habilitada = viaje.estado == EstadoViaje.ASIGNADO && !vencido && !ahora.isBefore(viaje.fhAgenda.minusHours(1)), alto56 = true)
                var confirmarFin by remember { mutableStateOf(false) }
                Accion(R.string.accion_finalizar, { confirmarFin = true }, habilitada = viaje.estado == EstadoViaje.EN_CURSO, alto56 = true)
                if (confirmarFin) AlertDialog(onDismissRequest = { confirmarFin = false }, text = { Text(stringResource(R.string.aviso_finalizar)) }, confirmButton = { TextButton(onClick = { confirmarFin = false; vm.ejecutar("finalizar") }) { Text(stringResource(R.string.accion_confirmar)) } }, dismissButton = { TextButton(onClick = { confirmarFin = false }) { Text(stringResource(R.string.accion_cancelar)) } })
                if (viaje.estado == EstadoViaje.ASIGNADO) {
                    Campo("motivo", R.string.campo_motivo)
                    Accion(R.string.accion_rechazar, { vm.ejecutar("rechazarViaje") }, TipoBoton.PELIGRO, habilitada = vm.valor("motivo").isNotBlank(), alto56 = true)
                }
            }
        }
        null -> Unit
    }
    Dato(R.string.campo_motivo, viaje.motivo)
}

@Composable
private fun PagoDelViaje(pago: Pago) {
    Bloque(stringResource(R.string.campo_pago)) {
        EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
        Dato(R.string.campo_monto, Formatos.monto(pago.monto))
        pago.montoReembolsado?.let { Dato(R.string.campo_monto_reembolsado, Formatos.monto(it)) }
    }
}

@Composable
private fun ContextoPantalla.FormularioPasajero(borrador: Boolean) {
    Campo(if (borrador) "pasajeroNombre" else "nombre", R.string.campo_nombre)
    Selector("tipoDocumento", R.string.campo_tipo_documento, TipoDocumento.entries.map { Opcion(it.name, stringResource(it.etiqueta)) })
    Campo("documento", R.string.campo_documento)
}
@Composable
private fun ContextoPantalla.NuevaSolicitudFormulario() {
    FechaHora()
    SelectorPlaces("origen", R.string.campo_origen)
    SelectorPlaces("destino", R.string.campo_destino)
    Campo("vuelo", R.string.campo_vuelo)
    FormularioPasajero(true)
    Accion(R.string.accion_agregar_pasajero, vm::agregarPasajero, TipoBoton.SECUNDARIO)
    estado.pasajeros.forEachIndexed { i, p -> Bloque(p.nombre) {
        Accion(R.string.accion_editar, { vm.editarPasajero(i) }, TipoBoton.SECUNDARIO)
        Accion(R.string.accion_quitar, { vm.quitarPasajero(i) }, TipoBoton.SECUNDARIO)
    } }
    Accion(R.string.accion_guardar, habilitada = estado.pasajeros.isNotEmpty() && vm.valor("origen").isNotBlank() && vm.valor("destino").isNotBlank())
}
@Composable
private fun ContextoPantalla.Perfil() {
    estado.cuenta?.let { cuenta ->
        Dato(R.string.campo_nombre, cuenta.nombre); Dato(R.string.campo_correo, cuenta.correo)
        Dato(R.string.campo_telefono, cuenta.telefono?.let(Formatos::telefono))
        Dato(R.string.campo_empresa, cuenta.razonSocial); Dato(R.string.campo_rut_empresa, cuenta.rutEmpresa?.let(Formatos::rut))
        Dato(R.string.campo_registrado, Formatos.fechaHoraDetalle(cuenta.fechaRegistro))
        if (cuenta.rol != Rol.CONDUCTOR) Enlace(R.string.accion_cambiar_password, PantallaDemo.CAMBIAR_PASSWORD, "")
        if (cuenta.rol == Rol.SOLICITANTE) Enlace(R.string.login_abordar_viaje, PantallaDemo.ABORDAR, "")
        if (cuenta.rol == Rol.ADMINISTRADOR) { Enlace(R.string.t06_registrar_admin, PantallaDemo.REGISTRAR_ADMIN, ""); Enlace(R.string.t06_cuentas_pendientes, PantallaDemo.CUENTAS_PENDIENTES, "") }
        if (BuildConfig.MODO_LOCAL) Enlace(R.string.accion_ver_escenarios, PantallaDemo.NOTIFICACION, "")
    }
    Accion(R.string.accion_cerrar_sesion, cerrarSesion, TipoBoton.SECUNDARIO)
}
@Composable
private fun ContextoPantalla.HoraActual(): LocalDateTime {
    var ahora by remember(vm) { mutableStateOf(vm.ahora()) }
    LaunchedEffect(vm) {
        while (true) { delay(1_000); ahora = vm.ahora() }
    }
    return ahora
}
@Composable
private fun ContextoPantalla.ConfirmarCancelacionSolicitud() {
    val ahora = HoraActual()
    val viaje = estado.viaje
    viaje?.let {
        Dato(R.string.campo_folio, it.folio)
        Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraDetalle(it.fhAgenda))
    }
    val cancelable = viaje?.cancelablePorSolicitante(ahora) == true
    if (viaje != null && !cancelable) Aviso(stringResource(R.string.cancelacion_no_disponible), TonoAviso.AVISO)
    else Aviso(stringResource(R.string.aviso_cancelar_solicitud), TonoAviso.AVISO)
    PagoDeCancelacion(confirmando = true)
    Accion(R.string.accion_confirmar, tipo = TipoBoton.PELIGRO,
        habilitada = cancelable && estado.error == null && (viaje.hashPago == null || estado.pago != null))
    Accion(R.string.accion_cancelar, volver, TipoBoton.SECUNDARIO)
}
@Composable
private fun ContextoPantalla.PagoDeCancelacion(confirmando: Boolean) {
    val viaje = estado.viaje ?: return
    val pago = estado.pago
    if (pago == null) {
        if (viaje.hashPago == null) Text(stringResource(R.string.aviso_sin_pago))
        return
    }
    EstadoChip(stringResource(pago.estado.etiqueta), pago.estado.estiloChip)
    when {
        confirmando && pago.estado == EstadoPago.PAGADO -> Aviso(stringResource(
            R.string.cancelacion_reembolso_previsto, Formatos.monto(pago.monto / 2)), TonoAviso.INFORMACION)
        confirmando && pago.estado == EstadoPago.PENDIENTE -> Text(stringResource(R.string.cancelacion_anula_pago))
        !confirmando && pago.estado == EstadoPago.REEMBOLSADO ->
            pago.montoReembolsado?.let { Dato(R.string.campo_reembolso, Formatos.monto(it)) }
        !confirmando && pago.estado == EstadoPago.ANULADO -> Text(stringResource(R.string.cancelacion_pago_anulado))
    }
}
@Composable
private fun ResumenAgendaConductor(conductor: Conductor) {
    Text(pluralStringResource(R.plurals.asignar_viajes_asignados, conductor.viajesAsignados, conductor.viajesAsignados))
    conductor.folioEnCurso?.let { Text(stringResource(R.string.asignar_en_curso, it), style = EstilosMono.dato) }
}
@Composable
private fun ContextoPantalla.HabilitacionesResumen() {
    estado.habilitaciones.forEach { h -> Bloque(h.patente) {
        Dato(R.string.campo_conductor, h.nombreConductor)
        Dato(R.string.campo_correo, h.correoConductor)
        Dato(R.string.campo_capacidad, h.capacidad.toString())
        Text(stringResource(R.string.habilitaciones_estado_vehiculo))
        EstadoChip(stringResource(h.estadoMovil.etiqueta(h.activoMovil)), h.estadoMovil.estiloChip(h.activoMovil))
        Text(stringResource(R.string.habilitaciones_estado_conductor))
        EstadoChip(stringResource(h.estadoConductor.etiqueta(h.activoConductor)), h.estadoConductor.estiloChip(h.activoConductor))
    } }
}
