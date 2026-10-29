# Actual ONNX Runtime CPU adapters — experimental

Pinned Android AAR: `com.microsoft.onnxruntime:onnxruntime-android:1.30.0` (MIT), verified Maven artifact listing and documented Android Java bindings.

`SileroOnnxCpu` executes a specific 16kHz recurrent Silero ONNX schema with `input`, `state`, `sr` tensors and outputs probability plus updated state. It rejects mismatched graphs and non-512-sample frames. Requires a legally distributed Silero ONNX model file in app-private local storage. **No such model is bundled**, and no voice activity classification accuracy is claimed.

`E5OnnxTokenEncoder` executes multilingual E5-small ONNX graphs **only when given token IDs and attention masks from the exact matching tokenizer**. Performs masked mean pooling and L2 normalization to 384 dimensions. This can be compared with golden vectors from Python. Free-form Android raw text tokenization is not available yet; it would be incorrect to substitute approximate tokenization. Thus **production E5 text retrieval is not complete**.

Both adapters are CPU-first, implement close/lifecycle, never access the Internet and do not hallucinate model availability. They can be invoked only after validated local model packs are installed. The app does not load these large models automatically.

References:
- https://onnxruntime.ai/docs/install/
- https://onnxruntime.ai/docs/build/android.html
- https://github.com/snakers4/silero-vad
- https://huggingface.co/intfloat/multilingual-e5-small
