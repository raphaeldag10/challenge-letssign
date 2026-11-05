package br.com.fiap.letssign.ui.signatory

import android.graphics.PointF
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import br.com.fiap.letssign.data.local.entity.allowedMethods
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import br.com.fiap.letssign.data.signature.CertificateSignature
import br.com.fiap.letssign.data.signature.IcpBrasilSigner
import java.io.File
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
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.BrandTopBar
import br.com.fiap.letssign.ui.common.BrandLogo
import br.com.fiap.letssign.ui.common.firstNameOf
import br.com.fiap.letssign.ui.common.initialsOf
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
import androidx.compose.foundation.layout.imePadding
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
import br.com.fiap.letssign.data.local.entity.SignatureMethod
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.PdfPageImage
import br.com.fiap.letssign.ui.common.ReadOnlyFields
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.common.formatDate
import br.com.fiap.letssign.ui.common.openPdf
import br.com.fiap.letssign.ui.common.statusLabel
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.file.TYPED_STYLE_COUNT
import br.com.fiap.letssign.ui.common.FieldShape

class SignatoryHomeViewModel(container: AppContainer, userId: Long) : ViewModel() {
    var pending by mutableStateOf<List<DocumentSummary>>(emptyList())
    var signed by mutableStateOf<List<DocumentSummary>>(emptyList())

    init {
        val repo = container.documentRepository
        viewModelScope.launch { repo.observeSummariesForSignatory(userId, signed = false).collect { pending = it } }
        viewModelScope.launch { repo.observeSummariesForSignatory(userId, signed = true).collect { signed = it } }
    }
}

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
        modifier = Modifier.imePadding(),
        topBar = {
            BrandTopBar(
                initials = initialsOf(user.fullName),
                onAccount = onAccount,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificações")
                    }
                }
            )
        },
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
        color = if (pending) toneColors(Tone.WARNING).second else toneColors(Tone.SUCCESS).second,
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
    /** Modos que o admin liberou; nulo até o documento carregar. */
    var allowedMethods by mutableStateOf<Set<SignatureMethod>?>(null)
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
                allowedMethods = doc.allowedMethods()
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
    onAssistant: () -> Unit,
    onBack: () -> Unit
) {
    val vm = appViewModel(key = "view-$documentId-${user.id}") { SignatoryDocumentViewModel(container, documentId, user) }
    val context = LocalContext.current
    FormScaffold(title = "Documento", onBack = onBack) { padding ->
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
                    OutlinedButton(onClick = onAssistant, modifier = Modifier.fillMaxWidth()) {
                        Text("Resumo e dúvidas")
                    }
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

    /** O que o signatário está desenhando ou digitando; aparece no campo dele no documento. */
    var preview by mutableStateOf<SignaturePreview?>(null)

    /** Assinatura desenhada: traços em coordenadas do bloco de desenho (padWidth x padHeight). */
    fun saveDrawn(strokes: List<List<PointF>>, padWidth: Int, padHeight: Int) =
        register(SignatureMethod.DRAW) {
            SignatureInput(container.files.saveSignature(strokes, padWidth, padHeight), user.fullName)
        }

    /** Assinatura digitada: o nome é desenhado com o estilo escolhido. */
    fun saveTyped(name: String, styleIndex: Int) =
        register(SignatureMethod.TYPED) {
            SignatureInput(container.files.saveTypedSignature(name, styleIndex), name.trim())
        }

    /**
     * Assinatura com certificado ICP-Brasil (A1 em .p12/.pfx). Assina o PDF original e grava a
     * assinatura CAdES; a imagem visual é o nome do titular, no mesmo estilo da assinatura digitada.
     * A senha é apagada da memória ao final.
     */
    fun saveCertificate(pkcs12: ByteArray, password: CharArray) =
        register(SignatureMethod.CERTIFICATE) {
            try {
                val document = requireNotNull(container.documentRepository.getDocumentById(documentId)) {
                    "Documento não encontrado"
                }
                val cpf = container.userRepository.getSignatory(user.id)?.cpf
                    ?: throw IllegalArgumentException("Cadastre seu CPF no perfil para assinar com certificado")
                val cert = IcpBrasilSigner.signPdf(pkcs12, password, cpf, File(document.filePath).readBytes())
                SignatureInput(container.files.saveTypedSignature(cert.signerName, 0), cert.signerName, cert)
            } finally {
                password.fill('\u0000')
            }
        }

    /** Resultado de uma assinatura: imagem visual, nome do signatário e certificado (se houver). */
    private class SignatureInput(
        val imagePath: String,
        val signerName: String,
        val certificate: CertificateSignature? = null
    )

    private fun register(method: SignatureMethod, makeSignature: suspend () -> SignatureInput) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                val input = withContext(Dispatchers.IO) { makeSignature() }
                val status = withContext(Dispatchers.IO) {
                    container.documentRepository.signDocument(
                        documentId, user.id, input.imagePath, method, input.signerName, input.certificate
                    )
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
                error = "Não foi possível confirmar o salvamento. Sua assinatura foi preservada; tente novamente."
            } finally {
                busy = false
            }
        }
    }
}

