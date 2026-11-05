package br.com.fiap.letssign.ui.newdoc

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fiap.letssign.AppContainer
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.arrayContaining
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentSourcePickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        Intents.init()
        // Keep the system file picker outside this UI test; its launch contract is asserted below.
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
        val container = AppContainer(ApplicationProvider.getApplicationContext())
        compose.setContent {
            MaterialTheme {
                NewDocumentScreen(container, null, "source-test", onCancel = {}, onSent = {})
            }
        }
    }

    @After
    fun tearDown() = Intents.release()

    @Test
    fun selectPdfOffersPhotoAndFileBeforeOpeningAnotherApp() {
        compose.onNodeWithText("Selecionar PDF").performScrollTo().performClick()
        compose.onNodeWithText("Foto").assertIsDisplayed()
        compose.onNodeWithText("Arquivo").assertIsDisplayed()
    }

    @Test
    fun dismissingSourceChoicePreservesDocumentTitle() {
        compose.onNodeWithText("Ex.: Contrato de prestação de serviços")
            .performScrollTo().performTextInput("Contrato fotografado")
        compose.onNodeWithText("Selecionar PDF").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Contrato fotografado").assertTextContains("Contrato fotografado")
        compose.onNodeWithText("Selecionar PDF").assertIsDisplayed()
    }

    @Test
    fun fileChoiceStillOpensThePdfPickerAndCancellationPreservesDraft() {
        compose.onNodeWithText("Selecionar PDF").performScrollTo().performClick()
        compose.onNodeWithText("Arquivo").performClick()
        intended(allOf(
            hasAction(Intent.ACTION_OPEN_DOCUMENT),
            hasType("*/*"),
            hasExtra(Intent.EXTRA_MIME_TYPES, arrayContaining("application/pdf"))
        ))
        compose.onNodeWithText("Selecionar PDF").assertIsDisplayed()
    }
}
