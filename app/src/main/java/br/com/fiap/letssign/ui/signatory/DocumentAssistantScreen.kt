package br.com.fiap.letssign.ui.signatory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.AiMessageEntity
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.domain.ai.ChatRole
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.appViewModel
import kotlinx.coroutines.launch

private const val DISCLAIMER =
    "Respostas geradas automaticamente. Elas não substituem a leitura completa do documento nem orientação jurídica."

class DocumentAssistantViewModel(
    private val container: AppContainer,
    private val documentId: Long,
    private val user: LoggedUser
) : ViewModel() {
    var title by mutableStateOf("")
    var summary by mutableStateOf<String?>(null)
    var summaryLoading by mutableStateOf(false)
    var messages by mutableStateOf<List<AiMessageEntity>>(emptyList())
    var asking by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var draft by mutableStateOf("")

    init {
        viewModelScope.launch {
            title = container.documentRepository.getDocumentById(documentId)?.title.orEmpty()
            refreshMessages()
            loadSummary(force = false)
        }
    }

    fun loadSummary(force: Boolean) {
        viewModelScope.launch {
            summaryLoading = true
            error = null
            try {
                summary = container.aiService.summary(documentId, user.id, forceRefresh = force)
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível gerar o resumo."
            } finally {
                summaryLoading = false
            }
        }
    }

    fun ask() {
        val question = draft.trim()
        if (question.isEmpty() || asking) return
        viewModelScope.launch {
            asking = true
            error = null
            try {
                container.aiService.ask(documentId, user.id, question)
                draft = ""
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível responder."
            } finally {
                refreshMessages()
                asking = false
            }
        }
    }

    private suspend fun refreshMessages() {
        messages = try {
            container.aiService.history(documentId, user.id)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Composable
fun DocumentAssistantScreen(
    container: AppContainer,
    documentId: Long,
    user: LoggedUser,
    onBack: () -> Unit
) {
    val vm = appViewModel(key = "assistant-$documentId-${user.id}") {
        DocumentAssistantViewModel(container, documentId, user)
    }
    FormScaffold(title = "Resumo e dúvidas", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text(vm.title, style = MaterialTheme.typography.titleMedium) }
                item { SummaryCard(vm) }
                item { Text("Dúvidas", style = MaterialTheme.typography.titleSmall) }
                if (vm.messages.isEmpty()) {
                    item {
                        Text(
                            "Pergunte algo sobre o documento, como \"qual o prazo?\" ou \"o que eu estou aprovando?\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(vm.messages) { message -> MessageBubble(message) }
                vm.error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
            Text(
                DISCLAIMER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = vm.draft,
                    onValueChange = { vm.draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Sua pergunta") },
                    enabled = !vm.asking,
                    maxLines = 3
                )
                Button(
                    onClick = vm::ask,
                    enabled = vm.draft.isNotBlank() && !vm.asking
                ) { Text("Enviar") }
            }
        }
    }
}

@Composable
private fun SummaryCard(vm: DocumentAssistantViewModel) {
    val summary = vm.summary
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumo", style = MaterialTheme.typography.titleSmall)
            when {
                vm.summaryLoading -> Text("Gerando resumo…", style = MaterialTheme.typography.bodyMedium)
                summary != null -> Text(summary, style = MaterialTheme.typography.bodyMedium)
                else -> Text("Resumo indisponível. Tente gerar novamente.", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(
                onClick = { vm.loadSummary(force = true) },
                enabled = !vm.summaryLoading
            ) { Text("Gerar novamente") }
        }
    }
}

@Composable
private fun MessageBubble(message: AiMessageEntity) {
    val isUser = message.role == ChatRole.USER.name
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
