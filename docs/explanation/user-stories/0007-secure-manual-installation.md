# US-007: Secure JAR Bundle Installation and Verification

## User Story

- **As a** security-conscious developer who needs to install Scopes
- **I want** to use JAR bundle packages with automatic cryptographic verification
- **So that** I can quickly and safely install Scopes with clear verification steps while ensuring supply chain security

## Persona Context

- **User Type**: Security-aware Developer / DevOps Engineer / Enterprise User
- **Experience Level**: Intermediate to Expert
- **Context**: Needs to install software with strong security guarantees. Familiar with modern installation patterns (Docker, Rust, Node.js). Values both convenience and security.

## Detailed Scenario

A developer wants to install Scopes but requires strong security guarantees due to organizational policies or personal security practices. They expect:

- **Modern installation experience**: Universal JAR bundle with included verification and platform-specific wrapper scripts
- **Included verification**: Verification tools and scripts included in the bundle package
- **Transparency**: Clear indication of what security checks are being performed
- **Cross-platform consistency**: Same JAR works across Linux, macOS, and Windows (requires Java 21+)
- **Supply chain security**: SLSA Level 3 provenance verification integrated seamlessly

Current pain points with manual installation approaches:
- Multi-step verification processes are error-prone
- Users often skip verification steps due to complexity
- Inconsistent security practices across platforms
- Manual hash verification is tedious and mistakes are common

## Acceptance Criteria

```gherkin
Feature: Secure JAR bundle installation

Scenario: JAR bundle installation with automatic verification
    Given I have downloaded the universal JAR bundle for my system
    When I extract the bundle package
    And I run the included './install.sh' script (Linux/macOS) or '.\install.ps1' (Windows)
    Then the JAR file hash is automatically verified using included verification files
    And the SLSA provenance is automatically verified
    And Scopes wrapper scripts are installed to the appropriate system location
    And I can immediately use the 'scopes' command
    And I see confirmation that all security checks passed

Scenario: Windows JAR bundle installation
    Given I have downloaded the JAR bundle package
    When I extract the bundle package
    And I run the included '.\install.ps1' script
    Then the JAR file is verified automatically using included verification files
    And wrapper scripts are installed with appropriate permissions
    And the PATH is updated to include Scopes
    And I see confirmation of successful security verification

Scenario: Installation with environment variables
    Given I want to customize the installation
    When I set environment variables like SCOPES_INSTALL_DIR=/opt/scopes
    And I run the bundle installation script
    Then the script uses my specified configuration
    And all verification steps still execute properly
    And the installation respects my preferences

Scenario: Verification failure handling
    Given the bundle JAR file fails hash verification
    When the installation script runs verification
    Then the installation is immediately aborted
    And I see a clear error message about the verification failure
    And no JAR file is installed on my system
    And I am warned not to use the compromised JAR

Scenario: Bundle extraction issues
    Given I have a corrupted bundle package
    When the extraction fails partway through
    Then I see a clear error message
    And temporary files are cleaned up
    And I can retry with a fresh bundle download

Scenario: Insufficient permissions
    Given I don't have write access to /usr/local/bin
    When I run the installation script
    Then I'm prompted about sudo requirements
    And I can choose an alternative installation directory
    And the script guides me through PATH setup

Scenario: SLSA verifier installation
    Given slsa-verifier is not installed on my system
    When the installation script needs to verify provenance
    Then it automatically installs slsa-verifier using Go
    And continues with verification seamlessly
    And informs me about the additional security tool installed

Scenario: Offline verification support
    Given I have downloaded a bundle package manually
    When I run the verification script directly from the bundle
    Then I can verify files without additional downloads
    And I get the same security guarantees
    And I can then install the verified JAR file

Scenario: Enterprise environment
    Given I'm in a restricted corporate environment
    When I extract and run the bundle installation
    Then it works without internet connectivity
    And verification uses included files
    And installation functions properly in restricted environments

Scenario: Bundle inspection before installation
    Given I want to inspect the bundle before installing
    When I extract the bundle package
    Then I can review the installation script and verification files
    And I can manually verify the JAR file using included tools
    And I can run the installation after inspection
```

## User Journey

1. **Discovery**: User learns about Scopes and decides to install securely
2. **Bundle Download**: User downloads universal JAR bundle package
3. **Bundle Extraction**: User extracts bundle to inspect contents
4. **Script Execution**: User runs included installation script
5. **Security Verification**: JAR hash and SLSA provenance verified automatically using included files
6. **Installation**: JAR and wrapper scripts installed to appropriate system location
7. **Verification**: Installation verified and PATH updated if needed
8. **Ready to Use**: User can immediately start using Scopes (requires Java 21+)

