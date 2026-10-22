package cl.belmontespa.myapplication.util

import cl.belmontespa.myapplication.model.*
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.Locale

/** Reglas de entrada compartidas por formularios y repositorios de la demo. */
object Validaciones {
    fun correo(valor: String) = valor.trim().lowercase(Locale.ROOT)
    fun telefono(valor: String?): String? = valor?.filterNot { it.isWhitespace() || it in "()-" }
        ?.takeIf { it.isNotBlank() }?.let { if (it.length == 9) "+56$it" else it }
    fun documento(valor: String) = valor.replace(Regex("[.\\-\\s]"), "").uppercase(Locale.ROOT)
    fun rut(valor: String): String = documento(valor).let { if (it.length < 2) it else it.dropLast(1) + "-" + it.last() }
    fun vuelo(valor: String?) = valor?.filterNot(Char::isWhitespace)?.uppercase(Locale.ROOT)?.takeIf(String::isNotBlank)
    fun patente(valor: String) = valor.replace(Regex("[\\s-]"), "").uppercase(Locale.ROOT)
    fun nombreCatalogo(valor: String) = Normalizer.normalize(valor, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT).trim()
        .replace(Regex("^region(?: de la| del| de)?\\s+"), "").replace(Regex("\\s+"), " ")

    fun rutValido(valor: String): Boolean {
        val dni = documento(valor)
        if (!dni.matches(Regex("[0-9]{7,8}[0-9K]"))) return false
        val suma = dni.dropLast(1).reversed().mapIndexed { i, c -> c.digitToInt() * (2 + i % 6) }.sum()
        val dv = when (val resto = 11 - suma % 11) { 11 -> '0'; 10 -> 'K'; else -> ('0'.code + resto).toChar() }
        return dni.last() == dv
    }
    fun passwordValida(valor: String) = valor.length in 8..72 && valor.toByteArray(Charsets.UTF_8).size <= 72 &&
        valor.any(Char::isLetter) && valor.any(Char::isDigit)
    fun documentoValido(tipo: TipoDocumento, valor: String) = when (tipo) {
        TipoDocumento.RUT -> rutValido(valor)
        TipoDocumento.PASAPORTE -> documento(valor).matches(Regex("[A-Z0-9]{5,20}"))
    }
    fun pasajero(p: NuevoPasajero): Map<String, String> = buildMap {
        if (p.nombre.trim().length !in 2..120) put("nombre", "Debe tener entre 2 y 120 caracteres.")
        if (!documentoValido(p.tipoDocumento, p.dni)) put("dni", "Ingresa un documento válido.")
    }
    fun registro(r: RegistroSolicitante): Map<String, String> = buildMap {
        if (r.nombre.trim().length !in 2..120) put("nombre", "Debe tener entre 2 y 120 caracteres.")
        if (!correo(r.correo).matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) || correo(r.correo).length > 150) put("correo", "Ingresa un correo válido.")
        if (telefono(r.telefono)?.matches(Regex("\\+56[0-9]{9}")) == false) put("telefono", "Ingresa un teléfono chileno válido.")
        if (!passwordValida(r.password)) put("password", "Usa entre 8 y 72 caracteres, con una letra y un dígito (máximo 72 bytes).")
        if (r.razonSocial.trim().length !in 2..50) put("razonSocial", "Debe tener entre 2 y 50 caracteres.")
        if (!rutValido(r.rutEmpresa)) put("rutEmpresa", "Ingresa un RUT válido.")
    }
    fun solicitud(s: NuevaSolicitud, ahora: LocalDateTime): Map<String, String> = buildMap {
        val agenda = s.fhAgenda.withSecond(0).withNano(0)
        if (agenda.isBefore(ahora.plusHours(2)) || agenda.isAfter(ahora.plusDays(90))) put("fhAgenda", "Debe estar entre 2 horas y 90 días desde ahora.")
        if (s.pasajeros.isEmpty()) put("pasajeros", "Agrega al menos un pasajero.")
        s.pasajeros.forEachIndexed { i, p -> pasajero(p).forEach { (k, v) -> put("pasajeros.$i.$k", v) } }
        if (s.pasajeros.map { documento(it.dni) }.distinct().size != s.pasajeros.size) put("pasajeros", "Hay pasajeros repetidos.")
        if (s.origen.latitud == s.destino.latitud && s.origen.longitud == s.destino.longitud) put("destino", "Debe ser distinto del origen.")
        listOf("origen" to s.origen, "destino" to s.destino).forEach { (campo, u) ->
            if (u.direccion.trim().length !in 1..200) put("$campo.direccion", "Ingresa una dirección válida.")
            if (!u.latitud.isFinite() || u.latitud !in -90.0..90.0 || !u.longitud.isFinite() || u.longitud !in -180.0..180.0) put(campo, "Selecciona una ubicación válida.")
            if (u.comuna.isBlank()) put("$campo.comuna", "Selecciona una comuna del catálogo.")
        }
        if (vuelo(s.numeroVuelo)?.matches(Regex("[A-Z0-9]{2}[0-9]{1,4}[A-Z]?")) == false) put("numeroVuelo", "Ingresa un número de vuelo válido.")
    }
}
