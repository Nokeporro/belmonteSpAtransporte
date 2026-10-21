package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Solicitante
import javax.inject.Inject

/** Cuentas de la build demo sobre Room. Cada operación la implementa la HU que se indica. */
class CuentaLocalRepository @Inject constructor() : CuentaRepository {

    override suspend fun obtenerPerfil(): Resultado<Cuenta> = TODO("T06: perfil de la cuenta de la sesión")

    override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> =
        TODO("HU22")

    override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> =
        TODO("HU22")

    override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> = TODO("HU27")

    override suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante> = TODO("HU27")

    override suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante> = TODO("HU27")

    override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> = TODO("HU28")
}
