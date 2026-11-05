package br.com.fiap.letssign.data.ai

import br.com.fiap.letssign.domain.ai.ChatMessage
import br.com.fiap.letssign.domain.ai.ChatRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDocumentAssistantTest {

    private val assistant = MockDocumentAssistant(latencyMs = 0)
    private val text = "O contratante pagará o valor de mil reais até o dia dez de cada mês. " +
        "A multa por atraso é de dez por cento sobre o valor devido. " +
        "Este contrato vigora por doze meses a partir da assinatura."

    @Test
    fun summarize_includesTitleAndFirstSentences() = runBlocking {
        val summary = assistant.summarize("Contrato", text)
        assertTrue(summary.contains("Contrato"))
        assertTrue(summary.contains("O contratante pagará"))
        assertTrue(summary.contains("Este contrato vigora"))
    }

    @Test
    fun summarize_blankTextSaysNotEnough() = runBlocking {
        val summary = assistant.summarize("Vazio", "   ")
        assertTrue(summary.contains("Não há texto suficiente"))
    }

    @Test
    fun answer_returnsMatchingSentence() = runBlocking {
        val answer = assistant.answer("Contrato", text, emptyList(), "Qual é a multa por atraso?")
        assertTrue(answer.contains("A multa por atraso"))
        assertFalse(answer.contains("doze meses"))
    }

    @Test
    fun answer_withoutMatchSaysNotFound() = runBlocking {
        val answer = assistant.answer("Contrato", text, emptyList(), "Existe cláusula de foro?")
        assertTrue(answer.contains("Não encontrei"))
    }

    @Test
    fun answer_shortQuestionAsksForMoreDetail() = runBlocking {
        val history = listOf(ChatMessage(ChatRole.USER, "oi"))
        val answer = assistant.answer("Contrato", text, history, "e?")
        assertTrue(answer.contains("mais específica"))
    }
}
