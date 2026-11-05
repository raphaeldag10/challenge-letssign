package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Resumo em cache por documento. [textHash] identifica o texto usado; se o PDF mudar, o resumo é refeito. */
@Entity(
    tableName = "document_ai_summaries",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentAiSummaryEntity(
    @PrimaryKey
    val documentId: Long,
    val summary: String,
    val textHash: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Mensagem do chat de dúvidas. [role] é "USER" ou "ASSISTANT" (ver ChatRole). */
@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val userId: Long,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
