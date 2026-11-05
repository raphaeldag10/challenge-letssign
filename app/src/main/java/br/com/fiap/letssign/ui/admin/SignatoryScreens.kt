package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.AuthMode
import br.com.fiap.letssign.data.local.entity.UserEntity
import androidx.compose.material.icons.outlined.AccountBox
import br.com.fiap.letssign.data.repository.Cpf
import br.com.fiap.letssign.ui.common.ActionShape
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.FieldLabel
import br.com.fiap.letssign.ui.common.FieldShape
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.CpfVisualTransformation
import br.com.fiap.letssign.ui.common.LabeledField
import br.com.fiap.letssign.ui.common.appViewModel
import kotlinx.coroutines.launch

class SignatoriesViewModel(container: AppContainer) : ViewModel() {
    private val users = container.userRepository
    var all by mutableStateOf<List<UserEntity>>(emptyList())
    var query by mutableStateOf("")

    init {
        viewModelScope.launch { users.observeSignatories().collect { all = it } }
    }

    val filtered: List<UserEntity>
        get() = all.filter {
            query.isBlank() ||
                it.fullName.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
        }
}

@Composable
fun SignatoriesScreen(container: AppContainer, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val vm = appViewModel { SignatoriesViewModel(container) }
    FormScaffold(
        title = "Signatários",
        bottomBar = {
            if (vm.all.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onNew,
                    shape = ActionShape,
                    contentPadding = PaddingValues(vertical = 18.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Adicionar signatário", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        if (vm.all.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState("Cadastre quem vai assinar seus documentos.") {
                    Button(onClick = onNew, shape = ActionShape) { Text("Adicionar signatário") }
                }
            }
        } else Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = { Text("Buscar signatário…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (vm.filtered.isEmpty()) {
                EmptyState("Nenhum resultado encontrado.") {
                    OutlinedButton(onClick = { vm.query = "" }) { Text("Limpar busca") }
                }
            } else {
                LazyColumn {
                    items(vm.filtered, key = { it.id }) { user ->
                        SignatoryRow(user, onOpen = { onOpen(user.id) })
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatoryRow(user: UserEntity, onOpen: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp)
    ) {
        InitialsAvatar(user.fullName)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções de ${user.fullName}")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        menuOpen = false
                        onOpen()
                    }
                )
            }
        }
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    val initials = name.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .let { parts -> listOfNotNull(parts.firstOrNull(), parts.drop(1).lastOrNull()) }
        .joinToString("") { it.first().uppercase() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

class SignatoryFormViewModel(private val container: AppContainer, private val signatoryId: Long?) : ViewModel() {
    private val users = container.userRepository
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var authMode by mutableStateOf(AuthMode.PASSWORD)
    var password by mutableStateOf("")
    var cpf by mutableStateOf("")
    /** CPF já preenchido pelo próprio signatário (o admin ainda pode alterar ou apagar). */
    var cpfFromSignatory by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var loaded by mutableStateOf(signatoryId == null)

    val isEditing: Boolean get() = signatoryId != null

    init {
        if (signatoryId != null) {
            viewModelScope.launch {
                users.getSignatory(signatoryId)?.let {
                    name = it.fullName
                    email = it.email
                    phone = it.phone
                    authMode = it.authMode
                    cpf = it.cpf.orEmpty()
                    cpfFromSignatory = it.cpf != null && !it.cpfLockedByAdmin
                }
                loaded = true
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (busy) return
        error = null
        busy = true
        viewModelScope.launch {
            try {
                if (signatoryId == null) {
                    users.registerSignatory(name, email, phone, authMode, password.ifBlank { null }, cpf)
                } else {
                    users.updateSignatory(signatoryId, name, email, phone, authMode, password.ifBlank { null }, cpf)
                }
                onDone()
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (e: IllegalStateException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun SignatoryFormScreen(container: AppContainer, signatoryId: Long?, onDone: () -> Unit) {
    val vm = appViewModel(key = "signatory-$signatoryId") { SignatoryFormViewModel(container, signatoryId) }
    var showPassword by remember { mutableStateOf(false) }
    FormScaffold(
        title = if (vm.isEditing) "Editar signatário" else "Novo signatário",
        onBack = onDone,
        bottomBar = {
            Button(
                onClick = { vm.save(onDone) },
                enabled = vm.loaded && !vm.busy,
                shape = ActionShape,
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(if (vm.isEditing) "Salvar" else "Adicionar", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NewSignatoryIllustration(Modifier.align(Alignment.CenterHorizontally))

            LabeledField(
                label = "Nome completo",
                required = true,
                value = vm.name,
                onValueChange = { vm.name = it },
                placeholder = "Ex.: Ana Silva",
                icon = Icons.Outlined.Person
            )
            LabeledField(
                label = "E-mail",
                required = true,
                value = vm.email,
                onValueChange = { vm.email = it },
                placeholder = "Ex.: ana@empresa.com",
                icon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            LabeledField(
                label = "CPF",
                required = false,
                value = vm.cpf,
                onValueChange = { vm.cpf = Cpf.digitsOf(it).take(Cpf.LENGTH) },
                placeholder = "000.000.000-00",
                visualTransformation = CpfVisualTransformation,
                icon = Icons.Outlined.AccountBox,
                keyboardType = KeyboardType.Number,
                supportingText = if (vm.cpfFromSignatory) {
                    "Preenchido pelo signatário. Ao informar um CPF aqui, ele passa a ser travado para o signatário."
                } else {
                    "Opcional. Se informado, o signatário não poderá alterá-lo."
                }
            )
            LabeledField(
                label = "Telefone",
                required = false,
                value = vm.phone,
                onValueChange = { vm.phone = it },
                placeholder = "Ex.: (11) 91234-5678",
                icon = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FieldLabel("Modo de autenticação", required = true)
                AuthMode.entries.forEach { mode ->
                    AuthModeOption(
                        mode = mode,
                        selected = vm.authMode == mode,
                        onSelect = { vm.authMode = mode }
                    )
                }
            }

            if (vm.authMode == AuthMode.PASSWORD) {
                LabeledField(
                    label = if (vm.isEditing) "Nova senha" else "Senha",
                    required = !vm.isEditing,
                    value = vm.password,
                    onValueChange = { vm.password = it },
                    placeholder = if (vm.isEditing) "Vazio mantém a atual" else "Mínimo 8 caracteres",
                    icon = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "Ocultar" else "Mostrar")
                        }
                    }
                )
            }
            ErrorText(vm.error)
        }
    }
}

@Composable
private fun NewSignatoryIllustration(modifier: Modifier = Modifier) {
    Box(modifier.padding(vertical = 8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(32.dp)
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AuthModeOption(mode: AuthMode, selected: Boolean, onSelect: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .border(1.dp, borderColor, FieldShape)
            .clickable(onClick = onSelect)
            .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(if (mode == AuthMode.PASSWORD) "Senha" else "Código por e-mail")
            Text(
                if (mode == AuthMode.PASSWORD) "Entra com e-mail e senha."
                else "Entra com e-mail e um código de uso único (10 min).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

