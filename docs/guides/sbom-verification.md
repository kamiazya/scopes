# SBOM Verification Guide

This guide explains how to verify and use Software Bill of Materials (SBOM) files distributed with Scopes releases.

## Overview

Scopes releases include comprehensive SBOM files that provide:
- **Complete dependency inventory**: All direct and transitive dependencies
- **Vulnerability tracking**: Integration with security databases
- **License compliance**: Full license information for all components
- **Supply chain transparency**: Build-time dependency resolution

## SBOM Types

Starting with the GraalVM Inspect Tool integration, Scopes generates two complementary types of SBOMs:

### Build-time SBOM (From Gradle)
- **Format**: CycloneDX JSON and XML
- **Files**: `sbom-build-{platform}-{arch}.json`, `sbom-build-{platform}-{arch}.xml`
- **Source**: Generated from Gradle dependency analysis
- **Content**: Declared Java dependencies, build-time artifacts
- **Best for**: License compliance, development security scanning

### Binary SBOM (From Syft)
- **Format**: CycloneDX JSON
- **Files**: `sbom-image-{platform}-{arch}.cyclonedx.json`
- **Source**: Generated using Syft binary analysis tool
- **Content**: Binary analysis results from native executable
- **Best for**: Production security analysis, runtime vulnerability scanning

#### CycloneDX Format Advantages
- Native vulnerability database integration
- Real-time security analysis capabilities
- OWASP ecosystem compatibility
- Continuous security monitoring support

For detailed comparison of all SBOM types, see the [SBOM Types Guide](sbom-types-guide.md).

## Verification Steps

### 1. Automated Verification (Recommended)

Use our cross-platform verification scripts that include SBOM validation:

```bash
# Linux/macOS - Using environment variables (recommended)
export SCOPES_VERSION=v1.0.0
export SCOPES_AUTO_DOWNLOAD=true
export SCOPES_VERIFY_SBOM=true
curl -L -o verify-release.sh https://raw.githubusercontent.com/kamiazya/scopes/main/install/verify-release.sh
chmod +x verify-release.sh
./verify-release.sh  # SBOM verification enabled via environment

# Linux/macOS - Command line parameters
./verify-release.sh --download --version v1.0.0 --verify-sbom
```

```powershell
# Windows PowerShell - Using environment variables (recommended)
$env:SCOPES_VERSION='v1.0.0'
$env:SCOPES_AUTO_DOWNLOAD='true'
$env:SCOPES_VERIFY_SBOM='true'
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/kamiazya/scopes/main/install/Verify-Release.ps1" -OutFile "Verify-Release.ps1"
.\Verify-Release.ps1  # SBOM verification enabled via environment

# Windows PowerShell - Command line parameters
.\Verify-Release.ps1 -AutoDownload -Version v1.0.0 -VerifySBOM
```

### 2. Manual Download and Verify Checksums

```bash
# Download all available SBOM files and checksums
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/sbom-build-linux-x64.json
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/sbom-image-linux-x64.cyclonedx.json
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/binary-hash-linux-x64.txt

# Verify SBOM integrity (SBOM hashes are included in binary hash files)
sha256sum sbom-build-linux-x64.json
sha256sum sbom-image-linux-x64.cyclonedx.json
```

### 2. Validate SBOM Format

```bash
# Install CycloneDX CLI tools
npm install -g @cyclonedx/cli

# Validate build-time SBOM format compliance
cyclonedx validate sbom-build-linux-x64.json

# Validate binary SBOM format compliance
cyclonedx validate sbom-image-linux-x64.cyclonedx.json
```

### 3. SLSA Provenance Integration

SBOM files are included in the SLSA provenance generation process:

```bash
# Verify SBOM files are covered by SLSA provenance
slsa-verifier verify-artifact sbom-build-linux-x64.json \
  --provenance-path multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes

slsa-verifier verify-artifact sbom-image-linux-x64.cyclonedx.json \
  --provenance-path multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes
```

## Security Analysis

### Vulnerability Scanning

```bash
# Scan build-time dependencies for known vulnerabilities
cyclonedx analyze sbom-build-linux-x64.json

# Scan binary components (more accurate for production)
cyclonedx analyze sbom-image-linux-x64.cyclonedx.json

# Generate vulnerability report
cyclonedx analyze sbom-image-linux-x64.cyclonedx.json --output-format json > vulnerabilities.json
```

### License Compliance

