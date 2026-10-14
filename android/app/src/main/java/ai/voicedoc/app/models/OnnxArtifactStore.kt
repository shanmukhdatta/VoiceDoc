package ai.voicedoc.app.models

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Developer-only ONNX artifact import; digest of copied bytes is NOT publisher authentication. */
class OnnxArtifactStore(private val context: Context, private val kind: String) {
    init { require(kind == "vad" || kind == "e5") }
    private val directory get() = File(context.noBackupFilesDir, "onnx-experiments")
    private val file get() = File(directory, "$kind.onnx")
    private val maxBytes = if (kind == "vad") 20_000_000L else 800_000_000L

    fun existing(): File? = file.takeIf { it.isFile && it.length() > 0 }

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        if (existing() != null) throw IOException("Delete the old $kind candidate first")
        directory.mkdirs()
        val staging = File(directory, "$kind.partial")
        val hash = MessageDigest.getInstance("SHA-256")
        var byteCount = 0L
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open ONNX document")
            input.use { source ->
                staging.outputStream().use { destination ->
                    val bytes = ByteArray(65536)
                    while (true) {
                        val n = source.read(bytes)
                        if (n < 0) break
                        byteCount += n
                        if (byteCount > maxBytes) throw IOException("File exceeds $maxBytes byte limit")
                        hash.update(bytes, 0, n)
                        destination.write(bytes, 0, n)
                    }
                }
            }
            if (byteCount < 100) throw IOException("Candidate model file too small")
            if (!staging.renameTo(file)) throw IOException("Could not finalize local model")
            "Imported $kind ($byteCount bytes). SHA-256 (COPY INTEGRITY ONLY): " +
                hash.digest().joinToString("") { "%02x".format(it) }
        } catch (error: Exception) {
            staging.delete()
            throw error
        }
    }

    fun remove() {
        if (file.exists() && !file.delete()) throw IOException("Unable to remove $kind ONNX candidate")
    }
}
