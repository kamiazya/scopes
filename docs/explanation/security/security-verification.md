# Security Verification Guide

This guide explains how to verify the authenticity and integrity of Scopes JAR distribution releases using SLSA (Supply-chain Levels for Software Artifacts) provenance and SHA256 checksums.

## Overview

All Scopes releases include comprehensive security verification mechanisms:

- **SHA256 Checksums**: Quick integrity verification for JAR files
- **SLSA Level 3 Provenance**: Cryptographic proof of build authenticity
- **Source Authenticity**: Verification of the source repository and commit
- **Build Environment**: Details about the build environment and process
- **Non-repudiation**: Cryptographic signatures that cannot be forged
- **SBOM Files**: Complete dependency inventory for vulnerability tracking

## Quick Verification

### Automated Installation with Verification (Recommended)

The installer scripts automatically verify SHA256 checksums and optionally verify SLSA provenance if `slsa-verifier` is available.

#### Linux/macOS/WSL
```bash
# Download JAR bundle
wget https://github.com/kamiazya/scopes/releases/latest/download/scopes-vX.X.X-jar-bundle.tar.gz
tar -xzf scopes-vX.X.X-jar-bundle.tar.gz
cd scopes-vX.X.X-jar-bundle

# Run installer with automatic verification
./install.sh
```

#### Windows
```powershell
# Download JAR bundle
Invoke-WebRequest -Uri "https://github.com/kamiazya/scopes/releases/latest/download/scopes-vX.X.X-jar-bundle.zip" -OutFile "scopes-bundle.zip"
Expand-Archive scopes-bundle.zip -DestinationPath .
cd scopes-vX.X.X-jar-bundle

# Run installer with automatic verification
.\install.ps1
```

The installer will:
1. ✅ Verify SHA256 checksum of scopes.jar
2. ✅ Optionally verify SLSA provenance (if slsa-verifier is installed)
3. ✅ Install verified JAR and wrapper scripts
4. ✅ Configure PATH automatically (with user consent)

### Manual SLSA Verification

1. **Install slsa-verifier:**
   ```bash
   go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
   ```

2. **Download and extract the JAR bundle:**
   ```bash
   # Download bundle
   wget https://github.com/kamiazya/scopes/releases/download/vX.X.X/scopes-vX.X.X-jar-bundle.tar.gz
   tar -xzf scopes-vX.X.X-jar-bundle.tar.gz
   cd scopes-vX.X.X-jar-bundle
   ```

3. **Verify the JAR file:**
   ```bash
   # Verify SLSA provenance
   slsa-verifier verify-artifact scopes.jar \
     --provenance-path verification/multiple.intoto.jsonl \
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

### Manual SHA256 Verification

For manual verification of the JAR file:

#### Linux/macOS/WSL (Bash)
```bash
# Extract JAR bundle
tar -xzf scopes-vX.X.X-jar-bundle.tar.gz
cd scopes-vX.X.X-jar-bundle

# Verify JAR file hash using included verification file
sha256sum -c verification/scopes.jar.sha256

# Alternative: manually compare
sha256sum scopes.jar
cat verification/scopes.jar.sha256
```

#### macOS (using shasum)
```bash
# Extract JAR bundle
tar -xzf scopes-vX.X.X-jar-bundle.tar.gz
cd scopes-vX.X.X-jar-bundle

# Verify JAR file hash
shasum -a 256 -c verification/scopes.jar.sha256
```

#### Windows (PowerShell)
```powershell
# Extract JAR bundle
Expand-Archive scopes-vX.X.X-jar-bundle.zip -DestinationPath .
cd scopes-vX.X.X-jar-bundle

# Calculate and compare hash
$actualHash = (Get-FileHash scopes.jar -Algorithm SHA256).Hash
$expectedHash = (Get-Content verification\scopes.jar.sha256).Split(' ')[0]

if ($actualHash -eq $expectedHash) {
    Write-Host "✓ SHA256 verification passed" -ForegroundColor Green
} else {
    Write-Host "✗ SHA256 verification failed" -ForegroundColor Red
    Write-Host "Expected: $expectedHash"
    Write-Host "Actual:   $actualHash"
}
```

## What SLSA Verification Confirms

### ✅ Verified Information
- **Source Repository**: Confirms the JAR was built from `github.com/kamiazya/scopes`
- **Commit Hash**: Shows the exact commit used for the build
- **Build Environment**: Verifies GitHub Actions runner details
- **Build Process**: Confirms the build followed the expected Gradle workflow
- **Timestamp**: When the build occurred
- **Builder Identity**: Confirms the build was performed by the official SLSA builder
- **Artifact Integrity**: Verifies the JAR file hasn't been tampered with

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

To verify that the JAR was built from a specific commit:

```bash
slsa-verifier verify-artifact scopes.jar \
  --provenance-path verification/multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes \
  --source-tag vX.X.X
```

## Security Best Practices

### For Users
1. **Always verify** JAR files before installation using SHA256 checksums
2. **Use installer scripts** for automatic verification when available
3. **Optionally verify SLSA provenance** for cryptographic proof of authenticity
4. **Check the source URI** matches the official repository (`github.com/kamiazya/scopes`)
5. **Verify the builder** is the official SLSA GitHub generator
6. **Store provenance files** for audit trails

### For Developers
1. **Never bypass** the SLSA build process
2. **Protect signing keys** (handled automatically by GitHub)
3. **Monitor** for unexpected provenance patterns
4. **Regular audit** of build configurations

## Troubleshooting

### Verification Fails
If verification fails, **DO NOT use the JAR file**. Possible causes:
- JAR file was tampered with or corrupted
- Provenance file is corrupted
- JAR and provenance don't match
- Network issues during download
- Incomplete download

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

JAR distribution bundles include comprehensive SBOM files for complete dependency transparency:

### SBOM Files
- **Source SBOM**: `sbom/scopes-sbom.json` and `sbom/scopes-sbom.xml` (Gradle dependencies)
- **Binary SBOM**: `sbom/scopes-binary-sbom.json` (Compiled JAR analysis with Syft)
- **Format**: CycloneDX - industry-standard SBOM format
- **Purpose**: Complete inventory of all dependencies with vulnerability tracking
- **Integration**: Compatible with OWASP Dependency-Track, Grype, and other security tools

### SBOM Verification
```bash
# Extract JAR bundle
tar -xzf scopes-vX.X.X-jar-bundle.tar.gz
cd scopes-vX.X.X-jar-bundle

# Validate source SBOM format
cyclonedx validate sbom/scopes-sbom.json

# Validate binary SBOM format
cyclonedx validate sbom/scopes-binary-sbom.json

# Scan for vulnerabilities in source dependencies
grype sbom:sbom/scopes-sbom.json

# Scan for vulnerabilities in compiled JAR
grype sbom:sbom/scopes-binary-sbom.json
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
