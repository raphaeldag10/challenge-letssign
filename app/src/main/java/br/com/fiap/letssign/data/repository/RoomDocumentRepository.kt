package br.com.fiap.letssign.data.repository

import br.com.fiap.letssign.data.local.dao.DocumentDao
import br.com.fiap.letssign.data.local.entity.*
import br.com.fiap.letssign.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class RoomDocumentRepository(
    private val documentDao: DocumentDao
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<DocumentEntity>> =
        documentDao.getAllDocuments()

    override fun getDocumentsByStatus(status: DocumentStatus): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByStatus(status)

    override fun getPendingDocumentsForSignatory(userId: Long): Flow<List<DocumentEntity>> =
        documentDao.getPendingDocumentsForSignatory(userId)

    override fun getSignedDocumentsForSignatory(userId: Long): Flow<List<DocumentEntity>> =
        documentDao.getSignedDocumentsForSignatory(userId)

    override suspend fun getDocumentById(id: Long): DocumentEntity? =
        documentDao.getDocumentById(id)

    override suspend fun createDocumentFlow(
        document: DocumentEntity,
        signatories: List<DocumentSignatoryEntity>,
        fields: List<SignatureFieldEntity>
    ): Long {
        return documentDao.insertFullDocumentFlow(document, signatories, fields)
    }

    override suspend fun signDocument(documentId: Long, userId: Long): Boolean {
        val record = documentDao.getSignatoryRecord(documentId, userId) ?: return false
        val updatedRecord = record.copy(
            hasSigned = true,
            signedAt = System.currentTimeMillis()
        )
        documentDao.updateSignatory(updatedRecord)

        documentDao.insertHistory(
            DocumentHistoryEntity(
                documentId = documentId,
                eventType = "SIGNED",
                description = "Assinatura realizada pelo participante ID: $userId"
            )
        )

        // Verifica o estado geral do documento
        val allSignatories = documentDao.getSignatoriesForDocument(documentId)
        val allSigned = allSignatories.isNotEmpty() && allSignatories.all { it.hasSigned }
        val anySigned = allSignatories.any { it.hasSigned }

        val doc = documentDao.getDocumentById(documentId)
        if (doc != null) {
            val newStatus = when {
                allSigned -> DocumentStatus.COMPLETED
                anySigned -> DocumentStatus.PARTIALLY_SIGNED
                else -> DocumentStatus.PENDING
            }

            if (doc.status != newStatus) {
                documentDao.updateDocument(doc.copy(status = newStatus))
                if (newStatus == DocumentStatus.COMPLETED) {
                    documentDao.insertHistory(
                        DocumentHistoryEntity(
                            documentId = documentId,
                            eventType = "COMPLETED",
                            description = "Documento finalizado com todas as assinaturas concluídas."
                        )
                    )
                }
            }
        }
        return true
    }

    override fun getDocumentTimeline(documentId: Long): Flow<List<DocumentHistoryEntity>> =
        documentDao.getHistoryByDocument(documentId)

    override suspend fun getSignatoriesForDocument(documentId: Long): List<DocumentSignatoryEntity> =
        documentDao.getSignatoriesForDocument(documentId)

    override suspend fun getSignatureFieldsForDocument(documentId: Long): List<SignatureFieldEntity> =
        documentDao.getSignatureFieldsForDocument(documentId)
}