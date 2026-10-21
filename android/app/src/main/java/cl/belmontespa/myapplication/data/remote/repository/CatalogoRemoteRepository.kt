package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.CatalogoRepository
import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import javax.inject.Inject

/** Implementación remota; hasta T14 responde Sin conexión. */
class CatalogoRemoteRepository @Inject constructor() : CatalogoRepository {

    override suspend fun obtenerCatalogos(): Resultado<Catalogos> = Resultado.Fallo(ErrorApp.SinConexion)
}
