#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
revision=180d3f15f482266d5cafc32a8bc70240c83c5870
if [[ ! -d upstream/.git ]]; then
  git clone https://github.com/mithun50/ALLM.git upstream
fi
if [[ "$(git -C upstream rev-parse HEAD)" != "$revision" ]]; then
  if [[ -n "$(git -C upstream status --porcelain)" ]]; then
    echo "Refusing to overwrite modified checkout" >&2; exit 1
  fi
  git -C upstream fetch origin "$revision"
  git -C upstream checkout --detach "$revision"
fi
if [[ -z "$(git -C upstream status --porcelain)" ]]; then
  python3 patch_qwen3.py upstream
else
  echo "Existing modified checkout; refusing automatic reuse. Review or use a fresh clone." >&2
  exit 1
fi
if [[ "${1:-}" == "--host" ]]; then
  cd upstream
  cargo build --locked --release -p allm-cli
elif [[ "${1:-}" == "--android" ]]; then
  : "${ANDROID_NDK_HOME:?Set Android NDK path}"
  : "${NDK_HOST_TAG:?Set actual NDK prebuilt host directory}"
  bin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$NDK_HOST_TAG/bin"
  export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$bin/aarch64-linux-android24-clang"
  export CC_aarch64_linux_android="$bin/aarch64-linux-android24-clang"
  export AR_aarch64_linux_android="$bin/llvm-ar"
  test -x "$CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER"
  rustup target add aarch64-linux-android
  cd upstream
  cargo build --locked --release -p allm-cli --target aarch64-linux-android
else
  echo "Patched source ready. Build manually with cargo when desired."
fi
