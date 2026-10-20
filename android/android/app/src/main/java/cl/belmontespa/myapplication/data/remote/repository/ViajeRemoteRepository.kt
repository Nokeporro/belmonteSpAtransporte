package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Implementación remota; hasta T14 no hay viajes guardados y toda acción responde Sin conexión. */
class ViajeRemoteRepository @Inject constructor() : ViajeRepository {

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = flowOf(emptyList())

    override fun observarViaje(folio: String): Flow<Viaje?> = flowOf(null)

    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun refrescarViaje(folio: String): Resultado<Unit> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> =
        Resultado.Fallo(ErrorApp.SinConexion)
}
