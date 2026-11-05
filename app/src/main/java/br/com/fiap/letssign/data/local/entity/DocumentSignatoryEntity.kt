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

/** Como o signatário assinou: desenhando, digitando o nome ou com certificado digital. */
enum class SignatureMethod {
    DRAW,
    TYPED,
    CERTIFICATE
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
    /** Caminho do PNG da assinatura desenhada ou digitada pelo participante. */
    val signaturePath: String? = null,
    /** Modo usado na assinatura. Nulo em assinaturas feitas antes da versão 6 (sempre desenhadas). */
    val signatureMethod: SignatureMethod? = null,
    /** Nome que o signatário assinou (digitado ou nome do titular do certificado). */
    val signerName: String? = null,
    /** CMS/CAdES destacado em Base64, só nas assinaturas com certificado ICP-Brasil. */
    val signatureCms: String? = null,
    /** Número de série do certificado usado (hexadecimal). */
    val certificateSerial: String? = null,
    /** Emissor do certificado usado. */
    val certificateIssuer: String? = null
)
