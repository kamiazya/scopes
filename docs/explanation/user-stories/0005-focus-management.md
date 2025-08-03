# US-005: Focus Management

## User Story

- **As a** developer working with multiple scopes across different contexts
- **I want** to focus on specific scopes or sets of scopes hierarchically
- **So that** I can concentrate on current work without distraction from unrelated scopes

## Persona Context

- **User Type**: Multi-Project Developer / Freelancer
- **Experience Level**: Intermediate (familiar with contexts and scopes)
- **Context**: Works on various tasks, needs to focus on specific work items while maintaining awareness of hierarchy and relationships

## Detailed Scenario

A developer manages hundreds of scopes across multiple projects and needs to focus on specific work items:

- **Single Scope Focus**: Deep work on one specific task
- **Parent Scope Focus**: Working on a feature with all its sub-tasks
- **Context-Based Focus**: Focusing within a specific context view
- **Workspace-Based Focus**: Directory-specific focus that persists

Current pain points:
- Getting distracted by unrelated scopes in the list
- Losing track of which scope they're working on
- Difficulty maintaining focus when switching between directories
- Need to manually track current work item

The developer wants a focus system that works at both user (global) and workspace (directory) levels, with clear hierarchy rules.

## Acceptance Criteria

```gherkin
Feature: Hierarchical focus management

Scenario: Focus on a single scope
    Given I have many scopes in my system
    When I focus on scope "scope-123"
    Then I see only that scope and its context
    And I can work without seeing other scopes
    And focus state persists across commands

Scenario: Focus on parent scope with children
    Given I have a parent scope with multiple child scopes
    When I focus on the parent scope
    Then I see the parent and all its descendants
    And sibling scopes are hidden from view
    And I can navigate within the focused hierarchy

Scenario: User-level focus management
    Given I am working globally across directories
    When I set a user-level focus on "scope-456"
    Then this focus applies everywhere I work
    And it persists across directory changes
    Until I explicitly change or clear it

Scenario: Workspace-level focus management
    Given I am in a specific project directory
    When I set a workspace focus on "scope-789"
    Then this focus applies only in this workspace
    And it overrides any user-level focus
    And other directories maintain their own focus

Scenario: Focus hierarchy resolution
    Given I have both user and workspace focus set
    When I run scopes commands
    Then workspace focus takes precedence
    And user focus applies when no workspace focus exists
    And I can see which focus is active
      ```typescript

## User Journey

1. **Focus Discovery**: User learns about focus feature for concentration
2. **First Focus**: User focuses on a single scope for deep work
3. **Hierarchical Understanding**: User discovers parent scope focus includes children
4. **Workspace Focus**: User sets directory-specific focus for projects
5. **Focus Workflow**: User integrates focus into daily workflow
6. **Efficient Work**: User maintains concentration with proper focus management

```mermaid
---
title: Focus Management Workflow
---
journey
        title Daily Focus Management
        section Morning Start
          Check current focus          : 3: User
          Set user focus on priority   : 4: User, System
          Work on focused scope        : 5: User, System
        section Project Switch
          Navigate to project dir      : 4: User
          Workspace focus activates    : 5: User, System
          Work in project context      : 5: User, System
        section Deep Work Session
          Focus on specific subtask    : 4: User, System
          Hidden distractions          : 5: User, System
          Complete focused work        : 5: User
        section Focus Management
          Clear completed focus        : 4: User
          Set new focus target         : 4: User, System
          Continue productive work     : 5: User, System
      ```typescript

## Success Metrics

- **Focus Adoption**: Users actively use focus feature daily
- **Deep Work Sessions**: Increased time spent in focused state
- **Context Switches**: Reduced cognitive overhead when switching work
- **Productivity**: Users report improved concentration and completion rates

## Dependencies

### Requires
- Basic scope management (US-002)
- Context views system (US-004)
- Scope hierarchy navigation
- Focus state persistence

### Enables
- Workspace management with automatic focus (future)
- AI focus-aware assistance
- Focus-based time tracking
- Distraction-free work modes

## Implementation Notes

### Focus Commands
```bash
# Focus on a single scope (using aliases)
scopes focus quiet-river-a4f7
scopes focus auth-feature

# Focus on parent (includes all children)
scopes focus quiet-river-a4f7 --recursive

# Show current focus
scopes focus current
scopes focus  # Short form

# Clear focus
scopes focus clear

# User vs Workspace focus
scopes focus --user auth-feature      # Set global focus
scopes focus --workspace bug-fix-ui   # Set workspace focus
scopes focus --user clear            # Clear global focus
      ```typescript

### Focus Display
```bash
# When focused, all commands respect focus
scopes list  # Shows only focused scope(s)
scopes tree  # Shows only focused hierarchy

# Bypass focus temporarily
scopes list --all  # Show all scopes ignoring focus
scopes list --no-focus  # Alternative syntax
      ```typescript

### Focus Hierarchy
      ```typescript
User Focus (Global)
        ↓ (applies everywhere)
Workspace Focus (Directory-specific)
        ↓ (overrides user focus in this directory)
Active Focus (What user sees)
      ```typescript

### Focus State Storage
      ```typescript
~/.scopes/
├── config.json
├── focus.json          # User-level focus state
└── scopes.db

/project/.scopes/
├── workspace.json
└── focus.json          # Workspace-level focus state
      ```typescript

### Focus with Contexts
```bash
# Focus works within active context
scopes context switch client-work
scopes focus auth-feature  # Focus within client-work context

# Focus + Context = Powerful filtering
# Context: "project=alpha"
# Focus: "auth-feature" (and children)
# Result: Only auth-feature tree within project alpha
      ```typescript

### Command Output Examples
```bash
# Setting focus with confirmation
$ scopes focus auth-feature
✓ Focused on scope 'auth-feature': Implement user authentication
    Including 2 child scopes

# Current focus status
$ scopes focus current
Current focus: auth-feature (Implement user authentication)
Level: workspace-level
Children included: 2 scopes

# Focused listing shows hierarchy
$ scopes list
[FOCUS: auth-feature] [CONTEXT: client-work]

auth-feature         Implement user authentication      priority=high
    ├── login-ui       Design login interface            priority=medium
    └── password-val   Add password validation           priority=high

# Recursive focus shows full tree
$ scopes focus auth-feature --recursive
✓ Focused on 'auth-feature' and all descendants (5 scopes total)

$ scopes list
[FOCUS: auth-feature (recursive)]

auth-feature         Implement user authentication      priority=high
    ├── login-ui       Design login interface            priority=medium
    │   └── form-val   Add form validation               priority=low
    └── password-val   Add password validation           priority=high
          └── hash-impl  Implement password hashing        priority=medium

# Status shows comprehensive view
$ scopes status
Current focus: auth-feature (workspace-level, recursive)
Current context: client-work
Visible scopes: 5 of 247 total
Focus set: 2 minutes ago
      ```typescript

## Future Considerations

### Focus Persistence
- Should focus auto-clear after scope completion?
- How long should focus persist without activity?
- Should there be focus history/recent focuses?

### Focus Behavior
- Multiple focus targets (focus on 2-3 specific scopes)?
- Focus groups (named sets of scopes to focus on)?
- Time-boxed focus sessions with reminders?

## Related Stories

- **US-002**: Create First Scope Hierarchy (provides scope structure)
- **US-004**: Named Context Views (works with focus for filtering)
- **Future**: Workspace Management (automatic focus based on directory)
- **Future**: AI Focus Integration (AI understands current focus for better assistance)

