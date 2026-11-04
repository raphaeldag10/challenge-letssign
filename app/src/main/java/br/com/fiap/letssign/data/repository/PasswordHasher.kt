package br.com.fiap.letssign.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasher {
    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encoder = Base64.getEncoder()
        return "pbkdf2:210000:${encoder.encodeToString(salt)}:${encoder.encodeToString(derive(password, salt))}"
    }

    fun verify(password: String, stored: String): Boolean = try {
        val parts = stored.split(':')
        if (parts.size != 4 || parts[0] != "pbkdf2" || parts[1] != "210000") false
        else MessageDigest.isEqual(
            Base64.getDecoder().decode(parts[3]),
            derive(password, Base64.getDecoder().decode(parts[2]))
        )
    } catch (_: IllegalArgumentException) { false }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 210000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
