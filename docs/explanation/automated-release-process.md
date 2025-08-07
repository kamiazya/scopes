# Automated Release Process

This document describes the automated release notes generation system implemented for Scopes.

## Overview

Scopes uses GitHub's native release notes generation combined with custom verification content to create comprehensive, standardized release notes for every release.

## How It Works

### 1. Automatic Categorization

Pull requests and commits are automatically categorized using labels defined in `.github/release.yml`:

- 🚀 **New Features**: `feature`, `enhancement`, `feat`, `new-feature`
- 🐛 **Bug Fixes**: `bug`, `bugfix`, `fix`, `hotfix`
- 📚 **Documentation**: `documentation`, `docs`
- 🔒 **Security**: `security`, `vulnerability`, `cve`
- ⚡ **Performance**: `performance`, `perf`, `optimization`
- 🧹 **Code Quality**: `refactor`, `refactoring`, `cleanup`, `code-quality`, `style`
- 🔧 **Infrastructure & CI/CD**: `ci`, `cd`, `infrastructure`, `build`, `workflow`, `github-actions`
- 📦 **Dependencies**: `dependencies`, `deps`, `dependency`
- 🔄 **Other Changes**: `chore`, `maintenance`, or catch-all for unlabeled items

### 2. Custom Security Content

Each release automatically includes:

- **Verification Instructions**: Quick one-liner installation with verification
- **Security Notice**: SLSA provenance and supply chain security information
- **Documentation Links**: Links to security guides and verification procedures
- **Artifact Information**: Details about checksums, SBOM files, and provenance

### 3. Release Notes Structure

The final release notes follow this structure:

```markdown
## 🔐 Verification Instructions
[Custom security content and quick verification]

## 🚀 What's Changed
[Auto-generated categorized changelog]

## Installation
[Standard installation instructions]

## Security & Verification
[Detailed SLSA and SBOM verification examples]
```

## For Maintainers

### Labeling Pull Requests

To ensure proper categorization, label your pull requests with appropriate labels:

```bash
# Examples
gh pr create --label "feature" --title "Add new CLI command"
gh pr create --label "bug" --title "Fix memory leak in task processing"
gh pr create --label "docs" --title "Update installation guide"
```

### Triggering Releases

Releases are triggered by pushing version tags:

```bash
# Create and push a release tag
git tag v1.0.0
git push origin v1.0.0

# Or for pre-releases
git tag v1.0.0-beta.1
git push origin v1.0.0-beta.1
```

### Excluding Content

To exclude certain PRs from release notes, use these labels:
- `skip-changelog`
- `duplicate`
- `invalid`
- `wontfix`

Bot accounts (dependabot, github-actions) are automatically excluded.

## Benefits

1. **Consistency**: Every release has the same structure and security information
2. **Automation**: No manual release notes creation required
3. **Security First**: Verification instructions are prominently displayed
4. **User-Friendly**: Clear categorization makes it easy to find relevant changes
5. **Maintainer-Friendly**: Simple labeling system for proper categorization

## Related Files

- `.github/release.yml` - Release notes configuration
- `.github/workflows/release.yml` - Release automation workflow
- `docs/guides/security-verification.md` - Security verification guide
- `docs/guides/sbom-verification.md` - SBOM verification guide
- `install/README.md` - Installation guide