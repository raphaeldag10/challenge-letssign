package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentHistoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignatoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // --- Gestão do Documento ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE status = :status ORDER BY createdAt DESC")
    fun getDocumentsByStatus(status: DocumentStatus): Flow<List<DocumentEntity>>

    // --- Signatários do Documento ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatories(signatories: List<DocumentSignatoryEntity>)

    @Query("SELECT * FROM document_signatories WHERE documentId = :documentId")
    suspend fun getSignatoriesForDocument(documentId: Long): List<DocumentSignatoryEntity>

    @Query("SELECT * FROM document_signatories WHERE documentId = :documentId AND userId = :userId LIMIT 1")
    suspend fun getSignatoryRecord(documentId: Long, userId: Long): DocumentSignatoryEntity?

    @Update
    suspend fun updateSignatory(signatory: DocumentSignatoryEntity)

    // Documentos pendentes para um signatário específico
    @Query("""
        SELECT d.* FROM documents d
        INNER JOIN document_signatories ds ON d.id = ds.documentId
        WHERE ds.userId = :userId AND ds.hasSigned = 0
        ORDER BY d.createdAt DESC
    """)
    fun getPendingDocumentsForSignatory(userId: Long): Flow<List<DocumentEntity>>

    // Documentos já assinados por um signatário específico
    @Query("""
        SELECT d.* FROM documents d
        INNER JOIN document_signatories ds ON d.id = ds.documentId
        WHERE ds.userId = :userId AND ds.hasSigned = 1
        ORDER BY d.createdAt DESC
    """)
    fun getSignedDocumentsForSignatory(userId: Long): Flow<List<DocumentEntity>>

    // --- Campos de Assinatura ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatureFields(fields: List<SignatureFieldEntity>)

    @Query("SELECT * FROM signature_fields WHERE documentId = :documentId")
    suspend fun getSignatureFieldsForDocument(documentId: Long): List<SignatureFieldEntity>

    // --- Linha do Tempo e Histórico ---
    @Insert
    suspend fun insertHistory(history: DocumentHistoryEntity)

    @Query("SELECT * FROM document_history WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun getHistoryByDocument(documentId: Long): Flow<List<DocumentHistoryEntity>>

    // --- Transação Completa de Envio de Documento ---
    @Transaction
    suspend fun insertFullDocumentFlow(
        document: DocumentEntity,
        signatories: List<DocumentSignatoryEntity>,
        fields: List<SignatureFieldEntity>
    ): Long {
        val docId = insertDocument(document)
        val boundSignatories = signatories.map { it.copy(documentId = docId) }
        insertSignatories(boundSignatories)

        val boundFields = fields.map { it.copy(documentId = docId) }
        insertSignatureFields(boundFields)

        insertHistory(
            DocumentHistoryEntity(
                documentId = docId,
                eventType = "CREATED",
                description = "Documento criado e disparado para assinatura."
            )
        )
        return docId
    }
}