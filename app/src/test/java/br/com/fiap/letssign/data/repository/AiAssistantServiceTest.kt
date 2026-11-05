package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.AiDao
import br.com.fiap.letssign.data.local.entity.AiMessageEntity
import br.com.fiap.letssign.data.local.entity.DocumentAiSummaryEntity
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.domain.ai.ChatMessage
import br.com.fiap.letssign.domain.ai.ChatRole
import br.com.fiap.letssign.domain.ai.DocumentAssistant
import br.com.fiap.letssign.domain.repository.DocumentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class AiAssistantServiceTest {

    private val ownerId = 1L
    private val otherUserId = 2L
    private val documentId = 10L
    private var canAccess = true
    private var document: DocumentEntity? = DocumentEntity(
        id = documentId, title = "Contrato", filePath = "/docs/c.pdf", createdById = ownerId
    )
    private var pdfText = "O contratante pagará mil reais até o dia dez de cada mês. Há multa de atraso."
    private var extractCalls = 0

    private val dao = FakeAiDao()
    private val assistant = RecordingAssistant()
    private lateinit var service: AiAssistantService

    /** Repositório mínimo: só canAccess e getDocumentById são usados pelo serviço. */
    private val documents: DocumentRepository = Proxy.newProxyInstance(
        DocumentRepository::class.java.classLoader,
        arrayOf(DocumentRepository::class.java)
    ) { _, method, args ->
        when (method.name) {
            "canAccess" -> canAccess
            "getDocumentById" -> document
            else -> throw UnsupportedOperationException(method.name)
        }
    } as DocumentRepository

    @Before
    fun setUp() {
        service = AiAssistantService(
            documents = documents,
            dao = dao,
            extractText = { path ->
                extractCalls++
                assertEquals("/docs/c.pdf", path)
                pdfText
            },
            assistant = assistant
        )
    }

    @Test
    fun summaryIsCachedWhileTextDoesNotChange() = runBlocking {
        val first = service.summary(documentId, ownerId)
        val second = service.summary(documentId, ownerId)

        assertEquals(first, second)
        assertEquals(1, assistant.summarizeCalls)
        assertEquals(1, dao.summaries.size)
    }

    @Test
    fun summaryIsRegeneratedWhenPdfTextChanges() = runBlocking {
        service.summary(documentId, ownerId)
        pdfText = "Texto completamente novo do contrato, com outras cláusulas."
        service.summary(documentId, ownerId)

        assertEquals(2, assistant.summarizeCalls)
    }

    @Test
    fun forceRefreshIgnoresCache() = runBlocking {
        service.summary(documentId, ownerId)
        service.summary(documentId, ownerId, forceRefresh = true)

        assertEquals(2, assistant.summarizeCalls)
    }

    @Test
    fun userWithoutAccessGetsErrorAndPdfIsNotRead() = runBlocking {
        canAccess = false
        val error = assertThrows { service.summary(documentId, otherUserId) }

        assertTrue(error.message!!.contains("sem acesso"))
        assertEquals(0, extractCalls)
    }

    @Test
    fun missingDocumentGetsError() = runBlocking {
        document = null
        val error = assertThrows { service.summary(documentId, ownerId) }

        assertTrue(error.message!!.contains("não encontrado"))
    }

    @Test
    fun pdfWithoutTextGetsError() = runBlocking {
        pdfText = "   "
        val error = assertThrows { service.summary(documentId, ownerId) }

        assertTrue(error.message!!.contains("texto selecionável"))
        assertEquals(0, assistant.summarizeCalls)
    }

    @Test
    fun askStoresQuestionAndAnswer() = runBlocking {
        val answer = service.ask(documentId, ownerId, "Qual a multa?")

        assertEquals("resposta para: Qual a multa?", answer)
        val messages = dao.messages
        assertEquals(listOf("USER", "ASSISTANT"), messages.map { it.role })
        assertEquals("Qual a multa?", messages[0].content)
        assertEquals(answer, messages[1].content)
    }

    @Test
    fun askSendsPreviousMessagesAsHistory() = runBlocking {
        service.ask(documentId, ownerId, "primeira")
        service.ask(documentId, ownerId, "segunda")

        assertEquals(listOf(ChatMessage(ChatRole.USER, "primeira"),
            ChatMessage(ChatRole.ASSISTANT, "resposta para: primeira")),
            assistant.lastHistory)
    }

    @Test
    fun askHistoryIsLimitedToTenMessages() = runBlocking {
        repeat(12) { service.ask(documentId, ownerId, "pergunta $it") }

        assertEquals(10, assistant.lastHistory.size)
        assertEquals("pergunta 6", assistant.lastHistory.first().content)
    }

    @Test
    fun historyIsSeparatedByUser() = runBlocking {
        service.ask(documentId, ownerId, "do dono")
        service.ask(documentId, otherUserId, "de outro")

        assertEquals(listOf("do dono", "resposta para: do dono"), service.history(documentId, ownerId).map { it.content })
        assertEquals(listOf("de outro", "resposta para: de outro"), service.history(documentId, otherUserId).map { it.content })
    }

    private suspend fun assertThrows(block: suspend () -> Unit): Exception {
        try {
            block()
        } catch (e: Exception) {
            return e
        }
        fail("Esperava exceção")
        throw IllegalStateException()
    }

    private class RecordingAssistant : DocumentAssistant {
        var summarizeCalls = 0
        var lastHistory: List<ChatMessage> = emptyList()

        override suspend fun summarize(title: String, text: String): String {
            summarizeCalls++
            return "resumo $summarizeCalls"
        }

        override suspend fun answer(title: String, text: String, history: List<ChatMessage>, question: String): String {
            lastHistory = history
            return "resposta para: $question"
        }
    }

    private class FakeAiDao : AiDao {
        val summaries = mutableMapOf<Long, DocumentAiSummaryEntity>()
        val messages = mutableListOf<AiMessageEntity>()

        override suspend fun getSummary(documentId: Long) = summaries[documentId]

        override suspend fun upsertSummary(summary: DocumentAiSummaryEntity) {
            summaries[summary.documentId] = summary
        }

        override suspend fun getMessages(documentId: Long, userId: Long) =
            messages.filter { it.documentId == documentId && it.userId == userId }

        override suspend fun insertMessage(message: AiMessageEntity): Long {
            messages.add(message.copy(id = messages.size + 1L))
            return messages.size.toLong()
        }
    }
}
