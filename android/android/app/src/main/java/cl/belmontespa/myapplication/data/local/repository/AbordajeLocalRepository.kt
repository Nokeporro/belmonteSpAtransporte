package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.repository.AbordajeRepository
import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import java.time.LocalDateTime
import javax.inject.Inject

/** Abordaje de la build demo: aplica sobre Room las reglas de README (Abordaje). */
class AbordajeLocalRepository @Inject constructor() : AbordajeRepository {

    override suspend fun registrarAbordaje(
        codigo: CodigoAbordaje,
        tipoDocumento: TipoDocumento,
        dni: String,
    ): Resultado<LocalDateTime> = TODO("HU29")
}
