package br.com.fiap.letssign.ui.newdoc

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.SignatureMethod
import br.com.fiap.letssign.data.local.entity.SignatoryRoleType
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.PdfPageImage
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.common.openPdf
import br.com.fiap.letssign.ui.common.signatoryColor
import kotlin.math.roundToInt

@Composable
fun NewDocumentScreen(
    container: AppContainer,
    versionOfId: Long?,
    viewModelKey: String,
    onCancel: () -> Unit,
    onSent: (Long) -> Unit
) {
    val vm = appViewModel(key = viewModelKey) { NewDocumentViewModel(container, versionOfId) }
    val title = if (vm.step == 3) "Revisar e enviar" else if (vm.parentId != null) "Nova versão ${vm.versionNumber}" else "Novo documento"
    FormScaffold(
        title = title,
        onBack = { if (vm.step == 0) onCancel() else vm.back() }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DocumentProgress(vm.step)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (vm.step) {
                    0 -> DocumentStep(container, vm)
                    1 -> PeopleStep(vm)
                    2 -> FieldsStep(vm)
                    else -> ReviewStep(container, vm)
                }
            }
            Surface(shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    ErrorText(vm.error)
                    Button(
                        onClick = { if (vm.step == STEP_TITLES.lastIndex) vm.send(onSent) else vm.next() },
                        enabled = !vm.loadingPdf && !vm.busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        if (vm.step == STEP_TITLES.lastIndex) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(if (vm.busy) "Enviando…" else if (vm.step == STEP_TITLES.lastIndex) "Enviar para assinatura" else "Avançar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentProgress(step: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val cell = size.width / STEP_TITLES.size
            val centerY = 18.dp.toPx()
            for (index in 0 until STEP_TITLES.lastIndex) {
                drawLine(
                    color = if (index < step) primary else outline,
                    start = Offset(cell * (index + 0.5f) + 23.dp.toPx(), centerY),
                    end = Offset(cell * (index + 1.5f) - 23.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            STEP_TITLES.forEachIndexed { index, label ->
                val current = index == step
                val completed = index < step
                Column(
                    Modifier.weight(1f).semantics {
                        stateDescription = if (current) "Etapa atual" else if (completed) "Concluída" else "Pendente"
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = when {
                            current -> primary
                            completed -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = if (current || completed) null else BorderStroke(1.dp, outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (completed) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = primary)
                            else Text("${index + 1}", style = MaterialTheme.typography.labelLarge,
                                color = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                        color = if (current || completed) primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DocumentStep(container: AppContainer, vm: NewDocumentViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFromUri(uri, uri.lastPathSegment ?: "arquivo.pdf")
    }
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(outline, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
            }
            Column(Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (vm.pdfPath != null) {
                    Box(Modifier.width(120.dp)) {
                        PdfPageImage(
                            pages = vm.pages, pageIndex = 0, aspect = vm.aspect(0),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .border(1.dp, outline, RoundedCornerShape(8.dp))
                        )
                    }
                    Text(vm.pdfName, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    Text("${vm.pageCount} página(s) · PDF selecionado",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("Importar documento (PDF)", fontWeight = FontWeight.SemiBold)
                    Text("Selecione no seu dispositivo o documento que deseja enviar para assinatura.",
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) },
                    enabled = !vm.loadingPdf, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (vm.loadingPdf) "Lendo o PDF…" else if (vm.pdfPath == null) "Selecionar PDF" else "Trocar arquivo")
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Título do documento *", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = vm.title, onValueChange = { vm.title = it },
                placeholder = { Text("Ex.: Contrato de prestação de serviços") },
                singleLine = true, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
        }
        vm.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

    }
}

@Composable
private fun PeopleStep(vm: NewDocumentViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Escolha quem assina e a função de cada pessoa neste documento.",
            modifier = Modifier.padding(16.dp)
        )
        if (vm.people.isEmpty()) {
            EmptyState("Cadastre quem vai assinar seus documentos na aba Signatários.")
        }
        vm.people.forEach { person -> PersonRow(vm, person) }
        ErrorText(vm.error)
    }

    vm.pendingRemoval?.let { userId ->
        val count = vm.fieldCount(userId)
        AlertDialog(
            onDismissRequest = vm::cancelRemoval,
            title = { Text("Remover signatário?") },
            text = { Text("Os $count campo(s) dele também serão removidos.") },
            confirmButton = { TextButton(onClick = vm::confirmRemoval) { Text("Remover") } },
            dismissButton = { TextButton(onClick = vm::cancelRemoval) { Text("Manter") } }
        )
    }

    vm.replacingUserId?.let { oldId ->
        val candidates = vm.people.filter { p -> vm.signatories.none { it.userId == p.id } }
        AlertDialog(
            onDismissRequest = { vm.replacingUserId = null },
            title = { Text("Substituir signatário") },
            text = {
                Column {
                    Text("Os campos posicionados serão mantidos e passam a ser da nova pessoa.")
                    if (candidates.isEmpty()) {
                        Text("Não há outras pessoas cadastradas disponíveis.", modifier = Modifier.padding(top = 8.dp))
                    }
                    candidates.forEach { p ->
                        TextButton(onClick = { vm.replaceSignatory(oldId, p) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${p.fullName} · ${p.email}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { vm.replacingUserId = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PersonRow(vm: NewDocumentViewModel, person: UserEntity) {
    val draft = vm.signatories.firstOrNull { it.userId == person.id }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft != null, onCheckedChange = { vm.togglePerson(person) })
            Column {
                Text(person.fullName, style = MaterialTheme.typography.titleSmall)
                Text(person.email, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (draft != null) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SignatoryRoleType.entries.forEach { role ->
                    FilterChip(
                        selected = draft.role == role,
                        onClick = { vm.setRole(person.id, role) },
                        label = { Text(roleLabel(role)) }
                    )
                }
            }
            TextButton(onClick = { vm.replacingUserId = person.id }) { Text("Substituir por outra pessoa") }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun FieldsStep(vm: NewDocumentViewModel) {
    val pages = vm.pages
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pages == null) {
            Text("Selecione um PDF no passo anterior.")
            return@Column
        }
        if (vm.signatories.isEmpty()) {
            Text("Adicione signatários no passo anterior.")
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.setPage(vm.currentPage - 1) }, enabled = vm.currentPage > 0) { Text("Anterior") }
            Text("Página ${vm.currentPage + 1} de ${vm.pageCount}")
            OutlinedButton(
                onClick = { vm.setPage(vm.currentPage + 1) },
                enabled = vm.currentPage < vm.pageCount - 1
            ) { Text("Próxima") }
        }

        Text("Signatário do campo:", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            vm.signatories.forEach { s ->
                FilterChip(
                    selected = vm.selectedUserId == s.userId,
                    onClick = { vm.selectPerson(s.userId) },
                    label = { Text("${s.fullName} (${vm.fieldCount(s.userId)})") }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.addField() }, enabled = vm.selectedUserId != null) { Text("Adicionar campo") }
            val selected = vm.selectedFieldId
            if (selected != null) {
                OutlinedButton(onClick = { vm.deleteField(selected) }) { Text("Excluir campo") }
            }
        }
        Text(
            "Arraste um campo para movê-lo. Use o canto inferior direito do campo selecionado para redimensioná-lo.",
            style = MaterialTheme.typography.bodySmall
        )

        val page = vm.currentPage
        PdfPageImage(pages = pages, pageIndex = page, aspect = vm.aspect(page)) { size ->
            vm.fields.filter { it.page == page }.forEach { field ->
                key(field.id) {
                    FieldBox(
                        field = field,
                        size = size,
                        name = vm.signatories.firstOrNull { it.userId == field.userId }?.fullName.orEmpty(),
                        selected = vm.selectedFieldId == field.id,
                        onSelect = { vm.selectField(field.id) },
                        onMove = { dx, dy -> vm.moveField(field.id, dx, dy) },
                        onResize = { dw, dh -> vm.resizeField(field.id, dw, dh) }
                    )
                }
            }
        }
        ErrorText(vm.error)
    }
}

@Composable
private fun BoxScope.FieldBox(
    field: br.com.fiap.letssign.ui.newdoc.DraftField,
    size: IntSize,
    name: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val color = signatoryColor(field.userId)
    val widthPx = field.w * size.width
    val heightPx = field.h * size.height
    Box(
        Modifier
            .offset { IntOffset((field.x * size.width).roundToInt(), (field.y * size.height).roundToInt()) }
            .size(with(density) { DpSize(widthPx.toDp(), heightPx.toDp()) })
            .border(if (selected) 2.dp else 1.dp, color)
            .background(color.copy(alpha = 0.18f))
            .clickable(onClick = onSelect)
            .pointerInput(field.id, size) {
                detectDragGestures { change, drag ->
                    change.consume()
                    if (size.width > 0 && size.height > 0) {
                        onMove(drag.x / size.width, drag.y / size.height)
                    }
                }
            }
    ) {
        Text(
            name.substringBefore(' '),
            color = Color.Black,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(2.dp)
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .background(color)
                    .pointerInput(field.id, size) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            if (size.width > 0 && size.height > 0) {
                                onResize(drag.x / size.width, drag.y / size.height)
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun ReviewStep(container: AppContainer, vm: NewDocumentViewModel) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ReviewCard {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text("PDF", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(vm.title, style = MaterialTheme.typography.titleSmall)
                    Text("${vm.pdfName} · ${vm.pageCount} página(s)", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            vm.pdfPath?.let { path ->
                TextButton(onClick = { openPdf(context, container.files.shareUri(path)) },
                    modifier = Modifier.padding(horizontal = 8.dp)) { Text("Abrir PDF completo") }
            }
        }
        ReviewCard {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, Modifier.size(20.dp))
                Text("Signatários (${vm.signatories.size})", Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { vm.step = 1 }, enabled = !vm.busy) { Text("Editar") }
            }
            vm.signatories.forEachIndexed { index, person ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(person.fullName, style = MaterialTheme.typography.titleSmall)
                        Text(person.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val pages = vm.fields.filter { it.userId == person.userId }.map { it.page + 1 }.distinct().sorted()
                        Text("${roleLabel(person.role)} · ${vm.fieldCount(person.userId)} campo(s) · páginas ${pages.joinToString()}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        ReviewCard {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 16.dp)) {
                    Text("Campos de assinatura", style = MaterialTheme.typography.titleSmall)
                    Text("${vm.fields.size} campo(s)", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.step = 2 }, enabled = !vm.busy) { Text("Editar") }
            }
        }
        Text("Como os signatários podem assinar", style = MaterialTheme.typography.titleSmall)
        Column {
            SignatureMethod.entries.forEach { method ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = method in vm.allowedMethods, enabled = !vm.busy, onCheckedChange = {
                        vm.allowedMethods = if (it) vm.allowedMethods + method
                        else (vm.allowedMethods - method).ifEmpty { vm.allowedMethods }
                    })
                    Text(signatureMethodLabel(method), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedTextField(value = vm.message, onValueChange = { vm.message = it },
            label = { Text("Mensagem do convite (opcional)") }, minLines = 2, enabled = !vm.busy,
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tudo certo!", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Confira as informações e envie o documento para assinatura.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

fun signatureMethodLabel(method: SignatureMethod): String = when (method) {
    SignatureMethod.DRAW -> "Desenhar"
    SignatureMethod.TYPED -> "Digitar nome"
    SignatureMethod.CERTIFICATE -> "Certificado digital"
}

fun roleLabel(role: SignatoryRoleType): String = when (role) {
    SignatoryRoleType.CONTRATANTE -> "Contratante"
    SignatoryRoleType.CONTRATADO -> "Contratado"
    SignatoryRoleType.TESTEMUNHA -> "Testemunha"
}
