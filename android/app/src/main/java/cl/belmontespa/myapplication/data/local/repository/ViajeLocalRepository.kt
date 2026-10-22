package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.dao.ViajeDao
import cl.belmontespa.myapplication.data.mapper.aModelo
import cl.belmontespa.myapplication.data.session.SesionStore
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
class ViajeLocalRepository @Inject constructor(
    private val viajeDao: ViajeDao,
    private val sesionStore: SesionStore,
    private val clock: Clock,
    private val operaciones: OperacionesViajeLocal,
) : ViajeRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> =
        sesionStore.estado.flatMapLatest { estado ->
            val sesion = (estado as? EstadoSesion.Autenticada)?.sesion

            // Sin sesión autenticada no se entregan viajes de la demo.
            if (sesion == null) {
                return@flatMapLatest flowOf(emptyList<Viaje>())
            }

            val consulta = if (
                sesion.rol == Rol.CONDUCTOR &&
                filtro.estado == null
            ) {
                // Sin filtros, el Conductor recibe la agenda de hoy,
                // incluyendo un viaje anterior que todavía siga en curso.
                viajeDao.observarAgendaConductor(
                    correo = sesion.correo,
                    fecha = (filtro.fecha ?: LocalDate.now(clock)).toString(),
                    incluirEnCursoAnterior = filtro.fecha == null || filtro.fecha == LocalDate.now(clock),
                )
            } else {
                // El filtro de cuenta se aplica en la consulta de Room.
                viajeDao.observarLista(
                    correoSolicitante = sesion.correo.takeIf {
                        sesion.rol == Rol.SOLICITANTE
                    },
                    correoConductor = sesion.correo.takeIf {
                        sesion.rol == Rol.CONDUCTOR
                    },
                    estado = filtro.estado?.name,
                    fecha = filtro.fecha?.toString(),
                    ascendente = filtro.estado in setOf(
                        EstadoViaje.PENDIENTE,
                        EstadoViaje.ASIGNADO,
                        EstadoViaje.EN_CURSO,
                    ),
                )
            }

            consulta.map { filas ->
                filas.map { it.aModelo().paraRol(sesion.rol) }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarViaje(folio: String): Flow<Viaje?> =
        sesionStore.estado.flatMapLatest { estado ->
            val sesion = (estado as? EstadoSesion.Autenticada)?.sesion

            if (sesion == null) {
                return@flatMapLatest flowOf<Viaje?>(null)
            }

            viajeDao.observar(folio).map { relacion ->
                // Comprueba el alcance antes de convertir y entregar el detalle.
                val permitido = relacion?.let {
                    when (sesion.rol) {
                        Rol.SOLICITANTE ->
                            it.viaje.correoSolicitante == sesion.correo

                        Rol.CONDUCTOR ->
                            it.viaje.correoConductor == sesion.correo

                        Rol.ADMINISTRADOR -> true
                    }
                } ?: false

                if (permitido) {
                    relacion.aModelo().paraRol(sesion.rol)
                } else {
                    null
                }
            }
        }

    // El README excluye la información de pago de los viajes del Conductor.
    private fun Viaje.paraRol(rol: Rol): Viaje =
        if (rol == Rol.CONDUCTOR) {
            copy(hashPago = null, estadoPago = null)
        } else {
            this
        }

    /** En la build demo Room es la fuente de verdad: no hay nada que refrescar. */
    override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = Resultado.Exito(Unit)

    override suspend fun refrescarViaje(folio: String): Resultado<Unit> = Resultado.Exito(Unit)

    override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = operaciones.crear(solicitud)

    override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = operaciones.agregar(folio, pasajero)

    override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> =
        operaciones.asignar(folio, correoConductor, patente)

    override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> =
        operaciones.cancelar(folio, motivo, montoReembolso)

    override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = operaciones.rechazar(folio, motivo)

    override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> =
        operaciones.estado(folio, estado)
}
