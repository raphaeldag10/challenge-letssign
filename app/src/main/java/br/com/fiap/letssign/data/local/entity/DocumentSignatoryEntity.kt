package br.com.fiap.letssign.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SignatoryRoleType {
    CONTRATANTE,
    CONTRATADO,
    TESTEMUNHA
}

@Entity(
    tableName = "document_signatories",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["documentId"]), Index(value = ["userId"])]
)
data class DocumentSignatoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val userId: Long,
    val roleType: SignatoryRoleType,
    val hasSigned: Boolean = false,
    val signedAt: Long? = null,
    /** Caminho do PNG da assinatura desenhada pelo participante. */
    val signaturePath: String? = null
)
