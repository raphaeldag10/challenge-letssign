package br.com.fiap.letssign.ui.signatory

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import br.com.fiap.letssign.R
import br.com.fiap.letssign.ui.theme.Tone
import br.com.fiap.letssign.ui.theme.toneColors
import br.com.fiap.letssign.ui.common.BrandLogo
import br.com.fiap.letssign.ui.common.firstNameOf
import br.com.fiap.letssign.ui.common.initialsOf
import br.com.fiap.letssign.ui.theme.BrandBlue
import br.com.fiap.letssign.ui.theme.BrandBlueSoft
import java.text.SimpleDateFormat
import java.util.Locale
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

class SignatoryHomeViewModel(container: AppContainer, userId: Long) : ViewModel() {
    var pending by mutableStateOf<List<DocumentSummary>>(emptyList())
    var signed by mutableStateOf<List<DocumentSummary>>(emptyList())

    init {
        val repo = container.documentRepository
        viewModelScope.launch { repo.observeSummariesForSignatory(userId, signed = false).collect { pending = it } }
        viewModelScope.launch { repo.observeSummariesForSignatory(userId, signed = true).collect { signed = it } }
    }
}

private val SignGreen = Color(0xFF22A447)
private val PdfRed = Color(0xFFE5483D)
private val CardShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(12.dp)
private val receivedFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))

@Composable
fun SignatoryHomeScreen(
    container: AppContainer,
    user: LoggedUser,
    onOpen: (Long) -> Unit,
    onSign: (Long) -> Unit,
    onAccount: () -> Unit
) {
    val vm = appViewModel(key = "home-${user.id}") { SignatoryHomeViewModel(container, user.id) }
    Scaffold(
        topBar = { HomeTopBar(user, onAccount) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Documentos") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onAccount,
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Perfil") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Greeting(user.fullName, vm.pending.size) }
            items(vm.pending, key = { "p-${it.id}" }) { doc ->
                SignatoryDocumentCard(doc, pending = true, onSign = { onSign(doc.id) }, onOpen = { onOpen(doc.id) })
            }
            if (vm.signed.isNotEmpty()) {
                item {
                    Text(
                        "Assinados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(vm.signed, key = { "s-${it.id}" }) { doc ->
                    SignatoryDocumentCard(doc, pending = false, onSign = {}, onOpen = { onOpen(doc.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(user: LoggedUser, onAccount: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        BrandLogo(iconSize = 32)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notificações")
        }
        Spacer(Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAccount)
        ) {
            Text(initialsOf(user.fullName), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Greeting(fullName: String, pendingCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                "Olá, ${firstNameOf(fullName)}! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                when (pendingCount) {
                    0 -> "Tudo em dia. Nenhum documento aguardando sua assinatura."
                    1 -> "Você tem 1 documento pendente de assinatura."
                    else -> "Você tem $pendingCount documentos pendentes de assinatura."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(start = 12.dp).size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_letssign_logo),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun SignatoryDocumentCard(doc: DocumentSummary, pending: Boolean, onSign: () -> Unit, onOpen: () -> Unit) {
    OutlinedCard(
        shape = CardShape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(width = 30.dp, height = 34.dp).background(PdfRed, RoundedCornerShape(6.dp))
                ) {
                    Text("PDF", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(pending)
            }
            InfoLine(Icons.Outlined.Person, "Enviado por: ", doc.senderName ?: "Administrador")
            InfoLine(Icons.Outlined.DateRange, "Recebido em: ", receivedFormat.format(doc.createdAt).replace(".", ""))
            if (pending) {
                Button(
                    onClick = onSign,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SignGreen, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Filled.Create, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Assinar agora", fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(
                onClick = onOpen,
                shape = ButtonShape,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver documento", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun StatusChip(pending: Boolean) {
    Text(
        if (pending) "Pendente" else "Assinado",
        color = if (pending) toneColors(Tone.WARNING).second else SignGreen,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(if (pending) toneColors(Tone.WARNING).first else toneColors(Tone.SUCCESS).first, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                append(label)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(value)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    var alreadySigned by mutableStateOf(false)

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
                if (e.message == "Assinatura já registrada") {
                    // Já existe uma assinatura deste participante (outro acesso ou toque repetido):
                    // não assina de novo; mostra a conclusão do registro existente.
                    alreadySigned = true
                    done = true
                } else {
                    error = e.message
                }
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
                Text(
                    if (vm.alreadySigned) "Sua assinatura já estava registrada." else "Sua participação foi concluída.",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    if (vm.alreadySigned) "Nada foi assinado novamente. O registro foi recarregado."
                    else "O documento só fica concluído quando todos os signatários obrigatórios assinarem."
                )
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
