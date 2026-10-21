package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Viaje
import kotlinx.coroutines.flow.Flow

/**
 * Viajes dentro del alcance de la sesión (el Solicitante los suyos, el Conductor los asignados, el
 * Administrador todos). Las lecturas son `Flow` desde Room; las acciones siguen las máquinas de estado.
 */
interface ViajeRepository {
    fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>>

    fun observarViaje(folio: String): Flow<Viaje?>

    suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit>

    suspend fun refrescarViaje(folio: String): Resultado<Unit>

    /** V1 con sus pasajeros; devuelve el folio del viaje creado. */
    suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String>

    suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit>

    /** V2: asignar o reasignar. */
    suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit>

    /** V5 (Solicitante, sin cuerpo) o V7 (Administrador, con motivo y monto si está vencido). */
    suspend fun cancelar(folio: String, motivo: String? = null, montoReembolso: Int? = null): Resultado<Unit>

    /** V8. */
    suspend fun rechazar(folio: String, motivo: String): Resultado<Unit>

    /** V3 o V4; sin conexión, la implementación remota responde `Encolado`. */
    suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit>
}
