package ai.voicedoc.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ai.voicedoc.app.speech.OnDeviceSpeechService
import ai.voicedoc.app.speech.DeviceOfflineTts
import ai.voicedoc.core.common.SupportedLanguage
import ai.voicedoc.app.metrics.LocalDeviceMetrics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ai.voicedoc.core.common.OfflineSafetyFallback

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) { content() }
}

@Composable
private fun Section(title: String, body: String) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun OnboardingPage(isTe: Boolean, onLanguage: (String) -> Unit, onProceed: () -> Unit) {
    Page {
        Spacer(Modifier.height(10.dp))
        AssistantOrb(size = 142.dp)
        Text("Voice Doctor AI", style = MaterialTheme.typography.headlineLarge, color = VoiceDocColors.Forest)
        Text(
            if (isTe) "ఆఫ్‌లైన్ ఆరోగ్య సమాచార సహాయకం"
            else "Offline health-information prototype",
            style = MaterialTheme.typography.titleMedium
        )
        Text(if (isTe) "భాషను ఎంచుకోండి" else "Choose your language")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isTe) {
                OutlinedButton(onClick = { onLanguage("en") }) { Text("English") }
                Button(onClick = { onLanguage("te") }) { Text("తెలుగు") }
            } else {
                Button(onClick = { onLanguage("en") }) { Text("English") }
                OutlinedButton(onClick = { onLanguage("te") }) { Text("తెలుగు") }
            }
        }
        Section(
            if (isTe) "ముఖ్యమైన సమాచారం" else "Important notice",
            if (isTe)
                "ఈ అప్లికేషన్ ఆరోగ్య సమాచారం కోసం మాత్రమే. వైద్యునికి ప్రత్యామ్నాయం కాదు. ప్రస్తుతం వైద్య AI మోడళ్లు అందుబాటులో లేవు."
            else
                "General health information only—not a doctor, diagnosis, treatment, or emergency service. Medical AI models are not installed in this build."
        )
        Button(onClick = onProceed, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "అర్థమైంది — కొనసాగించు" else "I understand — Continue")
        }
    }
}

