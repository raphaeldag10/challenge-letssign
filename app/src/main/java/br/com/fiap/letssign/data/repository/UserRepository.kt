package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.local.entity.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// A sessão expõe apenas os dados necessários às telas, nunca o hash da senha.
data class LoggedUser(val id: Long, val fullName: String, val email: String, val role: UserRole)

class UserRepository(private val dao: UserDao, private val hasher: PasswordHasher = PasswordHasher()) {
    private var session: LoggedUser? = null
    val currentUser: LoggedUser? get() = session

    suspend fun login(email: String, password: String): LoggedUser = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(normalize(email))
        require(user != null && hasher.verify(password, user.passwordHash)) { "E-mail ou senha inválidos" }
        LoggedUser(user.id, user.fullName, user.email, user.role).also { session = it }
    }

    fun logout() { session = null }

    // Usado uma única vez na configuração inicial; não cria senha padrão no código.
    suspend fun setupInitialAdmin(name: String, email: String, phone: String, password: String): Long =
        withContext(Dispatchers.IO) {
            validate(name, email, phone, password)
            dao.createInitialAdmin(UserEntity(fullName = name.trim(), email = normalize(email),
                phone = phone.trim(), passwordHash = hasher.hash(password), role = UserRole.ADMIN))
        }

    suspend fun registerSignatory(name: String, email: String, phone: String, password: String): Long =
        withContext(Dispatchers.IO) {
            requireAdmin()
            validate(name, email, phone, password)
            val normalized = normalize(email)
            require(dao.getUserByEmail(normalized) == null) { "E-mail já cadastrado" }
            dao.insertUser(UserEntity(fullName = name.trim(), email = normalized, phone = phone.trim(),
                passwordHash = hasher.hash(password), role = UserRole.SIGNATORY))
        }

    suspend fun updateSignatory(id: Long, name: String, email: String, phone: String, password: String? = null) =
        withContext(Dispatchers.IO) {
            requireAdmin()
            validate(name, email, phone, password)
            val user = requireNotNull(dao.getUserById(id)) { "Signatário não encontrado" }
            require(user.role == UserRole.SIGNATORY) { "Usuário não é signatário" }
            val normalized = normalize(email)
            val existing = dao.getUserByEmail(normalized)
            require(existing == null || existing.id == id) { "E-mail já cadastrado" }
            dao.updateUser(user.copy(fullName = name.trim(), email = normalized, phone = phone.trim(),
                passwordHash = password?.let(hasher::hash) ?: user.passwordHash))
        }

    private fun requireAdmin() {
        check(session?.role == UserRole.ADMIN) { "Apenas o administrador pode gerenciar signatários" }
    }

    private fun normalize(email: String) = email.trim().lowercase(Locale.ROOT)

    private fun validate(name: String, email: String, phone: String, password: String?) {
        require(name.trim().length >= 3) { "Informe o nome completo" }
        require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(normalize(email))) { "E-mail inválido" }
        require(phone.count(Char::isDigit) in 10..15) { "Telefone inválido" }
        require(password == null || password.length >= 8) { "A senha deve ter pelo menos 8 caracteres" }
    }
}
