# Security Policy

## Overview

Scopes is committed to providing a secure task management tool. As an AI-native, local-first application, we take security seriously while keeping things practical for individual developers.

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| Latest  | :white_check_mark: |
| < Latest | :x:               |

We currently support only the latest release with security updates. As this is an active development project, we recommend always using the most recent version.

## Reporting Vulnerabilities

### For General Issues

- Create a GitHub issue for non-sensitive security concerns
- Use the `security` label to help us prioritize

### For Sensitive Vulnerabilities

- Use [GitHub Security Advisories](https://github.com/kamiazya/scopes/security/advisories) for private reporting

### What to Include
When reporting a security issue, please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Any suggested fixes (if you have them)

## Response Process

1. **Acknowledgment**: We'll respond within 1 week
2. **Investigation**: We'll assess the issue and determine severity
3. **Fix**: We'll work on a fix and test it thoroughly
4. **Release**: We'll release the fix and notify reporters
5. **Disclosure**: Public disclosure after fix is available

## Security Features

Scopes includes several security measures:

### Supply Chain Security
- **SLSA Level 3 Compliance**: All releases include cryptographic provenance
- **Automated Dependency Scanning**: Via GitHub Dependabot and Dependency Review  
- **Software Bill of Materials (SBOM)**: Complete dependency transparency

For detailed usage instructions, see our security guides:
- [Security Verification Guide](./docs/guides/security-verification.md)
- [Dependency Security Guide](./docs/guides/dependency-security.md)
- [SBOM Verification Guide](./docs/guides/sbom-verification.md)

### AI Integration Security
- **Local-First**: AI interactions don't expose your private data by default
- **Configurable Privacy**: You control what data is shared with AI services
- **Transparent Processing**: Clear indication when AI features are active

### Binary Security
- **Native Compilation**: GraalVM native images for reduced attack surface
- **Automated Scanning**: Binary vulnerability scanning during builds
- **Cross-Platform Consistency**: Same security measures across all platforms

## Privacy and Data Protection

Since Scopes is local-first:
- **Your data stays local** unless you explicitly choose to sync
- **No telemetry** is collected without your consent
- **AI interactions** are clearly marked and configurable

## Best Practices for Users

- Keep Scopes updated to the latest version
- Review AI integration settings for your privacy needs
- Verify downloaded binaries using our SLSA provenance (see security guides)
- Report any suspicious behavior immediately

## Contact

- **GitHub Issues**: For general security questions
- **Security Advisories**: For sensitive vulnerability reports

## Acknowledgments

We appreciate security researchers and users who help improve Scopes' security. Contributors to security improvements will be acknowledged in release notes (unless they prefer anonymity).

---

**Note**: This security policy reflects our commitment to building secure software while maintaining the simplicity expected in personal development tools. We balance comprehensive security with practical usability.
