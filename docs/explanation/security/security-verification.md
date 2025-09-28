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

2. **Download the bundle and provenance:**
   - Download your platform's bundle package:
     - Linux x64: `scopes-v1.0.0-linux-x64-bundle.tar.gz`
     - Linux ARM64: `scopes-v1.0.0-linux-arm64-bundle.tar.gz`
     - macOS x64: `scopes-v1.0.0-darwin-x64-bundle.tar.gz`
     - macOS ARM64 (Apple Silicon): `scopes-v1.0.0-darwin-arm64-bundle.tar.gz`
     - Windows x64: `scopes-v1.0.0-win32-x64-bundle.zip`
     - Windows ARM64: `scopes-v1.0.0-win32-arm64-bundle.zip`
   - Or download the unified package: `scopes-v1.0.0-dist.tar.gz`
   - Download the provenance file (`multiple.intoto.jsonl`)

3. **Extract and verify the binary:**
   ```bash
   # Extract the bundle (example for Linux x64)
   tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
   cd scopes-v1.0.0-linux-x64-bundle

   # Verify the binary using SLSA verifier
   slsa-verifier verify-artifact scopes-v1.0.0-linux-x64 \
     --provenance-path ../multiple.intoto.jsonl \
     --source-uri github.com/kamiazya/scopes

   # For macOS ARM64 (Apple Silicon)
   tar -xzf scopes-v1.0.0-darwin-arm64-bundle.tar.gz
   cd scopes-v1.0.0-darwin-arm64-bundle
   slsa-verifier verify-artifact scopes-v1.0.0-darwin-arm64 \
     --provenance-path ../multiple.intoto.jsonl \
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

### Platform Bundle Installation (Recommended)

The most secure and efficient way to install Scopes is using platform-specific bundle packages that include automatic verification:

#### Linux/macOS/WSL
```bash
# Download platform-specific bundle (example for Linux x64)
wget https://github.com/kamiazya/scopes/releases/latest/download/scopes-v1.0.0-linux-x64-bundle.tar.gz
tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
cd scopes-v1.0.0-linux-x64-bundle

# Run installation with automatic verification
./install.sh
```

#### Windows
```powershell
# Download Windows bundle
Invoke-WebRequest -Uri "https://github.com/kamiazya/scopes/releases/latest/download/scopes-v1.0.0-win32-x64-bundle.zip" -OutFile "scopes-bundle.zip"
Expand-Archive scopes-bundle.zip -DestinationPath .
cd scopes-v1.0.0-win32-x64-bundle

# Run installation with automatic verification
.\install.ps1
```

#### Available Platform Bundles
- Linux x64: `scopes-vX.X.X-linux-x64-bundle.tar.gz`
- Linux ARM64: `scopes-vX.X.X-linux-arm64-bundle.tar.gz`
- macOS x64: `scopes-vX.X.X-darwin-x64-bundle.tar.gz`
- macOS ARM64: `scopes-vX.X.X-darwin-arm64-bundle.tar.gz`
- Windows x64: `scopes-vX.X.X-win32-x64-bundle.zip`
- Windows ARM64: `scopes-vX.X.X-win32-arm64-bundle.zip`

### Manual Bundle Verification

For manual verification of downloaded bundle packages:

#### Linux/macOS/WSL (Bash)
```bash
# Extract and verify platform bundle
tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
cd scopes-v1.0.0-linux-x64-bundle

# Verify binary hash using included verification files
sha256sum -c verification/binary-hash-linux-x64.txt

# Verify SLSA provenance (requires slsa-verifier)
slsa-verifier verify-artifact scopes-v1.0.0-linux-x64 \
  --provenance-path verification/multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes

# For ARM64 systems (e.g., Apple Silicon Mac)
tar -xzf scopes-v1.0.0-darwin-arm64-bundle.tar.gz
cd scopes-v1.0.0-darwin-arm64-bundle
sha256sum -c verification/binary-hash-darwin-arm64.txt
slsa-verifier verify-artifact scopes-v1.0.0-darwin-arm64 \
  --provenance-path verification/multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes
```

#### Windows (PowerShell)
```powershell
# Extract and verify Windows bundle
Expand-Archive scopes-v1.0.0-win32-x64-bundle.zip -DestinationPath .
cd scopes-v1.0.0-win32-x64-bundle

# Verify binary hash using included verification files
certutil -hashfile scopes-v1.0.0-win32-x64.exe SHA256
Get-Content verification\binary-hash-win32-x64.txt

# Verify SLSA provenance (requires slsa-verifier)
# Install slsa-verifier first if not available
slsa-verifier verify-artifact scopes-v1.0.0-win32-x64.exe `
  --provenance-path verification\multiple.intoto.jsonl `
  --source-uri github.com/kamiazya/scopes
```


### Manual Hash Verification

If you prefer manual verification:

1. **Extract the bundle** and find the checksums file (`binary-hash-<platform>-<arch>.txt`) inside
2. **Calculate the hash** of the binary from the bundle:
   ```bash
   # Extract bundle first (example for Linux x64)
   tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
   cd scopes-v1.0.0-linux-x64-bundle

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
3. **Compare** the calculated hash with the value in the included checksum file

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
- **Location**: Included in each platform-specific bundle and unified distribution package
- **Purpose**: Complete inventory of all dependencies with vulnerability tracking
- **Integration**: Compatible with OWASP Dependency-Track, Grype, and other security tools

### SBOM Verification
```bash
# Extract bundle and verify SBOM integrity
tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
cd scopes-v1.0.0-linux-x64-bundle

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
