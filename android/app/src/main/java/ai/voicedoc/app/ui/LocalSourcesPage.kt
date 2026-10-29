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
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import ai.voicedoc.app.data.KnowledgeSearchResult
import ai.voicedoc.app.data.KnowledgeSourceSummary
import ai.voicedoc.app.data.LocalKnowledgePack
import kotlinx.coroutines.launch

@Composable
internal fun LocalSourcesPage(isTe: Boolean) {
    val context = LocalContext.current
    val store = remember(context) { LocalKnowledgePack(context) }
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    val exists = remember(revision) { store.available() }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var sources by remember { mutableStateOf<List<KnowledgeSourceSummary>>(emptyList()) }
    var results by remember { mutableStateOf<List<KnowledgeSearchResult>>(emptyList()) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    val imported = store.installFromDocument(uri)
                    status = "Developer pack: ${imported.sourceCount} sources, ${imported.chunkCount} chunks; copied SHA-256 ${imported.sha256}. Publisher rights NOT independently checked."
                    sources = store.listSources()
                    revision++
                } catch (error: Exception) {
                    status = "Import failed: ${error.javaClass.simpleName} — ${error.message.orEmpty().take(100)}"
                } finally { busy = false }
            }
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            if (isTe) "ఆఫ్‌లైన్ సమాచార మూలాలు" else "Offline knowledge sources",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            if (isTe)
                "ఇది డెవలపర్ సమాచార ప్యాక్ పరీక్ష. లైసెన్స్, వైద్య సమీక్ష, అనువాదం సరిగ్గా ఉన్నాయని ఈ యాప్ హామీ ఇవ్వదు."
            else
                "Developer corpus viewer only. An imported SQLite file is not proof of licensing, clinician review, translation quality or medical reliability."
        )
        Text(if (exists) "Local candidate pack present" else "No source pack installed")
        OutlinedButton(
            enabled = !exists && !busy,
            onClick = { selector.launch(arrayOf("application/octet-stream", "*/*")) }
        ) { Text(if (isTe) "SQLite ప్యాక్ దిగుమతి" else "Import reviewed-source candidate SQLite") }
        if (exists) {
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        sources = store.listSources()
                        status = "Found ${sources.size} source records (up to 100)."
                    } catch (error: Exception) { status = "Read failed: ${error.javaClass.simpleName}" }
                    finally { busy = false }
                }
            }) { Text(if (isTe) "మూలాలను చూపు" else "List source provenance") }
            OutlinedTextField(
                value = search, onValueChange = { search = it.take(80) },
                label = { Text(if (isTe) "శోధన పదాలు" else "Local text search") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Button(enabled = !busy && search.trim().length >= 2, onClick = {
                busy = true
                scope.launch {
                    try {
                        results = store.search(search, if (isTe) "te" else "en")
                        status = "Found ${results.size} matching passages in selected language."
                    } catch (error: Exception) { status = "Search failed: ${error.javaClass.simpleName}" }
                    finally { busy = false }
                }
            }) { Text(if (isTe) "స్థానికంగా వెతుకు" else "Search local passages") }
            OutlinedButton(enabled = !busy, onClick = {
                try {
                    store.remove()
                    sources = emptyList()
                    results = emptyList()
                    revision++
                    status = "Candidate knowledge file deleted."
                } catch (error: Exception) { status = "Delete failed: ${error.javaClass.simpleName}" }
            }) { Text(if (isTe) "ప్యాక్ తొలగించు" else "Remove developer pack") }
        }
        if (status.isNotBlank()) Text(status)
        for (source in sources) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${source.id} · ${source.language}")
                    Text("Publisher: ${source.publisher} · License: ${source.license}")
                    Text("Claimed reviewer: ${source.reviewedBy}")
                }
            }
        }
        for (hit in results) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(hit.title, style = MaterialTheme.typography.titleMedium)
                    Text("${hit.sourceId} / ${hit.chunkId}", style = MaterialTheme.typography.labelSmall)
                    Text(hit.content)
                }
            }
        }
        Text(
            if (isTe) "శోధన ఫలితం వైద్య సలహా కాదు. ధృవీకరించని సమాచారంతో వైద్య నిర్ణయాలు తీసుకోకండి."
            else "Passages are not medical advice. Do not make medical decisions from unverified imported material.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
