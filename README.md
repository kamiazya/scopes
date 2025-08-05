# Configuration Files Directory

This directory contains configuration files for various development and security tools used in the Scopes project.

## Files

### Security Configuration

- **`owasp-suppression.xml`**: OWASP Dependency Check suppressions
  - Contains justified suppressions for false positive vulnerabilities
  - Each suppression includes detailed notes explaining the rationale
  - Temporary suppressions include expiration dates
  - Review quarterly for expired suppressions

### Code Quality Configuration

- **`detekt.yml`**: Detekt static analysis configuration
  - Kotlin code style and quality rules
  - Custom rule configurations for the project
  - Integrated with CI/CD pipeline for automated checking

### Git Hooks Configuration

- **`lefthook.yml`**: Git hooks configuration
  - Pre-commit hooks for code formatting and validation
  - Pre-push hooks for quality checks
  - Ensures consistent code quality across all commits

## Usage

These configuration files are automatically used by their respective tools:

```bash
# OWASP Dependency Check (uses etc/owasp-suppression.xml)
./gradlew dependencyCheckAnalyze

# Detekt static analysis (uses etc/detekt.yml)  
./gradlew detekt

# Lefthook git hooks (uses etc/lefthook.yml)
lefthook install
```

## Maintenance

### Security Suppressions
- Review suppressions quarterly via automated GitHub issues
- Remove or update expired suppressions
- Add clear justifications for new suppressions
- Prefer dependency updates over suppressions when possible

### Code Quality Rules
- Update detekt rules based on team feedback
- Align with project coding standards
- Review rule effectiveness regularly

### Git Hooks
- Keep hooks lightweight and fast
- Update hook configurations as tools evolve
- Ensure hooks work across different development environments

## Related Documentation

- [Security Dependency Check Guide](../docs/guides/security-dependency-check.md)
- [Coding Standards](../docs/guides/coding-standards.md)
- [Development Guidelines](../docs/guides/development-guidelines.md)
