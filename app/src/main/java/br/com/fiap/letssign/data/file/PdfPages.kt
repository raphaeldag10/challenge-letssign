package br.com.fiap.letssign.data.file

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File
import kotlin.math.roundToInt

class PdfException(message: String) : Exception(message)

/**
 * Acesso às páginas de um PDF. [PdfRenderer] só aceita uma página aberta por vez,
 * por isso as operações são sincronizadas.
 */
class PdfPages private constructor(
    private val renderer: PdfRenderer,
    private val descriptor: ParcelFileDescriptor
) : Closeable {

    val pageCount: Int get() = renderer.pageCount

    /** Largura e altura da página em pontos (1/72 pol.), que é a unidade do PDF. */
    @Synchronized
    fun pageSize(index: Int): Pair<Float, Float> =
        renderer.openPage(index).use { it.width.toFloat() to it.height.toFloat() }

    @Synchronized
    fun render(index: Int, targetWidthPx: Int): Bitmap =
        renderer.openPage(index).use { page ->
            val height = (page.height * targetWidthPx / page.width.toFloat()).roundToInt()
            val bitmap = Bitmap.createBitmap(targetWidthPx, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }

    @Synchronized
    override fun close() {
        renderer.close()
        descriptor.close()
    }

    companion object {
        /** Abre o arquivo validando-o. Lança [PdfException] com a mensagem a ser exibida ao usuário. */
        fun open(path: String): PdfPages {
            val file = File(path)
            if (!file.exists()) throw PdfException("Arquivo não encontrado.")
            val header = file.inputStream().use { it.readNBytes(5) }
            if (String(header) != "%PDF-") throw PdfException("Selecione um arquivo PDF.")

            val descriptor = try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: Exception) {
                throw PdfException("Não foi possível abrir este PDF.")
            }
            val renderer = try {
                PdfRenderer(descriptor)
            } catch (e: SecurityException) {
                descriptor.close()
                throw PdfException("Não foi possível processar este PDF. Escolha uma cópia sem proteção.")
            } catch (e: Exception) {
                descriptor.close()
                throw PdfException("Não foi possível abrir este PDF. Ele pode estar corrompido.")
            }
            if (renderer.pageCount == 0) {
                renderer.close()
                descriptor.close()
                throw PdfException("Este PDF não tem páginas.")
            }
            return PdfPages(renderer, descriptor)
        }
    }
}
