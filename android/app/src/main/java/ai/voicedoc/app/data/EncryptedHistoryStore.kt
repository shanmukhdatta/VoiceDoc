package ai.voicedoc.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedConversationTurn(val fromUser: Boolean, val text: String)

/**
 * Optional local AES-256/GCM encrypted text history.
 * Only invoke write() after explicit opt-in. No voice/audio ever persisted here.
 * This is an engineering implementation, not a substitute for device security review.
 */
class EncryptedHistoryStore(context: Context, namespace: String = "default") {
    init { require(Regex("^[a-zA-Z0-9_-]{1,32}$").matches(namespace)) }
    private val file = File(context.noBackupFilesDir, "conversation-history-v1-$namespace.enc")
    private val keyAlias = "ai.voicedoc.history.v1.$namespace"
    private val maxTurns = 80
    private val maxTextLength = 1000
    private val magic = byteArrayOf(0x56, 0x44, 0x01)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        val existing = store.getKey(keyAlias, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    @Synchronized
    fun write(turns: List<SavedConversationTurn>) {
        require(turns.size <= maxTurns)
        require(turns.all { it.text.length <= maxTextLength })
        val array = JSONArray()
        for (turn in turns) {
            array.put(JSONObject().put("role", if (turn.fromUser) "user" else "system").put("text", turn.text))
        }
        val plaintext = array.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        require(iv.size == 12)
        val encrypted = cipher.doFinal(plaintext)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.outputStream().use { out ->
            out.write(magic)
            out.write(iv)
            out.write(encrypted)
            out.fdSyncIfPossible()
        }
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IOException("Unable to atomically update encrypted history")
        }
    }

    @Synchronized
    fun read(): List<SavedConversationTurn> {
        if (!file.exists()) return emptyList()
        val bytes = file.readBytes()
        if (bytes.size < 32 || !bytes.copyOfRange(0, magic.size).contentEquals(magic)) {
            throw IOException("Invalid encrypted history header")
        }
        val iv = bytes.copyOfRange(3, 15)
        val ciphertext = bytes.copyOfRange(15, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val array = JSONArray(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        if (array.length() > maxTurns) throw IOException("History exceeds limit")
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val role = item.getString("role")
            val text = item.getString("text")
            if (role !in setOf("user", "system") || text.length > maxTextLength) {
                throw IOException("Invalid saved conversation item")
            }
            SavedConversationTurn(fromUser = role == "user", text = text)
        }
    }

    @Synchronized
    fun erase() {
        if (file.exists() && !file.delete()) throw IOException("Could not delete history file")
        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        if (store.containsAlias(keyAlias)) store.deleteEntry(keyAlias)
    }

    /** Test-only visibility; do not expose this in production UI or analytics. */
    internal fun ciphertextPathForTests(): File = file
}

private fun java.io.FileOutputStream.fdSyncIfPossible() {
    fd.sync()
}
