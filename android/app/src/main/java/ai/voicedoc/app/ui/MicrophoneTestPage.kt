package ai.voicedoc.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ai.voicedoc.app.speech.LocalPcmRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun MicrophoneTestPage(isTe: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { LocalPcmRecorder() }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED)
    }
    var running by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf("") }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) report = if (isTe) "మైక్రోఫోన్ అనుమతి నిరాకరించబడింది." else "Microphone permission denied."
    }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            if (isTe) "స్థానిక ఆడియో క్యాప్చర్ పరీక్ష" else "Local microphone diagnostic",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            if (isTe)
                "పరీక్షలో ఒక సెకను 16 kHz మోనో ఆడియో మెమరీలో మాత్రమే చదువుతాము. ఫైల్‌గా నిల్వ చేయము. మాటల గుర్తింపు ఇంకా లేదు."
            else
                "This test captures one second of 16 kHz mono audio in memory. No audio file or transcription is created, and nothing is uploaded."
        )
        if (!granted) {
            Button(onClick = { request.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(if (isTe) "మైక్రోఫోన్ అనుమతి ఇవ్వండి" else "Grant microphone permission")
            }
        } else {
            Button(onClick = {
                running = true
                report = ""
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) { recorder.captureOneSecond() }
                    }
                    report = result.fold(
                        onSuccess = {
                            "Captured ${it.capturedSamples} samples at ${it.sampleRate} Hz. RMS level: ${"%.4f".format(it.rmsAmplitude)}"
                        },
                        onFailure = { "Audio capture failed: ${it.javaClass.simpleName}" }
                    )
                    running = false
                }
            }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "Capturing..." else if (isTe) "ఒక సెకను పరీక్ష" else "Test 1-second microphone capture")
            }
        }
        if (report.isNotEmpty()) Text(report)
        Text(
            if (isTe) "ఈ పరీక్ష Whisper ASR లేదా వైద్య AI పనిచేస్తున్నట్లు నిరూపించదు."
            else "This does NOT verify Whisper ASR, VAD quality, or medical AI inference.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
