package br.com.fiap.letssign.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.DocumentStatus
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.common.BrandLogo
import br.com.fiap.letssign.ui.common.appViewModel
import br.com.fiap.letssign.ui.common.firstNameOf
import br.com.fiap.letssign.ui.common.initialsOf
import br.com.fiap.letssign.ui.theme.BrandBlue
import br.com.fiap.letssign.ui.theme.BrandBlueSoft
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AdminHomeViewModel(container: AppContainer) : ViewModel() {
    var inProgress by mutableStateOf(0)

    init {
        viewModelScope.launch {
            container.documentRepository.observeAllSummaries().collect { list ->
                inProgress = list.count { it.status != DocumentStatus.COMPLETED }
            }
        }
    }
}

@Composable
fun AdminHomeScreen(
    container: AppContainer,
    user: LoggedUser,
    onSendPdf: () -> Unit,
    onSignatories: () -> Unit,
    onDocuments: () -> Unit,
    onAccount: () -> Unit
) {
    val vm = appViewModel { AdminHomeViewModel(container) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BrandLogo(iconSize = 32)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAccount),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initialsOf(user.fullName),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Olá, ${firstNameOf(user.fullName)}! 👋",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Aqui você gerencia seus documentos.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                title = "Enviar PDF",
                subtitle = "Iniciar nova assinatura",
                icon = Icons.Filled.Add,
                modifier = Modifier.weight(1f),
                onClick = onSendPdf
            )
            HomeTile(
                title = "Signatários",
                subtitle = "Gerenciar pessoas",
                icon = Icons.Filled.Person,
                modifier = Modifier.weight(1f),
                onClick = onSignatories
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                title = "Documentos",
                subtitle = "Acompanhar status",
                icon = Icons.Filled.List,
                modifier = Modifier.weight(1f),
                onClick = onDocuments
            )
            HomeTile(
                title = "Relatórios",
                subtitle = "Em breve",
                icon = Icons.Filled.Star,
                modifier = Modifier.weight(1f),
                onClick = null
            )
        }

        val cardShape = RoundedCornerShape(16.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                .clickable(onClick = onDocuments)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Documentos em andamento", style = MaterialTheme.typography.bodyMedium)
                Text(
                    vm.inProgress.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}