@Composable
internal fun HomePage(isTe: Boolean, onNavigate: (Screen) -> Unit) {
    Page {
        SoftCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFFF0FAF4), Color.White)))
                    .padding(start = 20.dp, top = 24.dp, bottom = 22.dp, end = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isTe) "మీ ఆరోగ్య సహాయకుడు" else "YOUR PRIVATE HEALTH COMPANION", style = MaterialTheme.typography.labelMedium, color = VoiceDocColors.Green, fontWeight = FontWeight.Bold)
                    Text(if (isTe) "నేను ఎలా\nసహాయపడగలను?" else "How can I\nhelp you today?", style = MaterialTheme.typography.headlineMedium, color = VoiceDocColors.Ink, fontWeight = FontWeight.SemiBold)
                    Text(if (isTe) "మీ పరికరంలోనే. మీ నియంత్రణలో." else "On your device. In your control.", style = MaterialTheme.typography.bodySmall, color = VoiceDocColors.Muted)
                }
                AssistantOrb(size = 112.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(if (isTe) "స్థానిక గోప్యత" else "PRIVATE BY DESIGN")
            StatusPill(if (isTe) "EN · TE" else "ENGLISH · TELUGU")
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (isTe) "ఈరోజు మీకు ఏమి కావాలి?" else "What would you like help with?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(onClick = { onNavigate(Screen.CHAT) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Text(if (isTe) "✦   ఆరోగ్య ప్రశ్న అడగండి" else "✦   Ask a health question", modifier = Modifier.padding(vertical = 5.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard("▤", if (isTe) "మూలాలు" else "Sources", Modifier.weight(1f)) { onNavigate(Screen.SOURCES) }
                    QuickActionCard("◉", if (isTe) "మోడల్స్" else "Models", Modifier.weight(1f)) { onNavigate(Screen.MODELS) }
                }
            }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Text("i", color = VoiceDocColors.Forest, fontWeight = FontWeight.Bold, modifier = Modifier.size(28.dp).background(VoiceDocColors.Mint, RoundedCornerShape(10.dp)).padding(start = 10.dp, top = 3.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (isTe) "పరిశోధనా ప్రోటోటైప్" else "Research prototype", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (isTe) "వైద్య AI మోడల్ లేదా సమీక్షించిన జ్ఞాన ప్యాక్ ఈ బిల్డ్‌లో లేదు. చాట్ వైద్య సమాధానాలు ఇవ్వదు." else "Medical model and reviewed knowledge are not installed in this build. Chat does not provide medical answers.", style = MaterialTheme.typography.bodySmall, color = VoiceDocColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = VoiceDocColors.Forest, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(VoiceDocColors.Mint, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable
private fun QuickActionCard(icon: String, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = VoiceDocColors.PaleMint),
        border = androidx.compose.foundation.BorderStroke(1.dp, VoiceDocColors.Line)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon, color = VoiceDocColors.Forest, style = MaterialTheme.typography.titleLarge)
            Text(title, color = VoiceDocColors.Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun ChatPage(
    isTe: Boolean,
    messages: SnapshotStateList<ChatMessage>,
    onClear: () -> Unit,
    onMessagesChanged: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    var speechStatus by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val localSpeech = remember(context) {
        OnDeviceSpeechService(
            context = context,
            onText = { transcript -> input = transcript.take(1000) },
            onStatus = { status -> speechStatus = status }
        )
    }
    DisposableEffect(localSpeech) { onDispose { localSpeech.stop() } }
    var localVoiceReady by remember { mutableStateOf(false) }
    val localVoice = remember(context) { DeviceOfflineTts(context) { localVoiceReady = it } }
    DisposableEffect(localVoice) { onDispose { localVoice.close() } }
    var speechPlaybackStatus by remember { mutableStateOf("") }
    val voiceChoice = if (localVoiceReady)
        localVoice.available(if (isTe) SupportedLanguage.TELUGU else SupportedLanguage.ENGLISH).firstOrNull()
    else null
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            localSpeech.start(if (isTe) SupportedLanguage.TELUGU else SupportedLanguage.ENGLISH)
        } else {
            speechStatus = "Microphone permission denied. Use typed input instead."
        }
    }
    fun submit() {
        val clean = input.trim()
        if (clean.isEmpty() || clean.length > 1000) return
        messages.add(ChatMessage(true, clean))
        messages.add(ChatMessage(false, OfflineSafetyFallback.respond(clean, isTe)))
        onMessagesChanged()
        input = ""
    }
    Page {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistantOrb(size = 58.dp, listening = speechStatus.contains("Listening", ignoreCase = true))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (isTe) "మీ సహాయకుడు" else "Your assistant", style = MaterialTheme.typography.headlineSmall, color = VoiceDocColors.Forest)
                Text(if (isTe) "స్థానిక మరియు ప్రైవేట్" else "Private · on-device", style = MaterialTheme.typography.bodySmall, color = VoiceDocColors.Muted)
            }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (isTe) "ప్రోటోటైప్ స్థితి" else "Prototype status", style = MaterialTheme.typography.labelLarge, color = VoiceDocColors.Forest, fontWeight = FontWeight.SemiBold)
                Text(if (isTe) "వైద్య మోడల్ లేదా సమీక్షించిన జ్ఞాన ప్యాక్ లేదు; చాట్ వైద్య సమాధానాలు ఇవ్వదు." else "No medical model or reviewed knowledge pack is installed. Chat does not provide medical answers.", style = MaterialTheme.typography.bodySmall, color = VoiceDocColors.Muted)
            }
        }
        for (message in messages) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 320.dp),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = if (message.fromUser) 6.dp else 20.dp, bottomStart = if (message.fromUser) 20.dp else 6.dp),
                    colors = CardDefaults.cardColors(containerColor = if (message.fromUser) VoiceDocColors.Mint else VoiceDocColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (message.fromUser) (if (isTe) "మీరు" else "You")
                            else (if (isTe) "సిస్టమ్" else "Assistant"),
                            style = MaterialTheme.typography.labelMedium,
                            color = VoiceDocColors.Forest,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(message.message, color = VoiceDocColors.Ink)
                    }
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 1000) input = it },
            label = { Text(if (isTe) "మీ ప్రశ్న" else "Your question") },
            supportingText = { Text("${input.length}/1000") },
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() })
        )
        Button(onClick = { submit() }, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(if (isTe) "పంపించు" else "Send")
        }
        OutlinedButton(onClick = { onClear(); onMessagesChanged() }, enabled = messages.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "ఈ సెషన్ తొలగించు" else "Clear this session")
        }
        if (localSpeech.available()) {
            Button(
                onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    localSpeech.start(if (isTe) SupportedLanguage.TELUGU else SupportedLanguage.ENGLISH)
                } else {
                    permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoiceDocColors.Forest)
            ) {
                Text(if (isTe) "◉   ఆఫ్‌లైన్ మాట్లాడే ఇన్‌పుట్" else "◉   Speak to your assistant", modifier = Modifier.padding(vertical = 4.dp))
            }
            Text(if (isTe)
                "మీరు పంపించే ముందే గుర్తించిన వాక్యాన్ని సమీక్షించండి."
            else "Review the transcription before tapping Send. This is Android's device-only recognizer, not Whisper.")
        } else {
            Text(if (isTe)
                "ఈ ఫోన్‌లో ఆఫ్‌లైన్ స్పీచ్ గుర్తింపు అందుబాటులో లేదు."
            else "No device-only speech recognizer is installed. Use text input.")
        }
        if (speechStatus.isNotBlank()) {
            SoftCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistantOrb(size = 40.dp, listening = speechStatus.contains("Listening", ignoreCase = true))
                    Text(speechStatus, style = MaterialTheme.typography.labelMedium, color = VoiceDocColors.Forest)
                }
            }
        }
        val lastSystem = messages.lastOrNull { !it.fromUser }?.message
        if (lastSystem != null && voiceChoice != null) {
            OutlinedButton(onClick = {
                speechPlaybackStatus = if (localVoice.speak(lastSystem.take(300), voiceChoice))
                    "Offline speech request queued; verify actual playback in airplane mode."
                else "Installed offline voice failed to start."
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isTe) "భద్రతా సందేశాన్ని వినండి" else "Speak latest system notice offline")
            }
        } else if (lastSystem != null) {
            Text(if (isTe)
                "ఈ భాషలో స్థానిక ఆఫ్‌లైన్ వాయిస్ ఇంకా లేదు."
            else "No installed offline voice for this language.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (speechPlaybackStatus.isNotBlank()) Text(speechPlaybackStatus)
    }
}

