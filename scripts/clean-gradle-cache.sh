#!/usr/bin/env bash
# Clean Gradle cache to prevent spotless lint errors on generated files
#
# This script removes Gradle cache directories that may contain auto-generated
# files that fail spotless/ktlint checks. These files are regenerated on next build.
#
# Usage:
#   ./scripts/clean-gradle-cache.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Cleaning Gradle caches..."

# Remove .gradle-local directory (custom Gradle user home)
if [ -d "$PROJECT_ROOT/.gradle-local" ]; then
    echo "  Removing .gradle-local/"
    rm -rf "$PROJECT_ROOT/.gradle-local"
fi

# Remove .gradle directory (standard Gradle cache)
if [ -d "$PROJECT_ROOT/.gradle" ]; then
    echo "  Removing .gradle/"
    rm -rf "$PROJECT_ROOT/.gradle"
fi

# Remove .kotlin directory (Kotlin Gradle plugin cache)
if [ -d "$PROJECT_ROOT/.kotlin" ]; then
    echo "  Removing .kotlin/"
    rm -rf "$PROJECT_ROOT/.kotlin"
fi

# Remove build directories
echo "  Removing build directories..."
find "$PROJECT_ROOT" -type d -name "build" -not -path "*/node_modules/*" -exec rm -rf {} + 2>/dev/null || true

echo "✓ Gradle cache cleaned successfully"
echo ""
echo "Run './gradlew build' to regenerate caches"
