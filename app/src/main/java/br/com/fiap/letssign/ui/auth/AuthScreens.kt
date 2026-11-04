package br.com.fiap.letssign.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.ErrorText
import br.com.fiap.letssign.ui.common.ScreenScaffold
import br.com.fiap.letssign.ui.common.appViewModel
import kotlinx.coroutines.launch

class AuthViewModel(private val container: AppContainer) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)

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
fun LoginScreen(container: AppContainer, onLoggedIn: (LoggedUser) -> Unit) {
    val vm = appViewModel { AuthViewModel(container) }
    ScreenScaffold(title = "LetsSign") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Entrar", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = vm.email,
                onValueChange = { vm.email = it },
                label = { Text("E-mail") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = vm.password,
                onValueChange = { vm.password = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            ErrorText(vm.error)
            Button(
                onClick = { vm.login(onLoggedIn) },
                enabled = !vm.busy && vm.email.isNotBlank() && vm.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (vm.busy) "Entrando…" else "Entrar") }
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