@Composable
internal fun SourcesPage(isTe: Boolean) = LocalSourcesPage(isTe)

@Composable
internal fun ModelsPage(isTe: Boolean) = ExperimentalModelPage(isTe)

@Composable
internal fun SettingsPage(
    isTe: Boolean,
    onLanguage: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
    historyEnabled: Boolean,
    historyError: Boolean,
    onHistoryToggle: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Page {
        Text(if (isTe) "భాష" else "Language", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onLanguage("en") }) { Text("English") }
            OutlinedButton(onClick = { onLanguage("te") }) { Text("తెలుగు") }
        }
        HorizontalDivider()
        Section(
            if (isTe) "చరిత్ర" else "Conversation history",
            if (historyEnabled)
                (if (isTe) "మీ అనుమతితో మాత్రమే ఎన్‌క్రిప్టెడ్ చరిత్ర నిల్వ జరుగుతుంది." else "Opt-in encrypted local history is enabled.")
            else
                (if (isTe) "చరిత్ర ఆఫ్ ఉంది. ఈ సెషన్ చాట్ మాత్రమే అందుబాటులో ఉంది." else "History is off; chat stays in the current session only.")
        )
        if (historyError) Text(if (isTe) "చరిత్రను సురక్షితంగా చదవడం లేదా నిల్వ చేయడం విఫలమైంది." else "Encrypted history could not be safely read or saved.")
        OutlinedButton(
            onClick = { onHistoryToggle(!historyEnabled) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (historyEnabled) (if (isTe) "చరిత్ర ఆపివేసి తొలగించు" else "Disable and delete stored history")
            else (if (isTe) "ఎన్‌క్రిప్టెడ్ చరిత్రకు అనుమతించు" else "Enable encrypted history (opt in)"))
        }
        OutlinedButton(onClick = { onNavigate(Screen.HISTORY) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "చరిత్ర వివరాలు" else "History details")
        }
        OutlinedButton(onClick = { onNavigate(Screen.MIC_TEST) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "మైక్రోఫోన్ పరీక్ష" else "Test microphone capture")
        }
        OutlinedButton(onClick = { onNavigate(Screen.TTS_TEST) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "ఆఫ్‌లైన్ వాయిస్ పరీక్ష" else "Test offline voices")
        }
        OutlinedButton(onClick = { onNavigate(Screen.DIAGNOSTICS) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "డయాగ్నోస్టిక్స్" else "Diagnostics")
        }
        OutlinedButton(onClick = { onNavigate(Screen.PRIVACY) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "గోప్యత" else "Privacy")
        }
        OutlinedButton(onClick = { onNavigate(Screen.ABOUT) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "ఈ యాప్ గురించి" else "About")
        }
        TextButton(onClick = onReset) {
            Text(if (isTe) "ప్రారంభ స్క్రీన్ మళ్లీ చూపు" else "Show onboarding again")
        }
    }
}

