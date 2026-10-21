package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Resultado
import javax.inject.Inject

/** Implementación remota; hasta T14 responde Sin conexión. */
class FlotaRemoteRepository @Inject constructor() : FlotaRepository {

    override suspend fun listarConductores(): Resultado<List<Conductor>> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun obtenerConductor(correo: String): Resultado<Conductor> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun actualizarConductor(
        correo: String,
        nombre: String,
        telefono: String?,
        password: String?,
    ): Resultado<Conductor> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun listarMoviles(): Resultado<List<Movil>> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun obtenerMovil(patente: String): Resultado<Movil> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> =
        Resultado.Fallo(ErrorApp.SinConexion)
}
