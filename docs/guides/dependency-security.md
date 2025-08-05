# Dependency Security Guide

## Overview
This guide explains how to manage dependency vulnerabilities using GitHub's native security features including Dependabot, Dependency Graph, and Dependency Review.

## GitHub Security Features

### Dependabot Security Updates
Dependabot automatically monitors your dependencies for known vulnerabilities and creates pull requests to update them.

**Features:**
- Automatic vulnerability detection
- Security update pull requests  
- Weekly dependency updates
- Configurable through `.github/dependabot.yml`

**Configuration:**
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    labels:
      - "dependencies"
      - "security"
```

### Dependency Graph
GitHub automatically generates a dependency graph from your Gradle build files, providing visibility into your project's dependencies.

**Features:**
- Visual representation of dependencies
- Vulnerability alerts in the Security tab
- Integration with Dependabot
- Automatic detection from build files

**Accessing the Dependency Graph:**
1. Go to your repository
2. Click on the **Insights** tab
3. Select **Dependency graph** from the sidebar

### Dependency Review
The Dependency Review Action analyzes dependency changes in pull requests.

**Features:**
- Reviews new dependencies in PRs
- Alerts for vulnerable dependencies
- License compliance checking
- Automated PR comments

**Configuration:**
```yaml
# .github/workflows/dependency-review.yml
- name: Dependency Review
  uses: actions/dependency-review-action@v4
  with:
    fail-on-severity: high
    allow-licenses: Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC
```

## Working with Security Alerts

### Viewing Dependabot Alerts
1. Navigate to your repository
2. Click on the **Security** tab
3. Select **Dependabot alerts**
4. Review vulnerabilities and their severity

### Understanding Alert Information
Each alert includes:
- **Vulnerability details**: Description and CVSS score
- **Affected versions**: Which versions of the dependency are vulnerable
- **Patched versions**: Safe versions to upgrade to
- **Impact assessment**: How the vulnerability affects your project

### Responding to Alerts

#### 1. Automatic Updates (Recommended)
- Dependabot creates PRs for vulnerable dependencies
- Review and merge the PRs to apply security fixes
- Tests run automatically to ensure compatibility

#### 2. Manual Updates
If automatic updates aren't available:
```bash
# Update specific dependency in build.gradle.kts
implementation("com.example:library:2.1.0") // Updated version
```

#### 3. Dismissing Alerts
For false positives or non-applicable vulnerabilities:
1. Open the alert in the Security tab
2. Click **Dismiss alert**
3. Select appropriate reason
4. Add comment explaining the dismissal

## Dependency Submission Integration

### How It Works
The Dependency Submission Action ensures GitHub has complete visibility into your project's dependencies:

```yaml
# Integrated into security-check.yml
- name: Submit dependency graph to GitHub
  uses: gradle/actions/dependency-submission@v4
  with:
    dependency-graph: generate-and-submit
```

**Benefits:**
- More accurate dependency detection
- Better vulnerability coverage
- Improved Dependabot functionality

## Best Practices

### For Developers
1. **Enable Notifications**: Configure GitHub to notify you of security alerts
2. **Regular Updates**: Keep dependencies updated proactively
3. **Review PRs**: Carefully review Dependabot PRs before merging
4. **Test Thoroughly**: Ensure security updates don't break functionality

### For Project Maintainers
1. **Configure Dependabot**: Set up appropriate update schedules
2. **Monitor Security Tab**: Regularly check for new alerts
3. **Establish Response SLA**: Define timelines for addressing different severity levels
4. **Document Decisions**: Record rationale for dismissed alerts

## Vulnerability Response Process

### 1. Alert Triage (Within 24 hours)
- Assess severity and impact
- Determine if vulnerability affects your usage
- Prioritize based on CVSS score and exploitability

### 2. Response Planning (Critical: 48 hours, High: 1 week)
- **Critical/High**: Immediate patching required
- **Medium**: Plan update in next sprint
- **Low**: Include in regular maintenance cycle

### 3. Implementation
- Merge Dependabot PRs when available
- Manual updates for complex cases
- Test thoroughly before production deployment

### 4. Verification
- Confirm vulnerability is resolved in Security tab
- Verify no new vulnerabilities introduced
- Update documentation if needed

## Integration with CI/CD

### Security Check Workflow
Dependencies are automatically monitored through:
- **Dependency Submission**: Keeps GitHub's dependency graph current
- **SBOM Generation**: Creates software bill of materials
- **PR Reviews**: Analyzes changes for security impact

### Automated Actions
- Security alerts trigger Dependabot PRs
- PR checks include dependency review
- Failed security checks block merging

## Troubleshooting

### Common Issues

#### Dependabot Not Creating PRs
- Check repository settings for security updates
- Verify `.github/dependabot.yml` configuration
- Ensure branch protection allows Dependabot

#### Missing Vulnerabilities
- Verify dependency submission is working
- Check if dependencies are properly detected
- Consider private dependency sources

#### False Positives
- Review vulnerability details carefully
- Check if your usage is affected
- Dismiss with appropriate justification

### Getting Help
- GitHub Security documentation
- Dependabot troubleshooting guides
- Community forums and discussions

## Comparison with OWASP Dependency Check

| Feature | GitHub (New) | OWASP (Previous) |
|---------|-------------|------------------|
| **Performance** | Fast, cloud-based | Slow, local processing |
| **Setup** | Zero configuration | Requires NVD API key |
| **Updates** | Automatic PRs | Manual remediation |
| **Integration** | Native GitHub UI | External reports |
| **Maintenance** | GitHub managed | Self-managed |

## Migration Notes

Previous OWASP Dependency Check functionality is now handled by:
- **Vulnerability Detection**: Dependabot alerts
- **Automated Updates**: Dependabot PRs  
- **CI Integration**: Dependency Review Action
- **Reporting**: GitHub Security tab

This provides a more streamlined, efficient approach to dependency security management.

## Questions?

If you have questions about dependency security:
- Check the [Security Policy](../../SECURITY.md)
- Review GitHub's security documentation
- Open an issue in the repository

Remember: **GitHub's native security features are continuously updated** with the latest vulnerability data and best practices.
