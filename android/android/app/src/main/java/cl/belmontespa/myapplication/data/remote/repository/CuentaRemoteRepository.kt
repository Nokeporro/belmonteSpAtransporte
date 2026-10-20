package cl.belmontespa.myapplication.data.remote.repository

import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Solicitante
import javax.inject.Inject

/** Implementación remota; hasta T14 responde Sin conexión. */
class CuentaRemoteRepository @Inject constructor() : CuentaRepository {

    override suspend fun obtenerPerfil(): Resultado<Cuenta> = Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante> =
        Resultado.Fallo(ErrorApp.SinConexion)

    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> =
        Resultado.Fallo(ErrorApp.SinConexion)
}
