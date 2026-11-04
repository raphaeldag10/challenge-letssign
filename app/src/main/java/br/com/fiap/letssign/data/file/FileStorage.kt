package br.com.fiap.letssign.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

/** Campo de assinatura já convertido para coordenadas normalizadas (0 a 1) da página. */
data class FieldPlacement(
    val pageNumber: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val userId: Long
)

/**
 * Arquivos do app em filesDir: PDFs enviados (documents/), assinaturas (signatures/)
 * e PDFs finais (final/). Os caminhos gravados no Room apontam para esses arquivos.
 */
class FileStorage(private val context: Context) {

    private val documentsDir get() = dir("documents")
    private val signaturesDir get() = dir("signatures")
    private val finalDir get() = dir("final")

    private fun dir(name: String) = File(context.filesDir, name).apply { mkdirs() }

    /** Copia o PDF escolhido pelo usuário e valida. Se for inválido, o arquivo copiado é apagado. */
    fun importPdf(uri: Uri): String {
        val target = File(documentsDir, "${UUID.randomUUID()}.pdf")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw PdfException("Não foi possível ler o arquivo selecionado.")
            input.use { source -> FileOutputStream(target).use { source.copyTo(it) } }
            PdfPages.open(target.absolutePath).close()
        } catch (e: Exception) {
            target.delete()
            if (e is PdfException) throw e
            throw PdfException("Não foi possível ler o arquivo selecionado.")
        }
        return target.absolutePath
    }

    /** Copia um PDF já armazenado (usado ao criar uma nova versão, para que cada versão tenha seu arquivo). */
    fun copyPdf(sourcePath: String): String {
        val target = File(documentsDir, "${UUID.randomUUID()}.pdf")
        File(sourcePath).copyTo(target)
        return target.absolutePath
    }

    /**
     * Salva a assinatura desenhada como PNG recortado ao traço.
     * [strokes] usa as coordenadas do bloco de desenho (padWidth x padHeight).
     */
    fun saveSignature(strokes: List<List<PointF>>, padWidth: Int, padHeight: Int): String {
        val points = strokes.flatten()
        require(points.isNotEmpty()) { "Desenhe sua assinatura antes de confirmar" }
        val margin = 12f
        val minX = (points.minOf { it.x } - margin).coerceAtLeast(0f)
        val minY = (points.minOf { it.y } - margin).coerceAtLeast(0f)
        val maxX = (points.maxOf { it.x } + margin).coerceAtMost(padWidth.toFloat())
        val maxY = (points.maxOf { it.y } + margin).coerceAtMost(padHeight.toFloat())
        val width = (maxX - minX).roundToInt().coerceAtLeast(1)
        val height = (maxY - minY).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        strokes.forEach { stroke ->
            if (stroke.size == 1) {
                canvas.drawPoint(stroke[0].x - minX, stroke[0].y - minY, paint)
            } else {
                val path = Path()
                stroke.forEachIndexed { i, p ->
                    if (i == 0) path.moveTo(p.x - minX, p.y - minY) else path.lineTo(p.x - minX, p.y - minY)
                }
                canvas.drawPath(path, paint)
            }
        }
        val target = File(signaturesDir, "${UUID.randomUUID()}.png")
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return target.absolutePath
    }

    /**
     * Gera o PDF final: cada página do original com as assinaturas aplicadas nos campos.
     * [signatureByUser] mapeia userId -> caminho do PNG. A imagem é ajustada ao campo sem distorção.
     */
    fun generateFinalPdf(
        sourcePath: String,
        fields: List<FieldPlacement>,
        signatureByUser: Map<Long, String>,
        documentId: Long
    ): String {
        val target = File(finalDir, "doc_${documentId}_final_${System.currentTimeMillis()}.pdf")
        val pdf = PdfDocument()
        try {
            PdfPages.open(sourcePath).use { pages ->
                for (index in 0 until pages.pageCount) {
                    val (width, height) = pages.pageSize(index)
                    val pageWidth = width.roundToInt()
                    val pageHeight = height.roundToInt()
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                    val canvas = page.canvas

                    // Renderização em dobro da largura para não perder nitidez no PDF final.
                    val rendered = pages.render(index, pageWidth * 2)
                    canvas.drawBitmap(rendered, null, RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()), null)
                    rendered.recycle()

                    fields.filter { it.pageNumber == index }.forEach { field ->
                        val signaturePath = signatureByUser[field.userId] ?: return@forEach
                        val signature = BitmapFactory.decodeFile(signaturePath) ?: return@forEach
                        val box = RectF(
                            field.x * pageWidth,
                            field.y * pageHeight,
                            (field.x + field.width) * pageWidth,
                            (field.y + field.height) * pageHeight
                        )
                        canvas.drawBitmap(signature, null, fitInside(signature, box), null)
                        signature.recycle()
                    }
                    pdf.finishPage(page)
                }
            }
            FileOutputStream(target).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return target.absolutePath
    }

    /** Uri segura para abrir ou compartilhar um arquivo do app em outro aplicativo. */
    fun shareUri(path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))

    private fun fitInside(bitmap: Bitmap, box: RectF): RectF {
        val scale = min(box.width() / bitmap.width, box.height() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val left = box.left + (box.width() - w) / 2
        val top = box.top + (box.height() - h) / 2
        return RectF(left, top, left + w, top + h)
    }
}
