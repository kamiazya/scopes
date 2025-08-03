# ADR-0009: Adopt Apache License 2.0

## Status

Accepted

## Context

The project, initially licensed under the MIT License, has evolved to incorporate innovative features, particularly in AI-driven development (as per ADR-0002). The MIT License, while promoting broad adoption, lacks explicit provisions for patent rights and trademark protection. As the project matures and aims for wider enterprise adoption, ensuring legal clarity and protecting both users and contributors from potential patent litigation has become a critical requirement. A more robust license is needed to match the project's strategic goals of fostering a business-friendly ecosystem while safeguarding its core innovations.

## Decision

The project's official license will be changed from the MIT License to the Apache License 2.0. This decision is based on the Apache 2.0 license's explicit grant of patent rights, clear trademark protection, and its widespread acceptance within corporate environments. It strikes the optimal balance between permissive use and necessary legal protection, aligning with the project's long-term vision.

## Consequences

- **Positive:**
  - **Enhanced Patent Protection:** Provides an explicit grant of patent rights from contributors to users, significantly reducing legal risks for the community.
  - **Increased Enterprise Adoption:** The license is well-known and trusted by corporate legal teams, lowering the barrier to adoption for commercial use.
  - **Brand Protection:** The explicit trademark clause protects the project's name and identity.
  - **Improved Legal Clarity:** Offers a more detailed and robust legal framework compared to the MIT license.

- **Negative:**
  - **Increased Complexity:** The Apache 2.0 license text is longer and more complex than MIT, which could be a minor hurdle for some individual developers.

- **Neutral:**
  - Requires the inclusion of a `NOTICE` file for attribution.

## Alternatives Considered

- **Continue with MIT License:** Rejected because it fails to provide necessary patent protection for the project's AI-related innovations, creating unacceptable legal ambiguity for users and contributors.
- **Mozilla Public License 2.0 (MPL 2.0):** Rejected because, while it includes a patent grant, its "weak copyleft" nature introduces complexities and potential friction for some corporate adoption policies compared to the purely permissive Apache 2.0. The project's goal is to maximize adoption, including in proprietary software, making a permissive license more suitable.

## Related Decisions

- **Influenced by:** [ADR-0002: AI-Driven Development Architecture](./0002-ai-driven-development-architecture.md) - The focus on AI innovation highlighted the need for stronger patent protection.
- **Aligns with:** [ADR-0003: Adopt Industry Standards](./0003-adopt-industry-standards.md) - Apache 2.0 is a widely adopted and respected industry standard for open-source projects.

## Scope

- **Bounded Context:** This decision applies globally to the entire project.
- **Components:** All source code, documentation, and other assets within the repository.
- **External Systems:** Affects all downstream consumers and contributors to the project.

## Implementation Notes

- The `LICENSE` file in the repository root must be replaced with the full text of the Apache License 2.0.
- A `NOTICE` file must be created in the repository root, containing the project's copyright statement and any required notices from dependencies.
- **License headers will be added to distributed files during the build process, not directly into the source code.** This approach is chosen to keep the source code clean and minimize potential noise for AI tools that analyze the code, which is a key consideration for this AI-native project. This will require configuring the build tools (e.g., Vite) to prepend the license header to output files.
- The `README.md` and `CONTRIBUTING.md` files must be updated to reflect the new license.
- The repository settings on GitHub should be updated to identify the license as Apache 2.0.

## Tags

`license`, `legal`, `governance`, `community`
