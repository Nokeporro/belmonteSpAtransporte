package cl.belmontespa.myapplication.util

import java.security.MessageDigest
import java.util.Base64

/** Hashes de la build demo: Room nunca guarda contraseñas ni documentos en claro. */
object Hashes {
    /** SHA-256 en hexadecimal minúsculo; es `usuarios.password_sha256` de las cuentas ficticias. */
    fun password(password: String): String =
        sha256(password).joinToString("") { "%02x".format(it) }

    /**
     * `dni_hash` de la build demo: SHA-256 en Base64url sin relleno (43 caracteres) del documento
     * normalizado. El backend usa en cambio un HMAC con `DNI_SECRET`.
     */
    fun documento(documento: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(normalizarDocumento(documento)))

    /** Para el hash, todo documento va sin puntos, guion ni espacios y en mayúsculas. */
    fun normalizarDocumento(documento: String): String =
        documento.filterNot { it == '.' || it == '-' || it.isWhitespace() }.uppercase()

    private fun sha256(texto: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
}
