# Security Verification Guide

This guide explains how to verify the authenticity and integrity of Scopes releases using SLSA (Supply-chain Levels for Software Artifacts) provenance.

## Overview

All Scopes releases include SLSA Level 3 provenance attestations that provide:

- **Build integrity**: Proof that binaries were built from the expected source code
- **Source authenticity**: Verification of the source repository and commit  
- **Build environment**: Details about the build environment and process
- **Non-repudiation**: Cryptographic signatures that cannot be forged

## Quick Verification

### Using SLSA Verifier (Recommended)

1. **Install slsa-verifier:**
   ```bash
   go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
   ```

2. **Download the binary and provenance:**
   - Download your platform's binary:
     - Linux x64: `scopes-v1.0.0-linux-x64`
     - Linux ARM64: `scopes-v1.0.0-linux-arm64`
     - macOS x64: `scopes-v1.0.0-darwin-x64`
     - macOS ARM64 (Apple Silicon): `scopes-v1.0.0-darwin-arm64`
     - Windows x64: `scopes-v1.0.0-win32-x64.exe`
     - Windows ARM64: `scopes-v1.0.0-win32-arm64.exe`
   - Download the provenance file (`multiple.intoto.jsonl`)

3. **Verify the binary:**
   ```bash
   # For Linux x64
   slsa-verifier verify-artifact scopes-v1.0.0-linux-x64 \
     --provenance-path multiple.intoto.jsonl \
     --source-uri github.com/kamiazya/scopes
   
   # For macOS ARM64 (Apple Silicon)
   slsa-verifier verify-artifact scopes-v1.0.0-darwin-arm64 \
     --provenance-path multiple.intoto.jsonl \
     --source-uri github.com/kamiazya/scopes
   ```

   **Expected output:**
   ```
   Verified signature against tlog
   Verified GitHub token identity
   Verified workflow trigger
   Verified provenance authenticity
   PASSED: Verified SLSA provenance
   ```

### One-Liner Installation (Recommended)

The easiest and most secure way to install Scopes is using our one-liner installation scripts that include automatic verification:

#### Linux/macOS/WSL (Bash)
```bash
# One-liner installation with automatic verification
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

#### Windows PowerShell
```powershell
# One-liner installation with automatic verification
iwr https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.ps1 | iex
```

#### Windows
For Windows users, please use PowerShell (available on Windows 10/11 by default) with the one-liner installation shown above.

### Standalone Verification Scripts

For manual verification of already downloaded files, we provide standalone verification scripts:

#### Linux/macOS/WSL (Bash)
```bash
# Method 1: Using environment variables (recommended)
export SCOPES_VERSION=v1.0.0
export SCOPES_AUTO_DOWNLOAD=true
curl -L -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/install/verify-release.sh
chmod +x verify-release.sh
./verify-release.sh  # No parameters needed!

# Method 2: Command line parameters
./verify-release.sh --download --version v1.0.0

# Verify local files (auto-detects architecture)
./verify-release.sh --binary scopes-v1.0.0-linux-x64 --provenance multiple.intoto.jsonl --hash-file binary-hash-linux-x64.txt

# For ARM64 systems (e.g., Apple Silicon Mac)
./verify-release.sh --binary scopes-v1.0.0-darwin-arm64 --provenance multiple.intoto.jsonl --hash-file binary-hash-darwin-arm64.txt
```

#### Windows (PowerShell)
```powershell
# Method 1: Using environment variables (recommended)
$env:SCOPES_VERSION='v1.0.0'
$env:SCOPES_AUTO_DOWNLOAD='true'
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/kamiazya/scopes/main/install/Verify-Release.ps1" -OutFile "Verify-Release.ps1"
.\Verify-Release.ps1  # No parameters needed!

# Method 2: Command line parameters
.\Verify-Release.ps1 -AutoDownload -Version v1.0.0

