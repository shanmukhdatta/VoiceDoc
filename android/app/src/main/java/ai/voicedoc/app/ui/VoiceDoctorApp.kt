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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import ai.voicedoc.app.data.EncryptedHistoryStore
import ai.voicedoc.app.data.SavedConversationTurn

internal enum class Screen { HOME, CHAT, SOURCES, MODELS, SETTINGS, PRIVACY, HISTORY, DIAGNOSTICS, ABOUT, TTS_TEST, MIC_TEST }

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
    val encryptedStore = remember(context) { EncryptedHistoryStore(context) }
    var historyEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("encrypted_history_enabled", false)) }
    var historyError by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf<ChatMessage>().also { list ->
            if (prefs.getBoolean("encrypted_history_enabled", false)) {
                runCatching { encryptedStore.read() }
                    .onSuccess { turns -> list.addAll(turns.map { ChatMessage(it.fromUser, it.text) }) }
                    .onFailure { historyError = true }
            }
        }
    }
    fun saveHistory() {
        if (historyEnabled && !historyError) {
            val snapshot = messages.takeLast(80).map { SavedConversationTurn(it.fromUser, it.message) }
            runCatching { encryptedStore.write(snapshot) }.onFailure { historyError = true }
        }
    }
    fun eraseHistory() {
        messages.clear()
        runCatching { encryptedStore.erase() }.onFailure { historyError = true }
    }
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
        containerColor = VoiceDocColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text(if (route == Screen.HOME) "Voice Doctor AI" else route.displayName(isTe)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoiceDocColors.Canvas,
                    titleContentColor = VoiceDocColors.Ink,
                    navigationIconContentColor = VoiceDocColors.Forest
                ),
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
            NavigationBar(
                containerColor = VoiceDocColors.White,
                contentColor = VoiceDocColors.Muted,
                tonalElevation = 0.dp
            ) {
                for (tab in tabs) {
                    NavigationBarItem(
                        selected = route == tab,
                        onClick = { navigate(tab) },
                        icon = { Text(tab.iconText()) },
                        label = { Text(tab.displayName(isTe)) },
                        alwaysShowLabel = true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = VoiceDocColors.Forest,
                            selectedTextColor = VoiceDocColors.Forest,
                            indicatorColor = VoiceDocColors.Mint,
                            unselectedIconColor = VoiceDocColors.Muted,
                            unselectedTextColor = VoiceDocColors.Muted
                        )
                    )
                }
            }
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            Crossfade(targetState = route, animationSpec = tween(250), label = "screen-transition") { visibleRoute ->
                when (visibleRoute) {
                    Screen.HOME -> HomePage(isTe, onNavigate = ::navigate)
                    Screen.CHAT -> ChatPage(isTe, messages, onClear = { messages.clear() }, onMessagesChanged = { saveHistory() })
                    Screen.SOURCES -> SourcesPage(isTe)
                    Screen.MODELS -> ModelsPage(isTe)
                    Screen.SETTINGS -> SettingsPage(
                    isTe, onLanguage = ::chooseLanguage,
                    onNavigate = ::navigate,
                    historyEnabled = historyEnabled,
                    historyError = historyError,
                    onHistoryToggle = { enabled ->
                        if (enabled) {
                            historyEnabled = true
                            prefs.edit().putBoolean("encrypted_history_enabled", true).apply()
                            saveHistory()
                        } else {
                            historyEnabled = false
                            prefs.edit().putBoolean("encrypted_history_enabled", false).apply()
                            eraseHistory()
                        }
                    },
                    onReset = {
                        messages.clear()
                        prefs.edit().remove("onboarding_complete").apply()
                        accepted = false
                        navigate(Screen.HOME)
                    }
                    )
                    Screen.PRIVACY -> PrivacyPage(isTe, historyEnabled, onClear = { messages.clear(); saveHistory() })
                    Screen.HISTORY -> HistoryPage(isTe, historyEnabled, messages, onErase = { eraseHistory() })
                    Screen.DIAGNOSTICS -> DiagnosticsPage(isTe)
                    Screen.TTS_TEST -> OfflineVoiceTestPage(isTe)
                    Screen.MIC_TEST -> MicrophoneTestPage(isTe)
                    Screen.ABOUT -> AboutPage(isTe)
                }
            }
        }
    }
}

private fun Screen.iconText(): String = when (this) {
    Screen.HOME -> "⌂"
    Screen.CHAT -> "✦"
    Screen.SOURCES -> "▤"
    Screen.MODELS -> "◉"
    Screen.SETTINGS -> "⋯"
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
    Screen.TTS_TEST -> if (te) "వాయిస్ పరీక్ష" else "Voice test"
    Screen.MIC_TEST -> if (te) "మైక్ పరీక్ష" else "Mic test"
}
