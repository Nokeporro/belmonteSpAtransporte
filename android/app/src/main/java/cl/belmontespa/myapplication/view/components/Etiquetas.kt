package cl.belmontespa.myapplication.view.components

import androidx.annotation.StringRes
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Retraso
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.model.TipoRecibo


@get:StringRes
val EstadoViaje.etiqueta: Int
    get() = when (this) {
        EstadoViaje.PENDIENTE -> R.string.estado_viaje_pendiente
        EstadoViaje.ASIGNADO -> R.string.estado_viaje_asignado
        EstadoViaje.EN_CURSO -> R.string.estado_viaje_en_curso
        EstadoViaje.FINALIZADO -> R.string.estado_viaje_finalizado
        EstadoViaje.CANCELADO -> R.string.estado_viaje_cancelado
    }

val EstadoViaje.estiloChip: EstiloChip
    get() = when (this) {
        EstadoViaje.PENDIENTE -> EstiloChip.NEUTRO
        EstadoViaje.ASIGNADO -> EstiloChip.INFORMACION
        EstadoViaje.EN_CURSO -> EstiloChip.LLENO
        EstadoViaje.FINALIZADO -> EstiloChip.TENUE
        EstadoViaje.CANCELADO -> EstiloChip.PELIGRO
    }

@get:StringRes
val EstadoPago.etiqueta: Int
    get() = when (this) {
        EstadoPago.PENDIENTE -> R.string.estado_pago_pendiente
        EstadoPago.PAGADO -> R.string.estado_pago_pagado
        EstadoPago.RECHAZADO -> R.string.estado_pago_rechazado
        EstadoPago.ANULADO -> R.string.estado_pago_anulado
        EstadoPago.REEMBOLSADO -> R.string.estado_pago_reembolsado
    }

val EstadoPago.estiloChip: EstiloChip
    get() = when (this) {
        EstadoPago.PENDIENTE -> EstiloChip.NEUTRO
        EstadoPago.PAGADO -> EstiloChip.CORRECTO
        EstadoPago.RECHAZADO -> EstiloChip.PELIGRO
        EstadoPago.ANULADO -> EstiloChip.PELIGRO_BORDE
        EstadoPago.REEMBOLSADO -> EstiloChip.AVISO
    }

@get:StringRes
val EstadoConductor.etiqueta: Int
    get() = when (this) {
        EstadoConductor.DISPONIBLE -> R.string.estado_conductor_disponible
        EstadoConductor.OCUPADO -> R.string.estado_conductor_ocupado
    }

val EstadoConductor.estiloChip: EstiloChip
    get() = when (this) {
        EstadoConductor.DISPONIBLE -> EstiloChip.CORRECTO
        EstadoConductor.OCUPADO -> EstiloChip.LLENO
    }

@get:StringRes
val EstadoMovil.etiqueta: Int
    get() = when (this) {
        EstadoMovil.DISPONIBLE -> R.string.estado_movil_disponible
        EstadoMovil.EN_USO -> R.string.estado_movil_en_uso
    }

val EstadoMovil.estiloChip: EstiloChip
    get() = when (this) {
        EstadoMovil.DISPONIBLE -> EstiloChip.CORRECTO
        EstadoMovil.EN_USO -> EstiloChip.LLENO
    }

@get:StringRes
val EstadoSolicitante.etiqueta: Int
    get() = when (this) {
        EstadoSolicitante.PENDIENTE -> R.string.estado_solicitante_pendiente
        EstadoSolicitante.APROBADO -> R.string.estado_solicitante_aprobado
        EstadoSolicitante.RECHAZADO -> R.string.estado_solicitante_rechazado
    }

val EstadoSolicitante.estiloChip: EstiloChip
    get() = when (this) {
        EstadoSolicitante.PENDIENTE -> EstiloChip.AVISO
        EstadoSolicitante.APROBADO -> EstiloChip.CORRECTO
        EstadoSolicitante.RECHAZADO -> EstiloChip.PELIGRO
    }

@get:StringRes
val MedioPago.etiqueta: Int
    get() = when (this) {
        MedioPago.TRANSFERENCIA -> R.string.medio_pago_transferencia
        MedioPago.TARJETA_DEBITO -> R.string.medio_pago_tarjeta_debito
        MedioPago.TARJETA_CREDITO -> R.string.medio_pago_tarjeta_credito
        MedioPago.EFECTIVO -> R.string.medio_pago_efectivo
    }

@get:StringRes
val TipoRecibo.etiqueta: Int
    get() = when (this) {
        TipoRecibo.BOLETA -> R.string.tipo_recibo_boleta
        TipoRecibo.FACTURA -> R.string.tipo_recibo_factura
    }

@get:StringRes
val TipoDocumento.etiqueta: Int
    get() = when (this) {
        TipoDocumento.RUT -> R.string.tipo_documento_rut
        TipoDocumento.PASAPORTE -> R.string.tipo_documento_pasaporte
    }

@get:StringRes
val Rol.etiqueta: Int
    get() = when (this) {
        Rol.SOLICITANTE -> R.string.rol_solicitante
        Rol.ADMINISTRADOR -> R.string.rol_administrador
        Rol.CONDUCTOR -> R.string.rol_conductor
    }

@StringRes
fun EstadoConductor.etiqueta(activo: Boolean): Int =
    if (activo) etiqueta else EtiquetaCalculada.INACTIVO.etiqueta

fun EstadoConductor.estiloChip(activo: Boolean): EstiloChip =
    if (activo) estiloChip else EtiquetaCalculada.INACTIVO.estiloChip

@StringRes
fun EstadoMovil.etiqueta(activo: Boolean): Int =
    if (activo) etiqueta else EtiquetaCalculada.INACTIVO.etiqueta

fun EstadoMovil.estiloChip(activo: Boolean): EstiloChip =
    if (activo) estiloChip else EtiquetaCalculada.INACTIVO.estiloChip

enum class EtiquetaCalculada(
    @param:StringRes
    @get:StringRes
    val etiqueta: Int,
    val estiloChip: EstiloChip,
) {
    ATRASADO(R.string.etiqueta_atrasado, EstiloChip.AVISO),
    VENCIDO(R.string.etiqueta_vencido, EstiloChip.PELIGRO_BORDE),
    PENDIENTE_SINCRONIZAR(
        R.string.etiqueta_pendiente_sincronizar,
        EstiloChip.AVISO,
    ),
    ABORDADO(R.string.etiqueta_abordado, EstiloChip.CORRECTO),
    POR_ABORDAR(R.string.etiqueta_por_abordar, EstiloChip.NEUTRO),

    INACTIVO(R.string.etiqueta_inactivo, EstiloChip.TENUE),
}

val Retraso.etiquetaCalculada: EtiquetaCalculada
    get() = when (this) {
        Retraso.ATRASADO -> EtiquetaCalculada.ATRASADO
        Retraso.VENCIDO -> EtiquetaCalculada.VENCIDO
    }
