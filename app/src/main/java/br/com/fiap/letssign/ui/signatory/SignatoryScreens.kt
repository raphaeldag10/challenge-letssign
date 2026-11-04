package br.com.fiap.letssign.ui.signatory

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.file.FieldPlacement
import br.com.fiap.letssign.data.file.PdfPages
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.PdfPageImage
import br.com.fiap.letssign.ui.common.ReadOnlyFields
import br.com.fiap.letssign.ui.common.ScreenScaffold
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.common.formatDate
import br.com.fiap.letssign.ui.common.openPdf
import br.com.fiap.letssign.ui.common.statusLabel
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import br.com.fiap.letssign.ui.admin.DocumentRow

class SignatoryHomeViewModel(container: AppContainer, userId: Long) : ViewModel() {
    var pending by mutableStateOf<List<DocumentSummary>>(emptyList())
    var signed by mutableStateOf<List<DocumentSummary>>(emptyList())

    init {
        val repo = container.documentRepository
        viewModelScope.launch { repo.observeSummariesForSignatory(userId, signed = false).collect { pending = it } }
        viewModelScope.launch { repo.observeSummariesForSignatory(userId, signed = true).collect { signed = it } }
    }
}

@Composable
fun SignatoryHomeScreen(container: AppContainer, user: LoggedUser, onOpen: (Long) -> Unit, onAccount: () -> Unit) {
    val vm = appViewModel(key = "home-${user.id}") { SignatoryHomeViewModel(container, user.id) }
    var tab by remember { mutableStateOf(0) }
    ScreenScaffold(title = "Documentos recebidos") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onAccount) { Text("Conta") }
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pendentes") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Assinados") })
            }
            val list = if (tab == 0) vm.pending else vm.signed
            if (list.isEmpty()) {
                EmptyState(
                    if (tab == 0) "Tudo em dia. Nenhum documento aguardando sua assinatura."
                    else "Você ainda não assinou nenhum documento."
                )
            } else {
                LazyColumn {
                    items(list, key = { it.id }) { doc -> DocumentRow(doc, onClick = { onOpen(doc.id) }) }
                }
            }
        }
    }
}

class SignatoryDocumentViewModel(
    private val container: AppContainer,
    private val documentId: Long,
    private val user: LoggedUser
) : ViewModel() {
    var allowed by mutableStateOf<Boolean?>(null)
    var title by mutableStateOf("")
    var status by mutableStateOf(DocumentStatus.PENDING)
    var pages by mutableStateOf<PdfPages?>(null)
    var pageSizes by mutableStateOf<List<Pair<Float, Float>>>(emptyList())
    var placed by mutableStateOf<List<FieldPlacement>>(emptyList())
    var canSign by mutableStateOf(false)
    var finalPath by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            try {
                val repo = container.documentRepository
                allowed = repo.canAccess(documentId, user.id)
                if (allowed != true) return@launch
                val doc = repo.getDocumentById(documentId) ?: return@launch
                title = doc.title
                status = doc.status

                val handle = withContext(Dispatchers.IO) { PdfPages.open(doc.filePath) }
                pages = handle
                pageSizes = withContext(Dispatchers.IO) { (0 until handle.pageCount).map { handle.pageSize(it) } }

                val signatories = repo.getSignatoriesForDocument(documentId)
                placed = repo.getSignatureFieldsForDocument(documentId).mapNotNull { f ->
                    val s = signatories.firstOrNull { it.id == f.signatoryId } ?: return@mapNotNull null
                    FieldPlacement(f.pageNumber, f.normalizedX, f.normalizedY, f.normalizedWidth, f.normalizedHeight, s.userId)
                }
                canSign = repo.getSignatoryRecord(documentId, user.id)?.hasSigned == false
                if (status == DocumentStatus.COMPLETED) {
                    finalPath = container.documentService.ensureFinalPdf(documentId)
                }
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível abrir este documento."
            }
        }
    }

    fun aspect(page: Int): Float = pageSizes.getOrNull(page)?.let { it.first / it.second } ?: (1f / 1.414f)

    override fun onCleared() {
        pages?.close()
        pages = null
    }
}

