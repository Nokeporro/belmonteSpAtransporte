package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.repository.dto.ActivoRequest
import cl.belmontespa.myapplication.repository.dto.ConductorRequest
import cl.belmontespa.myapplication.repository.dto.HabilitacionRequest
import cl.belmontespa.myapplication.repository.dto.MovilRequest
import cl.belmontespa.myapplication.repository.dto.aModelo
import cl.belmontespa.myapplication.util.Validaciones

internal class FlotaRemoto(private val local: SoporteLocal) {
    private val api get() = ApiClient.de(local.sesiones)

    suspend fun listarConductores(): Resultado<List<Conductor>> = llamar { api.conductores().map { it.aModelo() } }

    suspend fun obtenerConductor(correo: String): Resultado<Conductor> = llamar {
        api.conductor(Validaciones.correo(correo)).aModelo()
    }

    suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> = llamar {
        api.registrarConductor(
            ConductorRequest(
                cuenta.nombre.trim(),
                Validaciones.correo(cuenta.correo),
                Validaciones.telefono(cuenta.telefono)?.takeIf { it.isNotBlank() },
                cuenta.password,
            ),
        ).aModelo()
    }

    suspend fun actualizarConductor(correo: String, nombre: String, telefono: String?, password: String?): Resultado<Conductor> = llamar {
        val correoNormalizado = Validaciones.correo(correo)
        api.actualizarConductor(
            correoNormalizado,
            ConductorRequest(
                nombre.trim(),
                correoNormalizado,
                Validaciones.telefono(telefono)?.takeIf { it.isNotBlank() },
                password?.takeIf { it.isNotBlank() },
            ),
        ).aModelo()
    }

    suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> = llamar {
        api.cambiarActivoConductor(Validaciones.correo(correo), ActivoRequest(activo)).aModelo()
    }

    suspend fun listarMoviles(): Resultado<List<Movil>> = llamar { api.moviles().map { it.aModelo() } }

    suspend fun obtenerMovil(patente: String): Resultado<Movil> = llamar { api.movil(Validaciones.patente(patente)).aModelo() }

    suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> = llamar {
        api.registrarMovil(MovilRequest(Validaciones.patente(movil.patente), movil.capacidad, movil.gpsSerial)).aModelo()
    }

    suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> = llamar {
        api.cambiarActivoMovil(Validaciones.patente(patente), ActivoRequest(activo)).aModelo()
    }

    suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> = llamar {
        api.habilitaciones(patente?.let(Validaciones::patente), correo?.let(Validaciones::correo)).map { it.aModelo() }
    }

    suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> = llamar {
        api.habilitar(HabilitacionRequest(Validaciones.patente(patente), Validaciones.correo(correoConductor))).aModelo()
    }
}
