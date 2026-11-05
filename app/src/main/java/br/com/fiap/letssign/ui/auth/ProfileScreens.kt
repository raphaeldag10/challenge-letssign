package br.com.fiap.letssign.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.data.local.entity.UserRole
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.theme.BrandBlue
import br.com.fiap.letssign.ui.theme.BrandBlueSoft

/**
 * Perfis que a conta pode usar. Cada conta tem um papel hoje, então só aparece ele;
 * o acesso continua vindo das credenciais, nunca da escolha feita aqui.
 */
fun profilesFor(user: LoggedUser): List<UserRole> = listOf(user.role)

@Composable
fun ProfileChoiceScreen(user: LoggedUser, onContinue: () -> Unit, onBack: () -> Unit) {
    val profiles = remember(user.id) { profilesFor(user) }
    var selected by rememberSaveable(user.id) { mutableStateOf(profiles.first()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Voltar") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Escolha seu perfil", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Como você vai usar o LetsSign?",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        profiles.forEach { role ->
            ProfileCard(
                role = role,
                selected = selected == role,
                onClick = { selected = role }
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continuar") }
    }
}

@Composable
private fun ProfileCard(role: UserRole, selected: Boolean, onClick: () -> Unit) {
    val isAdmin = role == UserRole.ADMIN
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) BrandBlueSoft else Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) BrandBlue else Color(0xFFD9DEE8), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandBlueSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isAdmin) Icons.Filled.Person else Icons.Filled.Create,
                contentDescription = null,
                tint = BrandBlue
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (isAdmin) "Administrador" else "Signatário",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (isAdmin) "Enviar documentos, definir signatários e acompanhar o andamento."
                else "Receber e assinar documentos enviados por outras pessoas.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}
