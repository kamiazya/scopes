# Scopes Installation Scripts

This directory contains secure, modern installation scripts for Scopes with a streamlined bundle-based distribution approach that eliminates download confusion.

## 🎯 Bundle Distribution Architecture

Scopes uses **optimized bundle packages** to provide clear, efficient installation options:

```mermaid
graph TB
    Bundles[📦 Platform-Specific Bundles<br/>scopes-vX.X.X-{platform}-{arch}-bundle.tar.gz<br/><br/>• Platform binary<br/>• Installation script<br/>• SBOM & verification<br/>• ~20MB each]

    Unified[📦 Unified Distribution Package<br/>scopes-vX.X.X-dist.tar.gz<br/><br/>• All platform binaries<br/>• Verification tools<br/>• Documentation<br/>• SBOM files<br/>• ~260MB]

    Online[🌐 Online Installer<br/>install.sh → Downloads appropriate bundle]
    PlatformOffline[💾 Platform Bundle<br/>Direct extraction + install.sh]
    UnifiedOffline[💾 Unified Package<br/>Multi-platform + install.sh]

    Bundles --> Online
    Bundles --> PlatformOffline
    Unified --> UnifiedOffline

    Online --> Result[✅ Scopes Installed]
    PlatformOffline --> Result
    UnifiedOffline --> Result
```

## 📋 Download Options

Choose the best installation approach for your needs:

### 🎯 Recommended: Platform-Specific Bundles (~20MB each)
Perfect for most users - contains everything needed for your specific platform:

| Platform | Architecture | Bundle Name | Contents |
|----------|-------------|-------------|----------|
| Linux | x64 | `scopes-vX.X.X-linux-x64-bundle.tar.gz` | Binary + installer + SBOM + verification |
| Linux | ARM64 | `scopes-vX.X.X-linux-arm64-bundle.tar.gz` | Binary + installer + SBOM + verification |
| macOS | x64 (Intel) | `scopes-vX.X.X-darwin-x64-bundle.tar.gz` | Binary + installer + SBOM + verification |
| macOS | ARM64 (Apple Silicon) | `scopes-vX.X.X-darwin-arm64-bundle.tar.gz` | Binary + installer + SBOM + verification |
| Windows | x64 | `scopes-vX.X.X-win32-x64-bundle.zip` | Binary + installer + SBOM + verification |
| Windows | ARM64 | `scopes-vX.X.X-win32-arm64-bundle.zip` | Binary + installer + SBOM + verification |

### 🏢 Enterprise: Unified Distribution Package (~260MB)
For multi-platform deployments or offline enterprise installations:
- **`scopes-vX.X.X-dist.tar.gz`** - All platforms, architectures, and tools in one package

### 🔒 Security: SLSA Provenance
All releases include cryptographic supply chain verification:
- **`multiple.intoto.jsonl`** - SLSA Level 3 provenance for all artifacts

## 🚀 Quick Start

### Recommended Installation

Download the platform-specific bundle package for your system from GitHub Releases and follow the included installation instructions.

### Installation Options

#### Option 1: Platform-Specific Bundle (Recommended)
Download the appropriate bundle for your platform from GitHub Releases:
```bash
# Example for Linux x64
tar -xzf scopes-vX.X.X-linux-x64-bundle.tar.gz
cd scopes-vX.X.X-linux-x64-bundle
./install.sh
```

#### Option 2: Unified Distribution Package
For multi-platform or enterprise deployments:
```bash
tar -xzf scopes-vX.X.X-dist.tar.gz
cd scopes-vX.X.X-dist
./install.sh
```

## 🔒 Security First

All installation methods provide **integrated cryptographic verification**:

- ✅ **SHA256 Hash Verification** - Every binary verified against published hashes
- ✅ **SLSA Level 3 Provenance** - Supply chain integrity verified cryptographically  
- ✅ **HTTPS-Only Downloads** - All network communication over encrypted channels
- ✅ **Automatic Verification** - No manual steps required
- ✅ **Unified Package Integrity** - Single package checksum for simplified verification

## 📋 Available Files

### PowerShell Installation Script
- **`install.ps1`** - PowerShell installation for Windows (manual installation guide)

### Distribution Package Contents
- **`offline/install.sh`** - Installer included in distribution package
- **`offline/README.md`** - Distribution package documentation

### Documentation
- **`README.md`** - Installation guide and package overview
- **`verify-README.md`** - Verification procedures documentation

## ⚙️ Environment Variables

All scripts support environment-based configuration to eliminate repetitive parameters:

