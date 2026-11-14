package cl.belmontespa.myapplication.repository.api

import androidx.room.withTransaction
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.ViajeResponse
import cl.belmontespa.myapplication.repository.dto.aGuardable
import retrofit2.HttpException

internal class CacheViajes(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)

    suspend fun guardar(viajes: List<ViajeResponse>) {
        val descargadoEn = local.clock.millis()
        local.db.withTransaction {
            for (viaje in viajes) {
                val conocidos = viaje.pasajeros
                    .filter { it.nombre == null }
                    .mapNotNull { pasajero -> local.db.pasajeroDao().obtener(pasajero.dniHash)?.let { pasajero.dniHash to it.nombre } }
                    .toMap()
                val guardable = viaje.aGuardable(descargadoEn, conocidos)
                local.db.viajeDao().guardarConPasajeros(guardable.viaje, guardable.pasajeros, guardable.detalles)
            }
        }
    }

    suspend fun refrescar(folio: String): Resultado<Unit> = intentar {
        try {
            guardar(listOf(api.viaje(folio)))
        } catch (e: HttpException) {
            if (e.code() == 403 || e.code() == 404) {
                local.db.withTransaction {
                    local.db.operacionPendienteDao().borrarDelViaje(folio)
                    local.db.viajeDao().borrar(folio)
                }
            }
            throw e
        }
        Resultado.Exito(Unit)
    }
}
