# ADR-0003: Adopt Industry Standards

## Status

Accepted

## Context

As an open-source project aiming for broad adoption and community contribution, Scopes must make deliberate choices about technology standards and conventions. Adopting established industry standards rather than creating custom solutions offers numerous benefits for both the project and the broader developer community.

Key considerations:

- Reduced learning curve for contributors and users
- Improved interoperability with existing tools and workflows
- Decreased maintenance burden through community-maintained standards
- Enhanced credibility as a professional open-source project
- Facilitated contribution from diverse developer backgrounds

## Decision

We will adopt a "Standards-First" approach across all technical decisions:

### Core Principle

**Prefer established industry standards over custom implementations** unless there is a compelling technical reason that makes the standard unsuitable for our specific use case.

### Implementation Guidelines

1. **Research Standards First**
   - Before implementing any feature, research existing standards
   - Document available standards and their trade-offs
   - Only deviate when standards cannot meet core requirements

2. **Standard Selection Criteria**
   - **Maturity**: Prefer stable, widely-adopted standards
   - **Community**: Active maintenance and community support
   - **Documentation**: Comprehensive and accessible documentation
   - **Tooling**: Availability of implementation libraries and tools
   - **License**: Compatible with our open-source license

3. **Areas of Standardization**

   The following are examples of standards to consider. The specific choice should be made based on project requirements and context:

   - **APIs**: REST, GraphQL, JSON-RPC, OpenAPI
   - **CLI**: POSIX, GNU conventions, modern CLI guidelines
   - **Configuration**: XDG Base Directory, dotenv, TOML/YAML/JSON
   - **Documentation**: Markdown, JSDoc, OpenAPI
   - **Testing**: TAP, JUnit XML, coverage formats
   - **Packaging**: npm, Docker, OS-specific standards
   - **Protocols**: HTTP/REST, WebSocket, gRPC
   - **Security**: OAuth 2.0, JWT, OWASP guidelines
   - **Accessibility**: WCAG for any UI components
   - **Internationalization**: ICU, CLDR, gettext

4. **Documentation Requirements**
   - Document which standard is being followed
   - Link to official standard documentation
   - Explain any necessary deviations with justification
   - Provide examples of standard compliance

### Specific Standards Adopted

These standards are adopted by this ADR and should be followed unless superseded by future ADRs. Note that technology choices not listed here should follow the principle of adopting appropriate industry standards:

- **CLI Design**: [Command Line Interface Guidelines](https://clig.dev/) and GNU conventions
- **API Design**: RESTful principles with OpenAPI 3.0 specification
- **Configuration**: XDG Base Directory specification for file locations
- **Semantic Versioning**: SemVer 2.0.0 for version numbering
- **Commit Messages**: Conventional Commits specification
- **Code Style**: Language-specific community standards (ESLint/Prettier for JS/TS)
- **Documentation**: CommonMark for Markdown files
- **Licensing**: SPDX license identifiers

## Consequences

### Positive

- Lower barrier to entry for new contributors
- Reduced need for custom documentation
- Better tooling support and ecosystem integration
- Increased trust from enterprise and community users
- Simplified decision-making for technical choices
- Natural alignment with best practices

### Negative

- Less flexibility in implementation approaches
- Potential limitations imposed by standards
- Need to stay updated with evolving standards
- May require refactoring if standards change significantly

### Neutral

- Requires research before implementation
- Standards knowledge becomes prerequisite
- Dependency on external standard bodies

## Implementation Notes

1. **Standard Compliance Checking**
   - Use automated tools where available (linters, validators)
   - Include standard compliance in code review checklist
   - Add CI/CD checks for standard adherence

2. **Contribution Guidelines**
   - Update CONTRIBUTING.md to reference this ADR
   - Provide links to relevant standards
   - Include examples of standard-compliant code

3. **Deviation Process**
   - Deviations must be documented in code comments
   - Major deviations may require their own ADR
   - Track deviations for potential future standardization

## Related ADRs

- Future ADRs should reference relevant standards defined here

## References

- [The Power of Standards](https://www.w3.org/standards/)
- [Open Source Best Practices](https://opensource.guide/best-practices/)
- [12 Factor App](https://12factor.net/) - Standard for cloud-native applications
- [CNCF Projects](https://www.cncf.io/projects/) - Examples of standard-driven projects
