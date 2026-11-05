package br.com.fiap.letssign.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.ui.common.ActionShape
import br.com.fiap.letssign.ui.common.BrandLogo
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.FieldShape
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.FormSectionTitle
import br.com.fiap.letssign.ui.common.LabeledField
import br.com.fiap.letssign.ui.common.appViewModel

private const val MIN_PASSWORD_LENGTH = 8
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Primeiro acesso: cria a conta do administrador deste aparelho.
 * [onBack] é nulo quando esta é a primeira tela do app (não há para onde voltar).
 */
@Composable
fun SetupScreen(container: AppContainer, onBack: (() -> Unit)?, onDone: () -> Unit) {
    val vm = appViewModel { AuthViewModel(container) }
    val context = LocalContext.current
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val nameValid = vm.name.trim().length >= 3
    val emailValid = EMAIL_PATTERN.matches(vm.setupEmail.trim())
    val phoneValid = vm.phone.count(Char::isDigit) in 10..15
    val passwordValid = vm.setupPassword.length >= MIN_PASSWORD_LENGTH
    val passwordsMatch = vm.setupPassword == vm.setupPasswordConfirm
    val canSubmit = nameValid && emailValid && phoneValid && passwordValid && passwordsMatch && !vm.busy
    val showMismatch = vm.setupPasswordConfirm.isNotEmpty() && !passwordsMatch

    FormScaffold(
        title = "Criar administrador",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = {
                    vm.setupAdmin {
                        Toast.makeText(context, "Administrador criado. Entre para continuar.", Toast.LENGTH_LONG).show()
                        onDone()
                    }
                },
                enabled = canSubmit,
                shape = ActionShape,
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(if (vm.busy) "Salvando…" else "Criar administrador", fontWeight = FontWeight.SemiBold)
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
            BrandLogo(Modifier.align(Alignment.CenterHorizontally), iconSize = 40)
            IntroNotice()

            FormSectionTitle("Seus dados")
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
                value = vm.setupEmail,
                onValueChange = { vm.setupEmail = it },
                placeholder = "Ex.: ana@empresa.com",
                icon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            LabeledField(
                label = "Telefone",
                required = true,
                value = vm.phone,
                onValueChange = { vm.phone = it },
                placeholder = "Ex.: (11) 91234-5678",
                icon = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone
            )

            FormSectionTitle("Acesso")
            LabeledField(
                label = "Senha",
                required = true,
                value = vm.setupPassword,
                onValueChange = { vm.setupPassword = it },
                placeholder = "Mínimo $MIN_PASSWORD_LENGTH caracteres",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = visibility(showPassword),
                trailing = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Ocultar" else "Mostrar")
                    }
                },
                supportingText = if (vm.setupPassword.isNotEmpty() && !passwordValid) {
                    "A senha precisa ter pelo menos $MIN_PASSWORD_LENGTH caracteres."
                } else {
                    null
                },
                isError = vm.setupPassword.isNotEmpty() && !passwordValid
            )
            LabeledField(
                label = "Confirmar senha",
                required = true,
                value = vm.setupPasswordConfirm,
                onValueChange = { vm.setupPasswordConfirm = it },
                placeholder = "Digite a senha novamente",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = visibility(showPassword),
                supportingText = if (showMismatch) "As senhas não coincidem." else null,
                isError = showMismatch
            )

            ErrorText(vm.error)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun IntroNotice() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = FieldShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Primeiro acesso",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Esta conta será a administradora deste aparelho. Ela envia documentos e cadastra signatários.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun visibility(shown: Boolean): VisualTransformation =
    if (shown) VisualTransformation.None else PasswordVisualTransformation()
