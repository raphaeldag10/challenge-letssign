package br.com.fiap.letssign.ui.newdoc

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import br.com.fiap.letssign.data.local.entity.SignatoryRoleType
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.PdfPageImage
import br.com.fiap.letssign.ui.common.ScreenScaffold
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
    val title = if (vm.parentId != null) "Nova versão ${vm.versionNumber}" else "Novo documento"
    ScreenScaffold(
        title = title,
        onBack = { if (vm.step == 0) onCancel() else vm.back() }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Passo ${vm.step + 1} de ${STEP_TITLES.size}: ${STEP_TITLES[vm.step]}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp)
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (vm.step) {
                    0 -> DocumentStep(container, vm)
                    1 -> PeopleStep(vm)
                    2 -> FieldsStep(vm)
                    else -> ReviewStep(container, vm, onSent)
                }
            }
            if (vm.step < STEP_TITLES.lastIndex) {
                ErrorText(vm.error)
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = { vm.next() }, enabled = !vm.loadingPdf, modifier = Modifier.weight(1f)) {
                        Text("Continuar")
                    }
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
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = vm.title,
            onValueChange = { vm.title = it },
            label = { Text("Título do documento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { picker.launch(arrayOf("application/pdf")) }, enabled = !vm.loadingPdf) {
            Text(if (vm.pdfPath == null) "Selecionar PDF" else "Trocar arquivo")
        }
        if (vm.loadingPdf) Text("Lendo o PDF…")
        vm.notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        ErrorText(vm.error)
        val path = vm.pdfPath
        if (path != null) {
            Text("${vm.pdfName} · ${vm.pageCount} página(s)", style = MaterialTheme.typography.bodyMedium)
            PdfPageImage(pages = vm.pages, pageIndex = 0, aspect = vm.aspect(0))
        }
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
private fun ReviewStep(container: AppContainer, vm: NewDocumentViewModel, onSent: (Long) -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(vm.title, style = MaterialTheme.typography.titleLarge)
        Text("Arquivo: ${vm.pdfName} · ${vm.pageCount} página(s)")
        vm.pdfPath?.let { path ->
            OutlinedButton(onClick = { openPdf(context, container.files.shareUri(path)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir PDF completo")
            }
        }
        HorizontalDivider()
        Text("Participantes", style = MaterialTheme.typography.titleSmall)
        vm.signatories.forEach { s ->
            val pages = vm.fields.filter { it.userId == s.userId }.map { it.page + 1 }.distinct().sorted()
            Text(
                "${s.fullName} · ${roleLabel(s.role)} · ${vm.fieldCount(s.userId)} campo(s) " +
                    "nas páginas ${pages.joinToString(", ")}"
            )
        }
        HorizontalDivider()
        OutlinedTextField(
            value = vm.message,
            onValueChange = { vm.message = it },
            label = { Text("Mensagem do convite (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        ErrorText(vm.error)
        Button(onClick = { vm.send(onSent) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (vm.busy) "Enviando…" else "Confirmar envio")
        }
    }
}

fun roleLabel(role: SignatoryRoleType): String = when (role) {
    SignatoryRoleType.CONTRATANTE -> "Contratante"
    SignatoryRoleType.CONTRATADO -> "Contratado"
    SignatoryRoleType.TESTEMUNHA -> "Testemunha"
}
