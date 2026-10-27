package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.PagoEntity

/**
 * Qué le pasa al pago cuando se cancela su viaje (README, Pago: P4 y P5). Es la misma regla que aplica el backend.
 *
 * - `PENDIENTE` queda `ANULADO` (P5). Los pagos terminales no cambian.
 * - `PAGADO` queda `REEMBOLSADO` con `montoReembolsado` (P4):
 *   - V5, el Solicitante: la mitad del monto, redondeada hacia abajo, aunque resulte 0.
 *   - V7, el Administrador: el total o, con el viaje vencido, el monto que fija (`fijado`, de 0 al total, ya
 *     validado); fuera de ese caso `fijado` se ignora. Con 0 el pago sigue `PAGADO`.
 */
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
