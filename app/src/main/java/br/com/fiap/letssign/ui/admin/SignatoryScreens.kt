package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.ui.common.EmptyState
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.ScreenScaffold
import br.com.fiap.letssign.ui.common.appViewModel
import kotlinx.coroutines.flow.collect
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
    ScreenScaffold(
        title = "Signatários",
        floatingAction = {
            ExtendedFloatingActionButton(onClick = onNew, text = { Text("Cadastrar") }, icon = {})
        }
    ) { padding ->
        when {
            vm.all.isEmpty() -> EmptyState("Cadastre quem vai assinar seus documentos.") {
                Button(onClick = onNew) { Text("Cadastrar signatário") }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    label = { Text("Buscar por nome ou e-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                if (vm.filtered.isEmpty()) {
                    EmptyState("Nenhum resultado encontrado.") {
                        OutlinedButton(onClick = { vm.query = "" }) { Text("Limpar busca") }
                    }
                } else {
                    LazyColumn {
                        items(vm.filtered, key = { it.id }) { user ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpen(user.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(user.fullName, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                Text(user.email, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                Text(user.phone, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

class SignatoryFormViewModel(private val container: AppContainer, private val signatoryId: Long?) : ViewModel() {
    private val users = container.userRepository
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var password by mutableStateOf("")
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
                }
                loaded = true
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (busy) return
        error = null
        if (signatoryId == null && password.isBlank()) {
            error = "Informe a senha do signatário."
            return
        }
        busy = true
        viewModelScope.launch {
            try {
                if (signatoryId == null) {
                    users.registerSignatory(name, email, phone, password)
                } else {
                    users.updateSignatory(signatoryId, name, email, phone, password.ifBlank { null })
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
    ScreenScaffold(
        title = if (vm.isEditing) "Editar signatário" else "Novo signatário",
        onBack = onDone
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(vm.name, { vm.name = it }, label = { Text("Nome completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(vm.email, { vm.email = it }, label = { Text("E-mail (login)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(vm.phone, { vm.phone = it }, label = { Text("Telefone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = vm.password,
                onValueChange = { vm.password = it },
                label = { Text(if (vm.isEditing) "Nova senha (vazio mantém a atual)" else "Senha (mínimo 8 caracteres)") },
                visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { showPassword = !showPassword }) {
                Text(if (showPassword) "Ocultar senha" else "Mostrar senha")
            }
            ErrorText(vm.error)
            Button(
                onClick = { vm.save(onDone) },
                enabled = vm.loaded && !vm.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar signatário") }
        }
    }
}
