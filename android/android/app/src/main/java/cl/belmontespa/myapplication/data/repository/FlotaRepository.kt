package cl.belmontespa.myapplication.data.repository

import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Resultado

/** Conductores, vehículos y habilitaciones (Administrador). */
interface FlotaRepository {
    suspend fun listarConductores(): Resultado<List<Conductor>>

    suspend fun obtenerConductor(correo: String): Resultado<Conductor>

    suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor>

    /** Sin `password` se conserva la actual. */
    suspend fun actualizarConductor(
        correo: String,
        nombre: String,
        telefono: String?,
        password: String?,
    ): Resultado<Conductor>

    suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor>

    suspend fun listarMoviles(): Resultado<List<Movil>>

    suspend fun obtenerMovil(patente: String): Resultado<Movil>

    suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil>

    suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil>

    /** Todas, o filtradas por vehículo (`patente`) o por conductor (`correo`). */
    suspend fun listarHabilitaciones(patente: String? = null, correo: String? = null): Resultado<List<Habilitacion>>

    suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion>
}