```mermaid
---
title: Secure Installation User Journey
---
journey
        title Secure JAR Bundle Installation and Verification
        section Discovery
          Need secure installation  : 2: User
          Find bundle download      : 3: User
        section Bundle Preparation
          Download JAR bundle        : 4: User
          Extract bundle package     : 4: User
          Inspect bundle contents    : 5: User
        section Verification & Installation
          Run installation script    : 4: User, System
          Auto-verify JAR hash      : 5: System
          Auto-verify SLSA          : 5: System
          Install JAR and wrappers  : 5: System
          Update PATH if needed      : 4: System
        section Success
          See security confirmation  : 5: User, System
          Run first command         : 5: User, System
          Feel confident about security : 5: User
```

## Success Metrics

- **Security Verification Rate**: 100% of installations perform both hash and SLSA verification
- **Installation Success Rate**: >98% of installations complete without manual intervention
- **Time to Installation**: Complete installation (including verification) within 2 minutes
- **Security Confidence**: Users report high confidence in installation security
- **Cross-Platform Consistency**: Same security level and user experience across all platforms

## Dependencies

### Requires
- GitHub Releases with SLSA provenance
- Universal JAR distribution (platform-independent)
- Hash files for each release
- Java 21 or later runtime
- Network connectivity for downloads
- Modern shell environment (bash/PowerShell/cmd)

### Enables
- Secure software distribution at scale
- Enterprise adoption with compliance requirements
- Trust in the software supply chain
- Simplified onboarding for security-conscious users

## Security Features

### Cryptographic Verification
- **SHA256 Hash Verification**: JAR file verified against published hashes
- **SLSA Level 3 Provenance**: Supply chain integrity verified cryptographically
- **HTTPS-Only Downloads**: All network communication over encrypted channels
- **Signature Verification**: Integration with GitHub's signing infrastructure

### Supply Chain Security
- **Build Transparency**: Complete visibility into build process through SLSA
- **Source Verification**: Cryptographic proof of source repository and commit
- **Builder Verification**: Confirmation of official build environment
- **Non-Repudiation**: Tamper-evident audit trail for all releases

### Threat Protection
- **Man-in-the-Middle Protection**: HTTPS and signature verification
- **JAR Tampering Detection**: Hash verification catches any modifications
- **Compromised Repository Protection**: SLSA provenance detects unauthorized changes
- **Supply Chain Attacks**: Multi-layer verification catches sophisticated attacks

## Implementation Notes

### Platform Support
- **Linux**: bash script included in JAR bundle
- **macOS**: Same bash script with macOS-specific optimizations
- **Windows**: PowerShell script included in JAR bundle
- **Requirement**: Java 21 or later must be installed

### Installation Methods
```bash
# Linux/macOS JAR bundle
tar -xzf scopes-v1.0.0-jar-bundle.tar.gz
cd scopes-v1.0.0-jar-bundle
./install.sh

# Windows JAR bundle
Expand-Archive scopes-v1.0.0-jar-bundle.zip -DestinationPath .
cd scopes-v1.0.0-jar-bundle
.\install.ps1
```

### Environment Variables
- `SCOPES_INSTALL_DIR`: Custom installation directory
- `SCOPES_SKIP_VERIFICATION`: Skip verification (not recommended)
- `SCOPES_FORCE_INSTALL`: Skip confirmation prompts
- `SCOPES_VERBOSE`: Enable verbose output during installation

### Error Handling
- Clear error messages for common failure scenarios
- Automatic cleanup of temporary files
- Guidance for manual intervention when needed
- Fallback options for restricted environments

## Security Considerations

### Trusted Execution
- Scripts are included in verified bundle packages
- Users can inspect bundle contents before execution
- Scripts can be reviewed before running installation
- All operations are transparent and logged

### Verification Integrity
- Hash verification prevents JAR file tampering
- SLSA provenance prevents supply chain attacks
- Multiple verification layers provide defense in depth
- Verification failures immediately abort installation

### Operational Security
- No sensitive data transmitted during installation
- Temporary files cleaned up automatically
- Installation can be performed in restricted environments without internet access
- Full audit trail of all verification steps

## Related Stories

- **US-001**: First-Time Installation and Setup - This story extends US-001 by providing the secure manual installation path for users who cannot or prefer not to use package managers
- **US-002**: Create First Scope Hierarchy - Users can proceed to creating their first scope after secure installation
- **US-003**: Aspect-Based Scope Classification - Foundation for organizing work after installation
- **US-004**: Personal Context Switching - Enables efficient workflow after setup

This story establishes the security foundation that enables trust in the Scopes ecosystem for security-conscious users and organizations.
