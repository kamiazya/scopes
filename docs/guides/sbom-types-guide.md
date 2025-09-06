# SBOM Types Guide

This guide explains the different types of Software Bill of Materials (SBOMs) generated for Scopes releases and their intended use cases.

## Overview

Scopes generates two complementary types of SBOMs to provide comprehensive supply chain transparency:

1. **Build-time SBOM** - Generated from Gradle build process
2. **Binary SBOM** - Generated from final native binary using Syft

## SBOM Types

### Build-time SBOM

**Files**: `sbom-build-{platform}-{arch}.json`, `sbom-build-{platform}-{arch}.xml`

Generated using CycloneDX Gradle plugin during the build process.

**Contents**:
- Java dependencies declared in Gradle
- Build-time artifacts and libraries
- Maven/Gradle dependency tree
- Metadata from `pom.xml` and `build.gradle.kts` files

**Use Cases**:
- License compliance analysis
- Vulnerability scanning of declared dependencies
- Build environment auditing
- Supply chain risk assessment

**Limitations**:
- May include dependencies that are optimized out during native compilation
- Does not reflect the final native binary composition
- Cannot detect runtime-only or dynamically linked components

### Binary SBOM

**Files**: `sbom-image-{platform}-{arch}.cyclonedx.json`

Generated using Syft binary analysis tool from the final compiled native binary.

**Contents**:
- Binary analysis of native executable
- Detected libraries and dependencies within the binary
- File signatures and package metadata
- Platform-specific native dependencies
- Components actually included in the final native binary

**Use Cases**:
- Final binary composition analysis
- Runtime vulnerability assessment
- Compliance verification for distributed binaries
- Security scanning of actual deployed artifacts
- Production security analysis

**Advantages**:
- Reflects the actual binary content after GraalVM optimizations
- Cross-platform binary analysis
- Industry-standard CycloneDX format
- Open-source tool with active community support
- No licensing requirements

## Verification

All SBOM types can be verified using the provided verification scripts:

### Linux/macOS
```bash
./install/verify-release.sh --verify-sbom --version v1.0.0 --download
```

### Windows PowerShell
```powershell
.\install\Verify-Release.ps1 -VerifySBOM -Version v1.0.0 -AutoDownload
```

### Manual Verification

#### CycloneDX Validation
```bash
# Install CycloneDX CLI
npm install -g @cyclonedx/cli

# Validate build-time SBOM
cyclonedx validate sbom-build-linux-x64.json

# Validate binary SBOM  
cyclonedx validate sbom-image-linux-x64.cyclonedx.json
```

#### Hash Verification
SHA256 hashes for all SBOM files are included in release assets:
- Check `binary-hash-{platform}-{arch}.txt` files
- Compare against calculated hashes

## Integration Examples

### Dependency-Track Integration
```bash
# Upload build-time SBOM
curl -X POST "http://dtrack-server/api/v1/bom" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d @sbom-build-linux-x64.json

# Upload binary SBOM for runtime analysis
curl -X POST "http://dtrack-server/api/v1/bom" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d @sbom-image-linux-x64.cyclonedx.json
```

### Vulnerability Scanning
```bash
# Scan with Grype
grype sbom:sbom-build-linux-x64.json
grype sbom:sbom-image-linux-x64.cyclonedx.json

# Scan with Syft
syft scan sbom-build-linux-x64.json
syft scan sbom-image-linux-x64.cyclonedx.json
```

## Best Practices

### For Development Teams
- Use **build-time SBOM** for:
  - License compliance during development
  - Dependency management and updates
  - Build process auditing

- Use **binary SBOM** for:
  - Final security assessment before release
  - Runtime vulnerability analysis
  - Customer compliance requirements

### For Security Teams
- Analyze both SBOM types for complete coverage
- Prioritize binary SBOM for production security scanning
- Use build-time SBOM for development pipeline security

### For Compliance Officers
- Binary SBOM provides the most accurate compliance picture
- Build-time SBOM useful for development process compliance
- Both SBOMs together provide comprehensive audit trail

## Troubleshooting

### SBOM Validation Failures
- **Cause**: Malformed JSON or missing required fields
- **Solution**: Re-download from official releases or regenerate

### Hash Mismatches
- **Cause**: File corruption during download or generation
- **Solution**: Re-download files and verify against official hashes

### Missing Binary SBOM
- **Cause**: Syft tool not available during build
- **Solution**: Verify Syft installation in CI environment

## References

- [CycloneDX Specification](https://cyclonedx.org/)
- [SPDX Specification](https://spdx.dev/)
- [Syft SBOM Generator](https://github.com/anchore/syft)
- [SLSA Provenance Guide](../security-verification.md)