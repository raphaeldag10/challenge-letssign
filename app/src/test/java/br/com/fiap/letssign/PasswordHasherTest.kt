package br.com.fiap.letssign

import br.com.fiap.letssign.data.repository.PasswordHasher
import org.junit.Assert.*
import org.junit.Test

class PasswordHasherTest {
    private val hasher = PasswordHasher()
    @Test fun validatesCorrectPasswordAndRejectsWrongOne() {
        val hash = hasher.hash("MinhaSenha123")
        assertTrue(hasher.verify("MinhaSenha123", hash))
        assertFalse(hasher.verify("OutraSenha123", hash))
    }
    @Test fun generatesDifferentSalts() {
        assertNotEquals(hasher.hash("MinhaSenha123"), hasher.hash("MinhaSenha123"))
    }
    @Test fun rejectsInvalidHash() {
        assertFalse(hasher.verify("senha", "texto-invalido"))
    }
}
