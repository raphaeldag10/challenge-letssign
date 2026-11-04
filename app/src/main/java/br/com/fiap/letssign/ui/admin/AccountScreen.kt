package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.data.local.entity.UserRole
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.ScreenScaffold

@Composable
fun AccountScreen(user: LoggedUser, onBack: (() -> Unit)?, onLogout: () -> Unit) {
    ScreenScaffold(title = "Conta", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(user.fullName, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(user.email)
            Text(if (user.role == UserRole.ADMIN) "Perfil: Administrador" else "Perfil: Signatário")
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
        }
    }
}
