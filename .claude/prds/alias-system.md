---
name: alias-system
description: Human-friendly alias system for scope identification replacing unfriendly ULIDs
status: backlog
created: 2025-08-25T14:45:27Z
---

# PRD: Alias System

## Executive Summary

The Alias System provides human-friendly identifiers for scopes, replacing the need to use or remember complex ULIDs (Universally Unique Lexicographically Sortable Identifiers). This system enables developers to reference scopes using short, memorable aliases while maintaining the robustness and uniqueness guarantees of ULIDs internally. The system supports both auto-generated canonical aliases using a ULID-seeded Haikunator pattern and custom user-defined aliases, with a 1-to-many relationship between scopes and aliases.

## Problem Statement

Scopes internally uses ULIDs (like `01H8XGJWBWBAQ1J3T3B8A0V0A8`) for guaranteed uniqueness and future multi-device synchronization. However, these identifiers create significant friction in daily workflows:

- **Cognitive Load**: Impossible to remember which ULID corresponds to which task
- **Typing Friction**: Constant need to copy/paste or carefully type long IDs
- **Communication Barriers**: Cannot verbally discuss scopes with others
- **Script Readability**: Automation scripts become unreadable with hardcoded ULIDs
- **External Integration**: Future integrations with tools like JIRA may use different identifiers

Without a user-friendly alias system, the tool's usability is severely compromised, especially for CLI power users who value typing efficiency and memorability.

## User Stories

### Primary Personas

1. **CLI Power User**: Developers who work primarily in terminal, create dozens to hundreds of scopes, value typing efficiency
2. **Multi-Project Developer**: Managing scopes across different projects, needs memorable references
3. **Automation Engineer**: Writing scripts that reference scopes, needs readable identifiers
4. **International Developer**: Working with AI assistants in their native language, needs flexible naming

### Core User Journeys

1. **Automatic Alias Assignment**
   - User creates a new scope
   - System automatically generates a canonical alias like "quiet-river-a4f7"
   - User can immediately reference the scope using this alias

2. **Custom Alias Creation**
   - User identifies an important scope needing a specific name
   - User assigns custom alias like "auth-feature" or "sprint-42-login"
   - Alias becomes available for all commands

3. **Multiple Alias Management**
   - User assigns multiple aliases to the same scope for different contexts
   - Example: "auth", "login-feature", "Q1-priority-1" all point to same scope
   - User can manage which alias is canonical (primary display name)

4. **Natural Language Integration**
   - User interacts with AI assistant using aliases
   - AI understands and resolves aliases through MCP integration
   - Commands like "show me @current-sprint" work naturally

## Requirements

### Functional Requirements

#### Core Alias Features
1. **Automatic Canonical Alias Generation**
   - Generate human-readable aliases using ULID-seeded Haikunator pattern
   - Format: `<adjective>-<noun>-<ulid-token>` (e.g., "quiet-river-a4f7")
   - Use last 20 bits from ULID's random section for uniqueness
   - Ensure no offensive or inappropriate word combinations

2. **Custom Alias Management**
   - Allow custom alias assignment at scope creation with `--alias` flag
   - Support adding multiple aliases to existing scopes
   - Enable changing the canonical (primary) alias
   - Provide alias removal functionality

3. **Alias Resolution**
   - Exact match resolution (highest priority)
   - Prefix matching when unique
   - Clear disambiguation when multiple matches exist
   - Case-insensitive matching for user convenience

4. **Canonical vs Custom Aliases**
   - One canonical alias per scope (shown in listings)
   - Unlimited custom aliases per scope
   - Clear visual distinction between canonical and custom aliases
   - Haikunator-generated aliases can be demoted to regular aliases

#### Command Interface
1. **Scope Creation**
   ```bash
   scopes create "Title" [--alias custom-name]
   ```

2. **Alias Management Commands**
   ```bash
   scopes alias add <scope-ref> <new-alias>
   scopes alias list <scope-ref>
   scopes alias set-canonical <scope-ref> <alias>
   scopes alias rm <alias>
   ```

3. **Alias Usage in All Commands**
   - All scope references accept aliases
   - Consistent alias resolution across commands
   - Clear error messages for ambiguous references

### Non-Functional Requirements

#### Performance
- Alias resolution in < 10ms for typical usage
- Support for 10,000+ aliases without degradation
- Efficient prefix matching algorithm
- In-memory storage for current implementation

#### Usability
- Aliases between 3-30 characters
- No special characters except hyphens
- Clear, memorable word choices for Haikunator
- Consistent alias format across the system

#### Compatibility
- Shell-friendly alias format (no spaces or special chars)
- Tab completion support preparation
- Future SQLite migration path
- MCP protocol integration readiness

#### Reliability
- No alias collisions in Haikunator generation
- Graceful handling of duplicate custom aliases
- Consistent state between alias and scope operations
- Clear rollback on operation failures

## Success Criteria

### Quantitative Metrics
- **Alias Adoption**: 95%+ of scope references use aliases instead of ULIDs
- **Generation Success**: 99.99%+ unique canonical aliases on first attempt
- **Resolution Speed**: < 10ms average alias resolution time
- **Custom Alias Usage**: 30%+ of important scopes have custom aliases

