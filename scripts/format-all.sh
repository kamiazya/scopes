#!/bin/bash

# Format All Files
# This script auto-fixes all linting and formatting issues

set -e

echo "🧹 Auto-fixing all linting and formatting issues..."

# Format Kotlin files
echo "📝 Formatting Kotlin files..."
./gradlew ktlintFormat --quiet

# Format Markdown files (using Docker)
echo "📄 Formatting Markdown files..."
if command -v docker &> /dev/null; then
    docker run --rm -v "$(pwd):/workspace" \
        -w /workspace \
        davidanson/markdownlint-cli2:latest \
        --config /workspace/.markdownlint.json \
        --fix \
        "docs/**/*.md" "*.md" 2>/dev/null || echo "  Some markdown files had formatting issues (non-critical)"
else
    echo "⚠️  Docker not found. Skipping Markdown formatting."
fi

# Fix EditorConfig issues
echo "⚙️  Fixing EditorConfig issues..."
# Remove trailing whitespace
find . -name "*.kt" -o -name "*.md" -o -name "*.yml" -o -name "*.yaml" -o -name "*.json" | \
    xargs sed -i '' 's/[[:space:]]*$//'

# Ensure final newline
find . -name "*.kt" -o -name "*.md" -o -name "*.yml" -o -name "*.yaml" -o -name "*.json" | \
    xargs -I {} sh -c 'if [ -s "{}" ] && [ "$(tail -c1 "{}")" != "" ]; then echo "" >> "{}"; fi'

echo "✅ All formatting complete!"
echo ""
echo "💡 Tip: Set up lefthook to auto-format on commit:"
echo "   lefthook install"
