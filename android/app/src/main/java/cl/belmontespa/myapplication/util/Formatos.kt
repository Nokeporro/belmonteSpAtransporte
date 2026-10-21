package cl.belmontespa.myapplication.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatos {

    private val localeChile = Locale.forLanguageTag("es-CL")

    private val formatoLista =
        DateTimeFormatter.ofPattern("dd-MM · HH:mm", localeChile)

    private val formatoDetalle =
        DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm", localeChile)

    private val formatoFecha =
        DateTimeFormatter.ofPattern("dd-MM-yyyy", localeChile)

    private val formatoHora =
        DateTimeFormatter.ofPattern("HH:mm", localeChile)

    private val telefonoNormalizado = Regex("""^\+56[0-9]{9}$""")

    fun fechaHoraLista(fechaHora: LocalDateTime): String =
        formatoLista.format(fechaHora)

    fun fechaHoraDetalle(fechaHora: LocalDateTime): String =
        formatoDetalle.format(fechaHora)

    fun fecha(fecha: LocalDate): String =
        formatoFecha.format(fecha)

    fun hora(fechaHora: LocalDateTime): String =
        formatoHora.format(fechaHora)

    fun monto(pesos: Int): String =
        "$" + conPuntosDeMiles(pesos.toString())

    fun rut(rut: String): String {
        val limpio = rut.replace(".", "").replace("-", "").uppercase()
        if (limpio.length < 2) return rut

        return conPuntosDeMiles(limpio.dropLast(1)) + "-" + limpio.last()
    }

    fun telefono(telefono: String): String {
        if (!telefonoNormalizado.matches(telefono)) return telefono

        val numero = telefono.removePrefix("+56")

        return "+56 ${numero.substring(0, 1)} " +
            "${numero.substring(1, 5)} ${numero.substring(5)}"
    }

    fun codigoAbordaje(codigo: String): String =
        codigo
            .filterNot { it.isWhitespace() }
            .uppercase()
            .chunked(4)
            .joinToString(" ")

    private fun conPuntosDeMiles(digitos: String): String =
        digitos.reversed().chunked(3).joinToString(".").reversed()
}
