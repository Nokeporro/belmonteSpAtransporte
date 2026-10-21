package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.repository.ViajeRepository
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Viajes de la build demo: aplica sobre Room las máquinas de estado (V1–V8), las validaciones y el
 * alcance por rol, con el `Clock` de la app. Cada operación la implementa la HU que se indica.
 */
class ViajeLocalRepository @Inject constructor() : ViajeRepository {

    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> =
        TODO("T06: listas por rol (HU03, HU04 y HU07)")

    override fun observarViaje(folio: String): Flow<Viaje?> = TODO("T06: detalle (HU08)")

    /** En la build demo Room es la fuente de verdad: no hay nada que refrescar. */
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = Resultado.Exito(Unit)

    override suspend fun refrescarViaje(folio: String): Resultado<Unit> = Resultado.Exito(Unit)

    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = TODO("HU02: V1")

    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = TODO("HU02")

    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> =
        TODO("HU05 y HU23: V2")

    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> =
        TODO("HU16: V5 y HU24: V7")

    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = TODO("HU25: V8")

    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> =
        TODO("HU09: V3 y V4")
}
