package ai.voicedoc.app

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launch after clearing only the non-sensitive onboarding preference fixture. */
@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun onboardingChatAndSafeUnavailableResponse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("voicedoc_public_settings", Context.MODE_PRIVATE)
            .edit()
            .remove("onboarding_complete")
            .putString("language", "en")
            .putBoolean("encrypted_history_enabled", false)
            .commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("I understand — Continue").performClick()
            compose.onNodeWithText("Ask a health question", substring = true).performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Hello app")
            compose.onNodeWithText("Send").performClick()
            compose.onNodeWithText("Assistant").assertExists()
            compose.onNodeWithText(
                "This development build does not have a verified medical knowledge pack",
                substring = true
            ).assertExists()
        }
    }
}
