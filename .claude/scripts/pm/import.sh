#!/bin/bash

# PM Import Script - Import GitHub issues into PM system
# Usage: pm:import [--epic <epic_name>] [--label <label>]

set -e

# Parse arguments
EPIC=""
LABEL=""
ARGUMENTS="$*"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --epic)
      EPIC="$2"
      shift 2
      ;;
    --label)
      LABEL="$2"
      shift 2
      ;;
    *)
      echo "❌ Unknown option: $1"
      echo "Usage: /pm:import [--epic <epic_name>] [--label <label>]"
      exit 1
      ;;
  esac
done

echo "📥 GitHub Issue Import"
echo "====================="
echo ""

# Check dependencies
if ! command -v gh &> /dev/null; then
  echo "❌ GitHub CLI (gh) not found. Please install it first."
  exit 1
fi

if ! gh auth status &> /dev/null; then
  echo "❌ GitHub CLI not authenticated. Please run 'gh auth login'"
  exit 1
fi

# Ensure directories exist
mkdir -p .claude/epics

# Get repository info
REPO_URL=$(git remote get-url origin 2>/dev/null || echo "")
if [[ -z "$REPO_URL" ]]; then
  echo "❌ No git remote found. This must be run in a git repository with a remote."
  exit 1
fi

# Extract owner/repo from URL
REPO=$(echo "$REPO_URL" | sed -E 's/.*[:/]([^/]+\/[^/]+)(\.git)?$/\1/' | sed 's/\.git$//')

echo "📡 Fetching issues from: $REPO"
if [[ -n "$LABEL" ]]; then
  echo "🏷️  Filtering by label: $LABEL"
fi
if [[ -n "$EPIC" ]]; then
  echo "📁 Importing into epic: $EPIC"
fi
echo ""

# Fetch GitHub issues
TEMP_FILE=$(mktemp)
if [[ -n "$LABEL" ]]; then
  gh issue list --repo "$REPO" --label "$LABEL" --limit 1000 --json number,title,body,state,labels,createdAt,updatedAt > "$TEMP_FILE"
else
  gh issue list --repo "$REPO" --limit 1000 --json number,title,body,state,labels,createdAt,updatedAt > "$TEMP_FILE"
fi

# Check if we got any issues
ISSUE_COUNT=$(jq length "$TEMP_FILE")
if [[ "$ISSUE_COUNT" -eq 0 ]]; then
  echo "ℹ️  No issues found to import."
  rm "$TEMP_FILE"
  exit 0
fi

echo "🔍 Found $ISSUE_COUNT issues. Checking for untracked issues..."
echo ""

# Initialize counters
IMPORTED_EPICS=0
IMPORTED_TASKS=0
SKIPPED_COUNT=0

# Track created epics for summary
declare -A EPIC_TASKS