/** Modos de assinatura da tela, com o modo correspondente no documento. */
private enum class SignTab(val label: String, val method: SignatureMethod) {
    DRAW("Desenhar", SignatureMethod.DRAW),
    TYPE("Digitar", SignatureMethod.TYPED),
    CERTIFICATE("Certificado", SignatureMethod.CERTIFICATE)
}

/** Prévia da assinatura em curso, desenhada no campo do signatário dentro do documento. */
sealed interface SignaturePreview {
    data class Drawn(val strokes: List<List<Offset>>) : SignaturePreview
    data class Typed(val name: String, val style: Int) : SignaturePreview
}

@Composable
fun SignatureScreen(container: AppContainer, documentId: Long, user: LoggedUser, onDone: () -> Unit) {
    val vm = appViewModel(key = "sign-$documentId-${user.id}") { SignatureViewModel(container, documentId, user) }
    // Mesma chave da tela de visualização: reaproveita as páginas já carregadas do PDF.
    val doc = appViewModel(key = "view-$documentId-${user.id}") { SignatoryDocumentViewModel(container, documentId, user) }

    FormScaffold(title = "Assinar", onBack = onDone) { padding ->
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
            Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DocumentBox(doc, user.id, vm.preview, Modifier.weight(0.55f))
                doc.allowedMethods?.let { SignatureBox(vm, it, Modifier.weight(0.45f)) }
                    ?: Box(Modifier.weight(0.45f))
            }
        }
    }
}

/** Caixa de cima: o documento com os campos, rolando na tela. O campo do signatário fica em destaque. */
@Composable
private fun DocumentBox(doc: SignatoryDocumentViewModel, userId: Long, preview: SignaturePreview?, modifier: Modifier) {
    val shape = FieldShape
    val firstOwnField = doc.placed.firstOrNull { it.userId == userId }
    val ownFieldRequester = remember { BringIntoViewRequester() }
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val handle = doc.pages
        when {
            doc.error != null -> ErrorText(doc.error)
            doc.allowed == false -> Text("Documento sem acesso.")
            handle == null -> Text("Carregando documento…", style = MaterialTheme.typography.bodySmall)
            else -> for (page in 0 until handle.pageCount) {
                PdfPageImage(pages = handle, pageIndex = page, aspect = doc.aspect(page)) { size ->
                    ReadOnlyFields(
                        fields = doc.placed.filter { it.pageNumber == page },
                        size = size,
                        highlightUserId = userId
                    )
                    doc.placed.filter { it.pageNumber == page && it.userId == userId }.forEach { field ->
                        SignatureFieldPreview(
                            field = field,
                            size = size,
                            preview = preview,
                            requester = ownFieldRequester.takeIf { field == firstOwnField }
                        )
                    }
                }
            }
        }
    }
}

