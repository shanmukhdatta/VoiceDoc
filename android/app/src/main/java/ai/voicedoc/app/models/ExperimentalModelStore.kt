package ai.voicedoc.app.models

import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Development-only model import. A measured SHA from imported bytes proves file integrity
 * during copying, NOT model authenticity, redistribution rights, tokenizer or safety.
 * Never route health answers through an experimental pack by default.
 */
data class ImportedModel(val name: String, val bytes: Long, val sha256: String, val file: File)

class ExperimentalModelStore(private val context: Context) {
    private val folder get() = File(context.noBackupFilesDir, "experimental-models")
    private val model get() = File(folder, "candidate.litertlm")
    private val limit = 1_500_000_000L

    fun deviceSupportsArm64(): Boolean = Build.SUPPORTED_ABIS.contains("arm64-v8a")
    fun existing(): File? = model.takeIf { it.isFile && it.length() > 0 }

    suspend fun importLitertModel(uri: Uri): ImportedModel = withContext(Dispatchers.IO) {
        folder.mkdirs()
        val temporary = File(folder, "incoming.tmp")
        if (existing() != null) throw IOException("Remove the previous candidate before importing another.")
        val sha = MessageDigest.getInstance("SHA-256")
        var size = 0L
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open selected document")
            stream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val bytes = ByteArray(1024 * 1024)
                    while (true) {
                        val count = input.read(bytes)
                        if (count < 0) break
                        size += count
                        if (size > limit) throw IOException("Candidate exceeds 1.5 GB import limit")
                        sha.update(bytes, 0, count)
                        output.write(bytes, 0, count)
                    }
                }
            }
            if (size < 1024) throw IOException("Model candidate is unexpectedly small")
            if (!temporary.renameTo(model)) throw IOException("Unable to finish local import")
            ImportedModel(model.name, size, sha.digest().joinToString("") { "%02x".format(it) }, model)
        } catch (e: Exception) {
            temporary.delete()
            throw e
        }
    }

    fun remove() {
        if (model.exists() && !model.delete()) throw IOException("Could not remove model candidate")
    }
}