```bash
# Common configuration
export SCOPES_VERSION=v1.0.0              # Version to install (default: latest)
export SCOPES_INSTALL_DIR=/opt/scopes/bin  # Installation directory
export SCOPES_GITHUB_REPO=kamiazya/scopes  # GitHub repository
export SCOPES_FORCE_INSTALL=true          # Skip confirmation prompts
export SCOPES_VERBOSE=true                # Enable verbose output

# Security options (not recommended to change)
export SCOPES_SKIP_VERIFICATION=false     # Skip SLSA verification
```

### Platform-Specific Defaults

| Platform | Architecture | Default Install Directory | Requires sudo |
|----------|-------------|--------------------------|---------------|
| Linux    | x64, ARM64  | `/usr/local/bin`         | Usually yes   |
| macOS    | x64, ARM64 (Apple Silicon) | `/usr/local/bin`         | Usually yes   |
| Windows  | x64, ARM64  | `C:\Program Files\Scopes\bin` | Usually yes |

## 📖 Usage Examples

### Basic Installation
```bash
# Download platform-specific bundle from GitHub Releases
# Example for Linux x64
tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
cd scopes-v1.0.0-linux-x64-bundle
./install.sh
```

### Custom Installation Directory
```bash
# Install to user directory (no sudo required)
export SCOPES_INSTALL_DIR=$HOME/.local/bin
./install.sh
```

### Enterprise/CI Installation
```bash
# Automated installation for CI/CD
export SCOPES_FORCE_INSTALL=true
export SCOPES_INSTALL_DIR=/usr/local/bin
./install.sh
```

### Multi-Platform Enterprise Installation
```bash
# Download unified package for enterprise environments
tar -xzf scopes-v1.0.0-dist.tar.gz
cd scopes-v1.0.0-dist
./install.sh
```

## 🛠️ Advanced Usage

### Bundle Inspection Before Installation
```bash
# Download and extract platform bundle for inspection
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/scopes-v1.0.0-linux-x64-bundle.tar.gz
tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
cd scopes-v1.0.0-linux-x64-bundle

# Review the installation script and documentation
less install.sh
less README.md

# Run after inspection
./install.sh
```

### Standalone Verification

#### Bundle Verification (Recommended)
```bash
# Download platform bundle and provenance (example for Linux x64)
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/scopes-v1.0.0-linux-x64-bundle.tar.gz
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/multiple.intoto.jsonl

# Extract bundle and verify
tar -xzf scopes-v1.0.0-linux-x64-bundle.tar.gz
cd scopes-v1.0.0-linux-x64-bundle

# Verify using included hash file and SLSA provenance
sha256sum -c verification/binary-hash-linux-x64.txt

# Optional: Verify SLSA provenance (requires slsa-verifier)
slsa-verifier verify-artifact scopes-v1.0.0-linux-x64 \
  --provenance-path verification/multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes

# For other platforms, replace with appropriate bundle:
# macOS Intel: scopes-v1.0.0-darwin-x64-bundle.tar.gz
# macOS Apple Silicon: scopes-v1.0.0-darwin-arm64-bundle.tar.gz
# Windows x64: scopes-v1.0.0-win32-x64-bundle.tar.gz
```

#### Unified Package Verification
```bash
# For enterprise/multi-platform verification
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/scopes-v1.0.0-dist.tar.gz
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/multiple.intoto.jsonl

# Extract and verify unified package
tar -xzf scopes-v1.0.0-dist.tar.gz
cd scopes-v1.0.0-dist

# The installation script handles verification automatically
./install.sh

# Manual verification of specific binaries
sha256sum -c verification/binary-hash-*.txt
```

### Windows PowerShell Advanced
```powershell
# Download and extract Windows bundle
Invoke-WebRequest -Uri "https://github.com/kamiazya/scopes/releases/download/v1.0.0/scopes-v1.0.0-win32-x64-bundle.zip" -OutFile "scopes-bundle.zip"
Expand-Archive scopes-bundle.zip -DestinationPath .
cd scopes-*-bundle

# Review bundle contents and documentation
Get-Content README.md | Select-Object -First 50
Get-Content install.ps1 | Select-Object -First 50

# Run with custom parameters
.\install.ps1 -InstallDir "C:\Tools\Scopes" -Verbose

# Or with environment variables
$env:SCOPES_INSTALL_DIR='C:\Tools\Scopes'
.\install.ps1
```

## 🔧 Prerequisites

### Linux/macOS
- `wget` or browser (for downloading bundle packages)
- `tar` and `gzip` (for extracting bundle packages)
- `sha256sum` or `shasum` (for hash verification)
- `go` (optional, for SLSA verification)

### Windows
- **PowerShell**: PowerShell 5.1+ or PowerShell Core
- `certutil` (for hash verification - built into Windows)
- `go` (optional, for SLSA verification)

