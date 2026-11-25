# VoiceDoc / ALLM — isolated Android inference experiment

Status: code prepared; not built or run. Production VoiceDoc remains unchanged.
Upstream: https://github.com/mithun50/ALLM (MIT).
Pinned revision: 180d3f15f482266d5cafc32a8bc70240c83c5870.

## Scope
Measure resident versus streamed CPU inference on the same ARM64 device, GGUF, prompt, output limit and threads. The upstream cache budget covers streamed weight blocks, not total process RAM. This is a CLI experiment, not Android JNI/UI integration.

The pinned upstream documents Qwen2.5-0.5B-Instruct. Its block loader does not establish Qwen3 Q/K normalization support. Do not relabel a Qwen2.5 result as Qwen3 or assume a successful Qwen3 load implies correct tokens. Qwen3 needs an upstream port (resident and streaming paths), tokenizer/chat-template and greedy token parity against Transformers or llama.cpp. No Qwen3 run is enabled by this wrapper until a separate reviewed port and parity gate are implemented.

## Later execution
From this directory:
1. Run `bash fetch_build.sh` on Mac/Linux after installing Rust. This only clones unless ANDROID_NDK_HOME and NDK_HOST_TAG are supplied.
2. For cross-build, set ANDROID_NDK_HOME to an installed NDK, NDK_HOST_TAG to its actual prebuilt host directory (e.g. darwin-x86_64), rerun script. Push the resulting binary to a connected phone and a separately acquired licensed Qwen2.5-0.5B-Instruct GGUF.
3. Use Termux or adb shell with executable and model paths accessible to the same user. For adb shell, example paths are /data/local/tmp/voicedoc-allm/allm and /data/local/tmp/voicedoc-allm/control.gguf. Use chmod +x on the binary. Do not put private conversations in prompts.
4. Run from the host:
```sh
python3 run_experiment.py --adb --binary /data/local/tmp/voicedoc-allm/allm --model /data/local/tmp/voicedoc-allm/control.gguf --output results/control.json
```
For desktop use omit --adb and supply local paths. The default run is a plan only: add --execute to actually perform inspection and generation. For more than one connected device use --serial.
5. Check raw stderr timing and cache statistics, output parity between modes, and collect Android process RSS/PSS and temperature separately; the runner does not claim those measurements. Cold/warm runs should be reported separately. The two prompts are benign continuation probes, not medical or chat-template evaluation.

No automatic model download; no cloud inference; no production health answers.