@Composable
fun SignatoryDocumentScreen(
    container: AppContainer,
    documentId: Long,
    user: LoggedUser,
    onSign: () -> Unit,
    onBack: () -> Unit
) {
    val vm = appViewModel(key = "view-$documentId-${user.id}") { SignatoryDocumentViewModel(container, documentId, user) }
    val context = LocalContext.current
    ScreenScaffold(title = "Documento", onBack = onBack) { padding ->
        when {
            vm.allowed == null && vm.error == null -> Unit
            vm.allowed == false || vm.error != null -> EmptyState(vm.error ?: "Documento não encontrado ou sem acesso.")
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(vm.title, style = MaterialTheme.typography.titleLarge)
                    Text(statusLabel(vm.status), style = MaterialTheme.typography.bodyMedium)
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val handle = vm.pages
                    if (handle != null) {
                        for (page in 0 until handle.pageCount) {
                            PdfPageImage(pages = handle, pageIndex = page, aspect = vm.aspect(page)) { size ->
                                ReadOnlyFields(
                                    fields = vm.placed.filter { it.pageNumber == page },
                                    size = size,
                                    highlightUserId = user.id
                                )
                            }
                        }
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vm.canSign) {
                        Button(onClick = onSign, modifier = Modifier.fillMaxWidth()) { Text("Assinar") }
                    } else if (vm.status != DocumentStatus.COMPLETED) {
                        Text("Você já assinou este documento. Aguardando os demais participantes.")
                    }
                    vm.finalPath?.let { path ->
                        OutlinedButton(onClick = { openPdf(context, container.files.shareUri(path)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Abrir PDF final")
                        }
                    }
                }
            }
        }
    }
}

class SignatureViewModel(
    private val container: AppContainer,
    private val documentId: Long,
    private val user: LoggedUser
) : ViewModel() {
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var done by mutableStateOf(false)

    fun save(strokes: List<List<PointF>>, padWidth: Int, padHeight: Int) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                val signaturePath = withContext(Dispatchers.IO) {
                    container.files.saveSignature(strokes, padWidth, padHeight)
                }
                val status = withContext(Dispatchers.IO) {
                    container.documentRepository.signDocument(documentId, user.id, signaturePath)
                }
                if (status == DocumentStatus.COMPLETED) {
                    // Se a geração do PDF final falhar, ele é tentado de novo ao abrir o documento.
                    runCatching { container.documentService.ensureFinalPdf(documentId) }
                }
                done = true
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (e: Exception) {
                error = "Não foi possível confirmar o salvamento. Seu desenho foi preservado; tente novamente."
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun SignatureScreen(container: AppContainer, documentId: Long, user: LoggedUser, onDone: () -> Unit) {
    val vm = appViewModel(key = "sign-$documentId-${user.id}") { SignatureViewModel(container, documentId, user) }
    val strokes = remember { mutableStateOf(listOf<List<Offset>>()) }
    val current = remember { mutableStateOf(listOf<Offset>()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    ScreenScaffold(title = "Assinar", onBack = onDone) { padding ->
        if (vm.done) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Sua participação foi concluída.", style = MaterialTheme.typography.titleLarge)
                Text("O documento só fica concluído quando todos os signatários obrigatórios assinarem.")
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Concluir") }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Desenhe sua assinatura no espaço abaixo.")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .border(1.dp, Color.Gray)
                        .onSizeChanged { padSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> current.value = listOf(offset) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    current.value = current.value + change.position
                                },
                                onDragEnd = {
                                    if (current.value.isNotEmpty()) strokes.value = strokes.value + listOf(current.value)
                                    current.value = emptyList()
                                },
                                onDragCancel = { current.value = emptyList() }
                            )
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        (strokes.value + listOf(current.value)).filter { it.isNotEmpty() }.forEach { stroke ->
                            val path = Path()
                            stroke.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                            drawPath(path, Color.Black, style = Stroke(width = 4f, cap = StrokeCap.Round))
                        }
                    }
                }
                ErrorText(vm.error)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { strokes.value = emptyList() },
                        enabled = strokes.value.isNotEmpty() && !vm.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Limpar") }
                    Button(
                        onClick = {
                            val all = strokes.value.map { s -> s.map { PointF(it.x, it.y) } }
                            vm.save(all, padSize.width, padSize.height)
                        },
                        enabled = strokes.value.isNotEmpty() && !vm.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (vm.busy) "Salvando…" else "Confirmar e salvar") }
                }
            }
        }
    }
}
