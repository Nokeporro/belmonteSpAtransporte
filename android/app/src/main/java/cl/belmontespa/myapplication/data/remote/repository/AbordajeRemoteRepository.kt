package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.AbordajeRepository
import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import java.time.LocalDateTime
import javax.inject.Inject

/** Implementación remota; hasta T14 responde Sin conexión. */
class AbordajeRemoteRepository @Inject constructor() : AbordajeRepository {

    override suspend fun registrarAbordaje(
        codigo: CodigoAbordaje,
        tipoDocumento: TipoDocumento,
        dni: String,
    ): Resultado<LocalDateTime> = Resultado.Fallo(ErrorApp.SinConexion)
}
