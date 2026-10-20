package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Solicitante

/** Cuenta propia (la de la sesión), aprobación de Solicitantes y alta de Administradores. */
interface CuentaRepository {
    suspend fun obtenerPerfil(): Resultado<Cuenta>

    suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta>

    suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit>

    suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>>

    suspend fun aprobarSolicitante(correo: String): Resultado<Solicitante>

    suspend fun rechazarSolicitante(correo: String): Resultado<Solicitante>

    suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta>
}
