package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.AiDao
import br.com.fiap.letssign.data.local.entity.AiMessageEntity
import br.com.fiap.letssign.data.local.entity.DocumentAiSummaryEntity
import br.com.fiap.letssign.domain.ai.ChatMessage
import br.com.fiap.letssign.domain.ai.ChatRole
import br.com.fiap.letssign.domain.ai.DocumentAssistant
import br.com.fiap.letssign.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** Erro de negócio exibível ao usuário (sem acesso, PDF sem texto, etc.). */
class AiException(message: String) : Exception(message)

/**
 * Orquestra o assistente: confere acesso, extrai o texto, usa o cache de resumo e grava o chat local.
 * [extractText] recebe o caminho do PDF; em produção é [br.com.fiap.letssign.data.file.PdfTextExtractor.extract].
 */
class AiAssistantService(
    private val documents: DocumentRepository,
    private val dao: AiDao,
    private val extractText: (String) -> String,
    private val assistant: DocumentAssistant
) {

    private class DocumentContext(val title: String, val text: String)

    /**
     * Retorna o resumo do cache se o texto não mudou; caso contrário (ou com [forceRefresh]), gera e grava um novo.
     */
    suspend fun summary(documentId: Long, userId: Long, forceRefresh: Boolean = false): String {
        val ctx = loadContext(documentId, userId)
        val hash = sha256(ctx.text)
        if (!forceRefresh) {
            dao.getSummary(documentId)
                ?.takeIf { it.textHash == hash }
                ?.let { return it.summary }
        }

        val summary = assistant.summarize(ctx.title, ctx.text)
        dao.upsertSummary(DocumentAiSummaryEntity(documentId = documentId, summary = summary, textHash = hash))
        return summary
    }

    suspend fun history(documentId: Long, userId: Long): List<AiMessageEntity> =
        dao.getMessages(documentId, userId)

    /** Grava a pergunta, responde com as últimas mensagens como contexto e grava a resposta. */
    suspend fun ask(documentId: Long, userId: Long, question: String): String {
        val ctx = loadContext(documentId, userId)
        val history = dao.getMessages(documentId, userId)
            .takeLast(MAX_HISTORY)
            .map { ChatMessage(ChatRole.valueOf(it.role), it.content) }

        dao.insertMessage(message(documentId, userId, ChatRole.USER, question))
        val answer = assistant.answer(ctx.title, ctx.text, history, question)
        dao.insertMessage(message(documentId, userId, ChatRole.ASSISTANT, answer))
        return answer
    }

    private suspend fun loadContext(documentId: Long, userId: Long): DocumentContext {
        if (!documents.canAccess(documentId, userId)) {
            throw AiException("Documento não encontrado ou sem acesso.")
        }
        val doc = documents.getDocumentById(documentId)
            ?: throw AiException("Documento não encontrado.")
        val text = withContext(Dispatchers.IO) { extractText(doc.filePath) }.take(MAX_CONTEXT_CHARS)
        if (text.isBlank()) {
            throw AiException("Este PDF não tem texto selecionável (pode ser escaneado).")
        }
        return DocumentContext(doc.title, text)
    }

    private fun message(documentId: Long, userId: Long, role: ChatRole, content: String) =
        AiMessageEntity(documentId = documentId, userId = userId, role = role.name, content = content)

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_CONTEXT_CHARS = 12_000
        const val MAX_HISTORY = 10
    }
}
