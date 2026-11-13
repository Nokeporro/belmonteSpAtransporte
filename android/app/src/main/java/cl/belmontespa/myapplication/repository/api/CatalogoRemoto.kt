package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.MotivoSinPermiso
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.aModelo
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.flow.first

internal class CatalogoRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)

    suspend fun obtenerCatalogos(): Resultado<Catalogos> = intentar {
        if (local.sesiones.estado.first() is EstadoSesion.Temporal) {
            return@intentar Resultado.Fallo(ErrorApp.SinPermiso(MotivoSinPermiso.CAMBIAR_PASSWORD))
        }
        val orden = Collator.getInstance(Locale.forLanguageTag("es-CL"))
        val catalogos = api.catalogos().aModelo()
        Resultado.Exito(
            catalogos.copy(
                bancos = catalogos.bancos.sortedWith(compareBy(orden) { it.nombre }),
                mediosPago = catalogos.mediosPago.sortedBy { it.name },
                tiposRecibo = catalogos.tiposRecibo.sortedBy { it.name },
                regiones = catalogos.regiones
                    .map { Region(it.nombre, it.comunas.sortedWith(orden)) }
                    .sortedWith(compareBy(orden) { it.nombre }),
            ),
        )
    }
}
