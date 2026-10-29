package ai.voicedoc.app.models

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

data class InstalledModelPack(
    val id: String,
    val version: String,
    val kind: String,
    val runtimeName: String,
    val runtimeVersion: String,
    val languages: Set<String>,
    val embeddingDimension: Int?,
    val manifestSha256: String,
    val directory: File,
    val assets: Map<String, String>
) {
    fun asset(role: String): File? = assets[role]?.let { File(directory, it) }
}

/**
 * Imports schema-v1 model packs into app-private, no-backup storage.
 * expectedManifestSha256 must come from independently verified publisher metadata. A self-authored
 * manifest, its license flags, and these byte checks do not prove authenticity or clinical quality.
 */
class LocalModelPackImporter(
    context: Context,
    private val runtimeName: String,
    private val runtimeVersion: String,
    private val expectedEmbeddingDimension: Int? = null,
    private val expectedKind: String? = null
) {
    private val root = File(context.noBackupFilesDir, "model-packs")
    private val resolver = context.applicationContext.contentResolver

    init {
        require(runtimeName.isNotBlank() && runtimeVersion.isNotBlank())
        require(expectedEmbeddingDimension == null || expectedEmbeddingDimension > 0)
        require(expectedKind == null || expectedKind in PACK_KINDS)
    }

    suspend fun import(uri: Uri, expectedManifestSha256: String): InstalledModelPack =
        withContext(Dispatchers.IO) {
            if (!SHA256.matches(expectedManifestSha256)) {
                throw IOException("A valid expected manifest SHA-256 is required")
            }
            if (!root.exists() && !root.mkdirs()) throw IOException("Cannot create model-pack storage")
            val staging = File(root, ".staging-${UUID.randomUUID()}")
            if (!staging.mkdir()) throw IOException("Cannot create unique model-pack staging directory")
            val archiveFile = File(staging, ".incoming.zip")
            try {
                copyArchive(uri, archiveFile)
                val installed = inspectAndExtract(archiveFile, staging, expectedManifestSha256)
                if (archiveFile.exists() && !archiveFile.delete()) {
                    throw IOException("Could not remove staged model-pack archive")
                }
                val completed = File(root, "pack-${installed.id}-${installed.version}")
                if (completed.exists()) throw IOException("This model-pack version is already installed")
                if (!staging.renameTo(completed)) throw IOException("Could not atomically install model pack")
                installed.copy(directory = completed)
            } catch (error: Exception) {
                staging.deleteRecursively()
                if (error is IOException) throw error
                throw IOException("Model-pack import failed: ${error.javaClass.simpleName}", error)
            }
        }

    suspend fun installed(): List<InstalledModelPack> = withContext(Dispatchers.IO) {
        if (!root.isDirectory) return@withContext emptyList()
        root.listFiles().orEmpty().asSequence()
            .filter { it.isDirectory && it.name.matches(INSTALLED_DIR) }
            .mapNotNull { directory -> readInstalled(directory) }
            .sortedWith(compareBy<InstalledModelPack> { it.id }.thenBy { it.version })
            .toList()
    }

    suspend fun remove(id: String, version: String) = withContext(Dispatchers.IO) {
        require(PACK_ID.matches(id) && PACK_VERSION.matches(version))
        val directory = File(root, "pack-$id-$version")
        if (directory.exists() && !directory.deleteRecursively()) {
            throw IOException("Could not remove model pack")
        }
    }

    private fun copyArchive(uri: Uri, target: File) {
        val source = resolver.openInputStream(uri) ?: throw IOException("Cannot open model-pack archive")
        var total = 0L
        source.use { input ->
            BufferedOutputStream(target.outputStream()).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_ARCHIVE_BYTES) throw IOException("Model-pack archive exceeds size limit")
                    output.write(buffer, 0, count)
                }
            }
        }
        if (total == 0L) throw IOException("Model-pack archive is empty")
    }

    private fun inspectAndExtract(
        archiveFile: File,
        staging: File,
        expectedManifestSha256: String
    ): InstalledModelPack {
        ZipFile(archiveFile).use { archive ->
            val entries = linkedMapOf<String, java.util.zip.ZipEntry>()
            val enumeration = archive.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement()
                if (entry.isDirectory || entries.put(entry.name, entry) != null) {
                    throw IOException("Directories and duplicate ZIP entries are not allowed")
                }
                validateRelativePath(entry.name)
                if (entries.size > MAX_ASSETS + 1) throw IOException("Model pack has too many files")
            }
            val manifestEntry = entries[MANIFEST_NAME] ?: throw IOException("Pack is missing manifest.json")
            if (manifestEntry.size !in 1..MAX_MANIFEST_BYTES) throw IOException("Invalid manifest size")
            val manifestBytes = archive.getInputStream(manifestEntry).use { it.readBounded(MAX_MANIFEST_BYTES) }
            val actualManifestHash = sha256(manifestBytes)
            if (!actualManifestHash.equals(expectedManifestSha256, ignoreCase = true)) {
                throw IOException("Manifest hash does not match independently supplied publisher metadata")
            }
            val pack = parseManifest(String(manifestBytes, Charsets.UTF_8), actualManifestHash, staging)
            val expectedEntries = pack.assets.values.toSet() + MANIFEST_NAME
            if (entries.keys != expectedEntries) {
                throw IOException("Archive entries must exactly match the manifest and its declared assets")
            }

            var expandedBytes = manifestBytes.size.toLong()
            for ((role, relativePath) in pack.assets) {
                val entry = entries.getValue(relativePath)
                val declared = pack.assetRecords.getValue(role)
                if (entry.size < 0L || entry.size != declared.bytes || entry.size > MAX_ASSET_BYTES) {
                    throw IOException("Asset size is invalid: $role")
                }
                expandedBytes += entry.size
                if (expandedBytes > MAX_EXPANDED_BYTES) throw IOException("Expanded model pack exceeds size limit")
                val target = containedFile(staging, relativePath)
                if (!target.parentFile.exists() && !target.parentFile.mkdirs()) {
                    throw IOException("Cannot create asset directory")
                }
                val hash = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                archive.getInputStream(entry).use { input ->
                    BufferedOutputStream(target.outputStream()).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            if (copied > declared.bytes || copied > MAX_ASSET_BYTES) {
                                throw IOException("Asset exceeded declared size: $role")
                            }
                            hash.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                if (copied != declared.bytes || !hash.digest().joinToString("") { "%02x".format(it) }
                        .equals(declared.sha256, ignoreCase = true)) {
                    throw IOException("Asset integrity check failed: $role")
                }
            }
            File(staging, MANIFEST_NAME).writeBytes(manifestBytes)
            return pack.toInstalledPack(staging)
        }
    }

    private fun parseManifest(raw: String, manifestHash: String, staging: File): ParsedPack {
        val json = try { JSONObject(raw) } catch (error: Exception) {
            throw IOException("Malformed model-pack manifest", error)
        }
        if (json.optInt("schema_version", -1) != 1) throw IOException("Unsupported model-pack schema")
        val id = json.requiredString("pack_id")
        val version = json.requiredString("version")
        val kind = json.requiredString("kind")
        if (!PACK_ID.matches(id) || !PACK_VERSION.matches(version)) throw IOException("Unsafe pack identity")
        if (kind !in PACK_KINDS) throw IOException("Unsupported model-pack kind")
        if (expectedKind != null && kind != expectedKind) throw IOException("Unexpected model-pack kind")

        val runtime = json.optJSONObject("runtime") ?: throw IOException("Runtime declaration missing")
        if (runtime.requiredString("name") != runtimeName || runtime.requiredString("version") != runtimeVersion) {
            throw IOException("Model pack targets an incompatible runtime or runtime version")
        }
        val languages = json.stringSet("languages", setOf("en", "te"))
        if (languages.isEmpty()) throw IOException("Model pack must declare English or Telugu")
        val dimension = if (json.has("embedding_dimension")) json.optInt("embedding_dimension", -1) else null
        if (kind == "embedding") {
            if (dimension == null || dimension <= 0) throw IOException("Embedding dimension is required")
            if (expectedEmbeddingDimension != null && dimension != expectedEmbeddingDimension) {
                throw IOException("Incompatible embedding dimension")
            }
        } else if (dimension != null) {
            throw IOException("Only embedding packs may declare embedding_dimension")
        }

        val array = json.optJSONArray("assets") ?: throw IOException("Pack assets are missing")
        if (array.length() !in 1..MAX_ASSETS) throw IOException("Pack must declare between 1 and $MAX_ASSETS assets")
        val records = linkedMapOf<String, PackAssetRecord>()
        val paths = mutableSetOf<String>()
        val assetIds = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val asset = array.optJSONObject(index) ?: throw IOException("Asset record must be an object")
            val role = asset.requiredString("role")
            val assetId = asset.requiredString("id")
            val path = asset.requiredString("path")
            val format = asset.requiredString("format")
            val source = asset.requiredString("source")
            asset.requiredString("revision")
            asset.requiredString("license")
            val reviewed = asset.opt("redistribution_reviewed") as? Boolean ?: false
            if (!reviewed) throw IOException("Each asset requires separate redistribution review")
            if (role !in ALLOWED_ROLES || records.containsKey(role)) throw IOException("Invalid or duplicate asset role")
            if (!ASSET_ID.matches(assetId) || !assetIds.add(assetId) || !paths.add(path)) {
                throw IOException("Invalid or duplicate asset ID/path")
            }
            validateRelativePath(path)
            if (path == MANIFEST_NAME) throw IOException("Asset path conflicts with manifest")
            if (format !in FORMATS || source.isBlank()) throw IOException("Invalid asset format or source")
            val bytes = asset.optLong("bytes", -1L)
            val sha = asset.optString("sha256", "")
            if (bytes !in 1..MAX_ASSET_BYTES || !SHA256.matches(sha)) throw IOException("Invalid size or SHA-256 for $role")
            val assetLanguages = asset.stringSet("languages", setOf("en", "te", "multi"))
            if (assetLanguages.isEmpty() || !(languages.containsAll(assetLanguages) ||
                    (assetLanguages == setOf("multi") && languages == setOf("en", "te")))) {
                throw IOException("Asset language tags exceed pack language declarations")
            }
            records[role] = PackAssetRecord(assetId, path, format, bytes, sha, assetLanguages)
        }
        val requiredRoles = when (kind) {
            "generation", "embedding" -> setOf("model", "tokenizer")
            "speech" -> setOf("model")
            else -> setOf("index")
        }
        if (!records.keys.containsAll(requiredRoles)) throw IOException("Pack is missing required asset roles")
        if (kind == "generation" && records.getValue("model").format != "litertlm") {
            throw IOException("Generation model must use LiteRT-LM format")
        }
        if (kind == "embedding" && records.getValue("model").format != "onnx") {
            throw IOException("Embedding model must use ONNX format")
        }
        if (kind == "knowledge" && records.getValue("index").format != "sqlite") {
            throw IOException("Knowledge index must use SQLite format")
        }
        return ParsedPack(id, version, kind, runtimeName, runtimeVersion, languages, dimension, manifestHash, records)
    }

    private fun readInstalled(directory: File): InstalledModelPack? = try {
        val bytes = File(directory, MANIFEST_NAME).readBytes()
        val hash = sha256(bytes)
        val parsed = parseManifest(String(bytes, Charsets.UTF_8), hash, directory)
        val filesOkay = parsed.assets.all { (role, path) ->
            val record = parsed.assetRecords.getValue(role)
            val asset = containedFile(directory, path)
            asset.isFile && asset.length() == record.bytes && digest(asset) == record.sha256
        }
        if (filesOkay) parsed.toInstalledPack(directory) else null
    } catch (_: Exception) { null }

    private fun validateRelativePath(path: String) {
        if (path.isBlank() || path.length > 240 || path.startsWith('/') || path.contains('\\') ||
            path.contains(':') || path.split('/').any { it.isBlank() || it == "." || it == ".." }) {
            throw IOException("Unsafe model-pack path")
        }
    }

    private fun containedFile(directory: File, relativePath: String): File {
        val base = directory.canonicalFile.path
        val target = File(directory, relativePath).canonicalFile
        if (target.path != base && !target.path.startsWith(base + File.separator)) {
            throw IOException("Asset path escapes pack")
        }
        return target
    }

    private fun JSONObject.requiredString(key: String): String {
        val value = opt(key) as? String ?: throw IOException("Missing string field: $key")
        if (value.isBlank()) throw IOException("Blank string field: $key")
        return value
    }

    private fun JSONObject.stringSet(key: String, allowed: Set<String>): Set<String> {
        val array = optJSONArray(key) ?: throw IOException("Missing array field: $key")
        if (array.length() !in 1..8) throw IOException("Invalid language list")
        val values = buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index, "")
                if (value !in allowed) throw IOException("Unsupported language tag")
                add(value)
            }
        }
        if (values.size != values.toSet().size) throw IOException("Duplicate language tags")
        return values.toSet()
    }

    private fun digest(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun java.io.InputStream.readBounded(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var size = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            size += count
            if (size > limit) throw IOException("Manifest exceeds size limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private data class PackAssetRecord(
        val id: String, val path: String, val format: String, val bytes: Long,
        val sha256: String, val languages: Set<String>
    )

    private data class ParsedPack(
        val id: String, val version: String, val kind: String, val runtimeName: String,
        val runtimeVersion: String, val languages: Set<String>, val embeddingDimension: Int?,
        val manifestSha256: String, val assetRecords: Map<String, PackAssetRecord>
    ) {
        val assets: Map<String, String> get() = assetRecords.mapValues { it.value.path }
        fun toInstalledPack(directory: File) = InstalledModelPack(
            id, version, kind, runtimeName, runtimeVersion, languages, embeddingDimension,
            manifestSha256, directory, assets
        )
    }

    private companion object {
        const val MANIFEST_NAME = "manifest.json"
        const val MAX_ASSETS = 32
        const val MAX_MANIFEST_BYTES = 256 * 1024
        const val MAX_ASSET_BYTES = 1_500_000_000L
        const val MAX_ARCHIVE_BYTES = 2_000_000_000L
        const val MAX_EXPANDED_BYTES = 2_500_000_000L
        val SHA256 = Regex("^[0-9a-fA-F]{64}$")
        val PACK_ID = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
        val PACK_VERSION = Regex("^[A-Za-z0-9][A-Za-z0-9._+-]{0,63}$")
        val ASSET_ID = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
        val INSTALLED_DIR = Regex("^pack-[A-Za-z0-9][A-Za-z0-9._-]{0,63}-[A-Za-z0-9][A-Za-z0-9._+-]{0,63}$")
        val PACK_KINDS = setOf("generation", "embedding", "speech", "knowledge")
        val ALLOWED_ROLES = setOf("model", "tokenizer", "vocabulary", "index", "config")
        val FORMATS = setOf("litertlm", "onnx", "ggml", "gguf", "sqlite", "bin")
    }
}
