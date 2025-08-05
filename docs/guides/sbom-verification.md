# SBOM Verification Guide

This guide explains how to verify and use Software Bill of Materials (SBOM) files distributed with Scopes releases.

## Overview

Scopes releases include comprehensive SBOM files that provide:
- **Complete dependency inventory**: All direct and transitive dependencies
- **Vulnerability tracking**: Integration with security databases
- **License compliance**: Full license information for all components
- **Supply chain transparency**: Build-time dependency resolution

## SBOM Formats

### CycloneDX (Recommended for Security)
- **Format**: JSON and XML
- **Focus**: Security-oriented with vulnerability tracking
- **Features**: VEX (Vulnerability Exploitability eXchange) support
- **Files**: `sbom-{platform}-{arch}.json`, `sbom-{platform}-{arch}.xml`

#### CycloneDX Advantages
- Native vulnerability database integration
- Real-time security analysis capabilities
- OWASP ecosystem compatibility
- Continuous security monitoring support

## Verification Steps

### 1. Download and Verify Checksums

```bash
# Download SBOM files and checksums
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/sbom-linux-x64.json
wget https://github.com/kamiazya/scopes/releases/download/v1.0.0/hashes.txt

# Verify SBOM integrity
sha256sum sbom-linux-x64.json
grep "sbom-linux-x64.json" hashes.txt
```

### 2. Validate SBOM Format

```bash
# Install CycloneDX CLI tools
npm install -g @cyclonedx/cli

# Validate SBOM format compliance
cyclonedx validate sbom-linux-x64.json
```

### 3. SLSA Provenance Integration

SBOM files are included in the SLSA provenance generation process:

```bash
# Verify SBOM is covered by SLSA provenance
slsa-verifier verify-artifact sbom-linux-x64.json \
  --provenance-path multiple.intoto.jsonl \
  --source-uri github.com/kamiazya/scopes
```

## Security Analysis

### Vulnerability Scanning

```bash
# Scan for known vulnerabilities
cyclonedx analyze sbom-linux-x64.json

# Generate vulnerability report
cyclonedx analyze sbom-linux-x64.json --output-format json > vulnerabilities.json
```

### License Compliance

```bash
# Extract license information
cyclonedx licenses sbom-linux-x64.json

# Generate license report
cyclonedx licenses sbom-linux-x64.json --output-format csv > licenses.csv
```

## Integration with Security Tools

### OWASP Dependency-Track

```bash
# Upload to Dependency-Track server
curl -X POST "http://dtrack-server/api/v1/bom" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d @sbom-linux-x64.json
```

### Grype Vulnerability Scanner

```bash
# Scan SBOM with Grype
grype sbom:sbom-linux-x64.json

# Generate detailed report
grype sbom:sbom-linux-x64.json -o json > grype-report.json
```

### Syft Analysis

```bash
# Analyze SBOM with Syft
syft scan sbom-linux-x64.json

# Convert between formats
syft convert sbom-linux-x64.json -o spdx-json > sbom.spdx.json
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
    - name: Download latest SBOM
      run: |
        wget https://github.com/kamiazya/scopes/releases/latest/download/sbom-linux-x64.json
    
    - name: Scan for vulnerabilities
      run: |
        grype sbom:sbom-linux-x64.json --fail-on critical
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