package br.com.fiap.letssign.domain.repository

import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentHistoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignatoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.local.model.SignatoryDetail
import kotlinx.coroutines.flow.Flow

/** Contrato de documentos. A implementação atual usa Room; a API REST da Sprint 2 implementará o mesmo contrato. */
interface DocumentRepository {
    fun observeAllSummaries(): Flow<List<DocumentSummary>>
    fun observeSummariesForSignatory(userId: Long, signed: Boolean): Flow<List<DocumentSummary>>
    suspend fun getDocumentById(id: Long): DocumentEntity?

    /** Criador ou participante do documento. Deve ser checado antes de abrir o documento ou seu PDF. */
    suspend fun canAccess(documentId: Long, userId: Long): Boolean

    /**
     * Grava o envio de forma atômica. [fields] referenciam o participante pelo userId.
     * Lança IllegalArgumentException se o envio for inválido.
     */
    suspend fun createDocumentFlow(
        document: DocumentEntity,
        signatories: List<DocumentSignatoryEntity>,
        fields: List<SignatureFieldEntity>
    ): Long

    /** Registra a assinatura do participante e retorna a nova situação do documento. */
    suspend fun signDocument(documentId: Long, userId: Long, signaturePath: String): DocumentStatus
    suspend fun setFinalFilePath(documentId: Long, path: String)

    suspend fun getSignatoryRecord(documentId: Long, userId: Long): DocumentSignatoryEntity?
    suspend fun getSignatoriesForDocument(documentId: Long): List<DocumentSignatoryEntity>
    suspend fun getSignatureFieldsForDocument(documentId: Long): List<SignatureFieldEntity>
    fun observeSignatoryDetails(documentId: Long): Flow<List<SignatoryDetail>>
    fun observeHistory(documentId: Long): Flow<List<DocumentHistoryEntity>>
}
