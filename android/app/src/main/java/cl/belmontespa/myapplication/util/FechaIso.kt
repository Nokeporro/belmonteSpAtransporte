package cl.belmontespa.myapplication.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FechaIso {
    private val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun texto(fechaHora: LocalDateTime): String = formato.format(fechaHora)

    fun leer(texto: String): LocalDateTime = LocalDateTime.parse(texto)
}
