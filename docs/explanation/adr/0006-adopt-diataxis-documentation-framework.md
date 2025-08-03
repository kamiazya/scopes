# ADR-0006: Adopt Diátaxis Documentation Framework

## Status

Accepted

## Context

Our current documentation structure has several issues:

1. **Unclear boundaries**: Technical documentation files have overlapping content with unclear distinctions between high-level architecture and detailed design
2. **Scattered information**: Similar concepts are defined in multiple places
3. **No clear organization principle**: Documents are placed based on ad-hoc categorization
4. **Difficult navigation**: Users struggle to find the right documentation for their needs

The project has grown to include various types of documentation:

- Architectural decisions and design documents
- Domain models and business requirements  
- Development guides and setup instructions
- API references and technical specifications

Without a clear framework, documentation becomes:

- Hard to maintain (unclear where to add new content)
- Difficult to navigate (users don't know where to look)
- Prone to duplication (same information in multiple places)
- Inconsistent in style and purpose

**Specific problem scenarios:**

- New developers struggle to find setup instructions, as they are scattered across multiple documents
- Contributors looking for specific API details have to search through explanatory documents
- Maintainers are unsure where to place new documentation, leading to inconsistent organization
- Users cannot easily distinguish between conceptual explanations and practical how-to guides

## Decision

We will adopt the **Diátaxis framework** for organizing our documentation. Diátaxis provides a systematic approach to technical documentation by recognizing that documentation serves four distinct user needs:

1. **Tutorials** - Learning-oriented guides for newcomers
2. **How-to guides** - Task-oriented recipes for specific goals
3. **Reference** - Information-oriented technical descriptions
4. **Explanation** - Understanding-oriented discussions

Our documentation will be reorganized into four main categories:

```
/docs/
├── README.md          # Navigation and overview
├── tutorials/         # Learning-oriented guides
├── guides/            # Task-oriented how-to guides
├── reference/         # Information-oriented technical descriptions
└── explanation/       # Understanding-oriented conceptual discussions
```

Each category serves a specific purpose and audience need, making it clear where to find and place documentation.

**Existing document classification examples:**

- **Tutorials**: New `docs/tutorials/getting-started.md` for new developers
- **How-to guides**: `docs/TEST_STRATEGY.md` → `docs/guides/testing-strategy.md`
- **Reference**: `docs/architecture/ARCHITECTURE.md` + `docs/architecture/DESIGN.md` → `docs/reference/architecture.md` (merged)
- **Explanation**: `docs/domain/REQUIREMENTS.md` → `docs/explanation/domain-overview.md`, `docs/architecture/adr/` → `docs/explanation/adr/`

**Documents remaining in current locations:**

- `CONTRIBUTING.md`: Stays at project root (OSS community best practice)
- `docs/README.md`: Remains as navigation guide

## Consequences

### Positive

1. **Clear purpose for each document**: Writers know what type of content belongs where
2. **Better user experience**: Readers can find information based on their current need
3. **Reduced duplication**: Single source of truth for each type of information
4. **Scalable structure**: Easy to add new documentation without confusion
5. **Industry-standard approach**: Diátaxis is widely recognized and proven
6. **Improved discoverability**: Users know where to look based on their goal

### Negative

1. **Major reorganization required**: All existing documentation needs to be moved/restructured
2. **Breaking changes**: Existing links to documentation will need updates
3. **Learning curve**: Contributors need to understand the Diátaxis categories
4. **Initial overhead**: Time investment to properly categorize and reorganize content

### Neutral

1. **Classification ambiguity**: Some documents may not fit neatly into one category and will require discussion
2. **Content quality dependency**: Framework improves organization but document quality still depends on individual contributors
3. **Maintenance commitment**: Ongoing effort required to maintain category boundaries as project evolves

## Alternatives Considered

1. **Maintain current ad-hoc structure**: Would avoid reorganization costs but perpetuate existing problems
2. **Create custom documentation categories**: Would allow project-specific optimization but lose industry-standard benefits
3. **Adopt GitBook or similar platform**: Would provide better tooling but require migration to external platform
4. **Use Read the Docs standard structure**: Simpler but less systematic than Diátaxis

Diátaxis was chosen because it provides a proven, systematic approach with clear user-centered categories.

## Related Decisions

- ADR-0005: CLI-First Interface Architecture (affects reference documentation structure)
- Future ADR: API Documentation Strategy (will define reference section details)

## Scope

This decision applies to all documentation in the `/docs` directory. It does not affect:

- `README.md` files in code packages
- Inline code documentation
- `CONTRIBUTING.md` (remains at project root)

## Tags

`documentation`, `strategy`, `user-experience`, `maintenance`

## References

- [Diátaxis Technical Documentation Framework](https://diataxis.fr/)
- [Write the Docs - Diátaxis](https://www.writethedocs.org/videos/eu/2017/the-four-kinds-of-documentation-and-why-you-need-to-understand-what-they-are-daniele-procida/)
- [Django's documentation](https://docs.djangoproject.com/) - Example of Diátaxis in practice
