# Release Workflow Testing Guide

This guide explains how to safely test the release workflow without creating production releases.

## Overview

The release workflow builds native binaries for multiple platforms and architectures, generates security artifacts (SLSA provenance, SBOM), and creates GitHub releases.

## Prerequisites

- GitHub CLI (`gh`) installed and authenticated
- Repository write access
- Understanding of GitHub Actions basics

## Quick Start

### Test Release Creation

```bash
# Generate test version with timestamp
TEST_VERSION="v0.0.0-test$(date +%Y%m%d%H%M%S)"

# Create the test tag first (required since workflow now validates tag existence)
git tag "$TEST_VERSION"
git push origin "$TEST_VERSION"

# Or trigger workflow manually (if tag already exists)
gh workflow run release.yml --field tag="$TEST_VERSION"

# Monitor execution
gh run list --workflow=release.yml --limit=1
```

## Testing Strategies

### 1. Manual Workflow Dispatch

Trigger the workflow for existing tags:

```bash
# Basic test run (tag must already exist)
gh workflow run release.yml --field tag=v0.0.0-test[TIMESTAMP]

# Specific branch testing (ensure tag exists on that branch)
gh workflow run release.yml --ref [BRANCH_NAME] --field tag=v0.0.0-test[TIMESTAMP]
```

Note: As of the latest workflow updates, the release workflow validates that tags exist before proceeding. This prevents accidental release creation with incorrect tags.

### 2. Monitoring Execution

```bash
# List recent runs
gh run list --workflow=release.yml --limit=5

# Watch specific run (real-time updates)
gh run watch [RUN_ID]

# Detailed status
gh run view [RUN_ID] --json status,conclusion,jobs | jq '.'
```

## Verification Checklist

### Build Matrix Verification

Verify all platform/architecture combinations build successfully:

```bash
# Check job statuses
gh run view [RUN_ID] --json jobs | jq '.jobs[] | {name: .name, conclusion: .conclusion}'
```

Expected successful jobs:
- All defined platform/architecture combinations in the matrix
- Hash collection job
- Provenance generation job
- Release creation job

### Artifact Verification

```bash
# View release artifacts
gh release view [TEST_VERSION] --json assets | jq '.assets[].name'

# Expected artifact types per platform:
# - Binary file (executable)
# - Hash file (binary-hash-*.txt)
# - SBOM file (sbom-*.json)
```

### Security Verification

#### Hash Verification
```bash
# Download and verify any binary
PLATFORM="linux"
ARCH="x64"
VERSION="[TEST_VERSION]"

# Download files
wget "$(gh release view $VERSION --json assets | jq -r ".assets[] | select(.name | contains(\"$PLATFORM-$ARCH\")) | .url")"

# Verify hash
sha256sum scopes-$VERSION-$PLATFORM-$ARCH
```

#### SBOM Validation
```bash
# Download SBOM
wget "$(gh release view $VERSION --json assets | jq -r ".assets[] | select(.name == \"sbom-$PLATFORM-$ARCH.json\") | .url")"

# Validate JSON structure
jq empty sbom-$PLATFORM-$ARCH.json && echo "✅ Valid JSON"

# Check for expected format
jq -r '.bomFormat' sbom-$PLATFORM-$ARCH.json  # Should output "CycloneDX"
```

## Platform-Specific Testing

### Cross-Platform Build Features

Different platforms may require specific build configurations:

- **Cross-compilation**: Check for QEMU or container builds
- **Native builds**: Verify runner architecture matches target
- **Special flags**: Confirm platform-specific build arguments

### Runner Configuration

Verify correct runner assignment:

```bash
# Check which runner was used for each job
gh run view [RUN_ID] --json jobs | jq '.jobs[] | {name: .name, runner_name: .runner_name}'
```

## Automated Test Script

Create a reusable test script:

```bash
#!/bin/bash
# test-release.sh

set -euo pipefail

# Configuration
WORKFLOW_FILE="${WORKFLOW_FILE:-release.yml}"
TEST_PREFIX="${TEST_PREFIX:-v0.0.0-test}"
CLEANUP="${CLEANUP:-true}"

# Generate test version
TEST_VERSION="${TEST_PREFIX}$(date +%Y%m%d%H%M%S)"
echo "🚀 Testing release workflow with version: $TEST_VERSION"

# Trigger workflow
echo "📝 Triggering workflow..."
gh workflow run "$WORKFLOW_FILE" --field tag="$TEST_VERSION"

# Wait for workflow to start
sleep 5

# Get run ID
RUN_ID=$(gh run list --workflow="$WORKFLOW_FILE" --limit=1 --json databaseId | jq -r '.[0].databaseId')
echo "🔍 Monitoring run ID: $RUN_ID"

# Monitor execution
gh run watch "$RUN_ID" || {
    echo "❌ Workflow failed"
    exit 1
}

# Verify release creation
echo "✅ Checking release artifacts..."
if gh release view "$TEST_VERSION" > /dev/null 2>&1; then
    # Count and verify artifacts
    ASSETS=$(gh release view "$TEST_VERSION" --json assets | jq '.assets')
    ASSET_COUNT=$(echo "$ASSETS" | jq 'length')
    
    echo "📦 Found $ASSET_COUNT artifacts:"
    echo "$ASSETS" | jq -r '.[].name' | head -10
    
    # Verify minimum expected artifacts
    MIN_ARTIFACTS=$(($(gh run view "$RUN_ID" --json jobs | jq '[.jobs[] | select(.name | contains("Build Release"))] | length') * 3))
    
    if [ "$ASSET_COUNT" -ge "$MIN_ARTIFACTS" ]; then
        echo "✅ Artifact count verified ($ASSET_COUNT >= $MIN_ARTIFACTS)"
    else
        echo "⚠️ Unexpected artifact count ($ASSET_COUNT < $MIN_ARTIFACTS)"
    fi
    
    # Cleanup if requested
    if [ "$CLEANUP" = "true" ]; then
        echo "🧹 Cleaning up test release..."
        gh release delete "$TEST_VERSION" --yes --cleanup-tag
        echo "✅ Cleanup completed"
    fi
else
    echo "❌ Release not found"
    exit 1
fi

echo "✅ Release workflow test completed successfully!"
```

