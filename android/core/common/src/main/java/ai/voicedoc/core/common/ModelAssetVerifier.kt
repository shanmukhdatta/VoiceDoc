package ai.voicedoc.core.common

import java.io.File
import java.security.MessageDigest

/**
 * Before loading an untrusted local model asset, verify its SHA-256 and containment.
 * Does not infer legal permission from a claimed license; approval is separately required.
 */
data class ModelAssetSpec(
    val id: String,
    val relativePath: String,
    val sha256: String,
    val expectedByteCount: Long,
    val format: String,
    val licenseIdentifier: String,
    val approvedForDistribution: Boolean,
    val languageTags: Set<String>
)
enum class AssetCheck { VERIFIED, MISSING, INVALID_MANIFEST, UNSAFE_PATH, WRONG_SIZE, HASH_MISMATCH }

object ModelAssetVerifier {
    private val digestPattern = Regex("^[0-9a-fA-F]{64}$")
    private val permittedFormats = setOf("litertlm", "onnx", "ggml", "gguf", "sqlite", "json", "bin")

    fun verify(root: File, spec: ModelAssetSpec): AssetCheck {
        if (
            spec.id.isBlank() || spec.relativePath.isBlank() ||
            spec.expectedByteCount <= 0L || !digestPattern.matches(spec.sha256) ||
            spec.format !in permittedFormats || spec.licenseIdentifier.isBlank() ||
            !spec.approvedForDistribution || spec.languageTags.isEmpty() ||
            !spec.languageTags.all { it in setOf("en", "te", "multi") }
        ) return AssetCheck.INVALID_MANIFEST

        val rootCanonical = root.canonicalFile
        val asset = File(root, spec.relativePath).canonicalFile
        if (!asset.toPath().startsWith(rootCanonical.toPath()) || asset == rootCanonical) {
            return AssetCheck.UNSAFE_PATH
        }
        if (!asset.isFile) return AssetCheck.MISSING
        if (asset.length() != spec.expectedByteCount) return AssetCheck.WRONG_SIZE

        val hash = MessageDigest.getInstance("SHA-256")
        asset.inputStream().buffered().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                hash.update(buf, 0, n)
            }
        }
        val found = hash.digest().joinToString("") { "%02x".format(it) }
        return if (found.equals(spec.sha256, ignoreCase = true)) AssetCheck.VERIFIED
        else AssetCheck.HASH_MISMATCH
    }
}
