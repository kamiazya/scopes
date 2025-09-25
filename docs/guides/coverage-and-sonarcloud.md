# Code Coverage and SonarCloud Integration

This guide explains how to use JaCoCo code coverage and SonarCloud quality analysis in the Scopes project.

## Overview

The project uses:
- **JaCoCo** for code coverage measurement
- **JaCoCo Report Aggregation** for multi-module coverage consolidation
- **SonarCloud** for continuous code quality and security analysis

## Local Development

### Running Tests with Coverage

To run all tests and generate coverage reports:

```bash
# Run tests with individual module coverage
./gradlew test jacocoTestReport

# Run tests and generate aggregated coverage report
./gradlew testWithCoverage

# View aggregated HTML report
open coverage-report/build/reports/jacoco/testCodeCoverageReport/html/index.html
```

### Running SonarQube Analysis Locally

To run a complete analysis with coverage:

```bash
# Set your SonarCloud token (get from https://sonarcloud.io/account/security)
export SONAR_TOKEN=your_token_here

# Run full analysis
./gradlew sonarqubeWithCoverage
```

This will:
1. Run all tests
2. Generate JaCoCo coverage reports
3. Run Detekt static analysis
4. Upload results to SonarCloud

## Coverage Reports

### Individual Module Reports

Each module generates its own coverage report:
- Location: `{module}/build/reports/jacoco/test/html/index.html`
- Format: HTML, XML

### Aggregated Report

The `coverage-report` module aggregates coverage from all modules:
- Location: `coverage-report/build/reports/jacoco/testCodeCoverageReport/`
- Formats:
  - HTML: `html/index.html`
  - XML: `testCodeCoverageReport.xml` (used by SonarCloud)

## CI/CD Integration

### GitHub Actions Workflow

The project includes a dedicated SonarCloud workflow (`.github/workflows/sonarcloud.yml`) that:
1. Runs on every push to main and pull request
2. Executes tests with coverage
3. Generates aggregated reports
4. Uploads results to SonarCloud

### Required Secrets

Configure these in GitHub repository settings:
- `SONAR_TOKEN`: Your SonarCloud authentication token
  - Get from: https://sonarcloud.io/account/security
  - Add in: Settings → Secrets → Actions

## SonarCloud Configuration

### Project Setup

1. Go to https://sonarcloud.io
2. Import your GitHub repository
3. Configure analysis method as "GitHub Actions"
4. Note your project key and organization

### Quality Gates

SonarCloud enforces quality gates for:
- Code coverage (default: 60% minimum)
- Code duplication
- Security vulnerabilities
- Code smells
- Bugs

### Viewing Results

Access your project dashboard at:
```
https://sonarcloud.io/project/overview?id=kamiazya_scopes
```

## Coverage Exclusions

The following are excluded from coverage:
- Test files (`*Test.kt`, `*Spec.kt`)
- Generated code (`**/generated/**`)
- Build directories (`**/build/**`)

## Gradle Tasks Reference

| Task | Description |
|------|-------------|
| `test` | Run unit tests |
| `jacocoTestReport` | Generate coverage report for a module |
| `testWithCoverage` | Run all tests and generate all coverage reports |
| `:coverage-report:testCodeCoverageReport` | Generate aggregated coverage report |
| `sonarqube` | Run SonarQube analysis |
| `sonarqubeWithCoverage` | Complete analysis with coverage |

## Troubleshooting

### No Coverage Data

If coverage shows 0%:
1. Ensure tests are actually running: `./gradlew test --info`
2. Check JaCoCo data files exist: `find . -name "*.exec"`
3. Verify test task configuration includes JaCoCo

### SonarCloud Authentication Failed

1. Verify token is set: `echo $SONAR_TOKEN`
2. Check token permissions in SonarCloud
3. Ensure token is not expired

### Module Not Included in Coverage

1. Check module is listed in `coverage-report/build.gradle.kts`
2. Verify module has Kotlin plugin applied
3. Ensure module has tests that execute

## Best Practices

1. **Run coverage locally** before pushing to verify changes
2. **Monitor trends** in SonarCloud rather than absolute values
3. **Fix critical issues** identified by SonarCloud promptly
4. **Exclude generated code** from analysis to avoid noise
5. **Write meaningful tests** that actually exercise code paths

## Additional Resources

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [SonarCloud Documentation](https://docs.sonarcloud.io/)
- [Gradle JaCoCo Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [SonarQube Gradle Plugin](https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner-for-gradle/)
