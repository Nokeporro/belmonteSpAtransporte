package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import java.time.LocalDateTime

/** Abordaje del pasajero con el QR del vehículo; no usa la sesión ni la cola. */
interface AbordajeRepository {
    /** Devuelve la hora de abordaje guardada (la misma si se repite). */
    suspend fun registrarAbordaje(
        codigo: CodigoAbordaje,
        tipoDocumento: TipoDocumento,
        dni: String,
    ): Resultado<LocalDateTime>
}
