package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    SIGNATORY
}

/** Como o usuário prova a identidade no login. */
enum class AuthMode {
    /** E-mail + senha. */
    PASSWORD,
    /** E-mail + código de uso único gerado pelo app. */
    EMAIL_CODE
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    /** Nulo quando o usuário entra apenas por código. */
    val passwordHash: String?,
    val role: UserRole,
    val authMode: AuthMode = AuthMode.PASSWORD,
    /** Hash do código de login pendente; nulo quando não há código ativo. */
    val loginCodeHash: String? = null,
    /** Instante (ms) em que o código pendente expira. */
    val loginCodeExpiresAt: Long? = null
)
