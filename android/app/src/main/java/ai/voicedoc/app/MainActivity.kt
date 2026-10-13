package ai.voicedoc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
private fun VoiceDoctorHome() {
    var tag by rememberSaveable { mutableStateOf(SupportedLanguage.ENGLISH.tag) }
    val telugu = tag == SupportedLanguage.TELUGU.tag
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Voice Doctor AI", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (telugu) "ఆఫ్‌లైన్ ఆరోగ్య సమాచార సహాయకం" else "Offline health-information assistant",
            style = MaterialTheme.typography.titleMedium
        )
        Text(if (telugu) "భాషను ఎంచుకోండి" else "Choose language")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (telugu) {
                OutlinedButton(onClick = { tag = "en" }) { Text("English") }
                Button(onClick = { tag = "te" }) { Text("తెలుగు") }
            } else {
                Button(onClick = { tag = "en" }) { Text("English") }
                OutlinedButton(onClick = { tag = "te" }) { Text("తెలుగు") }
            }
        }
        Text(
            if (telugu) "డెవలప్‌మెంట్ బిల్డ్ — AI మోడళ్లు ఇంకా ఇన్‌స్టాల్ కాలేదు."
            else "Development build — AI models are not installed.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            if (telugu)
                "స్పీచ్ గుర్తింపు, వైద్య సమాచారం శోధన, AI సమాధానాలు మరియు వాయిస్ అవుట్‌పుట్ ఇంకా అందుబాటులో లేవు."
            else
                "Speech recognition, medical retrieval, AI answers, and speech output are not available yet."
        )
        Text(
            if (telugu)
                "ఇది వైద్యునికి ప్రత్యామ్నాయం కాదు. అత్యవసర పరిస్థితుల్లో వైద్య సహాయం పొందండి."
            else
                "This app does not replace a clinician. Seek professional help for urgent concerns."
        )
        Text("Phase 01 · Android foundation only", style = MaterialTheme.typography.labelSmall)
    }
}
