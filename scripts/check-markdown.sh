#!/bin/bash
# Markdown linting with Docker

set -e

if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <markdown-files...>"
    exit 1
fi

echo "Running markdownlint on: $*"

docker run --rm \
    -v "$(pwd):/workspace" \
    -w /workspace \
    markdownlint/markdownlint:latest \
    --config .markdownlint.json \
    "$@"

echo "✅ Markdown linting passed"
