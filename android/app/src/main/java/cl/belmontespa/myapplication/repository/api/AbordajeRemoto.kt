package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.AbordajeRequest
import java.time.LocalDateTime

internal class AbordajeRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)

    suspend fun registrarAbordaje(codigo: CodigoAbordaje, tipoDocumento: TipoDocumento, dni: String): Resultado<LocalDateTime> = llamar {
        api.abordar(AbordajeRequest(codigo.patente, codigo.codigo, tipoDocumento.name, dni.trim())).fhAbordaje
    }
}
