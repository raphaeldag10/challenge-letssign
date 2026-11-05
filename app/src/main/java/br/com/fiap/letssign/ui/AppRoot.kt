package br.com.fiap.letssign.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import br.com.fiap.letssign.AppContainer
import br.com.fiap.letssign.data.local.entity.UserRole
import br.com.fiap.letssign.data.repository.LoggedUser
import br.com.fiap.letssign.ui.admin.AccountScreen
import br.com.fiap.letssign.ui.admin.AdminHomeScreen
import br.com.fiap.letssign.ui.admin.DocumentDetailScreen
import br.com.fiap.letssign.ui.admin.DocumentsScreen
import br.com.fiap.letssign.ui.admin.SignatoryFormScreen
import br.com.fiap.letssign.ui.admin.SignatoriesScreen
import br.com.fiap.letssign.ui.auth.LoginScreen
import br.com.fiap.letssign.ui.auth.ProfileChoiceScreen
import br.com.fiap.letssign.ui.auth.SetupScreen
import br.com.fiap.letssign.ui.common.CenteredLoading
import br.com.fiap.letssign.ui.newdoc.NewDocumentScreen
import br.com.fiap.letssign.ui.signatory.SignatoryDocumentScreen
import br.com.fiap.letssign.ui.signatory.SignatoryHomeScreen
import br.com.fiap.letssign.ui.signatory.SignatureScreen

/** Telas do app. Rotas com dados (id, nonce) mantêm o estado correto em cada navegação. */
sealed interface Route {
    data object Loading : Route
    data object Setup : Route
    data object Login : Route
    data object ProfileChoice : Route
    data object AdminHome : Route
    data object AdminDocuments : Route
    data object AdminSignatories : Route
    data object Account : Route
    data object SignatoryHome : Route
    data class DocumentDetail(val id: Long) : Route
    data class DocumentView(val id: Long) : Route
    data class SignDocument(val id: Long) : Route
    data class NewDocument(val versionOfId: Long?, val nonce: Long) : Route
    data class SignatoryForm(val id: Long?) : Route
}

fun homeFor(user: LoggedUser): Route =
    if (user.role == UserRole.ADMIN) Route.AdminHome else Route.SignatoryHome

/** Pilha simples de navegação. Fica no Composable raiz, então sobrevive às recomposições. */
class AppNavigator {
    private val stack = mutableStateListOf<Route>(Route.Loading)

    val current: Route get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(route: Route) {
        stack.add(route)
    }

    fun replaceAll(route: Route) {
        stack.clear()
        stack.add(route)
    }

    fun pop() {
        if (canPop) stack.removeAt(stack.lastIndex)
    }
}

@Composable
fun AppRoot(container: AppContainer) {
    val nav = remember { AppNavigator() }
    val session by container.userRepository.session.collectAsState()

    LaunchedEffect(Unit) {
        val user = container.userRepository.currentUser
        nav.replaceAll(
            when {
                user != null -> homeFor(user)
                container.userRepository.hasAdmin() -> Route.Login
                else -> Route.Setup
            }
        )
    }

    BackHandler(enabled = nav.canPop) { nav.pop() }

    val logout: () -> Unit = {
        container.userRepository.logout()
        nav.replaceAll(Route.Login)
    }

    when (val route = nav.current) {
        Route.Loading -> CenteredLoading()
        Route.Setup -> SetupScreen(container) { nav.replaceAll(Route.Login) }
        Route.Login -> LoginScreen(
            container = container,
            onSetup = { nav.replaceAll(Route.Setup) },
            onLoggedIn = { nav.replaceAll(Route.ProfileChoice) }
        )
        else -> {
            val user = session
            if (user == null) {
                LaunchedEffect(Unit) { nav.replaceAll(Route.Login) }
                CenteredLoading()
            } else {
                AuthenticatedRoute(route, user, nav, container, logout)
            }
        }
    }
}

