# Build Security Verification Guide

This guide explains how to verify the security of binaries built during the CI/CD process using automated security scanning and integrity checks.

## Overview

The build workflow includes comprehensive security verification for all platform binaries:

- **Cross-Platform Build**: Linux, macOS, and Windows native binaries
- **Integrity Verification**: SHA256 hash validation for all binaries
- **Vulnerability Scanning**: Grype analysis for known security issues
- **SBOM Generation**: Software Bill of Materials for binary analysis
- **Basic Security Checks**: File type, size, and string analysis

## Security Verification Process

### 1. Cross-Platform Native Build

Each platform builds a native binary independently:

```bash
# Build matrix covers:
- ubuntu-latest  (Linux x64)
- ubuntu-latest  (Linux ARM64 - via QEMU)
- macos-latest   (Darwin x64)
- macos-latest   (Darwin ARM64 - Apple Silicon)
- windows-latest (Windows x64)
```

### 2. Binary Integrity Checks

**Hash Generation**:
```bash
# Linux x64
sha256sum scopes-build-linux-x64

# Linux ARM64
sha256sum scopes-build-linux-arm64

# macOS x64
shasum -a 256 scopes-build-darwin-x64

# macOS ARM64 (Apple Silicon)
shasum -a 256 scopes-build-darwin-arm64

# Windows x64
certutil -hashfile scopes-build-win32-x64.exe SHA256
```

**Integrity Verification**:
- Each binary gets a SHA256 hash recorded during build
- Hash verification occurs in the security scan phase
- Any hash mismatch fails the build immediately

### 3. Basic Security Analysis

**File Type Verification**:
```bash
file scopes-build-linux-x64
# Expected: ELF 64-bit LSB executable, x86-64
```

**Executable Permission Check**:
- Linux/macOS binaries must have execute permissions
- Windows .exe files are validated for proper format

**String Analysis**:
- Scans for potentially suspicious strings
- Flags common command injection patterns
- Reports findings (may include false positives for CLI tools)

### 4. Vulnerability Scanning with Grype

**Binary Scanning**:
```bash
# Scan each platform binary
grype scopes-build-linux-x64 --output table
grype scopes-build-linux-x64 --output json
```

**Vulnerability Database**:
- Uses Anchore's vulnerability database
- Covers CVEs from multiple sources
- Updates automatically with latest threat data

### 5. SBOM Generation for Binaries

**Binary SBOM Creation**:
```bash
# Generate CycloneDX SBOM from binary
syft scopes-build-linux-x64 --output cyclonedx-json
```

**Binary SBOM vs Build SBOM**:
- **Build SBOM**: Dependencies used during compilation (from Gradle)
- **Binary SBOM**: Components actually embedded in the final binary
- Both perspectives provide different security insights

## Accessing Security Results

### GitHub Actions Artifacts

Security scan results are uploaded as artifacts:

```
security-scan-results/
├── scopes-build-linux-x64-grype-report.json
├── scopes-build-linux-arm64-grype-report.json
├── scopes-build-darwin-x64-grype-report.json
├── scopes-build-darwin-arm64-grype-report.json
├── scopes-build-win32-x64.exe-grype-report.json
├── scopes-build-linux-x64-sbom.json
├── scopes-build-linux-arm64-sbom.json
├── scopes-build-darwin-x64-sbom.json
├── scopes-build-darwin-arm64-sbom.json
└── scopes-build-win32-x64.exe-sbom.json
```

### Local Verification

**Download and Verify Build Binaries**:
```bash
# Download from GitHub Actions artifacts
# Verify hash matches recorded value
sha256sum scopes-build-linux-x64
cat binary-hash-linux-x64.txt

# Run local security scan
grype scopes-build-linux-x64
syft scopes-build-linux-x64
```

## Understanding Scan Results

### Grype Vulnerability Reports

**JSON Report Structure**:
```json
{
  "matches": [
    {
      "vulnerability": {
        "id": "CVE-2024-XXXX",
        "severity": "Medium"
      },
      "artifact": {
        "name": "component-name",
        "version": "1.2.3"
      }
    }
  ]
}
```

**Severity Levels**:
- **Critical**: Immediate action required
- **High**: Should be addressed quickly  
- **Medium**: Monitor and plan remediation
- **Low**: Track for future updates

### Binary SBOM Analysis

**Component Identification**:
- Native libraries linked into binary
- Runtime dependencies embedded
- Third-party components included

**Security Implications**:
- Vulnerable components in final binary
- License compliance for distributed code
- Supply chain transparency

## Security Policy Integration

### Build Failure Conditions

The build fails if:
- Binary integrity check fails (hash mismatch)
- Binary is not executable (Linux/macOS)
- Critical vulnerabilities found in binary scan
- SBOM generation fails for any platform

### Manual Review Triggers

Manual security review required for:
- New high/critical vulnerabilities detected
- Significant changes in binary SBOM components
- Unusual strings detected in binary analysis
- Platform-specific security scan failures

## Best Practices

### For Developers

1. **Monitor Build Logs**: Check security scan output regularly
2. **Review Artifacts**: Download and verify security reports
3. **Address Vulnerabilities**: Update dependencies causing security issues
4. **Test Locally**: Run similar scans in development environment

### For Security Teams

1. **Automated Monitoring**: Set up alerts for security scan failures
2. **Regular Review**: Examine weekly security scan trends
3. **Vulnerability Response**: Establish process for critical findings
4. **Tool Updates**: Keep Grype and Syft updated with latest databases

### For Release Management

1. **Pre-Release Scan**: Always verify clean security scans before release
2. **Binary Validation**: Confirm all platform binaries pass security checks
3. **Documentation**: Include security scan results in release notes
4. **Rollback Plan**: Prepare for releases with security issues

## Troubleshooting

### Build Failures

**Hash Mismatch**:
- Check for build environment contamination
- Verify no manual modifications to binaries
- Review build log for unexpected warnings

**Vulnerability Scan Failures**:
- Review specific CVEs reported
- Check if vulnerabilities affect runtime usage
- Consider adding suppressions for false positives

**SBOM Generation Issues**:
- Verify binary format compatibility
- Check Syft version compatibility
- Review binary size and structure

### False Positives

**Common False Positives**:
- CLI command strings flagged as suspicious
- Standard library components with old CVEs
- Build tool artifacts embedded in binary

**Handling Process**:
1. Investigate each finding thoroughly
2. Document rationale for dismissal
3. Consider suppressions for repeated false positives
4. Update scanning rules if needed

## Related Documentation

- [Security Verification Guide](security-verification.md) - SLSA provenance verification
- [SBOM Verification Guide](sbom-verification.md) - Build-time SBOM verification  
- [Dependency Security Guide](dependency-security.md) - GitHub native dependency security

## Questions?

If you have questions about build security verification:
- Check our [Security Policy](../../SECURITY.md)
- Open an issue in the repository
- Contact the security team directly

Remember: **Build security is supply chain security** - verifying our own builds is just as important as verifying external dependencies.
