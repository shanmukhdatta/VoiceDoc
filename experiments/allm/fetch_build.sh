#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
revision=180d3f15f482266d5cafc32a8bc70240c83c5870
if [[ ! -d upstream/.git ]]; then
  git clone https://github.com/mithun50/ALLM.git upstream
fi
git -C upstream fetch origin "$revision"
if [[ -n "$(git -C upstream status --porcelain)" ]]; then
  echo "Refusing to overwrite modified upstream checkout" >&2
  exit 1
fi
git -C upstream checkout --detach "$revision"
git -C upstream rev-parse HEAD
if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  echo "Clone prepared. Set ANDROID_NDK_HOME and NDK_HOST_TAG to cross-build."
  exit 0
fi
: "${NDK_HOST_TAG:?Set NDK_HOST_TAG to the actual NDK prebuilt host directory}"
bin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$NDK_HOST_TAG/bin"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$bin/aarch64-linux-android24-clang"
export CC_aarch64_linux_android="$bin/aarch64-linux-android24-clang"
export AR_aarch64_linux_android="$bin/llvm-ar"
test -x "$CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER"
rustup target add aarch64-linux-android
cd upstream
cargo build --locked --release -p allm-cli --target aarch64-linux-android
