package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.UserDao
import br.com.fiap.letssign.data.local.entity.AuthMode
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryProfileTest {

    private lateinit var dao: FakeUserDao
    private lateinit var repo: UserRepository
    private val hasher = PasswordHasher()

    @Before
    fun setUp() {
        dao = FakeUserDao()
        repo = UserRepository(dao, hasher)
    }

    @Test
    fun signatoryUpdatesOwnNameEmailAndPhone(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "11912345678", passwordHash = hasher.hash("senha1234"))
        login(id)

        // O e-mail do signatário é travado; nome e telefone continuam editáveis.
        val updated = repo.updateOwnProfile("Ana Souza", "ana@empresa.com", "")

        assertEquals("Ana Souza", updated.fullName)
        assertEquals("ana@empresa.com", updated.email)
        assertEquals("Ana Souza", repo.currentUser?.fullName)
        val stored = dao.getUserById(id)!!
        assertEquals("", stored.phone)
    }

    @Test
    fun adminMustKeepAValidPhone(): Unit = runBlocking {
        val id = seed(UserRole.ADMIN, "Carlos Lima", "carlos@empresa.com", "11912345678", passwordHash = hasher.hash("senha1234"))
        login(id)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateOwnProfile("Carlos Lima", "carlos@empresa.com", "") }
        }
    }

    @Test
    fun rejectsEmailAlreadyUsedByAnotherAccount(): Unit = runBlocking {
        // Só o admin pode trocar o próprio e-mail, então o teste usa uma conta de admin.
        val id = seed(UserRole.ADMIN, "Carlos Lima", "carlos@empresa.com", "11912345678", passwordHash = hasher.hash("senha1234"))
        seed(UserRole.SIGNATORY, "Bruno Costa", "bruno@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateOwnProfile("Carlos Lima", "bruno@empresa.com", "11912345678") }
        }
        assertEquals("E-mail já cadastrado", error.message)
    }

    @Test
    fun passwordChangeRequiresTheCurrentPassword(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", currentPassword = "errada123", newPassword = "novasenha99")
            }
        }
        assertEquals("Senha atual incorreta", error.message)
        assertTrue(hasher.verify("senha1234", dao.getUserById(id)!!.passwordHash!!))
    }

    @Test
    fun passwordChangeWithCorrectCurrentPasswordReplacesTheHash(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        repo.updateOwnProfile(
            "Ana Silva", "ana@empresa.com", "",
            currentPassword = "senha1234", newPassword = "novasenha99"
        )

        val hash = dao.getUserById(id)!!.passwordHash!!
        assertTrue(hasher.verify("novasenha99", hash))
        assertTrue(!hasher.verify("senha1234", hash))
    }

    @Test
    fun blankNewPasswordKeepsTheCurrentOne(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", newPassword = "  ")

        assertTrue(hasher.verify("senha1234", dao.getUserById(id)!!.passwordHash!!))
    }

    @Test
    fun codeLoginAccountCannotChangePassword(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = null, authMode = AuthMode.EMAIL_CODE)
        login(id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", currentPassword = "x", newPassword = "novasenha99")
            }
        }
        assertTrue(error.message!!.contains("entra por código"))
    }

    @Test
    fun cannotUpdateWithoutSession(): Unit = runBlocking {
        seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "") }
        }
    }

    @Test
    fun currentProfileReturnsPhoneAndAuthMode(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "11912345678", passwordHash = hasher.hash("senha1234"))
        login(id)

        val profile = repo.currentProfile()
        assertNotNull(profile)
        assertEquals("11912345678", profile!!.phone)
        assertEquals(AuthMode.PASSWORD, profile.authMode)
        assertNull(UserRepository(FakeUserDao(), hasher).currentProfile())
    }

    @Test
    fun signatoryCannotChangeEmailSetByAdmin(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateOwnProfile("Ana Silva", "outro@empresa.com", "") }
        }
        assertTrue(error.message!!.contains("não pode ser alterado"))
        assertEquals("ana@empresa.com", dao.getUserById(id)!!.email)
    }

    @Test
    fun signatorySetsCpfWhenAdminDidNotRegisterIt(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "529.982.247-25")

        val stored = dao.getUserById(id)!!
        assertEquals("52998224725", stored.cpf)
        assertEquals(false, stored.cpfLockedByAdmin)
    }

    @Test
    fun signatoryCanChangeCpfHeRegistered(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)
        repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "529.982.247-25")

        repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "123.456.789-09")

        assertEquals("12345678909", dao.getUserById(id)!!.cpf)
    }

    @Test
    fun cpfRegisteredByAdminIsLockedForSignatory(): Unit = runBlocking {
        seed(UserRole.ADMIN, "Carlos Lima", "carlos@empresa.com", "11912345678", passwordHash = hasher.hash("senha1234"))
        repo.login("carlos@empresa.com", "senha1234")
        val id = repo.registerSignatory(
            "Ana Silva", "ana@empresa.com", "", AuthMode.PASSWORD, "senha1234", cpf = "529.982.247-25"
        )
        assertTrue(dao.getUserById(id)!!.cpfLockedByAdmin)
        repo.logout()
        repo.login("ana@empresa.com", "senha1234")

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "123.456.789-09") }
        }
        assertTrue(error.message!!.contains("não pode ser alterado"))

        // Enviar o mesmo CPF não é uma alteração e continua permitido.
        repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "529.982.247-25")
        assertEquals("52998224725", dao.getUserById(id)!!.cpf)
    }

    @Test
    fun adminClearingCpfUnlocksItForSignatory(): Unit = runBlocking {
        seed(UserRole.ADMIN, "Carlos Lima", "carlos@empresa.com", "11912345678", passwordHash = hasher.hash("senha1234"))
        repo.login("carlos@empresa.com", "senha1234")
        val id = repo.registerSignatory(
            "Ana Silva", "ana@empresa.com", "", AuthMode.PASSWORD, "senha1234", cpf = "529.982.247-25"
        )

        repo.updateSignatory(id, "Ana Silva", "ana@empresa.com", "", AuthMode.PASSWORD, null, cpf = "")

        val stored = dao.getUserById(id)!!
        assertNull(stored.cpf)
        assertEquals(false, stored.cpfLockedByAdmin)
    }

    @Test
    fun invalidCpfIsRejected(): Unit = runBlocking {
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "111.111.111-11") }
        }
        assertEquals("CPF inválido", error.message)
    }

    @Test
    fun cpfAlreadyUsedByAnotherUserIsRejected(): Unit = runBlocking {
        dao.insertUser(
            UserEntity(
                fullName = "Bruno Costa", email = "bruno@empresa.com", phone = "",
                passwordHash = null, role = UserRole.SIGNATORY, cpf = "52998224725"
            )
        )
        val id = seed(UserRole.SIGNATORY, "Ana Silva", "ana@empresa.com", "", passwordHash = hasher.hash("senha1234"))
        login(id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateOwnProfile("Ana Silva", "ana@empresa.com", "", cpf = "529.982.247-25") }
        }
        assertEquals("CPF já cadastrado", error.message)
    }

    private suspend fun login(id: Long) {
        // Abre a sessão pelo fluxo real de login de cada modo de autenticação.
        val user = dao.getUserById(id)!!
        if (user.authMode == AuthMode.PASSWORD) {
            repo.login(user.email, PASSWORD_OF_TEST_USERS)
        } else {
            val code = repo.requestLoginCode(user.email)
            repo.loginWithCode(user.email, code)
        }
    }

    private suspend fun seed(
        role: UserRole,
        name: String,
        email: String,
        phone: String,
        passwordHash: String?,
        authMode: AuthMode = AuthMode.PASSWORD
    ): Long = dao.insertUser(
        UserEntity(
            fullName = name, email = email, phone = phone,
            passwordHash = passwordHash, role = role, authMode = authMode
        )
    )

    private companion object {
        const val PASSWORD_OF_TEST_USERS = "senha1234"
    }
}

/** DAO em memória: implementa só o que o UserRepository usa. */
private class FakeUserDao : UserDao {
    private val users = mutableMapOf<Long, UserEntity>()
    private var nextId = 1L

    override suspend fun insertUser(user: UserEntity): Long {
        val id = nextId++
        users[id] = user.copy(id = id)
        return id
    }

    override suspend fun updateUser(user: UserEntity) {
        users[user.id] = user
    }

    override suspend fun getUserById(id: Long): UserEntity? = users[id]

    override suspend fun getUserByEmail(email: String): UserEntity? = users.values.firstOrNull { it.email == email }

    override suspend fun getUserByCpf(cpf: String): UserEntity? = users.values.firstOrNull { it.cpf == cpf }

    override suspend fun getUsersByRole(role: UserRole): List<UserEntity> = users.values.filter { it.role == role }

    override fun getAllSignatories(): Flow<List<UserEntity>> =
        flowOf(users.values.filter { it.role == UserRole.SIGNATORY })
}
