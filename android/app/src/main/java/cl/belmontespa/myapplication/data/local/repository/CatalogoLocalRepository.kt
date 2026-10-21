package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.DatosDemo
import cl.belmontespa.myapplication.data.repository.CatalogoRepository
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoRecibo
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/** Catálogos de la build demo: constantes de [DatosDemo], ordenados por nombre como `GET /catalogos`. */
class CatalogoLocalRepository @Inject constructor() : CatalogoRepository {

    override suspend fun obtenerCatalogos(): Resultado<Catalogos> {
        // Orden en español: sin distinguir mayúsculas ni tildes, y la Ñ después de la N.
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