### Qualitative Metrics
- Users report significant productivity improvement
- Scripts and documentation become more readable
- Reduced errors from ULID typos
- Improved ability to discuss scopes verbally

### Acceptance Tests
- All commands accept and resolve aliases correctly
- Haikunator generates appropriate, unique aliases
- Multiple aliases per scope work without conflicts
- Canonical alias changes preserve all references
- Performance meets requirements with 10K+ aliases

## Constraints & Assumptions

### Technical Constraints
- Current implementation uses in-memory storage
- Must maintain backward compatibility with ULID system
- Alias format must be shell-safe
- Limited to ASCII characters for maximum compatibility

### Business Constraints
- Personal tool focus (no team sharing features)
- Must work offline without external dependencies
- Minimal configuration required (zero-config principle)

### Assumptions
- Users prefer memorable names over pure randomness
- Most scopes need only 1-2 aliases
- Haikunator pattern provides sufficient uniqueness
- Users will customize aliases for important scopes

## Out of Scope

### Current Release
- Team alias sharing/synchronization
- Alias namespaces or prefixes
- Alias templates or patterns
- Smart alias suggestions based on content
- Alias conflict resolution strategies
- Database persistence (using in-memory for now)
- Advanced tab completion implementation

### Future Considerations
- SQLite storage migration
- External tool integration (JIRA aliases)
- Multi-device alias synchronization
- AI-powered alias suggestions
- Alias usage analytics
- Internationalized word lists

## Dependencies

### Internal Dependencies
- **Scope Management System**: Core scope CRUD operations
- **ULID Generation**: Unique identifier system
- **Command Parser**: Alias resolution in all commands
- **Haikunator Implementation**: Already implemented for generation

### External Dependencies
- **Word Lists**: Curated adjective and noun lists for Haikunator
- **Future**: MCP protocol for AI integration
- **Future**: SQLite for persistent storage
- **Future**: Shell completion frameworks

### Integration Points
- All CLI commands must support alias resolution
- List/show commands must display aliases appropriately
- Focus and context systems must work with aliases
- Future sync must handle alias conflicts

## Technical Approach

### Architecture Overview
```
CLI Layer
    ↓ (alias input)
Alias Resolution Service
    ↓ (resolves to ULID)
Scope Repository
    ↑
Alias Repository (in-memory)
```

### Key Components
1. **AliasRepository**: Stores alias-to-ULID mappings
2. **AliasResolutionService**: Handles lookup and prefix matching
3. **HaikunatorService**: Generates canonical aliases
4. **AliasManagementUseCase**: Business logic for alias operations

### Data Model
```kotlin
data class Alias(
    val value: String,
    val scopeId: ULID,
    val isCanonical: Boolean,
    val createdAt: Instant
)

data class ScopeAliases(
    val scopeId: ULID,
    val canonicalAlias: String,
    val customAliases: Set<String>
)
```

## User Experience

### Command Examples
```bash
# Create with auto-generated alias
$ scopes create "Implement authentication"
Created scope 01H8XGJWBWBAQ1J3T3B8A0V0A8
Canonical alias: quiet-river-a4f7

# Create with custom canonical alias
$ scopes create "Fix login bug" --alias fix-login
Created scope 01H8XGJWCDEFG2K4L5M6N7P8Q9
Canonical alias: fix-login

# Add additional aliases
$ scopes alias add quiet-river-a4f7 auth-feature
✓ Alias 'auth-feature' assigned

# Use aliases naturally
$ scopes show auth-feature
$ scopes focus quiet-river-a4f7
$ scopes update fix-login --status in-progress
```

### Error Handling
```bash
# Ambiguous prefix
$ scopes show quiet
Error: Prefix 'quiet' matches multiple scopes:
- quiet-river-a4f7 "Implement authentication"
- quiet-mountain-b8e2 "Fix login bug"

# Duplicate alias
$ scopes alias add scope-123 auth-feature
Error: Alias 'auth-feature' already exists for scope 'quiet-river-a4f7'
```

## Migration & Rollout

### Phase 1: Core Implementation
- In-memory alias repository
- Basic alias CRUD operations
- Integration with existing commands
- Haikunator generation

### Phase 2: Enhanced Features
- Prefix matching and disambiguation
- Canonical alias management
- Multiple alias support
- Basic shell completion

### Phase 3: Future Extensions
- SQLite persistence
- AI integration via MCP
- Advanced suggestions
- External tool mappings

## Risk Mitigation

### Technical Risks
- **Alias Collisions**: Mitigated by ULID-seeded generation
- **Performance**: In-memory storage ensures speed
- **Data Loss**: Future SQLite migration will add persistence

### User Experience Risks
- **Learning Curve**: Intuitive design minimizes training
- **Alias Confusion**: Clear canonical alias concept
- **Migration Issues**: Gradual rollout with compatibility

## Success Indicators

### Short Term (1 month)
- Zero ULID usage in user commands
- Positive user feedback on productivity
- No critical bugs in alias resolution

### Medium Term (3 months)
- 90%+ scopes have custom aliases
- Scripts widely use aliases
- Ready for SQLite migration

### Long Term (6 months)
- Full AI integration via MCP
- External tool alias mapping
- Recognized as key differentiator
