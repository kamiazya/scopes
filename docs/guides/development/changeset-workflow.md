# Changeset Workflow Guide

This guide explains how to use Changesets for version management and automated releases in the Scopes project.

## Overview

Scopes uses [Changesets](https://github.com/changesets/changesets) to automate version management and releases. Since this is a Kotlin project that produces binary releases (not npm packages), we use a custom workflow that integrates Changesets with our existing GitHub Actions release pipeline.

## Development Workflow

### 1. Making Changes

When you make changes to the codebase that affect users, you need to create a changeset:

```bash
# Create a new changeset
pnpm changeset

# Or use the full command
pnpm changeset add
```

This will prompt you to:
- Select the type of change (patch, minor, major)
- Write a summary of the change for the changelog

### 2. Changeset Files

Changesets are stored as markdown files in `.changeset/`. Each file contains:
- Frontmatter specifying the version bump type
- A description of the change

Example changeset file:
```markdown
---
"scopes": minor
---

Add support for hierarchical context filtering with AND/OR operators
```

### 3. Pull Request Process

1. **Create PR**: Include your changeset file in the PR
2. **Changeset Bot**: The bot will comment on your PR confirming the changeset is present
3. **Review**: Code review happens as usual
4. **Merge**: When merged to main, the version workflow triggers

### 4. Automated Release Process

After merging a PR with changesets:

1. **Version PR Creation**: A new PR titled "Release new version" is automatically created
2. **Version PR Contents**:
   - Updated `package.json` version
   - Generated `CHANGELOG.md` entries
   - Consumed changeset files are removed
3. **Version PR Merge**: When this PR is merged, CI creates and pushes a SemVer tag `vX.Y.Z`
4. **Release Pipeline**: The `Release` workflow triggers on the tag push and builds binaries, SBOM, SLSA, and publishes a GitHub Release

## Available Scripts

```bash
# Create a changeset
pnpm changeset

# Check changeset status
pnpm changeset:status

# Generate version updates (done automatically)
pnpm version-packages

# Create git tags (internal to Changesets; CI creates the vX.Y.Z tag)
pnpm tag
```

## Changeset Types

### Patch (0.0.X)
- Bug fixes
- Documentation updates
- Internal refactoring that doesn't affect the API

### Minor (0.X.0)
- New features
- New CLI commands or options
- Backwards-compatible changes

### Major (X.0.0)
- Breaking changes
- CLI command removals or incompatible changes
- API breaking changes

## Bot Installation

The [Changeset Bot](https://github.com/apps/changeset-bot) is installed on this repository to:
- Check if PRs include appropriate changesets
- Provide helpful comments and links to create changesets
- Ensure consistent versioning practices

## Configuration

### Changeset Config (`.changeset/config.json`)
```json
{
  "changelog": ["@changesets/changelog-github", { "repo": "kamiazya/scopes" }],
  "access": "public",
  "baseBranch": "main"
}
```

### Key Features
- **GitHub Changelog**: Automatically links to PRs and contributors
- **Public Access**: Appropriate for open source projects
- **Main Branch**: Uses `main` as the base branch for comparisons

## Workflow Files

### Version and Release (`.github/workflows/version-and-release.yml`)
- **Trigger**: Push to main branch
- **Purpose**: Create version PRs; after merge, create and push `vX.Y.Z` tags
- **Actions**: Uses `changesets/action@v1`

### Release (`.github/workflows/release.yml`)
- **Trigger**: Tag push (`v*.*.*` and `v*.*.*-*`), or manual `workflow_dispatch`
- **Purpose**: Build and publish binaries
- **Integration**: Runs automatically when the CI pushes a version tag

## Special Considerations

### Non-NPM Project
Since Scopes is a Kotlin project, we don't publish to npm. Instead:
- Package is marked as `"private": true`
- The Changesets action still runs `pnpm tag` (creates package-style tags like `scopes@1.2.3` for traceability)
- CI then creates and pushes the SemVer tag `vX.Y.Z`, which triggers the release
- Release workflow builds native binaries for distribution

### Pre-releases
We support pre-releases with Changesets pre mode and SemVer pre tags.

1. Enter pre mode (example: release candidate series):
```bash
pnpm changeset pre enter rc
```
2. Create changesets as usual and merge PRs
3. Merge the auto-generated Version PR for each iteration
4. CI creates and pushes tags like `v1.2.3-rc.1`, which trigger the Release workflow
5. Exit pre mode when ready for stable:
```bash
pnpm changeset pre exit
```

Artifacts are named using the numeric version (without leading `v`), for example:
- Binary: `scopes-1.2.3-linux-x64`
- Offline package: `scopes-1.2.3-dist.tar.gz`

### Existing Build Pipeline
The changeset workflow integrates with the existing sophisticated build pipeline:
- Multi-platform native compilation (Linux, macOS, Windows)
- SLSA Level 3 provenance generation
- SBOM generation
- GitHub release creation

## Troubleshooting

### Missing Changesets
If the bot indicates a changeset is missing:
1. Run `pnpm changeset` to create one
2. Commit and push the changeset file
3. The bot will update its status

### Empty Changesets
For changes that don't require version bumps (tests, CI, etc.):
```bash
pnpm changeset --empty
```

### Version PR Issues
If the version PR has conflicts:
1. Pull the latest main branch
2. The workflow will automatically update the PR

## References

- [Changesets Documentation](https://github.com/changesets/changesets)
- [Changeset Bot](https://github.com/apps/changeset-bot)
- [GitHub Actions Integration](https://github.com/changesets/action)
