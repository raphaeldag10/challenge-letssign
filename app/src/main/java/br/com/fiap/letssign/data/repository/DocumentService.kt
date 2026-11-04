package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.file.FieldPlacement
import br.com.fiap.letssign.data.file.FileStorage
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Operações que combinam banco de dados e arquivos do app. */
class DocumentService(
    private val documents: DocumentRepository,
    private val files: FileStorage
) {
    /** Retorna o PDF final de um documento concluído, gerando-o se ainda não existir. */
    suspend fun ensureFinalPdf(documentId: Long): String? = withContext(Dispatchers.IO) {
        val document = documents.getDocumentById(documentId) ?: return@withContext null
        if (document.status != DocumentStatus.COMPLETED) return@withContext null
        document.finalFilePath?.takeIf { File(it).exists() }?.let { return@withContext it }

        val signatories = documents.getSignatoriesForDocument(documentId)
        // Campos guardam o ID do participante (linha de document_signatories); convertemos para userId.
        val placements = documents.getSignatureFieldsForDocument(documentId).mapNotNull { field ->
            val signatory = signatories.firstOrNull { it.id == field.signatoryId } ?: return@mapNotNull null
            FieldPlacement(
                pageNumber = field.pageNumber,
                x = field.normalizedX,
                y = field.normalizedY,
                width = field.normalizedWidth,
                height = field.normalizedHeight,
                userId = signatory.userId
            )
        }
        val signatures = signatories.mapNotNull { s -> s.signaturePath?.let { s.userId to it } }.toMap()
        val path = files.generateFinalPdf(document.filePath, placements, signatures, documentId)
        documents.setFinalFilePath(documentId, path)
        path
    }
}
