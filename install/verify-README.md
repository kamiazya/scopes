# Scopes Verification Scripts

This directory contains cross-platform verification scripts to validate the authenticity and integrity of Scopes releases using SLSA provenance and hash verification.

## Available Scripts

### 1. Bash Script (`verify-release.sh`)
**Platform Support:** Linux, macOS, Windows (Git Bash/WSL)

**Features:**
- Auto-detection of platform and architecture
- Automatic download of release files
- Cross-platform hash calculation
- SLSA provenance verification
- SBOM validation support
- Comprehensive error handling

**Usage:**
```bash
# Make executable
chmod +x verify-release.sh

# Auto-download and verify latest release
./verify-release.sh --download --version v1.0.0

# Verify local files
./verify-release.sh --binary scopes-v1.0.0-linux-x64 --provenance multiple.intoto.jsonl --hash-file binary-hash-linux-x64.txt

# Include SBOM verification
./verify-release.sh --download --version v1.0.0 --verify-sbom
```

### 2. PowerShell Script (`Verify-Release.ps1`)
**Platform Support:** Windows, macOS, Linux (PowerShell Core)

**Features:**
- Cross-platform PowerShell compatibility
- Automatic platform detection
- Built-in web request capabilities
- Rich error handling and colored output
- SBOM validation support

**Usage:**
```powershell
# Auto-download and verify
.\Verify-Release.ps1 -AutoDownload -Version v1.0.0

# Verify local files
.\Verify-Release.ps1 -BinaryPath scopes-v1.0.0-win32-x64.exe -HashFile binary-hash-win32-x64.txt

# Include SBOM verification
.\Verify-Release.ps1 -AutoDownload -Version v1.0.0 -VerifySBOM
```


## Common Parameters and Environment Variables

All scripts support these common parameters:

- `--version` / `-Version`: Release version to verify (e.g., v1.0.0)
- `--binary` / `-BinaryPath`: Path to binary file
- `--provenance` / `-ProvenancePath`: Path to provenance file
- `--hash-file` / `-HashFile`: Path to hash file
- `--download` / `-AutoDownload`: Auto-download release files
- `--skip-slsa` / `-SkipSLSA`: Skip SLSA verification
- `--skip-hash` / `-SkipHash`: Skip hash verification
- `--verify-sbom` / `-VerifySBOM`: Also verify SBOM files
- `--help` / `-Help`: Show help message

### Environment Variables

All scripts can be configured using environment variables, eliminating the need to repeat common settings:

- `SCOPES_VERSION`: Default version to verify
- `SCOPES_BINARY_PATH`: Default binary path
- `SCOPES_PROVENANCE_PATH`: Default provenance file path
- `SCOPES_HASH_FILE`: Default hash file path
- `SCOPES_VERIFY_SLSA`: Enable/disable SLSA verification (`true`/`false`)
- `SCOPES_VERIFY_HASH`: Enable/disable hash verification (`true`/`false`)
- `SCOPES_VERIFY_SBOM`: Enable/disable SBOM verification (`true`/`false`)
- `SCOPES_AUTO_DOWNLOAD`: Enable auto-download (`true`/`false`)
- `SCOPES_GITHUB_REPO`: GitHub repository (`owner/repo`)
- `SCOPES_PLATFORM`: Override platform detection (`linux`/`darwin`/`win32`)
- `SCOPES_ARCH`: Override architecture detection (`x64`/`arm64`)

## Prerequisites

### For Hash Verification
- **Linux**: `sha256sum` (usually pre-installed)
- **macOS**: `shasum` (pre-installed)
- **Windows**: `certutil` (pre-installed)

### For SLSA Verification
- **Go**: Required to install `slsa-verifier`
- **slsa-verifier**: Will be auto-installed if Go is available

```bash
# Install slsa-verifier manually
go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
```

### For SBOM Verification (Optional)
- **CycloneDX CLI**: For SBOM format validation

```bash
# Install CycloneDX CLI
npm install -g @cyclonedx/cli
```

## Security Best Practices

