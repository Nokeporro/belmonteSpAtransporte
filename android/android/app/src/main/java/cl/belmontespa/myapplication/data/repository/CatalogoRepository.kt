package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.Resultado

/** Bancos, medios de pago, tipos de recibo, regiones y comunas (`GET /catalogos`). */
interface CatalogoRepository {
    suspend fun obtenerCatalogos(): Resultado<Catalogos>
}
