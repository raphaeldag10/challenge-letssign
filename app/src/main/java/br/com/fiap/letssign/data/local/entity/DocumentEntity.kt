package br.com.fiap.letssign.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentStatus {
    PENDING,
    PARTIALLY_SIGNED,
    COMPLETED
}

/**
 * Documento enviado para assinatura. Após o envio ele não é editado: alterações
 * criam uma nova versão (versionNumber + 1) apontando para o original (parentDocumentId).
 */
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
    val createdAt: Long = System.currentTimeMillis(),
    val message: String? = null,
    val versionNumber: Int = 1,
    val parentDocumentId: Long? = null,
    val finalFilePath: String? = null,
    /** Nome original do PDF escolhido pelo Admin (ex.: contrato.pdf). Nulo em documentos anteriores à versão 4. */
    val fileName: String? = null,
    /** Modos de assinatura aceitos neste documento, separados por vírgula (ver [allowedMethods]). */
    @ColumnInfo(defaultValue = ALL_SIGNATURE_METHODS)
    val allowedSignatureMethods: String = ALL_SIGNATURE_METHODS
)

/** Valor padrão: todos os modos. Documentos anteriores à versão 8 recebem este valor. */
const val ALL_SIGNATURE_METHODS = "DRAW,TYPED,CERTIFICATE"

/** Modos que o admin liberou para este documento. */
fun DocumentEntity.allowedMethods(): Set<SignatureMethod> =
    allowedSignatureMethods.split(",")
        .mapNotNull { name -> runCatching { SignatureMethod.valueOf(name.trim()) }.getOrNull() }
        .toSet()

/** Texto gravado no banco; mantém a ordem do enum para ser estável. */
fun Set<SignatureMethod>.toStorage(): String =
    SignatureMethod.entries.filter { it in this }.joinToString(",") { it.name }