## 🚨 Security Considerations

### Trusted Sources
- Always download scripts from the official repository
- Verify the URL is `https://raw.githubusercontent.com/kamiazya/scopes/main/install/`
- Consider downloading and inspecting scripts before execution

### Verification Process
1. **Hash Verification**: SHA256 hash of binary matches published hash
2. **SLSA Provenance**: Cryptographic proof of build integrity
3. **Source Verification**: Confirms binary built from official repository
4. **Build Environment**: Verifies official GitHub Actions builder

### Network Security
- All downloads use HTTPS with certificate verification
- No sensitive information transmitted
- Scripts can work through corporate proxies

## 🐛 Troubleshooting

### Common Issues

#### Permission Denied
```bash
# Solution 1: Use sudo
sudo -E env PATH=$PATH bash -c 'curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh'

# Solution 2: Install to user directory
export SCOPES_INSTALL_DIR=$HOME/.local/bin
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

#### Network Issues
```bash
# Increase timeout and retry
export SCOPES_VERBOSE=true
curl -sSL --max-time 300 --retry 3 https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

#### Verification Failures
- **Hash Mismatch**: Download may be corrupted, retry installation
- **SLSA Failure**: Binary may be compromised, DO NOT install
- **Network Timeout**: Check network connectivity and proxy settings

#### slsa-verifier Installation Issues
```bash
# Install Go first, then retry
# Ubuntu/Debian
sudo apt update && sudo apt install golang-go

# macOS
brew install go

# Then retry installation
```

### Debug Mode
```bash
# Enable verbose output for troubleshooting
export SCOPES_VERBOSE=true
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

## 🏢 Enterprise Deployment

### Benefits of Bundle Distribution

1. **Optimized Downloads**: 92% size reduction - download only what you need (~20MB vs ~260MB)
2. **Clear User Experience**: No confusion about which files to download
3. **Platform-Specific**: Each bundle contains everything needed for your platform
4. **Enterprise Ready**: Unified package available for multi-platform deployments
5. **Consistent Verification**: Same security verification process for all packages
6. **Future-Proof**: Ready for daemon binary distribution in upcoming releases

### CI/CD Integration

#### GitHub Actions
```yaml
- name: Install Scopes
  env:
    SCOPES_VERSION: v1.0.0
    SCOPES_FORCE_INSTALL: 'true'
  run: |
    curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
    scopes --version
```

#### Docker
```dockerfile
# Option 1: Online installation
RUN curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | \
    SCOPES_FORCE_INSTALL=true SCOPES_INSTALL_DIR=/usr/local/bin sh

# Option 2: Offline installation (for reproducible builds)
COPY scopes-v1.0.0-dist.tar.gz /tmp/
RUN tar -xzf /tmp/scopes-v1.0.0-dist.tar.gz -C /tmp && \
    cd /tmp/scopes-v1.0.0-dist && \
    SCOPES_FORCE_INSTALL=true ./install.sh
```

### Security Scanning
All installation scripts and binaries can be scanned with standard security tools:
- Binary analysis with `grype`, `syft`, or similar tools
- Script analysis with `shellcheck`, `PSScriptAnalyzer`
- SBOM generation for compliance requirements

## 🔄 Updates

The installation scripts automatically install the latest version unless specified otherwise. For updates:

```bash
# Update to latest version
export SCOPES_FORCE_INSTALL=true  # Skip confirmation
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh

# Update to specific version
export SCOPES_VERSION=v1.1.0
export SCOPES_FORCE_INSTALL=true
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

## 🤝 Contributing

When modifying installation scripts:

1. **Test on all platforms** - Linux, macOS, Windows
2. **Maintain security properties** - All verification must remain intact
3. **Follow existing patterns** - Environment variables, error handling
4. **Update documentation** - Keep this README in sync
5. **Test edge cases** - Network failures, permission issues, etc.

## 📚 Related Documentation

- [Security Verification Guide](../docs/guides/security-verification.md)
- [SBOM Verification Guide](../docs/guides/sbom-verification.md)
- [User Story: Secure Manual Installation](../docs/explanation/user-stories/0002-secure-manual-installation.md)
- [Build Security Documentation](../docs/explanation/build-security.md)

## ❓ Support

For installation issues:
1. Check this README for common solutions
2. Enable verbose mode (`SCOPES_VERBOSE=true`) for detailed logs
3. Review the [Security Verification Guide](../docs/guides/security-verification.md)
4. Open an issue in the main repository
5. Contact maintainers for security-related concerns

---

**Remember**: When in doubt about security, always verify! The scripts are designed to make security verification automatic and transparent.