1. **Always verify scripts**: Before running, verify the scripts haven't been tampered with
2. **Use HTTPS**: Scripts download files over HTTPS only
3. **Verify checksums**: Scripts will fail if checksums don't match
4. **Check signatures**: SLSA provenance provides cryptographic verification
5. **Keep tools updated**: Ensure verification tools are up to date

## Examples

### Quick Verification (Recommended)

#### Using Environment Variables (Simplest)
```bash
# Set once, use multiple times
export SCOPES_VERSION=v1.0.0
export SCOPES_AUTO_DOWNLOAD=true

# Download and run verification script
curl -L -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/scripts/verify-release.sh
chmod +x verify-release.sh
./verify-release.sh  # No parameters needed!
```

#### Using Command Line Parameters
```bash
# Download and run verification script
curl -L -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/scripts/verify-release.sh
chmod +x verify-release.sh
./verify-release.sh --download --version v1.0.0
```

### Enterprise/CI Pipeline Usage

#### GitHub Actions with Environment Variables
```yaml
# Example GitHub Actions step
- name: Verify Scopes Binary
  env:
    SCOPES_VERSION: ${{ matrix.scopes-version }}
    SCOPES_AUTO_DOWNLOAD: 'true'
    SCOPES_VERIFY_SBOM: 'true'
  run: |
    curl -L -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/scripts/verify-release.sh
    chmod +x verify-release.sh
    ./verify-release.sh  # Configuration via environment variables
```

#### With Command Line Parameters
```yaml
- name: Verify Scopes Binary
  run: |
    curl -L -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/scripts/verify-release.sh
    chmod +x verify-release.sh
    ./verify-release.sh --download --version ${{ env.SCOPES_VERSION }}
```

### Advanced Verification

#### Using Environment Variables for Complex Setups
```bash
# Configure once for your organization
export SCOPES_GITHUB_REPO=your-org/scopes-fork
export SCOPES_VERIFY_SBOM=true
export SCOPES_VERSION=v1.0.0
export SCOPES_AUTO_DOWNLOAD=true

# All subsequent runs use these settings
./verify-release.sh  # Verify with SBOM from custom repo

# Override specific settings when needed
./verify-release.sh --version v1.0.1  # Different version, same other settings
```

#### Command Line Approach
```bash
# Verify everything including SBOM
./verify-release.sh --download --version v1.0.0 --verify-sbom

# Custom repository verification
./verify-release.sh --download --version v1.0.0 --repo your-org/your-fork

# Manual file verification
./verify-release.sh \
  --binary scopes-v1.0.0-linux-x64 \
  --provenance multiple.intoto.jsonl \
  --hash-file binary-hash-linux-x64.txt \
  --verify-sbom
```

#### Cross-Platform Environment Setup
```bash
# Linux/macOS/WSL
export SCOPES_VERSION=v1.0.0
export SCOPES_AUTO_DOWNLOAD=true

# Windows PowerShell
$env:SCOPES_VERSION='v1.0.0'
$env:SCOPES_AUTO_DOWNLOAD='true'
```

## Troubleshooting

### Script Download Issues
```bash
# If direct download fails, try with explicit SSL verification
curl -L --tlsv1.2 -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/scripts/verify-release.sh
```

### Permission Issues (Linux/macOS)
```bash
# Ensure script is executable
chmod +x verify-release.sh

# If running from mounted filesystem
chmod 755 verify-release.sh
```

### PowerShell Execution Policy (Windows)
```powershell
# Allow script execution (if needed)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Or run with bypass
powershell -ExecutionPolicy Bypass -File Verify-Release.ps1 -AutoDownload -Version v1.0.0
```

### Missing Dependencies
```bash
# Check if required tools are available
which sha256sum  # Linux
which shasum     # macOS
which certutil   # Windows (in Git Bash)
which go         # For SLSA verification
which slsa-verifier  # Should be in PATH after Go install
```

## Support

For issues with the verification scripts:
1. Check our [Security Verification Guide](../docs/guides/security-verification.md)
2. Review [SBOM Verification Guide](../docs/guides/sbom-verification.md)  
3. Open an issue in the main repository
4. Contact the maintainers directly

## Contributing

When updating these scripts:
1. Test on all supported platforms
2. Update documentation accordingly
3. Follow existing error handling patterns
4. Maintain backward compatibility where possible