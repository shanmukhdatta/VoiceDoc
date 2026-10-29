package ai.voicedoc.app

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.voicedoc.app.models.LocalModelPackImporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class LocalModelPackImporterTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val importer get() = LocalModelPackImporter(
        context, "test-runtime", "1.0", expectedKind = "speech"
    )
    private val archive = File(context.cacheDir, "model-pack-import-test.zip")

    @After fun cleanup() {
        runBlocking { importer.remove("import-fixture", "1.0") }
        archive.delete()
    }

    @Test fun validPackInstallsListsVerifiesAndRemoves() = runBlocking {
        val model = "synthetic-model-asset".toByteArray()
        val manifest = manifest(model)
        writeArchive(manifest, model)

        val installed = importer.import(Uri.fromFile(archive), sha256(manifest))

        assertEquals("import-fixture", installed.id)
        assertEquals("1.0", installed.version)
        assertEquals(model.size.toLong(), installed.asset("model")!!.length())
        assertEquals(1, importer.installed().size)
        assertTrue(importer.installed().single().asset("model")!!.isFile)
        importer.remove("import-fixture", "1.0")
        assertTrue(importer.installed().isEmpty())
    }

    @Test fun refusesReplacementAndHidesCorruptedInstalledAssets() = runBlocking {
        val model = "synthetic-model-asset".toByteArray()
        val manifest = manifest(model)
        writeArchive(manifest, model)
        importer.import(Uri.fromFile(archive), sha256(manifest))

        val duplicate = runCatching {
            importer.import(Uri.fromFile(archive), sha256(manifest))
        }.exceptionOrNull()
        assertTrue("Same ID/version must not overwrite an installed pack", duplicate is IOException)
        assertEquals(1, importer.installed().size)

        val installedModel = File(
            context.noBackupFilesDir, "model-packs/pack-import-fixture-1.0/assets/model.ggml"
        )
        installedModel.writeBytes("tampered".toByteArray())
        assertTrue("A pack with a corrupted asset must not be listed as usable", importer.installed().isEmpty())
    }

    @Test fun rejectsUntrustedManifestHashAndCleansStaging() = runBlocking {
        val model = byteArrayOf(1, 2, 3)
        val manifest = manifest(model)
        writeArchive(manifest, model)

        val failure = runCatching {
            importer.import(Uri.fromFile(archive), "0".repeat(64))
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(importer.installed().isEmpty())
        assertFalse(File(context.noBackupFilesDir, "model-packs/pack-import-fixture-1.0").exists())
        assertTrue(File(context.noBackupFilesDir, "model-packs").listFiles().orEmpty()
            .none { it.name.startsWith(".staging-") })
    }

    @Test fun rejectsTraversalAndRuntimeMismatch() = runBlocking {
        val model = byteArrayOf(1, 2, 3)
        val traversalManifest = manifest(model, path = "../escape.bin")
        writeArchive(traversalManifest, model, assetPath = "../escape.bin")
        val traversal = runCatching {
            importer.import(Uri.fromFile(archive), sha256(traversalManifest))
        }.exceptionOrNull()
        assertTrue(traversal is IOException)

        val normal = manifest(model, runtime = "other-runtime")
        writeArchive(normal, model)
        val mismatch = runCatching {
            importer.import(Uri.fromFile(archive), sha256(normal))
        }.exceptionOrNull()
        assertTrue(mismatch is IOException)
        assertTrue(importer.installed().isEmpty())
    }

    @Test fun rejectsWrongPackKind() = runBlocking {
        val model = byteArrayOf(1, 2, 3)
        val manifest = manifest(model, kind = "knowledge")
        writeArchive(manifest, model)
        val failure = runCatching {
            importer.import(Uri.fromFile(archive), sha256(manifest))
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(importer.installed().isEmpty())
    }

    @Test fun rejectsFileExtensionThatConflictsWithDeclaredFormat() = runBlocking {
        val model = byteArrayOf(1, 2, 3)
        val manifest = manifest(model, path = "assets/model.sqlite")
        writeArchive(manifest, model, assetPath = "assets/model.sqlite")
        val failure = runCatching {
            importer.import(Uri.fromFile(archive), sha256(manifest))
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(importer.installed().isEmpty())
    }

    private fun manifest(
        model: ByteArray,
        path: String = "assets/model.ggml",
        runtime: String = "test-runtime",
        kind: String = "speech"
    ): ByteArray {
        val assetHash = sha256(model)
        return """
            {
              "schema_version": 1,
              "pack_id": "import-fixture",
              "version": "1.0",
              "kind": "$kind",
              "runtime": {"name": "$runtime", "version": "1.0"},
              "languages": ["en"],
              "assets": [{
                "id": "synthetic-model",
                "role": "model",
                "path": "$path",
                "format": "ggml",
                "bytes": ${model.size},
                "sha256": "$assetHash",
                "source": "synthetic test fixture",
                "revision": "fixture-1",
                "license": "test-only",
                "redistribution_reviewed": true,
                "languages": ["en"]
              }]
            }
        """.trimIndent().toByteArray()
    }

    private fun writeArchive(manifest: ByteArray, model: ByteArray, assetPath: String = "assets/model.ggml") {
        archive.parentFile!!.mkdirs()
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(assetPath))
            zip.write(model)
            zip.closeEntry()
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
