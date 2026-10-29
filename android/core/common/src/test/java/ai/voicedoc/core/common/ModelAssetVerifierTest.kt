package ai.voicedoc.core.common

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelAssetVerifierTest {
    @get:Rule val temp = TemporaryFolder()
    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val bytes = byteArrayOf(1, 2, 3, 4)
    private fun spec(
        path: String = "model.onnx",
        hash: String = digest(bytes),
        size: Long = bytes.size.toLong(),
        allowed: Boolean = true
    ) = ModelAssetSpec("test", path, hash, size, "onnx", "MIT", allowed, setOf("en", "te"))

    @Test fun verifiesExactTrustedBytes() {
        temp.newFile("model.onnx").writeBytes(bytes)
        assertEquals(AssetCheck.VERIFIED, ModelAssetVerifier.verify(temp.root, spec()))
    }

    @Test fun rejectsModifiedAndTruncatedAssets() {
        temp.newFile("model.onnx").writeBytes(byteArrayOf(1, 2, 3, 5))
        assertEquals(AssetCheck.HASH_MISMATCH, ModelAssetVerifier.verify(temp.root, spec()))
        assertEquals(AssetCheck.WRONG_SIZE, ModelAssetVerifier.verify(temp.root, spec(size = 5)))
    }

    @Test fun rejectsMissingAndPathTraversal() {
        assertEquals(AssetCheck.MISSING, ModelAssetVerifier.verify(temp.root, spec()))
        assertEquals(AssetCheck.UNSAFE_PATH, ModelAssetVerifier.verify(temp.root, spec("../outside.onnx")))
    }

    @Test fun distributionRequiresApproval() {
        assertEquals(AssetCheck.INVALID_MANIFEST, ModelAssetVerifier.verify(temp.root, spec(allowed = false)))
        assertEquals(
            AssetCheck.INVALID_MANIFEST,
            ModelAssetVerifier.verify(temp.root, spec(hash = "garbage"))
        )
    }
}
