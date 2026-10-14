package ai.voicedoc.app.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.voicedoc.core.common.SupportedLanguage

internal enum class Screen { HOME, CHAT, SOURCES, MODELS, SETTINGS, PRIVACY, HISTORY, DIAGNOSTICS, ABOUT }

internal data class ChatMessage(val fromUser: Boolean, val message: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceDoctorApp() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("voicedoc_public_settings", Context.MODE_PRIVATE) }
    var languageTag by rememberSaveable { mutableStateOf(prefs.getString("language", "en") ?: "en") }
    var accepted by rememberSaveable { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }
    var routeName by rememberSaveable { mutableStateOf(Screen.HOME.name) }
    val route = Screen.entries.firstOrNull { it.name == routeName } ?: Screen.HOME
    val isTe = languageTag == SupportedLanguage.TELUGU.tag
    val messages = remember { mutableStateListOf<ChatMessage>() }
    fun chooseLanguage(code: String) {
        if (SupportedLanguage.fromTag(code) == null) return
        languageTag = code
        prefs.edit().putString("language", code).apply()
    }
    fun navigate(destination: Screen) { routeName = destination.name }

    BackHandler(enabled = accepted && route != Screen.HOME) { navigate(Screen.HOME) }

    if (!accepted) {
        OnboardingPage(
            isTe = isTe,
            onLanguage = ::chooseLanguage,
            onProceed = {
                prefs.edit().putBoolean("onboarding_complete", true).apply()
                accepted = true
            }
        )
        return
    }

    val tabs = listOf(Screen.HOME, Screen.CHAT, Screen.SOURCES, Screen.MODELS, Screen.SETTINGS)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (route == Screen.HOME) "Voice Doctor AI" else route.displayName(isTe)) },
                navigationIcon = {
                    if (route !in tabs || route != Screen.HOME) {
                        TextButton(onClick = { navigate(Screen.HOME) }) {
                            Text(if (isTe) "వెనుక" else "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                for (tab in tabs) {
                    NavigationBarItem(
                        selected = route == tab,
                        onClick = { navigate(tab) },
                        icon = { Text(tab.iconText()) },
                        label = { Text(tab.displayName(isTe)) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            when (route) {
                Screen.HOME -> HomePage(isTe, onNavigate = ::navigate)
                Screen.CHAT -> ChatPage(isTe, messages, onClear = { messages.clear() })
                Screen.SOURCES -> SourcesPage(isTe)
                Screen.MODELS -> ModelsPage(isTe)
                Screen.SETTINGS -> SettingsPage(
                    isTe, onLanguage = ::chooseLanguage,
                    onNavigate = ::navigate,
                    onReset = {
                        messages.clear()
                        prefs.edit().remove("onboarding_complete").apply()
                        accepted = false
                        navigate(Screen.HOME)
                    }
                )
                Screen.PRIVACY -> PrivacyPage(isTe, onClear = { messages.clear() })
                Screen.HISTORY -> HistoryPage(isTe)
                Screen.DIAGNOSTICS -> DiagnosticsPage(isTe)
                Screen.ABOUT -> AboutPage(isTe)
            }
        }
    }
}

private fun Screen.iconText(): String = when (this) {
    Screen.HOME -> "⌂"
    Screen.CHAT -> "✎"
    Screen.SOURCES -> "≡"
    Screen.MODELS -> "◇"
    Screen.SETTINGS -> "⚙"
    else -> "•"
}

private fun Screen.displayName(te: Boolean): String = when (this) {
    Screen.HOME -> if (te) "హోమ్" else "Home"
    Screen.CHAT -> if (te) "చాట్" else "Chat"
    Screen.SOURCES -> if (te) "మూలాలు" else "Sources"
    Screen.MODELS -> if (te) "మోడల్స్" else "Models"
    Screen.SETTINGS -> if (te) "సెట్టింగ్స్" else "Settings"
    Screen.PRIVACY -> if (te) "గోప్యత" else "Privacy"
    Screen.HISTORY -> if (te) "చరిత్ర" else "History"
    Screen.DIAGNOSTICS -> if (te) "పరీక్షలు" else "Diagnostics"
    Screen.ABOUT -> if (te) "గురించి" else "About"
}
