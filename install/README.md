# Scopes Installation Scripts

This directory contains secure, modern installation scripts for Scopes that provide one-liner installation with integrated cryptographic verification.

## 🚀 Quick Start

### Linux/macOS (One-liner)
```bash
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

### Windows PowerShell (One-liner)
```powershell
iwr https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.ps1 | iex
```

### Windows
For Windows users, please use PowerShell (available on Windows 10/11 by default).

## 🔒 Security First

All installation scripts include **integrated cryptographic verification**:

- ✅ **SHA256 Hash Verification** - Every binary verified against published hashes
- ✅ **SLSA Level 3 Provenance** - Supply chain integrity verified cryptographically  
- ✅ **HTTPS-Only Downloads** - All network communication over encrypted channels
- ✅ **Automatic Verification** - No manual steps required

## 📋 Available Scripts

### Installation Scripts
- **`install.sh`** - Unix installation with integrated verification (Linux/macOS/WSL)
- **`install.ps1`** - PowerShell installation for Windows (and cross-platform PowerShell)

### Verification Scripts  
- **`verify-release.sh`** - Standalone verification for downloaded releases (Unix)
- **`Verify-Release.ps1`** - Standalone verification for PowerShell

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

| Platform | Default Install Directory | Requires sudo |
|----------|--------------------------|---------------|
| Linux    | `/usr/local/bin`         | Usually yes   |
| macOS    | `/usr/local/bin`         | Usually yes   |
| Windows  | `C:\Program Files\Scopes\bin` | Usually yes |

## 📖 Usage Examples

### Basic Installation
```bash
# Download and install latest version
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

### Custom Version
```bash
# Install specific version
export SCOPES_VERSION=v1.0.0
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

### Custom Directory (No sudo required)
```bash
# Install to user directory
export SCOPES_INSTALL_DIR=$HOME/.local/bin
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

### Enterprise/CI Installation
```bash
# Automated installation for CI/CD
export SCOPES_VERSION=v1.0.0
export SCOPES_FORCE_INSTALL=true
export SCOPES_INSTALL_DIR=/usr/local/bin
curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
```

### Fork/Custom Repository
```bash
# Install from forked repository
export SCOPES_GITHUB_REPO=myorg/scopes-fork
export SCOPES_VERSION=v1.0.0-custom
curl -sSL https://raw.githubusercontent.com/myorg/scopes-fork/main/install/install.sh | sh
```

## 🛠️ Advanced Usage

### Download and Inspect Before Running
```bash
# Download script for inspection
curl -sSL -o install.sh https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh

# Review the script
less install.sh

# Run after inspection
chmod +x install.sh
./install.sh
```

### Standalone Verification
```bash
# Download release files manually
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/scopes-v1.0.0-linux-x64
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/binary-hash-linux-x64.txt
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/multiple.intoto.jsonl

# Verify with standalone script
./verify-release.sh --binary scopes-v1.0.0-linux-x64 --hash-file binary-hash-linux-x64.txt --provenance multiple.intoto.jsonl
```

### Windows PowerShell Advanced
```powershell
# Download and inspect
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.ps1" -OutFile "install.ps1"
Get-Content install.ps1 | Select-Object -First 50  # Review first 50 lines

# Run with parameters
.\install.ps1 -Version v1.0.0 -InstallDir "C:\Tools\Scopes" -Verbose

# Or with environment variables
$env:SCOPES_VERSION='v1.0.0'
$env:SCOPES_INSTALL_DIR='C:\Tools\Scopes'
.\install.ps1
```

## 🔧 Prerequisites

### Linux/macOS
- `curl` or `wget` (for downloads)
- `sha256sum` or `shasum` (for hash verification)
- `go` (optional, for SLSA verification - will be auto-installed if available)

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
RUN curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | \
    SCOPES_FORCE_INSTALL=true SCOPES_INSTALL_DIR=/usr/local/bin sh
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