```bash
# Extract license information from build-time SBOM
cyclonedx licenses sbom-build-linux-x64.json

# Generate comprehensive license report
cyclonedx licenses sbom-build-linux-x64.json --output-format csv > licenses.csv
```

## Integration with Security Tools

### OWASP Dependency-Track

```bash
# Upload build-time SBOM for development analysis
curl -X POST "http://dtrack-server/api/v1/bom" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d @sbom-build-linux-x64.json

# Upload binary SBOM for production analysis (preferred)
curl -X POST "http://dtrack-server/api/v1/bom" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d @sbom-image-linux-x64.cyclonedx.json
```

### Grype Vulnerability Scanner

```bash
# Scan build-time SBOM
grype sbom:sbom-build-linux-x64.json

# Scan binary SBOM (recommended for production)
grype sbom:sbom-image-linux-x64.cyclonedx.json

# Generate detailed report from binary SBOM
grype sbom:sbom-image-linux-x64.cyclonedx.json -o json > grype-report.json
```

### Syft Analysis

```bash
# Analyze build-time SBOM
syft scan sbom-build-linux-x64.json

# Analyze binary SBOM
syft scan sbom-image-linux-x64.cyclonedx.json

# Convert between formats
syft convert sbom-build-linux-x64.json -o spdx-json > sbom-build.spdx.json
```

## Continuous Monitoring

### Automated Vulnerability Alerts

```yaml
# Example GitHub Actions workflow for monitoring
name: SBOM Security Monitor
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  monitor:
    runs-on: ubuntu-latest
    steps:
    - name: Download latest SBOMs
      run: |
        wget https://github.com/kamiazya/scopes/releases/latest/download/sbom-build-linux-x64.json
        wget https://github.com/kamiazya/scopes/releases/latest/download/sbom-image-linux-x64.cyclonedx.json
    
    - name: Scan for vulnerabilities
      run: |
        # Scan build-time dependencies
        grype sbom:sbom-build-linux-x64.json --fail-on critical
        
        # Scan binary components (more accurate)
        grype sbom:sbom-image-linux-x64.cyclonedx.json --fail-on critical
```

### Policy Enforcement

```json
{
  "name": "Scopes Security Policy",
  "rules": [
    {
      "type": "vulnerability",
      "severity": "critical",
      "action": "fail"
    },
    {
      "type": "license",
      "allowed": ["Apache-2.0", "MIT", "BSD-3-Clause"],
      "action": "warn"
    }
  ]
}
```

## Best Practices

### For Users
1. **Always verify checksums** before using SBOM files
2. **Regularly scan** for new vulnerabilities
3. **Monitor license compliance** for your use case
4. **Store SBOM files** for audit and compliance requirements

### For Security Teams
1. **Integrate with existing tools** (SIEM, vulnerability management)
2. **Set up automated monitoring** for new releases
3. **Establish response procedures** for critical vulnerabilities
4. **Maintain audit trails** of SBOM verification activities

### For Compliance Teams
1. **Archive SBOM files** with release artifacts
2. **Document verification procedures** in compliance frameworks
3. **Regular compliance checks** against organizational policies
4. **Third-party audit support** with complete dependency information

## Troubleshooting

### SBOM Validation Fails
- Ensure you're using compatible tool versions
- Check for file corruption by verifying checksums
- Validate network connectivity for tool downloads

### Missing Dependencies
- Some dependencies may be build-time only
- Check both runtime and compile-time configurations
- Verify against actual deployed binaries

### Tool Compatibility Issues
- Use recommended tool versions from documentation
- Check tool-specific format requirements
- Consider format conversion when necessary

## Related Documentation

- [Security Verification Guide](security-verification.md) - SLSA provenance and integrity verification
- [Build Security Verification Guide](build-security-verification.md) - Binary security scanning
- [Dependency Security Guide](dependency-security.md) - GitHub native dependency security

## Resources

- [CycloneDX Official Documentation](https://cyclonedx.org/)  
- [OWASP Dependency-Track](https://dependencytrack.org/)
- [Grype Vulnerability Scanner](https://github.com/anchore/grype)
- [Syft SBOM Generator](https://github.com/anchore/syft)
- [SLSA Verification Guide](security-verification.md)

## Questions?

If you have questions about SBOM verification:
- Check our [Security Policy](../../SECURITY.md)
- Open an issue in the repository  
- Contact the maintainers directly

Remember: **SBOM files are living documents** - dependencies and vulnerabilities change over time, so regular verification is essential for maintaining security.
