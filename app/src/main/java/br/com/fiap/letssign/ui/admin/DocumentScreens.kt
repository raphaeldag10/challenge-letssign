package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentHistoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.local.model.SignatoryDetail
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.ScreenScaffold
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.common.formatDate
import br.com.fiap.letssign.ui.common.openPdf
import br.com.fiap.letssign.ui.common.statusLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DocumentFilter(val label: String) {
    ALL("Todos"), SENT("Enviados"), PARTIAL("Parciais"), COMPLETED("Concluídos");

    fun matches(status: DocumentStatus): Boolean = when (this) {
        ALL -> true
        SENT -> status == DocumentStatus.PENDING
        PARTIAL -> status == DocumentStatus.PARTIALLY_SIGNED
        COMPLETED -> status == DocumentStatus.COMPLETED
    }
}

class DocumentsViewModel(container: AppContainer) : ViewModel() {
    var summaries by mutableStateOf<List<DocumentSummary>>(emptyList())
    var loaded by mutableStateOf(false)
    var filter by mutableStateOf(DocumentFilter.ALL)

    init {
        viewModelScope.launch {
            container.documentRepository.observeAllSummaries().collect {
                summaries = it
                loaded = true
            }
        }
    }

    val visible: List<DocumentSummary> get() = summaries.filter { filter.matches(it.status) }
}

@Composable
fun DocumentsScreen(container: AppContainer, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val vm = appViewModel { DocumentsViewModel(container) }
    ScreenScaffold(
        title = "Documentos",
        floatingAction = {
            ExtendedFloatingActionButton(onClick = onNew, text = { Text("Novo documento") }, icon = {})
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DocumentFilter.entries.forEach { f ->
                    FilterChip(
                        selected = vm.filter == f,
                        onClick = { vm.filter = f },
                        label = { Text(f.label) }
                    )
                }
            }
            when {
                !vm.loaded -> Unit
                vm.summaries.isEmpty() -> EmptyState("Você ainda não enviou documentos.") {
                    Button(onClick = onNew) { Text("Novo documento") }
                }
                vm.visible.isEmpty() -> EmptyState("Nenhum documento nesta situação.")
                else -> LazyColumn {
                    items(vm.visible, key = { it.id }) { doc ->
                        DocumentRow(doc, onClick = { onOpen(doc.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentRow(doc: DocumentSummary, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(doc.title, style = MaterialTheme.typography.titleMedium)
        Text(
            "${statusLabel(doc.status)} · ${doc.signed} de ${doc.total} assinaturas" +
                if (doc.versionNumber > 1) " · versão ${doc.versionNumber}" else "",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(formatDate(doc.createdAt), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.padding(top = 12.dp))
    }
}

class DocumentDetailViewModel(
    private val container: AppContainer,
    private val documentId: Long,
    private val user: LoggedUser
) : ViewModel() {
    var allowed by mutableStateOf<Boolean?>(null)
    var document by mutableStateOf<DocumentEntity?>(null)
    var finalPath by mutableStateOf<String?>(null)
    var participants by mutableStateOf<List<SignatoryDetail>>(emptyList())
    var history by mutableStateOf<List<DocumentHistoryEntity>>(emptyList())
    var error by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val repo = container.documentRepository
            allowed = repo.canAccess(documentId, user.id)
            if (allowed == true) {
                document = repo.getDocumentById(documentId)
                try {
                    finalPath = container.documentService.ensureFinalPdf(documentId)
                } catch (e: Exception) {
                    error = "Não foi possível gerar o PDF final agora."
                }
                launch { repo.observeSignatoryDetails(documentId).collect { participants = it } }
                launch { repo.observeHistory(documentId).collect { history = it } }
            }
        }
    }
}

@Composable
fun DocumentDetailScreen(
    container: AppContainer,
    documentId: Long,
    user: LoggedUser,
    onBack: () -> Unit,
    onNewVersion: () -> Unit
) {
    val vm = appViewModel(key = "detail-$documentId") { DocumentDetailViewModel(container, documentId, user) }
    val context = LocalContext.current
    ScreenScaffold(title = "Documento", onBack = onBack) { padding ->
        val doc = vm.document
        when {
            vm.allowed == null -> Unit
            vm.allowed == false || doc == null -> EmptyState("Documento não encontrado ou sem acesso.")
            else -> Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(doc.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${statusLabel(doc.status)} · versão ${doc.versionNumber} · enviado em ${formatDate(doc.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                doc.message?.let { Text("Mensagem: $it", style = MaterialTheme.typography.bodyMedium) }

                Text("Participantes", style = MaterialTheme.typography.titleMedium)
                vm.participants.forEach { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${p.fullName} (${roleLabel(p.roleType.name)})", style = MaterialTheme.typography.titleSmall)
                            Text(p.email, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (p.hasSigned && p.signedAt != null) "Assinado em ${formatDate(p.signedAt)}" else "Pendente",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Text("Histórico", style = MaterialTheme.typography.titleMedium)
                vm.history.forEach { h ->
                    Text("${formatDate(h.timestamp)} — ${h.description}", style = MaterialTheme.typography.bodyMedium)
                }

                ErrorText(vm.error)
                OutlinedButton(
                    onClick = { openPdf(context, container.files.shareUri(doc.filePath)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Abrir PDF original") }
                vm.finalPath?.let { path ->
                    Button(onClick = { openPdf(context, container.files.shareUri(path)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir PDF final")
                    }
                }
                OutlinedButton(onClick = onNewVersion, modifier = Modifier.fillMaxWidth()) {
                    Text("Criar nova versão")
                }
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "CONTRATANTE" -> "Contratante"
    "CONTRATADO" -> "Contratado"
    "TESTEMUNHA" -> "Testemunha"
    else -> role
}