## Troubleshooting

### Common Issues

#### Build Failures

**Diagnosis**:
```bash
# Check failed job logs
gh run view [RUN_ID] --log-failed

# View specific job details
gh run view --job=[JOB_ID]
```

**Common causes**:
- Missing dependencies
- Incorrect build flags
- Runner architecture mismatch
- Container image unavailable

#### Provenance Generation Issues

**Diagnosis**:
```bash
# Check hash collection
gh run view [RUN_ID] --json jobs | jq '.jobs[] | select(.name | contains("Collect Hashes"))'

# Verify subject formatting
gh run view [RUN_ID] --log | grep -A5 "Collecting hashes"
```

#### Release Creation Failures

**Diagnosis**:
```bash
# Check if tag exists (required as of latest updates)
git ls-remote --tags origin | grep [TAG_NAME]

# Check for existing releases
gh release list --limit=10

# Verify permissions
gh api user -q .permissions

# Check release creation logs for --verify-tag errors
gh run view [RUN_ID] --log | grep "verify-tag"
```

**Common causes since workflow updates**:
- Tag doesn't exist on remote (workflow now validates this)
- Tag exists but points to different commit than expected
- Release already exists for the tag

## Performance Monitoring

Track build performance over time:

```bash
# Get build times for recent runs
gh run list --workflow=release.yml --limit=10 --json databaseId,createdAt,updatedAt | \
  jq '.[] | {
    id: .databaseId,
    duration: (((.updatedAt | fromdate) - (.createdAt | fromdate)) / 60 | floor),
    created: .createdAt
  }'
```

## Best Practices

### 1. Test Naming Convention
Use consistent naming for test releases:
- Development: `v0.0.0-dev[TIMESTAMP]`
- Testing: `v0.0.0-test[TIMESTAMP]`
- Validation: `v0.0.0-val[TIMESTAMP]`

### 2. Resource Management
- Always clean up test releases
- Monitor runner usage
- Use workflow concurrency limits

### 3. Validation Depth
Levels of testing:
1. **Quick**: Workflow completion only
2. **Standard**: Artifact count and naming
3. **Full**: Hash verification, SBOM validation, binary testing

### 4. Documentation
Record test results:
```bash
# Generate test report
cat > test-report-$(date +%Y%m%d).md << EOF
# Release Test Report - $(date +%Y-%m-%d)

## Test Configuration
- Version: $TEST_VERSION
- Branch: $(git branch --show-current)
- Commit: $(git rev-parse HEAD)

## Results
- Workflow Status: $(gh run view $RUN_ID --json conclusion -q .conclusion)
- Duration: $(gh run view $RUN_ID --json databaseId,createdAt,updatedAt | jq -r '(((.updatedAt | fromdate) - (.createdAt | fromdate)) / 60 | floor)') minutes
- Artifacts: $(gh release view $TEST_VERSION --json assets | jq '.assets | length')

## Platform Results
$(gh run view $RUN_ID --json jobs | jq -r '.jobs[] | "- \(.name): \(.conclusion)"')
EOF
```

## Integration with CI/CD

### Scheduled Testing
Add to `.github/workflows/test-release.yml`:

```yaml
name: Test Release Workflow
on:
  schedule:
    - cron: '0 0 * * 0'  # Weekly
  workflow_dispatch:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Test Release
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          ./scripts/test-release.sh
```

### PR Testing
Test on pull requests that modify release workflow:

```yaml
on:
  pull_request:
    paths:
      - '.github/workflows/release.yml'
      - 'build.gradle*'
      - 'gradle.properties'
```

## Security Considerations

### Test Environment Isolation
- Use separate test prefixes
- Avoid production version patterns
- Clean up immediately after testing

### Access Control
- Limit who can trigger test releases
- Use environment protection rules
- Audit workflow runs regularly

## Maintenance

### Regular Testing Schedule
- **Weekly**: Basic workflow execution
- **Monthly**: Full validation including binaries
- **Before releases**: Complete test suite

### Update Procedures
When updating the release workflow:
1. Test on feature branch
2. Validate all platforms
3. Performance comparison
4. Update this guide if needed

## Conclusion

Regular testing ensures reliable releases. Focus on:
- Workflow completion
- Artifact generation
- Security feature validation
- Cross-platform compatibility

The test process should be automated, repeatable, and well-documented.
