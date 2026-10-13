# Phase 00 — Device requirements and reproducible local checks
Date: 2026-10-13 · Status: provisional, hardware unknown.

## Target Android device — provisional tiers (NOT tested minimums)
| Tier | RAM | Free internal storage | Android | CPU / ABI | State |
|---|---|---|---|---|---|
| Experimental recommended | >= 8 GiB physical | >= 5 GiB free during preparation/installation | Android 11+ preferred | 64-bit ARM arm64-v8a | Planning target only |
| Constrained trial | >= 6 GiB | >= 4 GiB free | Android 10+ | arm64-v8a | Try separately; no guarantee |
| 4 GiB or 32-bit | <= 4 GiB or 32-bit | unspecified | any | may be incompatible | Not a supported baseline without measured proof |

These are deliberately conservative **engineering hypotheses**, not official system requirements of LiteRT-LM or a verified device list. Physical RAM != free process memory. Measure per-process RSS/PSS, graphics allocations and OOM behavior. GPU/NPU capability must be separately detected. Reserve storage for download/archive extraction, staging, integrity checks and rollback (at least ~2x largest asset). Supported storage tiers will be decided from devices tested.

## Android build environment baseline
- Windows 11 + Android Studio (stable channel) **or** native Linux Android Studio, with Android SDK platform/build tools and `adb`.
- Host 16 GiB RAM recommended, 25+ GiB disk space suggested for Gradle caches, NDK, SDK, ML artifacts; less might work.
- Java JDK 17 starting baseline; **pin and match actual Android Gradle Plugin (AGP), Gradle wrapper, Kotlin, Compose compiler plugin versions in Phase 01 using the official AGP compatibility matrix**. Don't copy unverified version combinations.
- minSdk proposed 29 (Android 10) for simplicity, targetSdk/compileSdk pinned to stable supported version when Phase 01 begins; minSdk is an app policy decision, NOT proof every runtime supports it.
- Android NDK + CMake for whisper.cpp JNI; prefer arm64-v8a; any native AAR must match ABI and C++ runtime.
- Android profiler, Logcat, Perfetto/Android benchmark tooling.
- Development on Windows: Android Gradle build and USB adb are simplest natively; use WSL2 Ubuntu for Python/model prep, but don't assume USB forwarding works transparently from WSL2.
- Python 3.10/3.11 isolated virtual environment initially, PyTorch + Transformers + PEFT + Accelerate for future training; onnx, onnxruntime, optimum or model-specific export tools for desktop evaluation. Use pinned lockfiles or exact package versions later. Model export can require Linux + sufficient memory; Kaggle GPU only if tested and necessary.
- All model downloads/conversions happen off-device during development or before offline use; installed app must perform inference with network disabled. A completely offline *first install* with bundled assets is a separate delivery/size decision.

## Run on Windows PowerShell (copy/paste)
```powershell
adb --version
adb devices -l
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.cpu.abilist
adb shell cat /proc/meminfo
adb shell df -h /data
adb shell getprop ro.soc.model
java -version
python --version
```
Most commands also work in a Linux terminal. The `ro.soc.model` property can be blank, so record `ro.hardware` and manufacturer/model too. **Do not paste private serial numbers or unrelated device account identifiers in reports.**

Alternatively run the repository's `scripts/phase00/collect_device_info.py` using Python and an installed `adb`. It does not fetch models or access private health data.

## Physical-device capability checklist (NOT YET RUN)
1. Capture sanitized device report including ABI, Android SDK, physical RAM, available /data space and SoC.
2. With upstream standalone sample (not VoiceDoc app), load the exact Gemma 3 1B .litertlm QAT artifact on CPU; record model hash, successful output, peak PSS/RSS, first-token and decode latency, thermal response. GPU second if supported; NPU only with official platform-specific bundle.
3. Load whisper.cpp multilingual base, transcribe known English and Telugu WAV examples (with reference text). Record WER/CER, numeric/medical term errors, latency and resident memory.
4. Check installed Android TTS voices for **English and Telugu**, record locale and `isNetworkConnectionRequired`, then perform synthesize/playback with airplane mode enabled. A voice listed as offline is only a preliminary signal.
5. Validate an ONNX embedding golden-vector cosine agreement for a fixed English/Telugu sentence pair; test tokenization, pre/postprocessing, dimension 384 and reproducible similarity.
6. Measure sequential pipeline resource behavior, cancellation, process restart and OOM recovery. No simultaneous full ASR and LLM load presumed.
7. Disable Wi-Fi/mobile data and confirm no inference/network calls; ensure local asset integrity after restart.
8. Log phone manufacturer/model, Android version, SoC, RAM, runtime versions, model hash and test timestamp; never share raw health history.

## Expected results file schema (fill after running)
```json
{
  "device": {"model": null, "android_api": null, "soc": null, "abi": null, "ram_gib": null, "free_storage_gib": null},
  "model": {"id": null, "sha256": null, "runtime": null, "backend": null, "context_tokens": null},
  "measurements": {"load_ms": null, "peak_pss_mb": null, "peak_rss_mb": null, "ttft_ms": null, "decode_tok_per_s": null, "asr_rtf": null},
  "tts": {"en_offline_succeeded": null, "te_offline_succeeded": null},
  "result": "NOT_RUN",
  "notes": null
}
```

## References
- LiteRT-LM Android API: https://developers.google.com/edge/litert-lm/android
- Official Android Gradle Plugin and Gradle compatibility: https://developer.android.com/build/releases/gradle-plugin
- Android device memory profiling: https://developer.android.com/studio/profile/memory-profiler
- Qualcomm NPU support matrix: https://developers.google.com/edge/litert/next/qualcomm
- Android TextToSpeech Voice: https://developer.android.com/reference/android/speech/tts/Voice
