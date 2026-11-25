package ai.voicedoc.app.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * Real ONNX CPU execution for a specific Silero VAD export:
 * input [1,512] float, state [2,1,128] float, sr scalar int64;
 * outputs probability and new recurrent state.
 *
 * If the artifact schema differs, initialization fails explicitly.
 * This does not imply language or medical ASR accuracy.
 */
class SileroOnnxCpu(model: File) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions()
    private val session: OrtSession
    private var state = FloatArray(256)

    init {
        require(model.isFile && model.extension == "onnx")
        session = environment.createSession(model.absolutePath, options)
        require(session.inputNames.containsAll(setOf("input", "state", "sr"))) {
            "Silero model schema is not the expected 16 kHz recurrent ONNX export"
        }
    }

    fun resetState() { state.fill(0f) }

    fun probability(frame: ShortArray): Float {
        require(frame.size == 512) { "Expected exactly 512 PCM16 samples at 16 kHz" }
        val input = FloatArray(512) { frame[it].toFloat() / 32768f }
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), longArrayOf(1, 512)).use { audio ->
            OnnxTensor.createTensor(environment, FloatBuffer.wrap(state), longArrayOf(2, 1, 128)).use { memory ->
                OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(16_000)), longArrayOf()).use { rate ->
                    session.run(mapOf("input" to audio, "state" to memory, "sr" to rate)).use { result ->
                        val probability = flattenFloatData(result[0].value).firstOrNull()
                            ?: error("Silero returned no probability")
                        val newState = flattenFloatData(result[1].value)
                        check(newState.size == state.size) { "Unexpected recurrent state size" }
                        check(probability.isFinite() && probability in 0f..1f) {
                            "Invalid probability from Silero model"
                        }
                        state = newState.toFloatArray()
                        return probability
                    }
                }
            }
        }
    }

    override fun close() {
        session.close()
        options.close()
    }
}

internal fun flattenFloatData(value: Any?): List<Float> = when (value) {
    is Float -> listOf(value)
    is FloatArray -> value.toList()
    is Array<*> -> value.flatMap { flattenFloatData(it) }
    else -> error("Unsupported ONNX output tensor value")
}

/**
 * Real E5-compatible ONNX graph runner AFTER caller supplies exact token IDs and masks
 * from the SAME pretrained tokenizer. This does NOT tokenize free text on Android yet.
 * Missing tokenization is a release blocker for full on-device multilingual E5 retrieval.
 */
class E5OnnxTokenEncoder(model: File) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions()
    private val session: OrtSession

    init {
        require(model.isFile && model.extension == "onnx")
        session = environment.createSession(model.absolutePath, options)
        require(session.inputNames.containsAll(setOf("input_ids", "attention_mask"))) {
            "E5 ONNX graph must accept input_ids and attention_mask"
        }
    }

    fun encodeTokenIds(ids: LongArray, attentionMask: LongArray): FloatArray {
        require(ids.isNotEmpty() && ids.size == attentionMask.size && ids.size <= 512)
        require(attentionMask.any { it == 1L })
        require(attentionMask.all { it == 0L || it == 1L })
        val dimensions = longArrayOf(1, ids.size.toLong())
        OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), dimensions).use { idsTensor ->
            OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), dimensions).use { maskTensor ->
                session.run(mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor)).use { result ->
                    // For standard feature-extraction exports, output is [batch,seq,384].
                    val batch = result[0].value as? Array<*>
                        ?: error("Unexpected E5 output rank")
                    val tokens = batch.firstOrNull() as? Array<*>
                        ?: error("Unexpected E5 token embeddings")
                    check(tokens.size == ids.size) { "E5 token count mismatch" }
                    val hidden = tokens.firstOrNull() as? FloatArray
                        ?: error("Unexpected E5 hidden vector format")
                    check(hidden.size == 384) { "E5-small should return 384 hidden dimensions" }
                    val pooled = FloatArray(384)
                    var count = 0
                    for (index in tokens.indices) {
                        if (attentionMask[index] != 1L) continue
                        val vector = tokens[index] as? FloatArray ?: error("Invalid E5 token output")
                        check(vector.size == 384)
                        for (h in 0 until 384) pooled[h] += vector[h]
                        count++
                    }
                    check(count > 0)
                    for (h in pooled.indices) pooled[h] /= count.toFloat()
                    val magnitude = sqrt(pooled.sumOf { it.toDouble() * it.toDouble() }).toFloat()
                    check(magnitude > 0f && magnitude.isFinite()) { "Invalid E5 embedding" }
                    for (h in pooled.indices) pooled[h] /= magnitude
                    return pooled
                }
            }
        }
    }

    override fun close() {
        session.close()
        options.close()
    }
}
