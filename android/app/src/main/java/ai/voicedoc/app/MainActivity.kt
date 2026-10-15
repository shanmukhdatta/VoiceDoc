package ai.voicedoc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.voicedoc.core.common.SupportedLanguage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { VoiceDoctorHome() }
            }
        }
    }
}

/**
 * Readiness-first UI: accepts text locally, but intentionally never fabricates an
 * AI answer while reviewed medical knowledge, speech and model packs are absent.
 */
@Composable
private fun VoiceDoctorHome() {
    var tag by rememberSaveable { mutableStateOf(SupportedLanguage.ENGLISH.tag) }
    var question by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(false) }
    val telugu = tag == SupportedLanguage.TELUGU.tag

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Voice Doctor AI", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (telugu) "ఆఫ్‌లైన్ ఆరోగ్య సమాచార సహాయకం"
            else "Offline health-information assistant",
            style = MaterialTheme.typography.titleMedium
        )
        Text(if (telugu) "భాషను ఎంచుకోండి" else "Choose language")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (telugu) {
                OutlinedButton(onClick = { tag = "en"; checked = false }) { Text("English") }
                Button(onClick = { tag = "te"; checked = false }) { Text("తెలుగు") }
            } else {
                Button(onClick = { tag = "en"; checked = false }) { Text("English") }
                OutlinedButton(onClick = { tag = "te"; checked = false }) { Text("తెలుగు") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (telugu) "ఆఫ్‌లైన్ మోడళ్లు ఇంకా సిద్ధంగా లేవు"
                    else "Offline intelligence not yet available",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (telugu)
                        "ధృవీకరించిన వైద్య సమాచార ప్యాక్, స్పీచ్ మోడల్, AI మోడల్ ఇన్‌స్టాల్ కాలేదు."
                    else
                        "A reviewed medical knowledge pack, speech model, and language model have not been installed."
                )
            }
        }
        OutlinedTextField(
            value = question,
            onValueChange = { question = it.take(2000); checked = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (telugu) "మీ ప్రశ్న (పరికరంలో మాత్రమే)" else "Your question (on-device only)") },
            minLines = 3,
            maxLines = 7
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { checked = true }, enabled = question.isNotBlank()) {
                Text(if (telugu) "స్థితి చూడండి" else "Check readiness")
            }
            OutlinedButton(onClick = { question = ""; checked = false }) {
                Text(if (telugu) "తొలగించు" else "Clear")
            }
        }
        if (checked) {
            Text(
                if (telugu)
                    "మీ ప్రశ్న పంపబడలేదు. ఇంకా ధృవీకరించిన ఆఫ్‌లైన్ సమాధానాలు అందుబాటులో లేవు."
                else
                    "Your question was not sent anywhere. Verified offline answers are not available yet."
            )
        }
        Text(
            if (telugu)
                "ఇది వైద్యునికి ప్రత్యామ్నాయం కాదు. అత్యవసర పరిస్థితుల్లో తక్షణ వైద్య సహాయం పొందండి."
            else
                "Not a substitute for professional medical care. In an emergency, seek immediate in-person help.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Development build · Model inference, ASR and TTS not connected",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
