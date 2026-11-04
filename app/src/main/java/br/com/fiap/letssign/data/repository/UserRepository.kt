package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.AuthMode
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.local.entity.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Locale

// A sessão expõe apenas os dados necessários às telas, nunca o hash da senha.
data class LoggedUser(val id: Long, val fullName: String, val email: String, val role: UserRole)

class UserRepository(
    private val dao: UserDao,
    private val hasher: PasswordHasher = PasswordHasher(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom()
) {

    private val _session = MutableStateFlow<LoggedUser?>(null)
    val session: StateFlow<LoggedUser?> = _session.asStateFlow()
    val currentUser: LoggedUser? get() = _session.value

    suspend fun hasAdmin(): Boolean = withContext(Dispatchers.IO) {
        dao.getUsersByRole(UserRole.ADMIN).isNotEmpty()
    }

    suspend fun login(email: String, password: String): LoggedUser = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(normalize(email))
        val stored = user?.passwordHash
        require(
            user != null && user.authMode == AuthMode.PASSWORD &&
                stored != null && hasher.verify(password, stored)
        ) { "E-mail ou senha inválidos" }
        openSession(user)
    }

    /**
     * Gera um código de uso único para um signatário que entra por código.
     * Nenhum e-mail é enviado nesta etapa: o código é devolvido para ser exibido na tela.
     */
    suspend fun requestLoginCode(email: String): String = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(normalize(email))
        require(user != null && user.authMode == AuthMode.EMAIL_CODE) {
            "Nenhum signatário com login por código encontrado para este e-mail"
        }
        val code = "%06d".format(random.nextInt(1_000_000))
        dao.updateUser(
            user.copy(
                loginCodeHash = hasher.hash(code),
                loginCodeExpiresAt = clock() + CODE_TTL_MS
            )
        )
        code
    }

    suspend fun loginWithCode(email: String, code: String): LoggedUser = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(normalize(email))
        val stored = user?.loginCodeHash
        val expiresAt = user?.loginCodeExpiresAt
        require(user != null && user.authMode == AuthMode.EMAIL_CODE && stored != null && expiresAt != null) {
            "Código inválido ou expirado"
        }
        require(clock() <= expiresAt && hasher.verify(code.trim(), stored)) { "Código inválido ou expirado" }
        // Código de uso único: invalida assim que for aceito.
        dao.updateUser(user.copy(loginCodeHash = null, loginCodeExpiresAt = null))
        openSession(user)
    }

    fun logout() {
        _session.value = null
    }

    // Usado uma única vez na configuração inicial; não cria senha padrão no código.
    suspend fun setupInitialAdmin(name: String, email: String, phone: String, password: String): Long =
        withContext(Dispatchers.IO) {
            validate(name, email, phone, password, requirePhone = true)
            requireNotNull(password)
            dao.createInitialAdmin(
                UserEntity(
                    fullName = name.trim(), email = normalize(email), phone = phone.trim(),
                    passwordHash = hasher.hash(password), role = UserRole.ADMIN, authMode = AuthMode.PASSWORD
                )
            )
        }

    suspend fun registerSignatory(
        name: String,
        email: String,
        phone: String,
        authMode: AuthMode,
        password: String?
    ): Long = withContext(Dispatchers.IO) {
        requireAdmin()
        validateSignatory(name, email, phone, authMode, password)
        val normalized = normalize(email)
        require(dao.getUserByEmail(normalized) == null) { "E-mail já cadastrado" }
        dao.insertUser(
            UserEntity(
                fullName = name.trim(), email = normalized, phone = phone.trim(),
                passwordHash = passwordHashFor(authMode, password), role = UserRole.SIGNATORY, authMode = authMode
            )
        )
    }

    suspend fun updateSignatory(
        id: Long,
        name: String,
        email: String,
        phone: String,
        authMode: AuthMode,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        requireAdmin()
        val user = requireNotNull(dao.getUserById(id)) { "Signatário não encontrado" }
        require(user.role == UserRole.SIGNATORY) { "Usuário não é signatário" }
        // Ao trocar para senha, é preciso informar uma; sem troca, senha em branco mantém a atual.
        val needsNewPassword = authMode == AuthMode.PASSWORD && user.passwordHash == null
        validateSignatory(name, email, phone, authMode, password, needsNewPassword)
        val normalized = normalize(email)
        val existing = dao.getUserByEmail(normalized)
        require(existing == null || existing.id == id) { "E-mail já cadastrado" }
        val newHash = when (authMode) {
            AuthMode.EMAIL_CODE -> null
            AuthMode.PASSWORD -> password?.takeIf { it.isNotBlank() }?.let(hasher::hash) ?: user.passwordHash
        }
        dao.updateUser(
            user.copy(
                fullName = name.trim(), email = normalized, phone = phone.trim(),
                passwordHash = newHash, authMode = authMode,
                // Código pendente deixa de valer se o modo mudou.
                loginCodeHash = if (authMode == AuthMode.EMAIL_CODE) user.loginCodeHash else null,
                loginCodeExpiresAt = if (authMode == AuthMode.EMAIL_CODE) user.loginCodeExpiresAt else null
            )
        )
    }

    fun observeSignatories(): Flow<List<UserEntity>> = dao.getAllSignatories()

    suspend fun getSignatory(id: Long): UserEntity? = withContext(Dispatchers.IO) {
        dao.getUserById(id)?.takeIf { it.role == UserRole.SIGNATORY }
    }

    private fun openSession(user: UserEntity): LoggedUser =
        LoggedUser(user.id, user.fullName, user.email, user.role).also { _session.value = it }

    private fun passwordHashFor(authMode: AuthMode, password: String?): String? =
        if (authMode == AuthMode.PASSWORD) hasher.hash(requireNotNull(password)) else null

    private fun requireAdmin() {
        check(_session.value?.role == UserRole.ADMIN) { "Apenas o administrador pode gerenciar signatários" }
    }

    private fun normalize(email: String) = email.trim().lowercase(Locale.ROOT)

    private fun validateSignatory(
        name: String,
        email: String,
        phone: String,
        authMode: AuthMode,
        password: String?,
        passwordRequired: Boolean = authMode == AuthMode.PASSWORD
    ) {
        validate(name, email, phone, password?.takeIf { it.isNotBlank() }, requirePhone = false)
        if (passwordRequired) {
            require(!password.isNullOrBlank()) { "Informe a senha do signatário." }
            require(password.length >= 8) { "A senha deve ter pelo menos 8 caracteres" }
        }
    }

    private fun validate(name: String, email: String, phone: String, password: String?, requirePhone: Boolean) {
        require(name.trim().length >= 3) { "Informe o nome completo" }
        require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(normalize(email))) { "E-mail inválido" }
        if (requirePhone || phone.isNotBlank()) {
            require(phone.count(Char::isDigit) in 10..15) { "Telefone inválido" }
        }
        require(password == null || password.length >= 8) { "A senha deve ter pelo menos 8 caracteres" }
    }

    private companion object {
        const val CODE_TTL_MS = 10 * 60 * 1000L
    }
}