/** Caixa de baixo: só os modos liberados pelo admin e o espaço para assinar. */
@Composable
private fun SignatureBox(vm: SignatureViewModel, allowed: Set<SignatureMethod>, modifier: Modifier) {
    val options = SignTab.entries.filter { it.method in allowed }
    var selected by rememberSaveable { mutableStateOf(options.firstOrNull() ?: SignTab.DRAW) }
    val tab = if (selected in options) selected else options.firstOrNull() ?: SignTab.DRAW
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (options.isEmpty()) {
            Text(
                "Nenhum modo de assinatura foi liberado para este documento. Fale com o administrador.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TabRow(selectedTabIndex = options.indexOf(tab)) {
                options.forEach { option ->
                    Tab(
                        selected = tab == option,
                        onClick = { selected = option },
                        text = { Text(option.label) }
                    )
                }
            }
            when (tab) {
                SignTab.DRAW -> DrawPad(vm)
                SignTab.TYPE -> TypePad(vm)
                SignTab.CERTIFICATE -> CertificatePad(vm)
            }
        }
    }
}

@Composable
private fun ColumnScope.DrawPad(vm: SignatureViewModel) {
    val strokes = remember { mutableStateOf(listOf<List<Offset>>()) }
    val current = remember { mutableStateOf(listOf<Offset>()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(strokes.value, current.value) {
        val all = (strokes.value + listOf(current.value)).filter { it.isNotEmpty() }
        vm.preview = if (all.isEmpty()) null else SignaturePreview.Drawn(all)
    }
    DisposableEffect(Unit) { onDispose { vm.preview = null } }
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(FieldShape)
            .background(Color.White) // Área de desenho com fundo de papel, nos dois modos.
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FieldShape)
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
                vm.saveDrawn(all, padSize.width, padSize.height)
            },
            enabled = strokes.value.isNotEmpty() && !vm.busy,
            modifier = Modifier.weight(1f)
        ) { Text(if (vm.busy) "Salvando…" else "Confirmar") }
    }
}

@Composable
private fun ColumnScope.TypePad(vm: SignatureViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf(0) }
    val valid = name.trim().length >= 3
    LaunchedEffect(name, style, valid) {
        vm.preview = if (valid) SignaturePreview.Typed(name.trim(), style) else null
    }
    DisposableEffect(Unit) { onDispose { vm.preview = null } }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome completo") },
        singleLine = true,
        shape = FieldShape,
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        for (index in 0 until TYPED_STYLE_COUNT) {
            OutlinedButton(
                onClick = { style = index },
                modifier = Modifier.weight(1f),
                border = BorderStroke(if (style == index) 2.dp else 1.dp, if (style == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) { Text("Estilo ${index + 1}") }
        }
    }
    // Prévia: mostra exatamente o PNG que será salvo.
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FieldShape),
        contentAlignment = Alignment.Center
    ) {
        if (valid) {
            Text(
                name.trim(),
                color = Color.Black,
                fontSize = 36.sp,
                fontFamily = typedFontFamily(style),
                fontWeight = if (style == 1) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (style == 2) FontStyle.Italic else FontStyle.Normal
            )
        } else {
            Text("Sua assinatura aparece aqui", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
    ErrorText(vm.error)
    Button(
        onClick = { vm.saveTyped(name, style) },
        enabled = valid && !vm.busy,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (vm.busy) "Salvando…" else "Confirmar") }
}

@Composable
private fun ColumnScope.CertificatePad(vm: SignatureViewModel) {
    val context = LocalContext.current
    var pkcs12 by remember { mutableStateOf<ByteArray?>(null) }
    var password by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pkcs12 = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (pkcs12 == null) vm.error = "Não foi possível ler o arquivo do certificado"
        }
    }
    Text(
        "Certificado A1 (.p12 ou .pfx) da ICP-Brasil, no seu CPF. O arquivo não fica salvo no aplicativo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(
        onClick = { picker.launch(arrayOf("application/x-pkcs12", "application/octet-stream")) },
        enabled = !vm.busy,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (pkcs12 != null) "Certificado selecionado" else "Escolher certificado") }
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Senha do certificado") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = FieldShape,
        enabled = !vm.busy,
        modifier = Modifier.fillMaxWidth()
    )
    ErrorText(vm.error)
    Spacer(Modifier.weight(1f))
    Button(
        onClick = {
            val bytes = pkcs12 ?: return@Button
            val typed = password.toCharArray()
            password = "" // a senha sai do estado da tela assim que é enviada
            vm.saveCertificate(bytes, typed)
        },
        enabled = pkcs12 != null && password.isNotEmpty() && !vm.busy,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (vm.busy) "Assinando…" else "Assinar com certificado") }
}

