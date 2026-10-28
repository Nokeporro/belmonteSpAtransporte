package cl.belmontespa.myapplication.util

import java.security.MessageDigest
import java.util.Base64

object Hashes {
    fun password(password: String): String =
        sha256(password).joinToString("") { "%02x".format(it) }

    fun documento(documento: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(normalizarDocumento(documento)))

    fun normalizarDocumento(documento: String): String =
        documento.filterNot { it == '.' || it == '-' || it.isWhitespace() }.uppercase()

    private fun sha256(texto: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
}
