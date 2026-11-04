package br.com.fiap.letssign.domain.repository

import br.com.fiap.letssign.data.local.entity.*
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<DocumentEntity>>
    fun getDocumentsByStatus(status: DocumentStatus): Flow<List<DocumentEntity>>
    fun getPendingDocumentsForSignatory(userId: Long): Flow<List<DocumentEntity>>
    fun getSignedDocumentsForSignatory(userId: Long): Flow<List<DocumentEntity>>
    suspend fun getDocumentById(id: Long): DocumentEntity?

    suspend fun createDocumentFlow(
        document: DocumentEntity,
        signatories: List<DocumentSignatoryEntity>,
        fields: List<SignatureFieldEntity>
    ): Long

    suspend fun signDocument(documentId: Long, userId: Long): Boolean
    fun getDocumentTimeline(documentId: Long): Flow<List<DocumentHistoryEntity>>
    suspend fun getSignatoriesForDocument(documentId: Long): List<DocumentSignatoryEntity>
    suspend fun getSignatureFieldsForDocument(documentId: Long): List<SignatureFieldEntity>
}