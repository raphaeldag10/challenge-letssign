package br.com.fiap.letssign.ui.newdoc

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.file.PdfPages
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** O scanner e o seletor compartilham o importador: exercita o PDF real e sua cópia privada. */
@RunWith(AndroidJUnit4::class)
class ScannedPdfImportTest {
    @get:Rule
    val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ViewModelStore()
    private lateinit var vm: NewDocumentViewModel
    private lateinit var source: File
    private var importedPath: String? = null

    @Before
    fun setUp() {
        compose.setContent { Text("Importação") }
        source = File.createTempFile("digitalizacao-", ".pdf", context.cacheDir)
        val pdf = PdfDocument()
        try {
            repeat(2) { index ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawText("Documento fotografado ${index + 1}", 40f, 80f, Paint().apply { textSize = 20f })
                pdf.finishPage(page)
            }
            source.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        compose.runOnIdle {
            vm = NewDocumentViewModel(AppContainer(context), null)
            store.put("import-test", vm)
            vm.title = "Contrato fotografado"
            vm.importFromUri(Uri.fromFile(source), "digitalizacao.pdf")
        }
        compose.waitUntil(10_000) { !vm.loadingPdf }
        importedPath = vm.pdfPath
    }

    @After
    fun tearDown() {
        compose.runOnIdle { store.clear() }
        importedPath?.let { File(it).delete() }
        source.delete()
    }

    @Test
    fun scannedPagesAreCopiedAndRemainReadableAfterTemporarySourceIsRemoved() {
        assertNotNull(vm.pdfPath)
        assertNotEquals(source.absolutePath, vm.pdfPath)
        assertEquals("digitalizacao.pdf", vm.pdfName)
        assertEquals("Contrato fotografado", vm.title)
        assertEquals(2, vm.pageCount)
        assertTrue(source.delete())
        PdfPages.open(requireNotNull(vm.pdfPath)).use { assertEquals(2, it.pageCount) }
        compose.runOnIdle { assertEquals(null, vm.validate(0)) }
    }

    @Test
    fun invalidReplacementPreservesPreviousPdfTitleAndSignatureFields() {
        val previousPath = vm.pdfPath
        val field = DraftField(1L, 1L, 0, 0.1f, 0.1f, 0.3f, 0.07f)
        source.writeText("arquivo inválido")
        compose.runOnIdle {
            vm.fields.add(field)
            vm.importFromUri(Uri.fromFile(source), "invalido.pdf")
        }
        compose.waitUntil(10_000) { !vm.loadingPdf }
        compose.runOnIdle {
            assertNotNull(vm.error)
            assertEquals(previousPath, vm.pdfPath)
            assertEquals("digitalizacao.pdf", vm.pdfName)
            assertEquals("Contrato fotografado", vm.title)
            assertEquals(listOf(field), vm.fields.toList())
        }
    }
}
