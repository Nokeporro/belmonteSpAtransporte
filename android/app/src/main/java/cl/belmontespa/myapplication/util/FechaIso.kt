package cl.belmontespa.myapplication.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Fechas de negocio como texto ISO 8601 sin zona, igual que la API: `2026-09-24T14:30:00`. */
object FechaIso {
    private val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun texto(fechaHora: LocalDateTime): String = formato.format(fechaHora)

    fun leer(texto: String): LocalDateTime = LocalDateTime.parse(texto)
}
