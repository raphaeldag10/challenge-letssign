package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.data.local.entity.UserRole
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.ActionShape
import br.com.fiap.letssign.ui.common.FieldShape
import br.com.fiap.letssign.ui.common.FormScaffold
import br.com.fiap.letssign.ui.common.initialsOf

@Composable
fun AccountScreen(user: LoggedUser, onBack: (() -> Unit)?, onLogout: () -> Unit, onEdit: () -> Unit) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    FormScaffold(
        title = "Conta",
        onBack = onBack,
        bottomBar = {
            OutlinedButton(
                onClick = { confirmLogout = true },
                shape = ActionShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Sair da conta", fontWeight = FontWeight.SemiBold)
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
            ProfileHeader(user)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FieldShape)
            ) {
                InfoRow(Icons.Outlined.Email, "E-mail", user.email)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(Icons.Outlined.Person, "Perfil", roleLabel(user.role))
            }
            FilledTonalButton(
                onClick = onEdit,
                shape = ActionShape,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Editar dados", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            shape = FieldShape,
            title = { Text("Sair da conta?", fontWeight = FontWeight.Bold) },
            text = { Text("Você precisará entrar novamente para acessar seus documentos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    }
                ) { Text("Sair", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProfileHeader(user: LoggedUser) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Text(
                initialsOf(user.fullName),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            user.fullName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun roleLabel(role: UserRole): String =
    if (role == UserRole.ADMIN) "Administrador" else "Signatário"
