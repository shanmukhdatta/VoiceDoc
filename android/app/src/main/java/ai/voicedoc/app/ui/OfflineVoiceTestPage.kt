package ai.voicedoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.voicedoc.app.speech.DeviceOfflineTts
import ai.voicedoc.core.common.SupportedLanguage

@Composable
internal fun OfflineVoiceTestPage(isTe: Boolean) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val synthesizer = remember(context) { DeviceOfflineTts(context) { ready = it } }
    DisposableEffect(synthesizer) { onDispose { synthesizer.close() } }
    val language = if (isTe) SupportedLanguage.TELUGU else SupportedLanguage.ENGLISH
    val voices = if (ready) synthesizer.available(language) else emptyList()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (isTe) "స్థానిక వాయిస్ లభ్యత" else "Installed offline voices",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            if (isTe)
                "ఈ పరీక్ష ఫోన్‌లో ఇన్‌స్టాల్ అయిన వాయిస్‌ను మాత్రమే ఉపయోగిస్తుంది. ఇంటర్నెట్ అవసరమయ్యే వాయిస్‌లను చూపదు."
            else
                "This checks Android's installed voice engine. Voices requiring a network connection are excluded."
        )
        if (!ready) {
            Text(if (isTe) "ఆఫ్‌లైన్ TTS ఇంజిన్ ఇంకా సిద్ధంగా లేదు." else "Offline TTS engine unavailable or initializing.")
        } else if (voices.isEmpty()) {
            Text(
                if (isTe) "తెలుగు ఆఫ్‌లైన్ వాయిస్ లేదు. వాయిస్ ఫీచర్ అందుబాటులో లేదు."
                else "No installed English offline voice found. Voice output is unavailable."
            )
        } else {
            Text("${voices.size} available voice(s) for ${language.tag}")
            voices.forEach { voice ->
                Button(onClick = {
                    val phrase = if (isTe) "ఇది ఆఫ్‌లైన్ వాయిస్ పరీక్ష మాత్రమే."
                    else "This is a test of an offline voice only."
                    message = if (synthesizer.speak(phrase, voice))
                        "Speech request queued; confirm audible output in airplane mode."
                    else "This voice could not start synthesis."
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Test ${voice.name}")
                }
            }
        }
        if (message.isNotBlank()) Text(message)
        Text(
            if (isTe)
                "వాయిస్ పనిచేస్తుందో తెలుసుకోవడానికి ఎయిర్‌ప్లేన్ మోడ్‌లో పరీక్షించండి. ఇది వైద్య AI సమాధానం కాదు."
            else
                "Verify actual playback in airplane mode. This does not enable medical AI answers.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
