package br.com.fiap.letssign.data.file

import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File

/**
 * Extrai o texto selecionável de um PDF. Chamar fora da thread principal.
 * PDF escaneado (só imagem) retorna texto vazio.
 */
class PdfTextExtractor(context: Context) {

    init {
        // Carrega as fontes e o CMap do pdfbox-android; é seguro chamar mais de uma vez.
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun extract(path: String): String =
        PDDocument.load(File(path)).use { document ->
            PDFTextStripper().getText(document).trim()
        }
}
