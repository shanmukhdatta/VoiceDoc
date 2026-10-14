package ai.voicedoc.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.voicedoc.app.models.ExperimentalModelStore
import ai.voicedoc.app.models.GemmaLiteRtDiagnostic
import kotlinx.coroutines.launch

@Composable
internal fun ExperimentalModelPage(isTe: Boolean) {
    val context = LocalContext.current
    val store = remember(context) { ExperimentalModelStore(context) }
    val evaluator = remember { GemmaLiteRtDiagnostic() }
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var information by remember { mutableStateOf("") }
    // Updates the view when a model was imported or removed.
    val file = remember(revision) { store.existing() }
    val selectModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    val outcome = store.importLitertModel(uri)
                    information = "Local SHA-256: ${outcome.sha256} (${outcome.bytes} bytes). Check against the publisher's trusted hash."
                    revision++
                } catch (e: Exception) {
                    information = "Local model import failed: ${e.javaClass.simpleName} — ${e.message.orEmpty().take(130)}"
                } finally { busy = false }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (isTe) "స్థానిక AI మోడల్ పరీక్ష" else "Offline model diagnostics",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            if (isTe)
                "ఇది పరిశోధన కోసం మాత్రమే. మీరు ఎంచుకున్న ఫైల్ నిజమైన మోడల్ అని, లైసెన్స్ అనుమతి ఉందని లేదా వైద్యపరంగా సరైనదని నిర్ధారించదు."
            else
                "Developer test only. Importing a file does NOT verify publisher authenticity, legal redistribution permission, model quality or medical safety."
        )
        Text("LiteRT-LM Android CPU backend · Experimental Gemma 3 candidate")
        Text(
            if (file != null) "Candidate present: ${file.length()} bytes"
            else "No local .litertlm candidate imported"
        )
        OutlinedButton(
            enabled = !busy && file == null,
            onClick = { selectModel.launch(arrayOf("application/octet-stream", "*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isTe) "మోడల్ ఫైల్ ఎంచుకోండి" else "Import test .litertlm file")
        }
        if (file != null) {
            Button(
                enabled = !busy && store.deviceSupportsArm64(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    scope.launch {
                        information = try {
                            evaluator.runFixedBenchmark(file)
                        } catch (e: Exception) {
                            "CPU model initialization/inference failed: ${e.javaClass.simpleName} — ${e.message.orEmpty().take(180)}"
                        }
                        busy = false
                    }
                }
            ) {
                Text(if (busy) "Working…" else "Run benign fixed-prompt CPU inference test")
            }
            if (!store.deviceSupportsArm64()) {
                Text("This device is not ARM64. Use a compatible real Android phone for the native model runtime.")
            }
            OutlinedButton(enabled = !busy, onClick = {
                try {
                    store.remove()
                    revision++
                    information = "Experimental model candidate deleted."
                } catch (e: Exception) { information = "Deletion failed: ${e.javaClass.simpleName}" }
            }) { Text(if (isTe) "మోడల్ ఫైల్ తొలగించు" else "Delete experimental model") }
        }
        if (information.isNotBlank()) Text(information)
        Text(
            if (isTe)
                "ఈ పరీక్ష వైద్య చాట్‌కు అనుసంధానం కాలేదు. వాడుక కోసం వైద్య నిపుణుల సమీక్ష, ఆధారాలు, లైసెన్స్ ధృవీకరణ అవసరం."
            else
                "The model is NOT connected to medical chat. Production health answers require approved knowledge, citations and clinician-reviewed safety evaluation.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