/**
 * Campo do signatário no documento. Mostra a assinatura em curso no mesmo lugar onde ela ficará
 * e, na primeira vez, rola o documento até o campo para ele ficar visível enquanto assina.
 */
@Composable
private fun BoxScope.SignatureFieldPreview(
    field: FieldPlacement,
    size: IntSize,
    preview: SignaturePreview?,
    requester: BringIntoViewRequester?
) {
    // O overlay é chamado antes de a página ter tamanho; só desenha quando já tem.
    if (size.width <= 0 || size.height <= 0) return
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    LaunchedEffect(Unit) { requester?.bringIntoView() }
    Canvas(
        Modifier
            .offset { IntOffset((field.x * size.width).roundToInt(), (field.y * size.height).roundToInt()) }
            .size(with(density) { DpSize((field.width * size.width).toDp(), (field.height * size.height).toDp()) })
            .then(if (requester != null) Modifier.bringIntoViewRequester(requester) else Modifier)
    ) {
        when (preview) {
            is SignaturePreview.Drawn -> drawFittedStrokes(preview.strokes)
            is SignaturePreview.Typed -> drawFittedName(measurer, preview.name, preview.style)
            null -> Unit
        }
    }
}

/** Mesma regra do PNG salvo: o traço é recortado e ajustado ao campo sem distorção. */
private fun DrawScope.drawFittedStrokes(strokes: List<List<Offset>>) {
    val points = strokes.flatten()
    if (points.isEmpty()) return
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val boxW = max(maxX - minX, 1f)
    val boxH = max(maxY - minY, 1f)
    val factor = min(size.width / boxW, size.height / boxH)
    val left = (size.width - boxW * factor) / 2f
    val top = (size.height - boxH * factor) / 2f
    val strokeWidth = 2.dp.toPx()
    strokes.forEach { stroke ->
        val path = Path()
        stroke.forEachIndexed { i, p ->
            val x = (p.x - minX) * factor + left
            val y = (p.y - minY) * factor + top
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color.Black, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

/** Nome digitado ajustado ao campo, com o mesmo estilo de letra do PNG salvo. */
private fun DrawScope.drawFittedName(measurer: TextMeasurer, name: String, style: Int) {
    val measured = measurer.measure(
        text = name,
        style = TextStyle(
            color = Color.Black,
            fontSize = 100.sp,
            fontFamily = typedFontFamily(style),
            fontWeight = if (style == 1) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style == 2) FontStyle.Italic else FontStyle.Normal
        )
    )
    val w = measured.size.width.toFloat()
    val h = measured.size.height.toFloat()
    if (w <= 0f || h <= 0f) return
    val factor = min(size.width / w, size.height / h)
    val left = (size.width - w * factor) / 2f
    val top = (size.height - h * factor) / 2f
    withTransform({
        translate(left, top)
        scale(factor, factor, pivot = Offset.Zero)
    }) {
        drawText(measured)
    }
}

/** Mesmos estilos de `FileStorage.saveTypedSignature`, para a prévia bater com o PNG salvo. */
private fun typedFontFamily(style: Int): FontFamily = when (style) {
    0, 1 -> FontFamily.Cursive
    else -> FontFamily.Serif
}
