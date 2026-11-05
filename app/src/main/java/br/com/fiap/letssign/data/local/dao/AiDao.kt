package br.com.fiap.letssign.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fiap.letssign.data.local.entity.AiMessageEntity
import br.com.fiap.letssign.data.local.entity.DocumentAiSummaryEntity

@Dao
interface AiDao {

    @Query("SELECT * FROM document_ai_summaries WHERE documentId = :documentId LIMIT 1")
    suspend fun getSummary(documentId: Long): DocumentAiSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DocumentAiSummaryEntity)

    @Query("SELECT * FROM ai_messages WHERE documentId = :documentId AND userId = :userId ORDER BY id ASC")
    suspend fun getMessages(documentId: Long, userId: Long): List<AiMessageEntity>

    @Insert
    suspend fun insertMessage(message: AiMessageEntity): Long
}
