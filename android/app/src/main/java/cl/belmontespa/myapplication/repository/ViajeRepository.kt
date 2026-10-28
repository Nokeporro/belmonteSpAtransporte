package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.dao.ViajeDao
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.aModelo
import cl.belmontespa.myapplication.session.SesionStore
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

open class ViajeRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    private val viajeDao: ViajeDao get() = local.db.viajeDao()
    private val sesionStore: SesionStore get() = local.sesiones
    private val clock: Clock get() = local.clock
    private val operaciones = OperacionesViajeLocal(local)

    open fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = flow {
        val sesion = sesionAutenticada()
        if (sesion == null) emit(emptyList()) else emitAll(listaPara(sesion, filtro))
    }

    open fun observarViaje(folio: String): Flow<Viaje?> = flow {
        val sesion = sesionAutenticada()
        if (sesion == null) emit(null) else emitAll(detallePara(sesion, folio))
    }

    private suspend fun sesionAutenticada(): Sesion? = (sesionStore.estado.first() as? EstadoSesion.Autenticada)?.sesion

    private fun listaPara(sesion: Sesion, filtro: FiltroViajes): Flow<List<Viaje>> {
        val esAgendaDelConductor = sesion.rol == Rol.CONDUCTOR && filtro.estado == null
        val consulta = if (esAgendaDelConductor) {
            val hoy = LocalDate.now(clock)
            viajeDao.observarAgendaConductor(
                correo = sesion.correo,
                fecha = (filtro.fecha ?: hoy).toString(),
                incluirEnCursoAnterior = filtro.fecha == null || filtro.fecha == hoy,
            )
        } else {
            viajeDao.observarLista(
                correoSolicitante = sesion.correo.takeIf { sesion.rol == Rol.SOLICITANTE },
                correoConductor = sesion.correo.takeIf { sesion.rol == Rol.CONDUCTOR },
                estado = filtro.estado?.name,
                fecha = filtro.fecha?.toString(),
                ascendente = filtro.estado in setOf(EstadoViaje.PENDIENTE, EstadoViaje.ASIGNADO, EstadoViaje.EN_CURSO),
            )
        }
        return consulta.map { filas -> filas.map { it.aModelo().paraRol(sesion.rol) } }
    }

    private fun detallePara(sesion: Sesion, folio: String): Flow<Viaje?> =
        viajeDao.observar(folio).map { relacion ->
            val enAlcance = relacion != null && when (sesion.rol) {
                Rol.SOLICITANTE -> relacion.viaje.correoSolicitante == sesion.correo
                Rol.CONDUCTOR -> relacion.viaje.correoConductor == sesion.correo
                Rol.ADMINISTRADOR -> true
            }
            if (enAlcance) relacion?.aModelo()?.paraRol(sesion.rol) else null
        }

    private fun Viaje.paraRol(rol: Rol): Viaje =
        if (rol == Rol.CONDUCTOR) {
            copy(hashPago = null, estadoPago = null)
        } else {
            this
        }

    open suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = Resultado.Exito(Unit)

    open suspend fun refrescarViaje(folio: String): Resultado<Unit> = Resultado.Exito(Unit)

    open suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> = operaciones.crear(solicitud)

    open suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> = operaciones.agregar(folio, pasajero)

    open suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> =
        operaciones.asignar(folio, correoConductor, patente)

    open suspend fun cancelar(folio: String, motivo: String? = null, montoReembolso: Int? = null): Resultado<Unit> =
        operaciones.cancelar(folio, motivo, montoReembolso)

    open suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = operaciones.rechazar(folio, motivo)

    open suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> =
        operaciones.estado(folio, estado)
}
