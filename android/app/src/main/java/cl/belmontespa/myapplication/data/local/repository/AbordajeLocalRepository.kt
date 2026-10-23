package cl.belmontespa.myapplication.data.local.repository

import cl.belmontespa.myapplication.data.local.Hashes
import cl.belmontespa.myapplication.data.repository.AbordajeRepository
import cl.belmontespa.myapplication.model.*
import cl.belmontespa.myapplication.util.FechaIso
import cl.belmontespa.myapplication.util.Validaciones
import java.time.LocalDateTime
import javax.inject.Inject

// El abordaje es público: no requiere sesión y devuelve la marca persistida. Guarda solo el hash del documento.
class AbordajeLocalRepository @Inject constructor(private val local: SoporteLocal) : AbordajeRepository {
    override suspend fun registrarAbordaje(codigo: CodigoAbordaje, tipoDocumento: TipoDocumento, dni: String): Resultado<LocalDateTime> = local.ejecutar {
        // Primero la forma (400) y después la búsqueda (404), como el backend.
        if (!Validaciones.documentoValido(tipoDocumento, dni)) falla(ErrorApp.Validacion(mapOf("dni" to "Ingresa un documento válido.")))
        // Patente inexistente, código ajeno, sin viaje candidato o documento que no viaja: siempre el mismo NoEncontrado.
        val movil = local.db.movilDao().obtener(codigo.patente.uppercase()) ?: falla(ErrorApp.NoEncontrado)
        if (movil.codigoAbordaje != codigo.codigo.filterNot(Char::isWhitespace).uppercase()) falla(ErrorApp.NoEncontrado)
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
            // Solo escribe si era NULL: repetir devuelve la misma hora.
            local.db.viajePasajeroDao().marcarAbordaje(viaje.folio, hash, FechaIso.texto(marca))
            break
        }
        marca ?: falla(ErrorApp.NoEncontrado)
    }
}
