package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoRecibo
import java.text.Collator
import java.util.Locale

open class CatalogoRepository {

    open suspend fun obtenerCatalogos(): Resultado<Catalogos> {
        val orden = Collator.getInstance(Locale.forLanguageTag("es-CL"))
        return Resultado.Exito(
            Catalogos(
                bancos = DatosDemo.bancos.sortedWith(compareBy(orden) { it.nombre }),
                mediosPago = MedioPago.entries.sortedBy { it.name },
                tiposRecibo = TipoRecibo.entries.sortedBy { it.name },
                regiones = DatosDemo.regiones
                    .map { Region(it.nombre, it.comunas.sortedWith(orden)) }
                    .sortedWith(compareBy(orden) { it.nombre }),
            ),
        )
    }
}