@Composable
private fun AuthenticatedRoute(
    route: Route,
    user: LoggedUser,
    nav: AppNavigator,
    container: AppContainer,
    logout: () -> Unit
) {
    val isAdmin = user.role == UserRole.ADMIN
    when (route) {
        Route.AdminHome, Route.AdminDocuments, Route.AdminSignatories -> AdminShell(route, nav, container, user, logout)
        Route.ProfileChoice -> ProfileChoiceScreen(
            user = user,
            onContinue = { nav.replaceAll(homeFor(user)) },
            onBack = logout
        )
        Route.Account -> if (isAdmin) {
            AdminShell(route, nav, container, user, logout)
        } else {
            AccountScreen(user, onBack = { nav.pop() }, onLogout = logout)
        }
        Route.SignatoryHome -> SignatoryHomeScreen(
            container = container,
            user = user,
            onOpen = { nav.push(Route.DocumentView(it)) },
            onAccount = { nav.push(Route.Account) }
        )
        is Route.DocumentView -> SignatoryDocumentScreen(
            container = container,
            documentId = route.id,
            user = user,
            onSign = { nav.push(Route.SignDocument(route.id)) },
            onBack = { nav.pop() }
        )
        is Route.SignDocument -> SignatureScreen(
            container = container,
            documentId = route.id,
            user = user,
            onDone = { nav.replaceAll(Route.SignatoryHome) }
        )
        is Route.DocumentDetail -> DocumentDetailScreen(
            container = container,
            documentId = route.id,
            user = user,
            onBack = { nav.pop() },
            onNewVersion = { nav.push(Route.NewDocument(route.id, System.nanoTime())) }
        )
        is Route.NewDocument -> NewDocumentScreen(
            container = container,
            versionOfId = route.versionOfId,
            viewModelKey = "new-${route.nonce}",
            onCancel = { nav.pop() },
            onSent = { id ->
                nav.pop()
                nav.push(Route.DocumentDetail(id))
            }
        )
        is Route.SignatoryForm -> SignatoryFormScreen(
            container = container,
            signatoryId = route.id,
            onDone = { nav.pop() }
        )
        Route.Loading, Route.Setup, Route.Login -> CenteredLoading()
    }
}

@Composable
private fun AdminShell(
    route: Route,
    nav: AppNavigator,
    container: AppContainer,
    user: LoggedUser,
    logout: () -> Unit
) {
    val tabs = listOf(
        Triple(Route.AdminHome, "Início", Icons.Filled.Home),
        Triple(Route.AdminDocuments, "Documentos", Icons.Filled.List),
        Triple(Route.AdminSignatories, "Signatários", Icons.Filled.Person),
        Triple(Route.Account, "Perfil", Icons.Filled.AccountCircle)
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (tab, label, icon) ->
                    NavigationBarItem(
                        selected = route == tab,
                        onClick = { if (route != tab) nav.replaceAll(tab) },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (route) {
                Route.AdminSignatories -> SignatoriesScreen(
                    container = container,
                    onOpen = { nav.push(Route.SignatoryForm(it)) },
                    onNew = { nav.push(Route.SignatoryForm(null)) }
                )
                Route.Account -> AccountScreen(user, onBack = null, onLogout = logout)
                Route.AdminHome -> AdminHomeScreen(
                    container = container,
                    user = user,
                    onSendPdf = { nav.push(Route.NewDocument(null, System.nanoTime())) },
                    onSignatories = { nav.replaceAll(Route.AdminSignatories) },
                    onDocuments = { nav.replaceAll(Route.AdminDocuments) },
                    onAccount = { nav.replaceAll(Route.Account) }
                )
                else -> DocumentsScreen(
                    container = container,
                    user = user,
                    onOpen = { nav.push(Route.DocumentDetail(it)) },
                    onNew = { nav.push(Route.NewDocument(null, System.nanoTime())) },
                    onAccount = { nav.replaceAll(Route.Account) }
                )
            }
        }
    }
}
