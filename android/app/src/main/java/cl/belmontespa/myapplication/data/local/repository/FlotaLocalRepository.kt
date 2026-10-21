package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Resultado
import javax.inject.Inject

/** Flota de la build demo sobre Room. Cada operación la implementa la HU que se indica. */
class FlotaLocalRepository @Inject constructor() : FlotaRepository {

    override suspend fun listarConductores(): Resultado<List<Conductor>> = TODO("HU15")

    override suspend fun obtenerConductor(correo: String): Resultado<Conductor> = TODO("HU15")

    override suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> = TODO("HU15")

    override suspend fun actualizarConductor(
        correo: String,
        nombre: String,
        telefono: String?,
        password: String?,
    ): Resultado<Conductor> = TODO("HU15")

    override suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> =
        TODO("HU26")

    override suspend fun listarMoviles(): Resultado<List<Movil>> = TODO("HU17")

    override suspend fun obtenerMovil(patente: String): Resultado<Movil> = TODO("HU17")

    override suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = TODO("HU17")

    override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = TODO("HU26")

    override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> =
        TODO("HU18")

    override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> =
        TODO("HU18")
}
