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
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.local.model.SignatoryDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // --- Documento ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("UPDATE documents SET finalFilePath = :path WHERE id = :id")
    suspend fun setFinalFilePath(id: Long, path: String)

    // Lista do Admin: todos os documentos com progresso de assinaturas.
    @Query("""
        SELECT d.id AS id, d.title AS title, d.status AS status, d.createdAt AS createdAt,
               d.versionNumber AS versionNumber, d.filePath AS filePath, d.fileName AS fileName,
               (SELECT COUNT(*) FROM document_signatories s WHERE s.documentId = d.id) AS total,
               (SELECT COUNT(*) FROM document_signatories s WHERE s.documentId = d.id AND s.hasSigned = 1) AS signed
        FROM documents d
        ORDER BY d.createdAt DESC
    """)
    fun observeAllSummaries(): Flow<List<DocumentSummary>>

    // Lista do Signatário: documentos em que ele é participante, pendentes ou assinados por ele.
    @Query("""
        SELECT d.id AS id, d.title AS title, d.status AS status, d.createdAt AS createdAt,
               d.versionNumber AS versionNumber, d.filePath AS filePath, d.fileName AS fileName,
               (SELECT COUNT(*) FROM document_signatories s WHERE s.documentId = d.id) AS total,
               (SELECT COUNT(*) FROM document_signatories s WHERE s.documentId = d.id AND s.hasSigned = 1) AS signed
        FROM documents d
        INNER JOIN document_signatories me ON me.documentId = d.id
        WHERE me.userId = :userId AND me.hasSigned = :signed
        ORDER BY d.createdAt DESC
    """)
    fun observeSummariesForSignatory(userId: Long, signed: Boolean): Flow<List<DocumentSummary>>

    // --- Participantes do documento ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatories(signatories: List<DocumentSignatoryEntity>): List<Long>

    @Query("SELECT * FROM document_signatories WHERE documentId = :documentId")
    suspend fun getSignatoriesForDocument(documentId: Long): List<DocumentSignatoryEntity>

    @Query("SELECT * FROM document_signatories WHERE documentId = :documentId AND userId = :userId LIMIT 1")
    suspend fun getSignatoryRecord(documentId: Long, userId: Long): DocumentSignatoryEntity?

    @Query("""
        SELECT u.id AS userId, u.fullName AS fullName, u.email AS email,
               ds.roleType AS roleType, ds.hasSigned AS hasSigned, ds.signedAt AS signedAt
        FROM document_signatories ds
        INNER JOIN users u ON u.id = ds.userId
        WHERE ds.documentId = :documentId
        ORDER BY u.fullName ASC
    """)
    fun observeSignatoryDetails(documentId: Long): Flow<List<SignatoryDetail>>

    // Acesso: o usuário é participante do documento (criador é checado no repositório).
    @Query("SELECT COUNT(*) FROM document_signatories WHERE documentId = :documentId AND userId = :userId")
    suspend fun countSignatoryLink(documentId: Long, userId: Long): Int

    @Update
    suspend fun updateSignatory(signatory: DocumentSignatoryEntity)

    // --- Campos de assinatura ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatureFields(fields: List<SignatureFieldEntity>)

    @Query("SELECT * FROM signature_fields WHERE documentId = :documentId")
    suspend fun getSignatureFieldsForDocument(documentId: Long): List<SignatureFieldEntity>

    // --- Linha do tempo ---
    @Insert
    suspend fun insertHistory(history: DocumentHistoryEntity)

    @Query("SELECT * FROM document_history WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun observeHistory(documentId: Long): Flow<List<DocumentHistoryEntity>>

    // --- Transação completa de envio ---
    @Transaction
    suspend fun insertFullDocumentFlow(
        document: DocumentEntity,
        signatories: List<DocumentSignatoryEntity>,
        fields: List<SignatureFieldEntity>
    ): Long {
        val docId = insertDocument(document)

        // Um mesmo usuário não pode aparecer duas vezes no documento.
        require(signatories.map { it.userId }.distinct().size == signatories.size) {
            "Signatário repetido no documento"
        }
        val boundSignatories = signatories.map { it.copy(documentId = docId) }
        val generatedSignatoryIds = insertSignatories(boundSignatories)

        // Os IDs dos signatários só existem após a inserção. Os campos referenciam
        // o signatário pelo userId (único no documento) e recebem o ID gerado.
        val signatoryIdByUser = signatories.zip(generatedSignatoryIds)
            .associate { (signatory, generatedId) -> signatory.userId to generatedId }
        val boundFields = fields.map { field ->
            val realSignatoryId = requireNotNull(signatoryIdByUser[field.signatoryId]) {
                "Campo associado a um signatário que não faz parte do documento"
            }
            field.copy(documentId = docId, signatoryId = realSignatoryId)
        }
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
