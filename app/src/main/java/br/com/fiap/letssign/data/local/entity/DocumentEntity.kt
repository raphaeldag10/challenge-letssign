package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentStatus {
    PENDING,
    PARTIALLY_SIGNED,
    COMPLETED
}

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdById"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["createdById"])]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val status: DocumentStatus = DocumentStatus.PENDING,
    val createdById: Long,
    val createdAt: Long = System.currentTimeMillis()
)
