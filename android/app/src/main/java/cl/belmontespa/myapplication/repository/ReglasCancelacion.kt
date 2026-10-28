package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.PagoEntity

internal fun PagoEntity.alCancelarElViaje(administrador: Boolean, vencido: Boolean, fijado: Int?): PagoEntity = when (estadoPago) {
    "PENDIENTE" -> copy(estadoPago = "ANULADO")
    "PAGADO" -> {
        val reembolso = when {
            !administrador -> monto / 2
            vencido -> requireNotNull(fijado) { "El monto a reembolsar debe validarse antes" }
            else -> monto
        }
        if (administrador && reembolso == 0) this else copy(estadoPago = "REEMBOLSADO", montoReembolsado = reembolso)
    }
    else -> this
}
