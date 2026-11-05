package br.com.fiap.letssign.domain.ai

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val content: String)

/**
 * Agente que resume um documento e responde dúvidas sobre ele.
 * O MVP usa [br.com.fiap.letssign.data.ai.MockDocumentAssistant]; um motor real (ex.: API da Claude)
 * implementa esta mesma interface.
 */
interface DocumentAssistant {
    suspend fun summarize(title: String, text: String): String

    suspend fun answer(
        title: String,
        text: String,
        history: List<ChatMessage>,
        question: String
    ): String
}
