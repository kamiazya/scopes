# US-003: Aspect-Based Scope Classification

## User Story

- **As a** developer managing multiple scopes with different characteristics
- **I want** to classify and query scopes using flexible metadata
- **So that** I can organize and find relevant work efficiently across different dimensions

## Persona Context

- **User Type**: Tech Lead / AI-Driven Developer
- **Experience Level**: Intermediate (familiar with scope creation)
- **Context**: Has created several scopes and needs better organization beyond just hierarchy. Works on features with different priorities, types, and states.

## Detailed Scenario

A developer has created a scope hierarchy for their authentication system but realizes they need additional ways to organize and filter their work:

- Some scopes are high priority, others are exploratory
- Some scopes are UI work, others are backend or documentation
- Some scopes are blocked by dependencies, others are ready to work on
- They want to query across these dimensions: "Show me all high-priority backend work that's ready to start"

Traditional tools use fixed categories or simple tags. The developer needs a flexible system that allows:
- Key-value metadata that can be customized for their workflow
- Advanced querying with comparison operators
- Both standard development presets and personal customization

Current pain points:
- Fixed categorization doesn't match personal workflow needs
- Simple tags are too limiting for complex queries
- Need to combine multiple criteria for effective filtering

## Acceptance Criteria

```gherkin
Feature: Aspect-based scope classification

Scenario: Use development presets
    Given I have created several scopes in my project
    When I assign standard development aspects to scopes
    Then I can categorize work by priority, type, and status
    And these aspects provide sensible defaults for development workflows

Scenario: Create custom aspects for personal workflow
    Given I need project-specific classification
    When I define custom aspects that match my workflow
    Then I can categorize scopes using my own terminology
    And these custom aspects work just like standard ones

Scenario: Query with comparison operators
    Given I have scopes with numeric or ordered aspects
    When I query with comparison operators like >= or <=
    Then I can find scopes based on relative values
    And I can filter by ranges or thresholds effectively

Scenario: Combine multiple criteria
    Given I have a complex project with many scopes
    When I need to find specific combinations of characteristics
    Then I can combine multiple aspects with logical operators
    And get exactly the scopes that match all my criteria
      ```typescript

## User Journey

1. **Recognition**: User realizes simple hierarchy isn't enough for personal organization
2. **Preset Discovery**: User learns about development-focused aspect presets
3. **Basic Usage**: User adds aspects to existing scopes using provided defaults
4. **Personal Customization**: User defines aspects that match their specific workflow
5. **Query Mastery**: User learns advanced operators for complex personal filtering
6. **Workflow Integration**: User incorporates aspect-based queries into daily routine

```mermaid
---
title: Learning Aspect-Based Personal Organization
---
journey
        title Personal Scope Organization with Aspects
        section Recognition
          Need better organization : 2: User
          Learn about aspects     : 3: User
        section Basic Usage
          Use development presets : 4: User, System
          Add aspects to scopes   : 4: User, System
          Query by single aspect  : 4: User, System
        section Personalization
          Define custom aspects   : 5: User, System
          Use comparison operators: 5: User, System
          Combine multiple aspects: 5: User, System
        section Mastery
          Create complex queries  : 5: User, System
          Integrate into routine  : 5: User
          Refine personal system  : 5: User
      ```typescript

## Success Metrics

- **Aspect Adoption**: Users add aspects to >70% of their scopes
- **Personal Customization**: Users define custom aspects beyond presets
- **Query Complexity**: Users successfully use compound queries with multiple conditions
- **Personal Efficiency**: Users report faster scope discovery and organization

## Dependencies

### Requires
- Basic scope creation and management (US-002)
- Key-value metadata storage system
- Query parsing and execution engine
- CLI interface for aspect management

### Enables
- Personal workspace and context management
- Efficient scope filtering and discovery
- Individual productivity optimizations
- AI-assisted scope recommendations based on personal patterns

## Implementation Notes

### Aspect System Design
- **Development Presets**: Common aspects for software development workflows
- **Personal Customization**: Full flexibility to define project-specific aspects
- **Value Types**: Support for text, numbers, and ordered values
- **Query Operators**: Comparison (>=, <=), pattern matching, logical operators

### Example Usage Patterns
```bash
# Using development presets
scopes list -a priority=high
scopes list -a status=ready

# Personal custom aspects
scopes list -a complexity>=medium
scopes list -a blocked=false

# Complex personal queries
scopes list -a "priority>=high AND type=implementation"
scopes list -a "ready=true AND estimate<=4h"
      ```typescript

### Aspect Management
```bash
# Set aspects on scopes (using aliases)
scopes aspect set quiet-river-a4f7 priority=high
scopes aspect set auth-feature status=ready complexity=medium

# Query scopes by aspects
scopes list -a priority=high
scopes list -a complexity>=medium
scopes list -a "priority>=high AND status=ready"

# Manage aspect definitions
scopes aspect define priority --type ordered --values low,medium,high
scopes aspect define status --type text --values todo,ready,done
scopes aspect list --definitions
      ```typescript

### Command Output Examples
```bash
# Setting aspects with visual feedback
$ scopes aspect set quiet-river-a4f7 priority=high status=ready
✓ Set aspects on scope 'quiet-river-a4f7':
    priority: high
    status: ready

# Querying with filtered results
$ scopes list -a priority=high
Found 3 scopes with priority=high:

quiet-river-a4f7    Implement authentication          priority=high status=ready
wise-ocean-b2k8     Redesign user dashboard          priority=high status=todo
swift-mountain-c9x4 Fix critical security bug        priority=high status=done

# Complex queries show matching criteria
$ scopes list -a "priority>=medium AND status!=done"
Found 5 scopes matching criteria:

calm-forest-d1m2    Add user preferences             priority=medium status=todo
quiet-river-a4f7    Implement authentication         priority=high   status=ready
wise-ocean-b2k8     Redesign user dashboard         priority=high   status=todo
brave-star-e5n3     Optimize database queries       priority=medium status=ready
gentle-cloud-f8p6   Update documentation            priority=low    status=todo
      ```typescript

## Related Stories

- **US-002**: Create First Scope Hierarchy (provides foundation)
- **Future**: Personal Context Management (uses aspects for context switching)
- **Future**: AI Integration (learns from personal aspect patterns)
- **Future**: Personal Productivity Analytics (analyzes aspect usage patterns)

