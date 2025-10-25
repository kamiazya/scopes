# Security Documentation

This directory contains comprehensive security documentation for the Scopes project, including verification procedures, build security measures, and dependency analysis.

## Available Documentation

### Release Verification
- **[Security Verification](./security-verification.md)** - Complete guide for verifying Scopes releases using SLSA provenance
- **[SBOM Verification](./sbom-verification.md)** - Software Bill of Materials verification and vulnerability scanning

### Build Security
- **[Build Security Verification](./build-security-verification.md)** - CI/CD pipeline security verification procedures
- **[Dependency Security](./dependency-security.md)** - Dependency scanning and security analysis

## Security Framework

Scopes implements a comprehensive security framework:

### Supply Chain Security
- **SLSA Level 3** compliance for all releases
- **Provenance attestations** for build verification
- **Signed releases** with cryptographic verification
- **SBOM generation** for complete dependency transparency

### Development Security
- **Dependency scanning** with automated vulnerability detection
- **Secret scanning** to prevent credential leaks
- **Code analysis** with security-focused linting
- **Architecture testing** to enforce security boundaries

## For End Users

If you're installing Scopes:

1. **[Security Verification Guide](./security-verification.md)** - How to verify downloaded JAR distributions
2. **Installer scripts** - Include automatic SHA256 and SLSA verification (recommended)

## For Developers

If you're contributing to Scopes:

1. **[Build Security Guide](./build-security-verification.md)** - Understanding the secure build process
2. **[Dependency Security](./dependency-security.md)** - Managing secure dependencies

## Security Policies

- [Security Policy](../../../SECURITY.md) - Vulnerability reporting and response
- [Contributing Guidelines](../../guides/development/) - Secure development practices

## Questions?

For security-related questions:
- Review the [Security Policy](../../../SECURITY.md)
- Contact maintainers for sensitive issues
- Open public issues for general security discussions
