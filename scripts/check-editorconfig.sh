#!/bin/bash
# EditorConfig checking with Docker

set -Eeuo pipefail

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <files...>"
    exit 1
fi

echo "Running editorconfig-checker on: $*"

docker run --rm \
    -v "$(pwd):/workspace" \
    -w /workspace \
    mstruebing/editorconfig-checker:latest \
    ec \
    "$@"

echo "✅ EditorConfig checking passed"
