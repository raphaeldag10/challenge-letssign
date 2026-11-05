package br.com.fiap.letssign.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.AuthMode
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.data.local.entity.UserRole
import br.com.fiap.letssign.data.repository.Cpf
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.ActionShape
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.FormSectionTitle
import br.com.fiap.letssign.ui.common.LabeledField
import br.com.fiap.letssign.ui.common.appViewModel
import kotlinx.coroutines.launch

class EditProfileViewModel(private val container: AppContainer, private val userId: Long) : ViewModel() {
    private val users = container.userRepository

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var cpf by mutableStateOf("")
    /** Verdadeiro quando o admin cadastrou o CPF; o próprio usuário não pode mais alterá-lo. */
    var cpfLocked by mutableStateOf(false)
    var role by mutableStateOf(UserRole.SIGNATORY)
    var authMode by mutableStateOf(AuthMode.PASSWORD)
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            users.currentProfile()?.let(::fill)
            loaded = true
        }
    }

    private fun fill(profile: UserEntity) {
        name = profile.fullName
        email = profile.email
        phone = profile.phone
        cpf = Cpf.format(profile.cpf.orEmpty())
        cpfLocked = profile.cpfLockedByAdmin
        role = profile.role
        authMode = profile.authMode
    }

    /** Campos de senha só fazem sentido para contas que entram com senha. */
    val canChangePassword: Boolean get() = authMode == AuthMode.PASSWORD

    /** O e-mail do signatário é definido pelo admin e não pode ser alterado por ele. */
    val emailLocked: Boolean get() = role == UserRole.SIGNATORY

    /** O CPF é informado pelo signatário, quando ainda não foi cadastrado pelo admin. */
    val showCpf: Boolean get() = role == UserRole.SIGNATORY

    fun save(onSaved: (LoggedUser) -> Unit) {
        if (busy) return
        error = null
        busy = true
        viewModelScope.launch {
            try {
                val updated = users.updateOwnProfile(
                    name = name,
                    email = email,
                    phone = phone,
                    currentPassword = currentPassword.ifBlank { null },
                    newPassword = newPassword.ifBlank { null },
                    cpf = cpf.ifBlank { null }
                )
                currentPassword = ""
                newPassword = ""
                onSaved(updated)
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
fun EditProfileScreen(
    container: AppContainer,
    user: LoggedUser,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val vm = appViewModel(key = "edit-profile-${user.id}") { EditProfileViewModel(container, user.id) }
    val context = LocalContext.current
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }

    FormScaffold(
        title = "Editar dados",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = {
                    vm.save { _ ->
                        Toast.makeText(context, "Dados atualizados", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                enabled = vm.loaded && !vm.busy,
                shape = ActionShape,
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(if (vm.busy) "Salvando…" else "Salvar alterações", fontWeight = FontWeight.SemiBold)
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
            FormSectionTitle("Dados pessoais")
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
                keyboardType = KeyboardType.Email,
                enabled = !vm.emailLocked,
                supportingText = if (vm.emailLocked) "Definido pelo administrador. Fale com ele para alterar." else null
            )
            if (vm.showCpf) {
                LabeledField(
                    label = "CPF",
                    required = false,
                    value = vm.cpf,
                    onValueChange = { vm.cpf = Cpf.format(it) },
                    placeholder = "000.000.000-00",
                    icon = Icons.Outlined.AccountBox,
                    keyboardType = KeyboardType.Number,
                    enabled = !vm.cpfLocked,
                    supportingText = if (vm.cpfLocked) {
                        "Cadastrado pelo administrador. Não pode ser alterado."
                    } else {
                        "Informe seu CPF. Se o administrador não tiver cadastrado, você pode alterá-lo aqui."
                    }
                )
            }
            LabeledField(
                label = "Telefone",
                required = vm.role == UserRole.ADMIN,
                value = vm.phone,
                onValueChange = { vm.phone = it },
                placeholder = "Ex.: (11) 91234-5678",
                icon = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone
            )

            if (vm.canChangePassword) {
                FormSectionTitle("Alterar senha")
                Text(
                    "Deixe em branco para manter a senha atual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LabeledField(
                    label = "Senha atual",
                    required = false,
                    value = vm.currentPassword,
                    onValueChange = { vm.currentPassword = it },
                    placeholder = "Necessária para trocar a senha",
                    icon = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = visibility(showCurrent),
                    trailing = {
                        TextButton(onClick = { showCurrent = !showCurrent }) {
                            Text(if (showCurrent) "Ocultar" else "Mostrar")
                        }
                    }
                )
                LabeledField(
                    label = "Nova senha",
                    required = false,
                    value = vm.newPassword,
                    onValueChange = { vm.newPassword = it },
                    placeholder = "Mínimo 8 caracteres",
                    icon = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = visibility(showNew),
                    trailing = {
                        TextButton(onClick = { showNew = !showNew }) {
                            Text(if (showNew) "Ocultar" else "Mostrar")
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Sua conta entra com código enviado por e-mail, então não há senha para alterar aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ErrorText(vm.error)
        }
    }
}

private fun visibility(shown: Boolean): VisualTransformation =
    if (shown) VisualTransformation.None else PasswordVisualTransformation()
