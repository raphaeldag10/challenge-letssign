package br.com.fiap.letssign.data.ai

import br.com.fiap.letssign.domain.ai.ChatMessage
import br.com.fiap.letssign.domain.ai.ChatRole
import br.com.fiap.letssign.domain.ai.DocumentAssistant
import kotlinx.coroutines.delay

/**
 * Assistente simulado, sem rede: resume pelas primeiras frases e responde buscando trechos
 * que contenham as palavras da pergunta. Serve para validar a interface e o banco local.
 */
class MockDocumentAssistant(private val latencyMs: Long = 400L) : DocumentAssistant {

    override suspend fun summarize(title: String, text: String): String {
        delay(latencyMs)
        val sentences = splitSentences(text).take(MAX_SUMMARY_SENTENCES)
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        return buildString {
            append("Resumo simulado de \"$title\" (aprox. $words palavras).\n\n")
            if (sentences.isEmpty()) {
                append("Não há texto suficiente para resumir.")
            } else {
                sentences.forEach { append("• ").append(it).append('\n') }
            }
        }.trimEnd()
    }

    override suspend fun answer(
        title: String,
        text: String,
        history: List<ChatMessage>,
        question: String
    ): String {
        delay(latencyMs)
        val keywords = question.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .toSet()
        if (keywords.isEmpty()) {
            return "Faça uma pergunta mais específica sobre o documento."
        }
        val matches = splitSentences(text)
            .filter { sentence -> keywords.any { sentence.lowercase().contains(it) } }
            .take(MAX_ANSWER_SENTENCES)
        return if (matches.isEmpty()) {
            "Não encontrei trechos do documento relacionados a essa pergunta. " +
                "Leia o documento completo antes de assinar."
        } else {
            "Com base no documento:\n\n" + matches.joinToString("\n\n") { "“$it”" }
        }
    }

    private fun splitSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length >= MIN_SENTENCE_LENGTH }
            .map { if (it.length > MAX_SENTENCE_LENGTH) it.take(MAX_SENTENCE_LENGTH) + "…" else it }

    private companion object {
        const val MAX_SUMMARY_SENTENCES = 3
        const val MAX_ANSWER_SENTENCES = 3
        const val MIN_KEYWORD_LENGTH = 4
        const val MIN_SENTENCE_LENGTH = 20
        const val MAX_SENTENCE_LENGTH = 400
    }
}
