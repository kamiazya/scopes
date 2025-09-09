# CI/CD and Code Quality

This document describes the Continuous Integration (CI) setup, code quality tools, and coverage reporting for the Scopes project.

## Overview

The project uses GitHub Actions for continuous integration with the following quality assurance tools:

- **Test Coverage**: JaCoCo for code coverage measurement
- **Coverage Reporting**: Codecov for coverage visualization and PR comments
- **Static Analysis**: SonarCloud for comprehensive code quality analysis
- **Linting**: Detekt for Kotlin static analysis and ktlint for code formatting

## Workflows

### Test Workflow (`.github/workflows/test.yml`)

Runs on every push to `main` and pull requests:

- **Purpose**: Execute unit tests and generate coverage reports
- **Coverage**: Uploads to Codecov automatically for public repositories
- **Artifacts**: Test results and coverage reports are stored as build artifacts

### Code Quality Workflow (`.github/workflows/code-quality.yml`)

Runs on every push to `main` and pull requests:

- **Lint Job**: Runs ktlint and Detekt static analysis
- **SonarCloud Job**: Comprehensive analysis including:
  - Code coverage integration via JaCoCo
  - Code smells and bug detection
  - Security hotspot identification
  - Quality gate enforcement

## Code Coverage

### Local Coverage Generation

Generate coverage reports locally:

```bash
# Run tests and generate coverage for all modules
./gradlew test jacocoRootReport

# View HTML report
open build/reports/jacoco/jacocoRootReport/html/index.html
```

### Coverage Configuration

- **Tool**: JaCoCo plugin configured in root `build.gradle.kts`
- **Output**: XML reports for CI integration, HTML reports for local viewing
- **Aggregation**: `jacocoRootReport` task combines coverage from all Kotlin submodules

### Coverage Thresholds

Coverage reports are generated for:

- Line coverage
- Branch coverage
- Method coverage
- Class coverage

Currently no minimum thresholds are enforced, but this can be configured in the future.

## SonarCloud Integration

### Configuration

- **Config File**: `sonar-project.properties`
- **Organization**: `kamiazya`
- **Project Key**: `kamiazya_scopes`

### What SonarCloud Analyzes

- **Coverage**: Integrates JaCoCo XML reports
- **Code Quality**: Code smells, bugs, vulnerabilities
- **Maintainability**: Technical debt and complexity metrics
- **Detekt Results**: Kotlin-specific static analysis results

### Quality Gate

SonarCloud runs a Quality Gate check on every PR that:

- Ensures coverage doesn't decrease significantly
- Prevents introduction of new bugs or vulnerabilities
- Maintains code quality standards

## Dashboards and Links

### Codecov Dashboard
- **URL**: `https://codecov.io/gh/kamiazya/scopes`
- **Features**: Coverage trends, PR coverage diffs, file-level coverage

### SonarCloud Dashboard
- **URL**: `https://sonarcloud.io/project/overview?id=kamiazya_scopes`
- **Features**: Code quality metrics, security analysis, technical debt tracking

## Local Development

### Running Quality Checks Locally

```bash
# Run all tests with coverage
./gradlew test jacocoRootReport

# Run static analysis
./gradlew ktlintCheck detekt

# Auto-fix formatting issues
./gradlew ktlintFormat

# Run architecture compliance tests
./gradlew konsistTest
```

### Pre-commit Hooks

The project uses Lefthook for Git hooks. Quality checks run automatically before commits to ensure code quality.

## CI/CD Configuration

### Required Secrets

For full CI/CD functionality, the following GitHub secrets must be configured:

- `SONAR_TOKEN`: SonarCloud authentication token
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions

### Permissions

The workflows require the following permissions:

- `contents: read` - Access repository content
- `actions: read` - Access workflow artifacts
- `pull-requests: write` - Comment on PRs (for coverage reports)

## Troubleshooting

### Coverage Not Appearing

1. Verify JaCoCo reports are generated: `ls -la build/reports/jacoco/`
2. Check Codecov upload logs in the Actions workflow
3. Ensure the XML report path matches the configuration

### SonarCloud Analysis Failing

1. Verify `SONAR_TOKEN` secret is configured
2. Check that the project exists in SonarCloud
3. Ensure the organization name matches in `sonar-project.properties`

### Quality Gate Failures

1. Review SonarCloud dashboard for specific issues
2. Address code smells and bugs identified
3. Ensure coverage doesn't decrease significantly

## Future Enhancements

Potential improvements to consider:

- **Coverage Thresholds**: Enforce minimum coverage percentages
- **Performance Testing**: Add performance regression detection
- **Security Scanning**: Additional security vulnerability scanning
- **Dependency Updates**: Automated dependency update PRs