@Composable
internal fun PrivacyPage(isTe: Boolean, historyEnabled: Boolean, onClear: () -> Unit) {
    Page {
        Section(
            if (isTe) "ఈ బిల్డ్‌లో గోప్యత" else "Privacy in this build",
            if (isTe)
                "ఇంటర్నెట్ అనుమతి లేదు. మైక్రోఫోన్‌కు మీ అనుమతి అవసరం. చరిత్రను అనుమతిస్తే Android Keystore ఆధారిత ఎన్‌క్రిప్షన్ ఉపయోగిస్తాము."
            else
                "No INTERNET permission. Microphone diagnostic requires your explicit permission. Opt-in history uses Android Keystore-backed AES-GCM encryption and no-backup storage."
        )
        Section(
            if (isTe) "ప్రస్తుతం చరిత్ర స్థితి" else "History consent status",
            if (historyEnabled)
                (if (isTe) "ఎన్‌క్రిప్టెడ్ చరిత్ర ఆన్ ఉంది." else "Encrypted local history is ON by explicit consent.")
            else
                (if (isTe) "శాశ్వత చాట్ నిల్వ ఆఫ్ ఉంది." else "Persistent conversation storage is OFF.")
        )
        Button(onClick = onClear) {
            Text(if (isTe) "ఈ సెషన్ తొలగించు" else "Clear chat session")
        }
    }
}

@Composable
internal fun HistoryPage(
    isTe: Boolean,
    enabled: Boolean,
    messages: SnapshotStateList<ChatMessage>,
    onErase: () -> Unit
) {
    Page {
        Section(
            if (isTe) "స్థానిక చరిత్ర" else "Local conversation history",
            if (enabled)
                (if (isTe) "మీ అనుమతితో ఎన్‌క్రిప్ట్ చేసి స్థానికంగా నిల్వ చేయబడుతుంది."
                else "Enabled with your consent. Stored encrypted on this device without cloud backup.")
            else
                (if (isTe) "చరిత్ర నిల్వ ఆఫ్ ఉంది. సెట్టింగ్స్‌లో అనుమతించవచ్చు."
                else "History is disabled. You can explicitly enable it in Settings.")
        )
        if (enabled) {
            for (message in messages) {
                Section(
                    if (message.fromUser) (if (isTe) "మీరు" else "You") else "System",
                    message.message
                )
            }
            Button(onClick = onErase, enabled = messages.isNotEmpty()) {
                Text(if (isTe) "సేవ్ చేసిన చరిత్ర తొలగించు" else "Erase all saved history")
            }
        }
    }
}

@Composable
internal fun DiagnosticsPage(isTe: Boolean) {
    val context = LocalContext.current
    var sample by remember { mutableStateOf<ai.voicedoc.app.metrics.DeviceMemorySnapshot?>(null) }
    Page {
        Section("Device", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
        Button(onClick = { sample = LocalDeviceMetrics.snapshot(context) }) {
            Text(if (isTe) "రామ్ కొలత తీసుకోండి" else "Measure local memory now")
        }
        if (sample != null) {
            val m = sample!!
            Section("Live device memory",
                "Total RAM: ${m.totalRamMb} MB · Available RAM: ${m.availableRamMb} MB · Process PSS: ${m.processPssMb} MB · Native heap: ${m.nativeHeapMb} MB · Low-memory: ${m.lowMemory}")
        }
        Section(
            if (isTe) "నడుస్తున్న మోడల్" else "Runtime backend",
            if (isTe) "ఏ AI మోడల్ లోడ్ కాలేదు. బెంచ్‌మార్క్ ఫలితాలు లేవు."
            else "No AI models loaded. No inference benchmarks have been executed."
        )
        Section(
            if (isTe) "అనుమతులు" else "Permissions",
            if (isTe) "ఇంటర్నెట్ అనుమతి లేదు. మైక్రోఫోన్ అనుమతి అడిగిన తర్వాత మాత్రమే తీసుకుంటాము."
            else "This build does not request INTERNET. RECORD_AUDIO is requested only for local microphone diagnostics."
        )
    }
}

@Composable
internal fun AboutPage(isTe: Boolean) {
    Page {
        Section(
            "Voice Doctor AI",
            if (isTe)
                "పరిశోధన/అభివృద్ధిలో ఉన్న స్థానిక ఆరోగ్య సమాచార యాప్. వైద్యునికి ప్రత్యామ్నాయం కాదు."
            else
                "Offline-first health-information research project. Not a medical professional or emergency service."
        )
        Section("Version", "0.2.0-dev · English and Telugu only")
    }
}
