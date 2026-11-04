package br.com.fiap.letssign.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.BrandLogo
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.ScreenScaffold
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.theme.BrandBlue
import kotlinx.coroutines.launch

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)

    // Login por código de uso único (signatário com AuthMode.EMAIL_CODE).
    var codeRequested by mutableStateOf(false)
    var demoCode by mutableStateOf<String?>(null)
    var codeInput by mutableStateOf("")

    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var setupEmail by mutableStateOf("")
    var setupPassword by mutableStateOf("")

    fun login(onSuccess: (LoggedUser) -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                onSuccess(container.userRepository.login(email, password))
            } catch (e: IllegalArgumentException) {
                error = "E-mail ou senha incorretos."
            } finally {
                busy = false
            }
        }
    }

    fun requestCode() {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                // Sem envio de e-mail nesta etapa: o código é exibido na própria tela.
                demoCode = container.userRepository.requestLoginCode(email)
                codeRequested = true
                codeInput = ""
            } catch (e: IllegalArgumentException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    fun loginWithCode(onSuccess: (LoggedUser) -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                onSuccess(container.userRepository.loginWithCode(email, codeInput))
            } catch (e: IllegalArgumentException) {
                error = e.message ?: "Código inválido ou expirado"
            } finally {
                busy = false
            }
        }
    }

    fun setupAdmin(onDone: () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                container.userRepository.setupInitialAdmin(name, setupEmail, phone, setupPassword)
                onDone()
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível concluir a configuração."
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun LoginScreen(container: AppContainer, onSetup: () -> Unit, onLoggedIn: (LoggedUser) -> Unit) {
    val vm = appViewModel { AuthViewModel(container) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showCodeLogin by rememberSaveable { mutableStateOf(false) }
    val buttonColors = ButtonDefaults.buttonColors(containerColor = BrandBlue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrandLogo(iconSize = 48)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bem-vindo ao LetsSign!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            "Assine e gerencie documentos de forma simples e segura.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = vm.email,
            onValueChange = { vm.email = it },
            label = { Text("E-mail") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = vm.password,
            onValueChange = { vm.password = it },
            label = { Text("Senha") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Ocultar" else "Mostrar")
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        ErrorText(vm.error)
        Button(
            onClick = { vm.login(onLoggedIn) },
            enabled = !vm.busy && vm.email.isNotBlank() && vm.password.isNotBlank(),
            colors = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (vm.busy) "Entrando…" else "Entrar") }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(Modifier.weight(1f))
            Text("  ou  ", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = { showCodeLogin = !showCodeLogin },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (showCodeLogin) "Ocultar acesso por código" else "Entrar com código por e-mail") }

        if (showCodeLogin) {
            OutlinedButton(
                onClick = { vm.requestCode() },
                enabled = !vm.busy && vm.email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (vm.codeRequested) "Gerar novo código" else "Receber código") }

            if (vm.codeRequested) {
                Text(
                    "Código de acesso: ${vm.demoCode}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Nenhum e-mail é enviado nesta etapa. Válido por 10 minutos.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = vm.codeInput,
                    onValueChange = { vm.codeInput = it.filter(Char::isDigit).take(6) },
                    label = { Text("Código de 6 dígitos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { vm.loginWithCode(onLoggedIn) },
                    enabled = !vm.busy && vm.codeInput.length == 6,
                    colors = buttonColors,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirmar código") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Primeiro acesso? ", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onSetup) { Text("Crie sua conta", color = BrandBlue) }
        }
    }
}

@Composable
fun SetupScreen(container: AppContainer, onDone: () -> Unit) {
    val vm = appViewModel { AuthViewModel(container) }
    ScreenScaffold(title = "Configurar administrador") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Primeiro acesso: crie a conta do administrador deste aparelho.")
            OutlinedTextField(vm.name, { vm.name = it }, label = { Text("Nome completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(vm.setupEmail, { vm.setupEmail = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(vm.phone, { vm.phone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                vm.setupPassword, { vm.setupPassword = it },
                label = { Text("Senha (mínimo 8 caracteres)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            ErrorText(vm.error)
            Button(
                onClick = { vm.setupAdmin(onDone) },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (vm.busy) "Salvando…" else "Criar administrador") }
        }
    }
}
