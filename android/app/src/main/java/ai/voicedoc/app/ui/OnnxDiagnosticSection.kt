package ai.voicedoc.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import ai.voicedoc.app.models.E5OnnxTokenEncoder
import ai.voicedoc.app.models.OnnxArtifactStore
import ai.voicedoc.app.models.SileroOnnxCpu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs

private data class E5Golden(
    val ids: LongArray,
    val mask: LongArray,
    val normalizedVector: FloatArray
)

private fun parseGolden(json: String): E5Golden {
    require(json.length < 2_000_000)
    val root = JSONObject(json)
    fun longs(name: String): LongArray {
        val batch = root.getJSONArray(name).getJSONArray(0)
        return LongArray(batch.length()) { batch.getLong(it) }
    }
    val vec = root.getJSONArray("normalized_embeddings").getJSONArray(0)
    require(vec.length() == 384)
    return E5Golden(
        longs("input_ids"), longs("attention_mask"),
        FloatArray(vec.length()) { vec.getDouble(it).toFloat() }
    )
}

/** Separate diagnostic page section: no model automatically loads at startup. */
@Composable
internal fun OnnxDiagnosticSection(isTe: Boolean) {
    val context = LocalContext.current
    val vad = remember(context) { OnnxArtifactStore(context, "vad") }
    val e5 = remember(context) { OnnxArtifactStore(context, "e5") }
    val scope = rememberCoroutineScope()
    var changed by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf("") }
    var golden by remember { mutableStateOf<E5Golden?>(null) }
    val vadFile = remember(changed) { vad.existing() }
    val e5File = remember(changed) { e5.existing() }

    fun importFile(uri: Uri, store: OnnxArtifactStore) {
        if (busy) return
        busy = true
        scope.launch {
            report = try { store.import(uri) }
            catch (e: Exception) { "ONNX import failed: ${e.javaClass.simpleName} — ${e.message.orEmpty().take(140)}" }
            finally { busy = false; changed++ }
        }
    }
    val vadPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFile(uri, vad)
    }
    val e5Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFile(uri, e5)
    }
    val goldenPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                report = try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                            reader.readText()
                        } ?: error("Could not open golden JSON")
                    }
                    golden = parseGolden(text)
                    "Imported golden token IDs and 384-d reference vector (first reference sample)."
                } catch (e: Exception) { "Golden reference invalid: ${e.javaClass.simpleName}" }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text(if (isTe) "ONNX మోడల్ పరీక్ష" else "ONNX Runtime diagnostics",
            style = MaterialTheme.typography.titleLarge)
        Text(
            if (isTe)
                "Silero VAD మరియు E5 ONNX ప్రయోగాత్మక మోడళ్లు మాత్రమే. పంపిణీ అనుమతి, డేటా మూలం మరియు వైద్య ఖచ్చితత్వం వేరుగా ధృవీకరించాలి."
            else
                "Optional developer model tests, not production inference. Verify publisher hashes, exact formats, tokenizer revisions and licensing separately."
        )
        Text(if (vadFile == null) "Silero ONNX: not imported" else "Silero ONNX: ${vadFile.length()} bytes")
        OutlinedButton(enabled = vadFile == null && !busy, onClick = {
            vadPicker.launch(arrayOf("application/octet-stream", "*/*"))
        }) { Text("Import Silero ONNX") }
        Button(enabled = vadFile != null && !busy, onClick = {
            val file = vadFile ?: return@Button
            busy = true
            scope.launch {
                report = try {
                    withContext(Dispatchers.IO) {
                        SileroOnnxCpu(file).use { engine ->
                            "Silero ONNX CPU executed on zero-amplitude 512-sample frame. Probability: ${engine.probability(ShortArray(512))}. This is NOT validation of speech accuracy."
                        }
                    }
                } catch (e: Exception) {
                    "VAD ONNX inference failed: ${e.javaClass.simpleName} — ${e.message.orEmpty().take(120)}"
                } finally { busy = false }
            }
        }) { Text("Test VAD zero-input inference") }
        if (vadFile != null) {
            OutlinedButton(enabled = !busy, onClick = {
                try { vad.remove(); changed++; report = "Removed Silero test artifact." }
                catch (e: Exception) { report = "Deletion failed: ${e.javaClass.simpleName}" }
            }) { Text("Remove Silero test model") }
        }
        HorizontalDivider()
        Text(if (e5File == null) "E5-small ONNX: not imported" else "E5 ONNX: ${e5File.length()} bytes")
        OutlinedButton(enabled = e5File == null && !busy, onClick = {
            e5Picker.launch(arrayOf("application/octet-stream", "*/*"))
        }) { Text("Import E5-small encoder.onnx") }
        OutlinedButton(enabled = !busy, onClick = {
            goldenPicker.launch(arrayOf("application/json", "*/*"))
        }) { Text("Import E5 golden-reference.json") }
        Button(enabled = e5File != null && golden != null && !busy, onClick = {
            val file = e5File ?: return@Button
            val expected = golden ?: return@Button
            busy = true
            scope.launch {
                report = try {
                    withContext(Dispatchers.IO) {
                        E5OnnxTokenEncoder(file).use { encoder ->
                            val embedding = encoder.encodeTokenIds(expected.ids, expected.mask)
                            val maxDifference = embedding.indices.maxOf {
                                abs(embedding[it] - expected.normalizedVector[it])
                            }
                            "E5 ONNX CPU tensor inference succeeded. Max absolute error vs Python golden reference: $maxDifference. Model/tokenizer provenance must still be verified."
                        }
                    }
                } catch (e: Exception) {
                    "E5 ONNX inference failed: ${e.javaClass.simpleName} — ${e.message.orEmpty().take(120)}"
                } finally { busy = false }
            }
        }) { Text("Test E5 ONNX vs golden reference") }
        if (e5File != null) {
            OutlinedButton(enabled = !busy, onClick = {
                try { e5.remove(); changed++; report = "Removed E5 test artifact." }
                catch (e: Exception) { report = "Deletion failed: ${e.javaClass.simpleName}" }
            }) { Text("Remove E5 test model") }
        }
        if (report.isNotBlank()) {
            Text(report, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
