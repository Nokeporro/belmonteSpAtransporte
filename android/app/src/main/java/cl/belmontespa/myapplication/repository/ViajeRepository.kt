package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.dao.OperacionPendienteDao
import cl.belmontespa.myapplication.dao.ViajeDao
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoEnvio
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.model.aCambioPendiente
import cl.belmontespa.myapplication.model.aModelo
import cl.belmontespa.myapplication.repository.api.ViajeRemoto
import cl.belmontespa.myapplication.repository.api.intentar
import cl.belmontespa.myapplication.session.SesionStore
import cl.belmontespa.myapplication.sync.ResultadoSync
import cl.belmontespa.myapplication.sync.SincronizadorDeCola
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

open class ViajeRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    private val viajeDao: ViajeDao get() = local.db.viajeDao()
    private val cola: OperacionPendienteDao get() = local.db.operacionPendienteDao()
    private val sesionStore: SesionStore get() = local.sesiones
    private val clock: Clock get() = local.clock
    private val operaciones = OperacionesViajeLocal(local)
    private val remoto by lazy { ViajeRemoto(local) }

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
        return combine(consulta, cola.observarDeCuenta(sesion.correo)) { filas, operaciones ->
            val porFolio = operaciones.groupBy { it.folio }
            filas.map { it.aModelo().copy(cambioPendiente = porFolio[it.viaje.folio]?.aCambioPendiente()).paraRol(sesion.rol) }
        }
    }

    private fun detallePara(sesion: Sesion, folio: String): Flow<Viaje?> =
        combine(viajeDao.observar(folio), cola.observarDelViaje(folio)) { relacion, operaciones ->
            val enAlcance = relacion != null && when (sesion.rol) {
                Rol.SOLICITANTE -> relacion.viaje.correoSolicitante == sesion.correo
                Rol.CONDUCTOR -> relacion.viaje.correoConductor == sesion.correo
                Rol.ADMINISTRADOR -> true
            }
            if (enAlcance) relacion?.aModelo()?.copy(cambioPendiente = operaciones.aCambioPendiente())?.paraRol(sesion.rol) else null
        }

    private fun Viaje.paraRol(rol: Rol): Viaje =
        if (rol == Rol.CONDUCTOR) {
            copy(hashPago = null, estadoPago = null)
        } else {
            this
        }

    open suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> =
        if (!local.modoLocal) remoto.refrescarViajes(filtro) else Resultado.Exito(Unit)

    open suspend fun refrescarViaje(folio: String): Resultado<Unit> =
        if (!local.modoLocal) remoto.refrescarViaje(folio) else Resultado.Exito(Unit)

    open suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> =
        if (!local.modoLocal) remoto.crearSolicitud(solicitud) else operaciones.crear(solicitud)

    open suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> =
        if (!local.modoLocal) remoto.agregarPasajero(folio, pasajero) else operaciones.agregar(folio, pasajero)

    open suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> =
        if (!local.modoLocal) remoto.asignar(folio, correoConductor, patente) else operaciones.asignar(folio, correoConductor, patente)

    open suspend fun cancelar(folio: String, motivo: String? = null, montoReembolso: Int? = null): Resultado<Unit> =
        if (!local.modoLocal) remoto.cancelar(folio, motivo, montoReembolso) else operaciones.cancelar(folio, motivo, montoReembolso)

    open suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> =
        if (!local.modoLocal) remoto.rechazar(folio, motivo) else operaciones.rechazar(folio, motivo)

    open suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> =
        if (!local.modoLocal) remoto.cambiarEstado(folio, estado) else operaciones.estado(folio, estado)

    open suspend fun descartarCambio(folio: String): Resultado<Unit> = intentar {
        cola.borrarDelViaje(folio)
        refrescarViaje(folio)
    }

    open suspend fun contarPendientes(): Int {
        val sesion = sesionAutenticada() ?: return 0
        return cola.contarDeLaCuenta(sesion.correo)
    }

    open suspend fun sincronizarAhora(): Resultado<Unit> = intentar {
        val sesion = sesionAutenticada() ?: return@intentar Resultado.Fallo(ErrorApp.SesionExpirada)
        if (local.modoLocal) return@intentar Resultado.Exito(Unit)
        when (SincronizadorDeCola(local).enviar()) {
            ResultadoSync.REINTENTAR -> Resultado.Fallo(ErrorApp.SinConexion)
            ResultadoSync.ESPERAR_SESION -> Resultado.Fallo(ErrorApp.SesionExpirada)
            ResultadoSync.LISTO -> {
                val restantes = cola.observarDeCuenta(sesion.correo).first()
                if (restantes.isEmpty()) {
                    Resultado.Exito(Unit)
                } else {
                    val mensaje = restantes.firstOrNull { it.estadoEnvio == EstadoEnvio.CONFLICTO.name }?.mensajeError
                    Resultado.Fallo(ErrorApp.Conflicto(mensaje ?: "No pudimos sincronizar el cambio."))
                }
            }
        }
    }
}
