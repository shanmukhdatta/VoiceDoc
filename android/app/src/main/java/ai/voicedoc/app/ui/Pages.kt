package ai.voicedoc.app.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ai.voicedoc.core.common.OfflineSafetyFallback

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) { content() }
}

@Composable
private fun Section(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun OnboardingPage(isTe: Boolean, onLanguage: (String) -> Unit, onProceed: () -> Unit) {
    Page {
        Spacer(Modifier.height(16.dp))
        Text("Voice Doctor AI", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (isTe) "ఆఫ్‌లైన్ ఆరోగ్య సమాచార సహాయకం"
            else "An offline health-information assistant",
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
        Text(if (isTe) "స్వాగతం" else "Welcome", style = MaterialTheme.typography.headlineMedium)
        Section(
            if (isTe) "ప్రస్తుతం అందుబాటులో ఉన్నవి" else "Available now",
            if (isTe) "ఇంగ్లీష్/తెలుగు ఇంటర్‌ఫేస్, స్థానిక సెట్టింగ్స్, గోప్యత సమాచారం, టెక్స్ట్ ఇన్‌పుట్."
            else "English/Telugu interface, local settings, privacy information, and text entry."
        )
        Section(
            if (isTe) "ఇంకా అందుబాటులో లేవు" else "Not available yet",
            if (isTe) "ఆఫ్‌లైన్ స్పీచ్, ధృవీకరించిన వైద్య సమాచార శోధన, స్థానిక AI మోడల్, మరియు వాయిస్ అవుట్‌పుట్."
            else "Offline speech, verified medical retrieval, on-device language model, and voice output."
        )
        Button(onClick = { onNavigate(Screen.CHAT) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "టెక్స్ట్ చాట్ తెరువు" else "Open text chat")
        }
        OutlinedButton(onClick = { onNavigate(Screen.PRIVACY) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "గోప్యత వివరాలు" else "Review privacy")
        }
    }
}

@Composable
internal fun ChatPage(
    isTe: Boolean,
    messages: SnapshotStateList<ChatMessage>,
    onClear: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    fun submit() {
        val clean = input.trim()
        if (clean.isEmpty() || clean.length > 1000) return
        messages.add(ChatMessage(true, clean))
        messages.add(ChatMessage(false, OfflineSafetyFallback.respond(clean, isTe)))
        input = ""
    }
    Page {
        Text(if (isTe) "టెక్స్ట్ చాట్" else "Text chat", style = MaterialTheme.typography.headlineSmall)
        Section(
            if (isTe) "భద్రతా స్థితి" else "Safety status",
            if (isTe) "వైద్య జ్ఞాన ప్యాక్/AI ఇంకా అందుబాటులో లేదు. ఈ చాట్ వైద్య సమాధానాలు ఇవ్వదు."
            else "Medical knowledge/model assets are unavailable. This chat does not provide medical answers."
        )
        for (message in messages) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (message.fromUser) (if (isTe) "మీరు" else "You")
                        else (if (isTe) "సిస్టమ్" else "System"),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(message.message)
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
        Button(onClick = { submit() }, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "పంపించు" else "Send")
        }
        OutlinedButton(onClick = onClear, enabled = messages.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "ఈ సెషన్ తొలగించు" else "Clear this session")
        }
        Text(
            if (isTe) "మైక్రోఫోన్ ఇంకా అందుబాటులో లేదు." else "Microphone input is not implemented yet.",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
internal fun SourcesPage(isTe: Boolean) {
    Page {
        Section(
            if (isTe) "వైద్య సమాచార మూలాలు" else "Medical knowledge and sources",
            if (isTe) "ఇంకా అనుమతి పొందిన, వైద్య నిపుణులు సమీక్షించిన మూలాలు ఇన్‌స్టాల్ కాలేదు. కల్పిత మూలాలు చూపబడవు."
            else "No licensed, reviewed medical knowledge pack is installed. We do not invent references or cite unavailable sources."
        )
        Text(
            if (isTe) "మూలాలు జోడించినప్పుడు ప్రచురణకర్త, శీర్షిక, తేదీ, భాష, లైసెన్స్ కనిపిస్తాయి."
            else "Approved sources will display publisher, title, date, language, and license.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun ModelsPage(isTe: Boolean) {
    Page {
        Text(if (isTe) "ఆఫ్‌లైన్ మోడల్ స్థితి" else "Offline model status", style = MaterialTheme.typography.headlineSmall)
        for (name in listOf("Voice Activity Detection", "Speech recognition (en/te)", "Multilingual embedding", "Local language model", "English/Telugu TTS")) {
            Section(name, if (isTe) "ఇన్‌స్టాల్/ధృవీకరణ కాలేదు" else "Not installed or verified")
        }
        Text(
            if (isTe) "మోడల్ ఫైల్‌ను పేరు మార్చడం ద్వారా సరైన మోడల్‌గా గుర్తించము; SHA-256, లైసెన్స్, ఫార్మాట్ పరీక్షలు అవసరం."
            else "Models require SHA-256, license, format and runtime checks before loading."
        )
    }
}

@Composable
internal fun SettingsPage(
    isTe: Boolean,
    onLanguage: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
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
            if (isTe) "ఎన్‌క్రిప్టెడ్ చరిత్ర ఇంకా అందుబాటులో లేదు. చాట్ సందేశాలు ఈ సెషన్‌లో మాత్రమే ఉంటాయి."
            else "Encrypted persistent history is not implemented. Chat messages remain in this session only."
        )
        OutlinedButton(onClick = { onNavigate(Screen.HISTORY) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isTe) "చరిత్ర వివరాలు" else "History details")
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
internal fun PrivacyPage(isTe: Boolean, onClear: () -> Unit) {
    Page {
        Section(
            if (isTe) "ఈ బిల్డ్‌లో గోప్యత" else "Privacy in this build",
            if (isTe)
                "INTERNET అనుమతి లేదు. మైక్రోఫోన్ అనుమతి లేదు. చాట్ సందేశాలు శాశ్వతంగా నిల్వ చేయబడవు."
            else
                "No INTERNET permission and no microphone permission. Chat text is not persisted; local sessions are cleared when the process exits."
        )
        Section(
            if (isTe) "భవిష్యత్ ఎంపికలు" else "Future options",
            if (isTe) "ఎన్‌క్రిప్టెడ్ చరిత్ర, ఆటో తొలగింపు, మోడల్ ప్యాక్ ధృవీకరణ అభివృద్ధిలో ఉన్నాయి."
            else "Opt-in encrypted history, clear/delete controls and signed model manifests are planned, not active."
        )
        Button(onClick = onClear) { Text(if (isTe) "ఈ సెషన్ తొలగించు" else "Clear chat session now") }
    }
}

@Composable
internal fun HistoryPage(isTe: Boolean) {
    Page {
        Section(
            if (isTe) "చరిత్ర నిల్వ లేదు" else "History storage disabled",
            if (isTe) "ఎన్‌క్రిప్షన్ అమలయ్యే వరకు ఆరోగ్య సంభాషణలు శాశ్వతంగా నిల్వ కావు."
            else "Persistent health conversation storage remains disabled until consent, encryption, deletion, and tests are implemented."
        )
    }
}

@Composable
internal fun DiagnosticsPage(isTe: Boolean) {
    Page {
        Section("Device", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
        Section(
            if (isTe) "నడుస్తున్న మోడల్" else "Runtime backend",
            if (isTe) "ఏ AI మోడల్ లోడ్ కాలేదు. బెంచ్‌మార్క్ ఫలితాలు లేవు."
            else "No AI models loaded. No inference benchmarks have been executed."
        )
        Section(
            if (isTe) "అనుమతులు" else "Permissions",
            if (isTe) "ఈ ఫౌండేషన్ బిల్డ్‌లో INTERNET/RECORD_AUDIO అనుమతులు లేవు."
            else "Foundation build does not request INTERNET or RECORD_AUDIO."
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
