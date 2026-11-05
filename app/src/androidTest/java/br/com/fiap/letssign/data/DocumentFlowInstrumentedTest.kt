package br.com.fiap.letssign.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fiap.letssign.data.local.AppDatabase
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignatoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.entity.SignatureMethod
import br.com.fiap.letssign.data.local.entity.SignatoryRoleType
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.local.entity.UserRole
import br.com.fiap.letssign.data.repository.RoomDocumentRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Fluxo de envio e assinatura com Room em memória. Roda em dispositivo ou emulador. */
@RunWith(AndroidJUnit4::class)
class DocumentFlowInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomDocumentRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomDocumentRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun user(role: UserRole, email: String): Long = runBlocking {
        db.userDao().insertUser(
            UserEntity(fullName = "Pessoa $email", email = email, phone = "11999999999", passwordHash = "x", role = role)
        )
    }

    private fun field(userId: Long, y: Float) = SignatureFieldEntity(
        documentId = 0, signatoryId = userId, pageNumber = 0,
        normalizedX = 0.1f, normalizedY = y, normalizedWidth = 0.3f, normalizedHeight = 0.07f
    )

    private fun newDocument(admin: Long) = DocumentEntity(title = "Contrato", filePath = "/tmp/contrato.pdf", createdById = admin)

    @Test
    fun sendWithFieldsReferencesGeneratedSignatoryIds() = runBlocking {
        val admin = user(UserRole.ADMIN, "admin@teste.com")
        val s1 = user(UserRole.SIGNATORY, "a@teste.com")
        val s2 = user(UserRole.SIGNATORY, "b@teste.com")

        val docId = repo.createDocumentFlow(
            newDocument(admin),
            listOf(
                DocumentSignatoryEntity(documentId = 0, userId = s1, roleType = SignatoryRoleType.CONTRATANTE),
                DocumentSignatoryEntity(documentId = 0, userId = s2, roleType = SignatoryRoleType.CONTRATADO)
            ),
            listOf(field(s1, 0.1f), field(s2, 0.3f))
        )

        val signatories = repo.getSignatoriesForDocument(docId)
        val fields = repo.getSignatureFieldsForDocument(docId)
        assertEquals(2, fields.size)
        fields.forEach { f -> assertTrue(signatories.any { it.id == f.signatoryId }) }
    }

    @Test
    fun sendWithoutFieldForSignatoryIsRejected() {
        val admin = user(UserRole.ADMIN, "admin2@teste.com")
        val s1 = user(UserRole.SIGNATORY, "c@teste.com")
        try {
            runBlocking {
                repo.createDocumentFlow(
                    newDocument(admin),
                    listOf(DocumentSignatoryEntity(documentId = 0, userId = s1, roleType = SignatoryRoleType.CONTRATANTE)),
                    emptyList()
                )
            }
            fail("Esperava recusa do envio sem campo para o signatário")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("campo"))
        }
    }

    @Test
    fun statusGoesFromPendingToPartialToCompleted() = runBlocking {
        val admin = user(UserRole.ADMIN, "admin3@teste.com")
        val s1 = user(UserRole.SIGNATORY, "d@teste.com")
        val s2 = user(UserRole.SIGNATORY, "e@teste.com")
        val docId = repo.createDocumentFlow(
            newDocument(admin),
            listOf(
                DocumentSignatoryEntity(documentId = 0, userId = s1, roleType = SignatoryRoleType.CONTRATANTE),
                DocumentSignatoryEntity(documentId = 0, userId = s2, roleType = SignatoryRoleType.TESTEMUNHA)
            ),
            listOf(field(s1, 0.1f), field(s2, 0.3f))
        )

        assertEquals(DocumentStatus.PARTIALLY_SIGNED, repo.signDocument(docId, s1, "/tmp/a.png", SignatureMethod.DRAW, "Pessoa d@teste.com"))
        assertEquals(DocumentStatus.COMPLETED, repo.signDocument(docId, s2, "/tmp/b.png", SignatureMethod.DRAW, "Pessoa e@teste.com"))
        assertEquals(DocumentStatus.COMPLETED, repo.getDocumentById(docId)?.status)
    }

    @Test
    fun signingTwiceIsRejected() = runBlocking {
        val admin = user(UserRole.ADMIN, "admin4@teste.com")
        val s1 = user(UserRole.SIGNATORY, "f@teste.com")
        val s2 = user(UserRole.SIGNATORY, "g@teste.com")
        val docId = repo.createDocumentFlow(
            newDocument(admin),
            listOf(
                DocumentSignatoryEntity(documentId = 0, userId = s1, roleType = SignatoryRoleType.CONTRATANTE),
                DocumentSignatoryEntity(documentId = 0, userId = s2, roleType = SignatoryRoleType.CONTRATADO)
            ),
            listOf(field(s1, 0.1f), field(s2, 0.3f))
        )
        repo.signDocument(docId, s1, "/tmp/a.png", SignatureMethod.DRAW, "Pessoa f@teste.com")
        try {
            repo.signDocument(docId, s1, "/tmp/a2.png", SignatureMethod.DRAW, "Pessoa f@teste.com")
            fail("Esperava recusa de assinatura repetida")
        } catch (e: IllegalArgumentException) {
            assertEquals("Assinatura já registrada", e.message)
        }
    }

    @Test
    fun onlyCreatorAndParticipantsCanAccess() = runBlocking {
        val admin = user(UserRole.ADMIN, "admin5@teste.com")
        val s1 = user(UserRole.SIGNATORY, "h@teste.com")
        val outsider = user(UserRole.SIGNATORY, "i@teste.com")
        val docId = repo.createDocumentFlow(
            newDocument(admin),
            listOf(DocumentSignatoryEntity(documentId = 0, userId = s1, roleType = SignatoryRoleType.CONTRATANTE)),
            listOf(field(s1, 0.1f))
        )
        assertTrue(repo.canAccess(docId, admin))
        assertTrue(repo.canAccess(docId, s1))
        assertFalse(repo.canAccess(docId, outsider))
    }
}
