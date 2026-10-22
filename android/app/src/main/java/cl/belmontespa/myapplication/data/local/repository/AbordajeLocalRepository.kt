package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.repository.AbordajeRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

// El abordaje es público: no requiere sesión y devuelve la marca persistida.
class AbordajeLocalRepository @Inject constructor(private val local: SoporteLocal) : AbordajeRepository {
    override suspend fun registrarAbordaje(codigo: CodigoAbordaje, tipoDocumento: TipoDocumento, dni: String): Resultado<LocalDateTime> = local.ejecutar {
        val m = local.db.movilDao().obtener(codigo.patente.uppercase()) ?: falla(ErrorApp.NoEncontrado)
        val token = codigo.codigo.filterNot(Char::isWhitespace).uppercase()
        if (m.codigoAbordaje != token || dni.isBlank()) falla(ErrorApp.NoEncontrado)
        val hash = Hashes.documento(dni)
        val ahora = local.ahora()
        val candidatos = local.viajes().filter { v -> v.patenteMovil == m.patente &&
            (v.estado == "EN_CURSO" || v.estado == "ASIGNADO" && !ahora.isBefore(FechaIso.leer(v.fhAgenda).minusHours(1)) && !v.vencido(local))
        }.sortedBy { kotlin.math.abs(Duration.between(ahora, FechaIso.leer(it.fhAgenda)).seconds) }
        var marca: LocalDateTime? = null
        for (v in candidatos) {
            val pasajero = local.db.viajePasajeroDao().obtenerDelViaje(v.folio).find { it.dniHash == hash } ?: continue
            marca = pasajero.fhAbordaje?.let(FechaIso::leer) ?: ahora
            local.db.viajePasajeroDao().marcarAbordaje(v.folio, hash, FechaIso.texto(marca))
            break
        }
        marca ?: falla(ErrorApp.NoEncontrado)
    }
}
