#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -d upstream-v1/.git ]]; then
  git clone https://github.com/Tiiny-AI/PowerInfer.git upstream-v1
fi
git -C upstream-v1 rev-parse HEAD
echo "Original PowerInfer source for reference only; NOT PowerInfer-2."
