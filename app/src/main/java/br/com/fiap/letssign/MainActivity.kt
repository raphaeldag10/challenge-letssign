package br.com.fiap.letssign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.fiap.letssign.ui.AppRoot
import br.com.fiap.letssign.ui.theme.LetssignTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LetssignTheme {
                AppRoot(container = (application as LetsSignApp).container)
            }
        }
    }
}
