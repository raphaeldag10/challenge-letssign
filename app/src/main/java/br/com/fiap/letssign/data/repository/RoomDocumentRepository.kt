package br.com.fiap.letssign.data.repository

import androidx.room.withTransaction
import br.com.fiap.letssign.data.local.AppDatabase
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.allowedMethods
import br.com.fiap.letssign.data.local.entity.DocumentHistoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignatoryEntity
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.SignatureMethod
import br.com.fiap.letssign.data.signature.CertificateSignature
import br.com.fiap.letssign.data.local.model.DocumentSummary
import br.com.fiap.letssign.data.local.model.SignatoryDetail
import br.com.fiap.letssign.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RoomDocumentRepository(private val db: AppDatabase) : DocumentRepository {

    private val dao = db.documentDao()

    override fun observeAllSummaries(): Flow<List<DocumentSummary>> = dao.observeAllSummaries()

    override fun observeSummariesForSignatory(userId: Long, signed: Boolean): Flow<List<DocumentSummary>> =
        dao.observeSummariesForSignatory(userId, signed)

    override suspend fun getDocumentById(id: Long): DocumentEntity? = withContext(Dispatchers.IO) {
        dao.getDocumentById(id)
    }

    override suspend fun canAccess(documentId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        val document = dao.getDocumentById(documentId) ?: return@withContext false
        document.createdById == userId || dao.countSignatoryLink(documentId, userId) > 0
    }

    override suspend fun createDocumentFlow(
        document: DocumentEntity,
        signatories: List<DocumentSignatoryEntity>,
        fields: List<SignatureFieldEntity>
    ): Long = withContext(Dispatchers.IO) {
        require(document.title.isNotBlank()) { "Informe o título do documento" }
        require(document.filePath.isNotBlank()) { "Selecione um arquivo PDF" }
        require(signatories.isNotEmpty()) { "Adicione pelo menos um signatário" }

        val signatoryUserIds = signatories.map { it.userId }.toSet()
        require(signatoryUserIds.size == signatories.size) { "Signatário repetido no documento" }

        fields.forEach { field ->
            require(field.pageNumber >= 0) { "Página inválida" }
            require(field.normalizedWidth > 0f && field.normalizedHeight > 0f) { "Campo sem tamanho" }
            require(
                field.normalizedX >= 0f && field.normalizedY >= 0f &&
                    field.normalizedX + field.normalizedWidth <= 1f + EPS &&
                    field.normalizedY + field.normalizedHeight <= 1f + EPS
            ) { "Campo fora dos limites da página" }
            require(field.signatoryId in signatoryUserIds) {
                "Campo associado a um signatário que não faz parte do documento"
            }
        }
        require(document.allowedMethods().isNotEmpty()) { "Escolha ao menos um modo de assinatura" }
        // Regra do guia de telas: cada signatário precisa de ao menos um campo.
        signatoryUserIds.forEach { userId ->
            require(fields.any { it.signatoryId == userId }) { "Adicione um campo para cada signatário" }
        }

        db.withTransaction { dao.insertFullDocumentFlow(document, signatories, fields) }
    }

    override suspend fun signDocument(
        documentId: Long,
        userId: Long,
        signaturePath: String,
        method: SignatureMethod,
        signerName: String,
        certificate: CertificateSignature?
    ): DocumentStatus =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val record = dao.getSignatoryRecord(documentId, userId)
                    ?: throw IllegalArgumentException("Você não é participante deste documento")
                require(!record.hasSigned) { "Assinatura já registrada" }
                val document = requireNotNull(dao.getDocumentById(documentId)) { "Documento não encontrado" }
                require(method in document.allowedMethods()) { "Este documento não aceita esse modo de assinatura" }

                dao.updateSignatory(
                    record.copy(
                        hasSigned = true,
                        signedAt = System.currentTimeMillis(),
                        signaturePath = signaturePath,
                        signatureMethod = method,
                        signerName = signerName,
                        signatureCms = certificate?.cmsBase64,
                        certificateSerial = certificate?.serial,
                        certificateIssuer = certificate?.issuer
                    )
                )
                dao.insertHistory(
                    DocumentHistoryEntity(
                        documentId = documentId,
                        eventType = "SIGNED",
                        description = when (method) {
                            SignatureMethod.DRAW -> "Assinatura desenhada registrada pelo participante."
                            SignatureMethod.TYPED -> "Assinatura digitada registrada pelo participante."
                            SignatureMethod.CERTIFICATE -> "Assinatura com certificado digital registrada pelo participante."
                        }
                    )
                )

                val all = dao.getSignatoriesForDocument(documentId)
                val newStatus = when {
                    all.all { it.hasSigned } -> DocumentStatus.COMPLETED
                    all.any { it.hasSigned } -> DocumentStatus.PARTIALLY_SIGNED
                    else -> DocumentStatus.PENDING
                }
                if (newStatus != document.status) {
                    dao.updateDocument(document.copy(status = newStatus))
                    when (newStatus) {
                        DocumentStatus.PARTIALLY_SIGNED -> dao.insertHistory(
                            DocumentHistoryEntity(
                                documentId = documentId,
                                eventType = "PARTIALLY_SIGNED",
                                description = "Documento parcialmente assinado."
                            )
                        )
                        DocumentStatus.COMPLETED -> dao.insertHistory(
                            DocumentHistoryEntity(
                                documentId = documentId,
                                eventType = "COMPLETED",
                                description = "Documento concluído com todas as assinaturas."
                            )
                        )
                        DocumentStatus.PENDING -> Unit
                    }
                }
                newStatus
            }
        }

    override suspend fun setFinalFilePath(documentId: Long, path: String) = withContext(Dispatchers.IO) {
        dao.setFinalFilePath(documentId, path)
    }

    override suspend fun getSignatoryRecord(documentId: Long, userId: Long): DocumentSignatoryEntity? =
        withContext(Dispatchers.IO) { dao.getSignatoryRecord(documentId, userId) }

    override suspend fun getSignatoriesForDocument(documentId: Long): List<DocumentSignatoryEntity> =
        withContext(Dispatchers.IO) { dao.getSignatoriesForDocument(documentId) }

    override suspend fun getSignatureFieldsForDocument(documentId: Long): List<SignatureFieldEntity> =
        withContext(Dispatchers.IO) { dao.getSignatureFieldsForDocument(documentId) }

    override fun observeSignatoryDetails(documentId: Long): Flow<List<SignatoryDetail>> =
        dao.observeSignatoryDetails(documentId)

    override fun observeHistory(documentId: Long): Flow<List<DocumentHistoryEntity>> =
        dao.observeHistory(documentId)

    private companion object {
        const val EPS = 1e-4f
    }
}
