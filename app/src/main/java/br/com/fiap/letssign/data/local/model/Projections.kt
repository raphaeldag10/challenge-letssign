package br.com.fiap.letssign.data.local.model

import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.entity.SignatoryRoleType

/** Linha de lista de documentos, com progresso de assinaturas. */
data class DocumentSummary(
    val id: Long,
    val title: String,
    val status: DocumentStatus,
    val createdAt: Long,
    val versionNumber: Int,
    val total: Int,
    val signed: Int,
    val filePath: String,
    val fileName: String?
)

/** Participante com nome e situação, para a tela de acompanhamento. */
data class SignatoryDetail(
    val userId: Long,
    val fullName: String,
    val email: String,
    val roleType: SignatoryRoleType,
    val hasSigned: Boolean,
    val signedAt: Long?
)
