package br.com.fiap.letssign.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.UUID
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.file.PdfPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/** Largura de renderização das páginas na tela. */
private const val RENDER_WIDTH_PX = 1000

/**
 * ViewModel por instância de tela. A chave inclui um id guardado com rememberSaveable,
 * então abrir a mesma tela de novo começa do zero, mas uma rotação mantém o estado.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: () -> VM): VM {
    val instance = rememberSaveable { UUID.randomUUID().toString() }
    return viewModel<VM>(
        key = "${key ?: VM::class.java.name}-$instance",
        factory = viewModelFactory { initializer { create() } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    floatingAction: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) TextButton(onClick = onBack) { Text("Voltar") }
                }
            )
        },
        floatingActionButton = floatingAction
    ) { content(it) }
}

@Composable
fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Carregando…")
    }
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(message: String, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, textAlign = TextAlign.Center)
        if (action != null) {
            Box(Modifier.padding(top = 16.dp)) { action() }
        }
    }
}

fun statusLabel(status: DocumentStatus): String = when (status) {
    DocumentStatus.PENDING -> "Enviado"
    DocumentStatus.PARTIALLY_SIGNED -> "Parcialmente assinado"
    DocumentStatus.COMPLETED -> "Concluído"
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

fun formatDate(millis: Long): String = dateFormat.format(millis)

/** Cores estáveis por participante, usadas nos campos do editor e da leitura. */
private val signatoryPalette = listOf(
    Color(0xFF6230B8), Color(0xFF20B7DD), Color(0xFF16734A),
    Color(0xFF956000), Color(0xFFB3261E), Color(0xFF241475)
)

fun signatoryColor(userId: Long): Color =
    signatoryPalette[(userId % signatoryPalette.size).toInt()]

/** Abre um PDF do app em outro aplicativo, com permissão de leitura temporária. */
fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Nenhum aplicativo disponível para abrir PDF.", Toast.LENGTH_LONG).show()
    }
}

/**
 * Página de um PDF com área de sobreposição. [aspect] é largura/altura da página, então
 * as coordenadas do overlay são as mesmas coordenadas normalizadas gravadas no banco.
 */
@Composable
fun PdfPageImage(
    pages: PdfPages?,
    pageIndex: Int,
    aspect: Float,
    modifier: Modifier = Modifier,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.(IntSize) -> Unit = {}
) {
    var bitmap by remember(pages, pageIndex) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pages, pageIndex) {
        bitmap = pages?.let {
            // O PDF pode ser fechado enquanto a página é lida (ao trocar o arquivo); nesse caso não há imagem.
            runCatching { withContext(Dispatchers.IO) { it.render(pageIndex, RENDER_WIDTH_PX).asImageBitmap() } }.getOrNull()
        }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .background(Color.White)
            .onSizeChanged { size = it }
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Página ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        overlay(size)
    }
}

/** Ponto de referência para arrastar em coordenadas normalizadas. */
fun Offset.normalizedBy(size: IntSize): Pair<Float, Float> =
    (x / size.width.coerceAtLeast(1)) to (y / size.height.coerceAtLeast(1))