# Process each issue
while IFS= read -r issue; do
  number=$(echo "$issue" | jq -r '.number')
  title=$(echo "$issue" | jq -r '.title')
  body=$(echo "$issue" | jq -r '.body // ""')
  state=$(echo "$issue" | jq -r '.state')
  created=$(echo "$issue" | jq -r '.createdAt')
  updated=$(echo "$issue" | jq -r '.updatedAt')
  labels=$(echo "$issue" | jq -r '.labels[].name' | tr '\n' ',' | sed 's/,$//')

  issue_url="https://github.com/$REPO/issues/$number"

  # Check if issue is already tracked
  if grep -r "$issue_url" .claude/ &>/dev/null; then
    echo "⏭️  Skipping #$number: $title (already tracked)"
    ((SKIPPED_COUNT++))
    continue
  fi

  # Determine epic and task type from labels
  IS_EPIC=false
  TARGET_EPIC=""

  # Check labels to determine type and epic assignment
  if [[ "$labels" == *"epic"* ]]; then
    IS_EPIC=true
  fi

  # Look for epic:{name} labels
  if [[ "$labels" =~ epic:([^,]+) ]]; then
    TARGET_EPIC="${BASH_REMATCH[1]}"
  elif [[ -n "$EPIC" ]]; then
    TARGET_EPIC="$EPIC"
  elif [[ "$IS_EPIC" == "false" ]]; then
    TARGET_EPIC="imported"
  fi

  # Convert state
  STATUS="open"
  if [[ "$state" == "closed" ]]; then
    STATUS="closed"
  fi

  # Create issue content
  if [[ "$IS_EPIC" == "true" ]]; then
    # Create epic
    epic_name=$(echo "$title" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/-/g' | sed 's/--*/-/g' | sed 's/^-\|-$//g')
    epic_dir=".claude/epics/$epic_name"

    if [[ -d "$epic_dir" ]]; then
      echo "⏭️  Epic '$epic_name' already exists, skipping"
      ((SKIPPED_COUNT++))
      continue
    fi

    mkdir -p "$epic_dir"

    cat > "$epic_dir/epic.md" << EOF
---
name: $title
status: $STATUS
created: $created
updated: $updated
github: $issue_url
imported: true
---

# $title

$body
EOF

    echo "✅ Created epic: $epic_name"
    ((IMPORTED_EPICS++))
    EPIC_TASKS["$epic_name"]=0

  else
    # Create task
    if [[ -z "$TARGET_EPIC" ]]; then
      TARGET_EPIC="imported"
    fi

    epic_dir=".claude/epics/$TARGET_EPIC"
    mkdir -p "$epic_dir"

    # Ensure epic.md exists for imported tasks
    if [[ ! -f "$epic_dir/epic.md" && "$TARGET_EPIC" == "imported" ]]; then
      current_date=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
      cat > "$epic_dir/epic.md" << EOF
---
name: Imported Issues
status: open
created: $current_date
updated: $current_date
github:
imported: true
---

# Imported Issues

This epic contains issues imported from GitHub that didn't have specific epic assignments.
EOF
      echo "✅ Created imported epic container"
      if [[ -z "${EPIC_TASKS["imported"]}" ]]; then
        ((IMPORTED_EPICS++))
        EPIC_TASKS["imported"]=0
      fi
    fi

    # Find next available task number
    task_num=1
    while [[ -f "$epic_dir/$(printf "%03d" $task_num).md" ]]; do
      ((task_num++))
    done

    task_file="$epic_dir/$(printf "%03d" $task_num).md"

    cat > "$task_file" << EOF
---
name: $title
status: $STATUS
created: $created
updated: $updated
github: $issue_url
imported: true
---

# $title

$body
EOF

    echo "✅ Created task: $TARGET_EPIC/$(printf "%03d" $task_num).md - $title"
    ((IMPORTED_TASKS++))

    if [[ -n "${EPIC_TASKS["$TARGET_EPIC"]}" ]]; then
      ((EPIC_TASKS["$TARGET_EPIC"]++))
    else
      EPIC_TASKS["$TARGET_EPIC"]=1
    fi
  fi

done < <(jq -c '.[]' "$TEMP_FILE")

# Cleanup
rm "$TEMP_FILE"

echo ""
echo "📥 Import Complete"
echo "=================="
echo ""
echo "Imported:"
echo "  Epics: $IMPORTED_EPICS"
echo "  Tasks: $IMPORTED_TASKS"
echo ""

if [[ ${#EPIC_TASKS[@]} -gt 0 ]]; then
  echo "Created structure:"
  for epic in "${!EPIC_TASKS[@]}"; do
    task_count=${EPIC_TASKS[$epic]}
    echo "  $epic/"
    echo "    - $task_count tasks"
  done
  echo ""
fi

if [[ $SKIPPED_COUNT -gt 0 ]]; then
  echo "Skipped (already tracked): $SKIPPED_COUNT"
  echo ""
fi

echo "Next steps:"
echo "  Run /pm:status to see imported work"
echo "  Run /pm:sync to ensure full synchronization"
