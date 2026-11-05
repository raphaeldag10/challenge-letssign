package br.com.fiap.letssign.ui.newdoc

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** As duas origens entregam um PDF ao mesmo importador do assistente. */
@Composable
internal fun DocumentSourcePicker(
    label: String,
    enabled: Boolean,
    onPdfSelected: (Uri, String) -> Unit,
    onError: (String) -> Unit,
    onPreparingScan: (Boolean) -> Unit
) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var choosingSource by rememberSaveable { mutableStateOf(false) }
    val importPdf by rememberUpdatedState(onPdfSelected)
    val reportError by rememberUpdatedState(onError)
    val preparingScan by rememberUpdatedState(onPreparingScan)
    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPdf(uri, uri.lastPathSegment ?: "arquivo.pdf")
    }
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        when {
            result.data?.hasExtra(ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION) == true ->
                reportError("Não foi possível abrir a câmera. Tente novamente ou escolha Arquivo.")
            result.resultCode == Activity.RESULT_OK -> {
                val pdf = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pdf
                if (pdf != null && pdf.pageCount > 0) {
                    importPdf(pdf.uri, "digitalizacao-${System.currentTimeMillis()}.pdf")
                } else {
                    reportError("A digitalização não gerou um PDF. Fotografe o documento novamente.")
                }
            }
            result.resultCode != Activity.RESULT_CANCELED ->
                reportError("Não foi possível concluir a digitalização. Tente novamente.")
            // Voltar/cancelar não modifica o PDF, os campos nem o título do rascunho.
        }
    }

    OutlinedButton(
        onClick = { choosingSource = true },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(label)
    }

    if (choosingSource) {
        AlertDialog(
            onDismissRequest = { choosingSource = false },
            title = { Text("Como deseja importar?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Fotografe o documento ou selecione um PDF do celular.")
                    OutlinedButton(
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            choosingSource = false
                            if (activity == null) {
                                reportError("Não foi possível abrir a câmera nesta tela. Tente novamente.")
                            } else {
                                scope.launch {
                                    preparingScan(true)
                                    try {
                                        val intentSender = scanner.getStartScanIntent(activity).awaitScanIntent()
                                        scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        reportError(
                                            "Não foi possível iniciar a digitalização. Verifique a conexão e o Google Play Services, " +
                                                "tente novamente ou escolha Arquivo."
                                        )
                                    } finally {
                                        preparingScan(false)
                                    }
                                }
                            }
                        }
                    ) { Text("Foto") }
                    Text("Use a câmera, ajuste o recorte e os filtros e salve como PDF.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            choosingSource = false
                            try {
                                filePicker.launch(arrayOf("application/pdf"))
                            } catch (e: Exception) {
                                reportError("Não foi possível abrir o seletor de arquivos. Tente novamente.")
                            }
                        }
                    ) { Text("Arquivo") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingSource = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Cancela a preparação ao sair da tela, sem abrir uma câmera para uma tela já encerrada. */
private suspend fun Task<IntentSender>.awaitScanIntent(): IntentSender = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
