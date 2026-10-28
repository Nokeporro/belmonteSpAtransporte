package cl.belmontespa.myapplication.repository

import android.app.Application
import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Hashes
import cl.belmontespa.myapplication.util.Validaciones
import java.time.LocalDateTime

open class AbordajeRepository(private val local: SoporteLocal) {
    constructor(application: Application) : this(SoporteLocal.de(application))

    open suspend fun registrarAbordaje(codigo: CodigoAbordaje, tipoDocumento: TipoDocumento, dni: String): Resultado<LocalDateTime> = local.ejecutar {
        if (!Validaciones.documentoValido(tipoDocumento, dni)) return@ejecutar invalido(mapOf("dni" to "Ingresa un documento válido."))
        val movil = local.db.movilDao().obtener(codigo.patente.uppercase()) ?: return@ejecutar noEncontrado
        val hash = Hashes.documento(dni)
        val ahora = local.ahora()
        val candidatos = ordenarPorCercania(
            local.viajes().filter { it.patenteMovil == movil.patente && esCandidatoDeAbordaje(EstadoViaje.valueOf(it.estado), FechaIso.leer(it.fhAgenda), ahora) },
            ahora,
        ) { FechaIso.leer(it.fhAgenda) }
        var marca: LocalDateTime? = null
        for (viaje in candidatos) {
            val pasajero = local.db.viajePasajeroDao().obtenerDelViaje(viaje.folio).find { it.dniHash == hash } ?: continue
            marca = pasajero.fhAbordaje?.let(FechaIso::leer) ?: ahora
            local.db.viajePasajeroDao().marcarAbordaje(viaje.folio, hash, FechaIso.texto(marca))
            break
        }
        if (marca == null) noEncontrado else Resultado.Exito(marca)
    }
}
