package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.database.DatosDemo
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.repository.api.CatalogoRemoto
import java.text.Collator
import java.util.Locale

open class CatalogoRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    private val remoto by lazy { CatalogoRemoto(local) }

    open suspend fun obtenerCatalogos(): Resultado<Catalogos> {
        if (!local.modoLocal) return remoto.obtenerCatalogos()
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
