package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.theme.Tone
import br.com.fiap.letssign.ui.theme.toneColors
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.common.initialsOf
import br.com.fiap.letssign.ui.common.statusLabel
import br.com.fiap.letssign.ui.theme.LetssignTheme
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Paleta da tela de documentos (roxo), separada da cor azul da marca usada nas telas de acesso.
private val TopBarPurple = Color(0xFF3B1F7A)
private val Purple = Color(0xFF6D3FD1)
private val PdfRed = Color(0xFFE03131)


enum class DocumentFilter(val label: String) {
    ALL("Todos"), SENT("Enviados"), PARTIAL("Parciais"), COMPLETED("Concluídos");

    fun matches(status: DocumentStatus): Boolean = when (this) {
        ALL -> true
        SENT -> status == DocumentStatus.PENDING
        PARTIAL -> status == DocumentStatus.PARTIALLY_SIGNED
        COMPLETED -> status == DocumentStatus.COMPLETED
    }
}

/** Item exibido na lista: o resumo do banco mais o tamanho do PDF lido do armazenamento. */
data class DocumentListItem(
    val summary: DocumentSummary,
    val sizeBytes: Long
) {
    val fileName: String get() = summary.fileName ?: "${summary.title}.pdf"
}

/** Contagens do cabeçalho, calculadas sobre todos os documentos (não sobre o filtro). */
data class DocumentCounts(val awaiting: Int, val partial: Int, val completed: Int) {
    companion object {
        fun of(items: List<DocumentListItem>) = DocumentCounts(
            awaiting = items.count { it.summary.status == DocumentStatus.PENDING },
            partial = items.count { it.summary.status == DocumentStatus.PARTIALLY_SIGNED },
            completed = items.count { it.summary.status == DocumentStatus.COMPLETED }
        )
    }
}

class DocumentsViewModel(container: AppContainer) : ViewModel() {
    var items by mutableStateOf<List<DocumentListItem>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var filter by mutableStateOf(DocumentFilter.ALL)
    var query by mutableStateOf("")

    init {
        viewModelScope.launch {
            container.documentRepository.observeAllSummaries().collect { list ->
                // Lê o tamanho dos arquivos fora da thread principal.
                items = withContext(Dispatchers.IO) {
                    list.map { s ->
                        val size = runCatching { File(s.filePath).length() }.getOrDefault(0L)
                        DocumentListItem(s, size)
                    }
                }
                loaded = true
            }
        }
    }

    val counts: DocumentCounts get() = DocumentCounts.of(items)

    val visible: List<DocumentListItem>
        get() {
            val term = query.trim()
            return items.filter { item ->
                filter.matches(item.summary.status) &&
                    (term.isEmpty() || item.summary.title.contains(term, ignoreCase = true))
            }
        }
}

@Composable
fun DocumentsScreen(
    container: AppContainer,
    user: LoggedUser,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onAccount: () -> Unit
) {
    val vm = appViewModel { DocumentsViewModel(container) }
    DocumentsContent(
        loaded = vm.loaded,
        counts = vm.counts,
        items = vm.visible,
        filter = vm.filter,
        query = vm.query,
        initials = initialsOf(user.fullName),
        onFilter = { vm.filter = it },
        onQuery = { vm.query = it },
        onOpen = onOpen,
        onNew = onNew,
        onAccount = onAccount
    )
}

/** Conteúdo da tela sem estado de ViewModel: recebe tudo pronto, o que facilita o preview. */
@Composable
fun DocumentsContent(
    loaded: Boolean,
    counts: DocumentCounts,
    items: List<DocumentListItem>,
    filter: DocumentFilter,
    query: String,
    initials: String,
    onFilter: (DocumentFilter) -> Unit,
    onQuery: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onAccount: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DocumentsTopBar(initials = initials, onAccount = onAccount)

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Documentos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) { Text("+ Novo documento") }
            Text(
                "${counts.awaiting} aguardando · ${counts.partial} parciais · ${counts.completed} concluídos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SearchField(query = query, onQuery = onQuery)
            FilterChips(selected = filter, onSelect = onFilter)
        }

        when {
            !loaded -> Unit
            items.isEmpty() && query.isBlank() && filter == DocumentFilter.ALL ->
                EmptyState("Você ainda não enviou documentos.")
            items.isEmpty() -> EmptyState("Nenhum documento encontrado.")
            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.summary.id }) { item ->
                    DocumentCard(item = item, onClick = { onOpen(item.summary.id) })
                }
            }
        }
    }
}

@Composable
private fun DocumentsTopBar(initials: String, onAccount: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(TopBarPurple)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
            Text("LetsSign", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onAccount),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text("Admin", color = TopBarPurple, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar documento") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun FilterChips(selected: DocumentFilter, onSelect: (DocumentFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DocumentFilter.entries.forEach { f ->
            FilterChip(
                selected = selected == f,
                onClick = { onSelect(f) },
                label = { Text(f.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Purple,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun DocumentCard(item: DocumentListItem, onClick: () -> Unit) {
    val doc = item.summary
    val progress = if (doc.total == 0) 0f else doc.signed.toFloat() / doc.total
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PdfBadge()
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        doc.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${item.fileName} · ${formatFileSize(item.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip(doc.status)
                Text(
                    "${doc.signed} de ${doc.total} assinaturas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = Purple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PdfBadge() {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PdfRed.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text("PDF", color = PdfRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun StatusChip(status: DocumentStatus) {
    val (bg, fg) = when (status) {
        DocumentStatus.PENDING -> toneColors(Tone.INFO)
        DocumentStatus.PARTIALLY_SIGNED -> toneColors(Tone.WARNING)
        DocumentStatus.COMPLETED -> toneColors(Tone.SUCCESS)
    }
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(statusLabel(status), color = fg, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L -> "tamanho indisponível"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.forLanguageTag("pt-BR"), "%.0f KB", bytes / 1024.0)
    else -> String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / (1024.0 * 1024.0))
}

// ---------- Preview com dados de exemplo ----------

private fun sample(id: Long, title: String, status: DocumentStatus, signed: Int, total: Int, file: String, kb: Long) =
    DocumentListItem(
        summary = DocumentSummary(
            id = id,
            title = title,
            status = status,
            createdAt = 0L,
            versionNumber = 1,
            total = total,
            signed = signed,
            filePath = "",
            fileName = file
        ),
        sizeBytes = kb * 1024
    )

private val sampleItems = listOf(
    sample(1, "Contrato de prestação de serviços", DocumentStatus.PARTIALLY_SIGNED, 1, 3, "contrato_servicos.pdf", 842),
    sample(2, "Termo de confidencialidade", DocumentStatus.PENDING, 0, 2, "termo_confidencialidade.pdf", 316),
    sample(3, "Acordo de estágio", DocumentStatus.COMPLETED, 2, 2, "acordo_estagio.pdf", 1280)
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun DocumentsContentPreview() {
    LetssignTheme {
        DocumentsContent(
            loaded = true,
            counts = DocumentCounts.of(sampleItems),
            items = sampleItems,
            filter = DocumentFilter.ALL,
            query = "",
            initials = "RG",
            onFilter = {},
            onQuery = {},
            onOpen = {},
            onNew = {},
            onAccount = {}
        )
    }
}