# Verify local files
.\Verify-Release.ps1 -BinaryPath scopes-v1.0.0-win32-x64.exe -HashFile binary-hash-win32-x64.txt -ProvenancePath multiple.intoto.jsonl
```


### Manual Hash Verification

If you prefer manual verification:

1. **Download the checksums file** (`binary-hash-<platform>-<arch>.txt`) from the release
2. **Calculate the hash** of your downloaded binary:
   ```bash
   # Linux x64
   sha256sum scopes-v1.0.0-linux-x64
   
   # Linux ARM64
   sha256sum scopes-v1.0.0-linux-arm64
   
   # macOS x64
   shasum -a 256 scopes-v1.0.0-darwin-x64
   
   # macOS ARM64 (Apple Silicon)
   shasum -a 256 scopes-v1.0.0-darwin-arm64
   
   # Windows x64
   certutil -hashfile scopes-v1.0.0-win32-x64.exe SHA256
   
   # Windows ARM64
   certutil -hashfile scopes-v1.0.0-win32-arm64.exe SHA256
   ```
3. **Compare** the calculated hash with the value in the checksum file

## What SLSA Verification Confirms

### ✅ Verified Information
- **Source Repository**: Confirms the binary was built from `github.com/kamiazya/scopes`
- **Commit Hash**: Shows the exact commit used for the build
- **Build Environment**: Verifies GitHub Actions runner details
- **Build Process**: Confirms the build followed the expected workflow
- **Timestamp**: When the build occurred
- **Builder Identity**: Confirms the build was performed by the official SLSA builder

### ⚠️ What It Doesn't Verify
- **Source Code Content**: SLSA verifies the build process, not the source code itself
- **Runtime Behavior**: Verification only covers the build process
- **Dependencies**: While build dependencies are recorded, their security isn't verified

## Advanced Verification

### Inspect Provenance Details

You can examine the provenance file to see detailed build information:

```bash
# Pretty print the provenance JSON
cat multiple.intoto.jsonl | jq '.'
```

Key fields to examine:
- `subject`: List of artifacts and their hashes
- `predicate.builder.id`: Should be the official SLSA GitHub generator
- `predicate.buildDefinition.buildType`: Build process type
- `predicate.buildDefinition.externalParameters`: Build configuration
- `predicate.runDetails.builder.id`: Builder environment details

### Verify Specific Commit

To verify that a binary was built from a specific commit:

```bash
slsa-verifier verify-artifact scopes-v1.0.0-linux-x64 \
  --provenance-path multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes \
  --source-tag v1.0.0
```

## Security Best Practices

### For Users
1. **Always verify** binaries before installation
2. **Check the source URI** matches the official repository
3. **Verify the builder** is the official SLSA GitHub generator
4. **Store provenance files** for audit trails

### For Developers
1. **Never bypass** the SLSA build process
2. **Protect signing keys** (handled automatically by GitHub)
3. **Monitor** for unexpected provenance patterns
4. **Regular audit** of build configurations

## Troubleshooting

### Verification Fails
If verification fails, **DO NOT use the binary**. Possible causes:
- Binary was tampered with
- Provenance file is corrupted
- Binary and provenance don't match
- Network issues during verification

### Missing Provenance
All official releases should include provenance. If missing:
- Check the release page for the `.intoto.jsonl` file
- Verify you're downloading from the official repository
- Contact maintainers if provenance is genuinely missing

### Tool Installation Issues
If you can't install `slsa-verifier`:
- Use the manual hash verification method
- Download pre-built slsa-verifier binaries from their releases
- Use alternative verification tools that support SLSA

## Software Bill of Materials (SBOM)

This release includes comprehensive SBOM files for complete dependency transparency:

### SBOM Files
- **CycloneDX Format**: `sbom-{platform}-{arch}.json` and `sbom-{platform}-{arch}.xml`
- **Purpose**: Complete inventory of all dependencies with vulnerability tracking
- **Integration**: Compatible with OWASP Dependency-Track, Grype, and other security tools

### SBOM Verification
```bash
# Verify SBOM integrity
sha256sum sbom-linux-x64.json
grep "sbom-linux-x64.json" binary-hash-linux-x64.txt

# Validate SBOM format
cyclonedx validate sbom-linux-x64.json

# Scan for vulnerabilities
grype sbom:sbom-linux-x64.json
```

For complete SBOM usage instructions, see the [SBOM Verification Guide](sbom-verification.md).

## Resources

- [SLSA Official Documentation](https://slsa.dev/)
- [SLSA Verifier Repository](https://github.com/slsa-framework/slsa-verifier)
- [SBOM Verification Guide](sbom-verification.md)
- [Build Security Verification Guide](build-security-verification.md)
- [GitHub Security Features](https://docs.github.com/en/actions/security-guides)
- [Supply Chain Security Best Practices](https://github.com/ossf/wg-best-practices-os-developers)

## Questions?

If you have questions about security verification:
- Check our [Security Policy](../../SECURITY.md) 
- Open an issue in the repository
- Contact the maintainers directly

Remember: **When in doubt, verify!** It's always better to be safe than sorry when it comes to software security.
