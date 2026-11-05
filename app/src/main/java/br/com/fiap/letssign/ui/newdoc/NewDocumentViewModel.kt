package br.com.fiap.letssign.ui.newdoc

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.file.PdfException
import br.com.fiap.letssign.data.file.PdfPages
import br.com.fiap.letssign.data.local.entity.DocumentEntity
import br.com.fiap.letssign.data.local.entity.DocumentSignatoryEntity
import br.com.fiap.letssign.data.local.entity.SignatoryRoleType
import br.com.fiap.letssign.data.local.entity.SignatureFieldEntity
import br.com.fiap.letssign.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Participante no rascunho. A função pertence a este documento, não ao cadastro. */
data class DraftSignatory(
    val userId: Long,
    val fullName: String,
    val email: String,
    val role: SignatoryRoleType
)

/** Campo no rascunho, em coordenadas normalizadas (0 a 1) da página. [userId] identifica o participante. */
data class DraftField(
    val id: Long,
    val userId: Long,
    val page: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

val STEP_TITLES = listOf("Documento", "Pessoas", "Campos", "Revisão")

class NewDocumentViewModel(private val container: AppContainer, private val versionOfId: Long?) : ViewModel() {

    private val files = container.files

    var step by mutableStateOf(0)
    var title by mutableStateOf("")
    var message by mutableStateOf("")
    var pdfPath by mutableStateOf<String?>(null)
    var pdfName by mutableStateOf("")
    var pages by mutableStateOf<PdfPages?>(null)
    var pageSizes by mutableStateOf<List<Pair<Float, Float>>>(emptyList())
    var currentPage by mutableStateOf(0)

    var people by mutableStateOf<List<UserEntity>>(emptyList())
    val signatories = mutableStateListOf<DraftSignatory>()
    val fields = mutableStateListOf<DraftField>()
    var selectedUserId by mutableStateOf<Long?>(null)
    var selectedFieldId by mutableStateOf<Long?>(null)
    var pendingRemoval by mutableStateOf<Long?>(null)
    var replacingUserId by mutableStateOf<Long?>(null)

    var parentId by mutableStateOf<Long?>(null)
    var versionNumber by mutableStateOf(1)

    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var loadingPdf by mutableStateOf(false)
    var busy by mutableStateOf(false)

    private var nextFieldId = 1L

    val pageCount: Int get() = pageSizes.size

    fun aspect(page: Int): Float =
        pageSizes.getOrNull(page)?.let { it.first / it.second } ?: (1f / 1.414f)

    init {
        viewModelScope.launch {
            container.userRepository.observeSignatories().collect { people = it }
        }
        if (versionOfId != null) loadVersion(versionOfId)
    }

    // ---------- Passo 1: PDF ----------

    /** Copia o PDF escolhido no seletor para o app, valida e o carrega no rascunho. */
    fun importFromUri(uri: android.net.Uri, name: String) {
        loadingPdf = true
        error = null
        notice = null
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) { files.importPdf(uri) }
                val (handle, sizes) = withContext(Dispatchers.IO) {
                    val handle = PdfPages.open(path)
                    handle to (0 until handle.pageCount).map { handle.pageSize(it) }
                }
                replacePdf(path, name, handle, sizes)
            } catch (e: PdfException) {
                error = e.message
            } catch (e: Exception) {
                error = "Não foi possível ler o arquivo selecionado."
            } finally {
                loadingPdf = false
            }
        }
    }

    private fun replacePdf(path: String, name: String, handle: PdfPages, sizes: List<Pair<Float, Float>>) {
        pages?.close()
        pdfPath?.takeIf { it != path }?.let { File(it).delete() }
        pages = handle
        pageSizes = sizes
        pdfPath = path
        pdfName = name
        currentPage = 0
        if (fields.isNotEmpty()) {
            fields.clear()
            selectedFieldId = null
            notice = "Os campos do arquivo anterior foram descartados."
        }
    }

    // ---------- Nova versão ----------

    private fun loadVersion(id: Long) {
        loadingPdf = true
        viewModelScope.launch {
            try {
                val repo = container.documentRepository
                val original = withContext(Dispatchers.IO) { repo.getDocumentById(id) }
                    ?: throw PdfException("Documento original não encontrado.")
                val copy = withContext(Dispatchers.IO) { files.copyPdf(original.filePath) }
                val (handle, sizes) = withContext(Dispatchers.IO) {
                    val handle = PdfPages.open(copy)
                    handle to (0 until handle.pageCount).map { handle.pageSize(it) }
                }
                replacePdf(copy, "versão de ${original.title}", handle, sizes)
                title = original.title
                message = original.message.orEmpty()
                parentId = original.id
                versionNumber = original.versionNumber + 1

                val sigs = withContext(Dispatchers.IO) { repo.getSignatoriesForDocument(id) }
                val originalFields = withContext(Dispatchers.IO) { repo.getSignatureFieldsForDocument(id) }
                signatories.clear()
                for (s in sigs) {
                    val user = withContext(Dispatchers.IO) { container.userRepository.getSignatory(s.userId) } ?: continue
                    signatories.add(DraftSignatory(user.id, user.fullName, user.email, s.roleType))
                }
                fields.clear()
                for (f in originalFields) {
                    val s = sigs.firstOrNull { it.id == f.signatoryId } ?: continue
                    fields.add(
                        DraftField(
                            id = nextFieldId++, userId = s.userId, page = f.pageNumber,
                            x = f.normalizedX, y = f.normalizedY, w = f.normalizedWidth, h = f.normalizedHeight
                        )
                    )
                }
                notice = "Nova versão baseada no documento original. Revise antes de enviar."
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível carregar o documento original."
            } finally {
                loadingPdf = false
            }
        }
    }

    // ---------- Passo 2: pessoas ----------

    fun togglePerson(person: UserEntity) {
        if (signatories.any { it.userId == person.id }) {
            requestRemoval(person.id)
        } else {
            val role = if (signatories.isEmpty()) SignatoryRoleType.CONTRATANTE else SignatoryRoleType.CONTRATADO
            signatories.add(DraftSignatory(person.id, person.fullName, person.email, role))
        }
    }

    fun setRole(userId: Long, role: SignatoryRoleType) {
        val i = signatories.indexOfFirst { it.userId == userId }
        if (i >= 0) signatories[i] = signatories[i].copy(role = role)
    }

    fun fieldCount(userId: Long): Int = fields.count { it.userId == userId }

    private fun requestRemoval(userId: Long) {
        if (fieldCount(userId) > 0) pendingRemoval = userId else removeSignatory(userId)
    }

    fun confirmRemoval() {
        pendingRemoval?.let { removeSignatory(it) }
        pendingRemoval = null
    }

    fun cancelRemoval() {
        pendingRemoval = null
    }

    /**
     * Troca o signatário [oldUserId] por [newPerson], mantendo a função e reassociando
     * os campos já posicionados (posição e página não mudam).
     */
    fun replaceSignatory(oldUserId: Long, newPerson: UserEntity) {
        val i = signatories.indexOfFirst { it.userId == oldUserId }
        if (i < 0) return
        if (signatories.any { it.userId == newPerson.id }) {
            error = "${newPerson.fullName} já está neste documento."
            return
        }
        val role = signatories[i].role
        signatories[i] = DraftSignatory(newPerson.id, newPerson.fullName, newPerson.email, role)
        for (k in fields.indices) {
            if (fields[k].userId == oldUserId) fields[k] = fields[k].copy(userId = newPerson.id)
        }
        if (selectedUserId == oldUserId) selectedUserId = newPerson.id
        replacingUserId = null
        error = null
    }

    private fun removeSignatory(userId: Long) {
        signatories.removeAll { it.userId == userId }
        fields.removeAll { it.userId == userId }
        if (selectedUserId == userId) selectedUserId = null
    }

    // ---------- Passo 3: campos ----------

    fun setPage(index: Int) {
        currentPage = index.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        selectedFieldId = null
    }

    fun selectPerson(userId: Long) {
        selectedUserId = userId
    }

    fun addField() {
        val userId = selectedUserId ?: run {
            error = "Escolha um signatário para o campo."
            return
        }
        val onPage = fields.count { it.page == currentPage }
        val field = DraftField(
            id = nextFieldId++, userId = userId, page = currentPage,
            x = 0.1f, y = 0.1f + 0.1f * (onPage % 6), w = 0.3f, h = 0.07f
        )
        fields.add(field)
        selectedFieldId = field.id
        error = null
    }

    fun selectField(id: Long) {
        selectedFieldId = id
        fields.firstOrNull { it.id == id }?.let { selectedUserId = it.userId }
    }

    /** Move o campo em fração da página (dx/dy normalizados), mantendo-o dentro do papel. */
    fun moveField(id: Long, dx: Float, dy: Float) {
        val i = fields.indexOfFirst { it.id == id }
        if (i < 0) return
        val f = fields[i]
        val (x, y) = FieldGeometry.move(f.x, f.y, f.w, f.h, dx, dy)
        fields[i] = f.copy(x = x, y = y)
    }

    fun resizeField(id: Long, dw: Float, dh: Float) {
        val i = fields.indexOfFirst { it.id == id }
        if (i < 0) return
        val f = fields[i]
        val (w, h) = FieldGeometry.resize(f.x, f.y, f.w, f.h, dw, dh)
        fields[i] = f.copy(w = w, h = h)
    }

    fun deleteField(id: Long) {
        fields.removeAll { it.id == id }
        if (selectedFieldId == id) selectedFieldId = null
    }

    // ---------- Validação e navegação ----------

    /** Mensagem de erro do passo, ou null se o passo estiver válido. */
    fun validate(step: Int): String? = when (step) {
        0 -> when {
            title.isBlank() -> "Informe o título do documento."
            pdfPath == null -> "Selecione um arquivo PDF."
            else -> null
        }
        1 -> if (signatories.isEmpty()) "Adicione pelo menos um signatário." else null
        2 -> signatories.firstOrNull { s -> fields.none { it.userId == s.userId } }
            ?.let { "Adicione um campo para ${it.fullName}." }
            ?: overlapError()
        else -> validate(0) ?: validate(1) ?: validate(2)
    }

    private fun overlapError(): String? {
        fields.groupBy { it.page }.forEach { (page, list) ->
            for (i in list.indices) for (j in i + 1 until list.size) {
                if (overlaps(list[i], list[j])) {
                    return "Campos sobrepostos na página ${page + 1}. Reposicione antes de continuar."
                }
            }
        }
        return null
    }

    private fun overlaps(a: DraftField, b: DraftField): Boolean =
        FieldGeometry.overlaps(a.x, a.y, a.w, a.h, b.x, b.y, b.w, b.h)

    fun next() {
        val problem = validate(step)
        error = problem
        if (problem == null) step = (step + 1).coerceAtMost(STEP_TITLES.lastIndex)
    }

    fun back() {
        error = null
        step = (step - 1).coerceAtLeast(0)
    }

    // ---------- Envio ----------

    fun send(onSent: (Long) -> Unit) {
        if (busy) return
        validate(3)?.let {
            error = it
            return
        }
        val path = pdfPath ?: return
        val admin = container.userRepository.currentUser ?: run {
            error = "Sessão expirada. Entre novamente."
            return
        }

        val document = DocumentEntity(
            title = title.trim(),
            filePath = path,
            createdById = admin.id,
            message = message.trim().ifBlank { null },
            versionNumber = versionNumber,
            parentDocumentId = parentId
        )
        // Campos referenciam o participante pelo userId; o DAO converte para o ID gerado.
        val signatoryEntities = signatories.map {
            DocumentSignatoryEntity(documentId = 0, userId = it.userId, roleType = it.role)
        }
        val fieldEntities = fields.map {
            SignatureFieldEntity(
                documentId = 0, signatoryId = it.userId, pageNumber = it.page,
                normalizedX = it.x, normalizedY = it.y, normalizedWidth = it.w, normalizedHeight = it.h
            )
        }

        busy = true
        error = null
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    container.documentRepository.createDocumentFlow(document, signatoryEntities, fieldEntities)
                }
                onSent(id)
            } catch (e: IllegalArgumentException) {
                error = e.message ?: "Envio inválido."
            } catch (e: Exception) {
                // A gravação é uma transação: se falhou, nada foi salvo e pode tentar de novo.
                error = "Envio não confirmado. Nada foi gravado e seus dados foram preservados. Tente novamente."
            } finally {
                busy = false
            }
        }
    }

    override fun onCleared() {
        pages?.close()
        pages = null
    }
}
