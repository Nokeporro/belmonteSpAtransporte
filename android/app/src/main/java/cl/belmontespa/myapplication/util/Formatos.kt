package cl.belmontespa.myapplication.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Formatos de presentación (README, Textos y tema); la API mantiene los suyos. */
object Formatos {
    private val fechaHoraLista = DateTimeFormatter.ofPattern("dd-MM · HH:mm")
    private val fechaHoraDetalle = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")
    private val fechaSola = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val horaSola = DateTimeFormatter.ofPattern("HH:mm")

    /** Listas: `24-09 · 14:30`. */
    fun fechaHoraLista(fechaHora: LocalDateTime): String = fechaHoraLista.format(fechaHora)

    /** Detalles y push: `24-09-2026 · 14:30`. */
    fun fechaHoraDetalle(fechaHora: LocalDateTime): String = fechaHoraDetalle.format(fechaHora)

    /** Selectores de fecha: `24-09-2026`. */
    fun fecha(fecha: LocalDate): String = fechaSola.format(fecha)

    /** Hora sola, en 24 horas: `14:30`. */
    fun hora(fechaHora: LocalDateTime): String = horaSola.format(fechaHora)

    /** Pesos chilenos con punto de miles y sin decimales: `$48.000`. */
    fun monto(pesos: Int): String = "$" + conPuntosDeMiles(pesos.toString())

    /** RUT con puntos y guion: `76543210-3` → `76.543.210-3`. */
    fun rut(rut: String): String {
        val limpio = rut.replace(".", "").replace("-", "").uppercase()
        if (limpio.length < 2) return rut
        return conPuntosDeMiles(limpio.dropLast(1)) + "-" + limpio.last()
    }

    /** Teléfono normalizado `+56955550101` → `+56 9 5555 0101`; otro valor se muestra tal cual. */
    fun telefono(telefono: String): String {
        if (!Regex("""^\+56[0-9]{9}$""").matches(telefono)) return telefono
        val numero = telefono.removePrefix("+56")
        return "+56 ${numero.substring(0, 1)} ${numero.substring(1, 5)} ${numero.substring(5)}"
    }

    /** Código de abordaje en grupos de 4: `7F3A 9C21 B04E 58D6`. */
    fun codigoAbordaje(codigo: String): String = codigo.chunked(4).joinToString(" ")

    private fun conPuntosDeMiles(digitos: String): String =
        digitos.reversed().chunked(3).joinToString(".").reversed()
}
