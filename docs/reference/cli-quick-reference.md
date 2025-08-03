# Scopes CLI Quick Reference

This document provides a concise reference for all Scopes commands. All commands use **aliases** to reference scopes (never internal ULIDs).

## Core Concepts

- **Scope**: The unified entity (projects, tasks, subtasks) identified by human-readable aliases
- **Alias**: User-friendly identifier (e.g., `quiet-river-a4f7`, `auth-feature`)
- **Context**: Named filter views to organize work
- **Focus**: Temporary concentration on specific scopes
- **Aspect**: Flexible metadata for classification

## Scope Management

### Creating Scopes
```bash
# Auto-generate canonical alias
$ scopes create "Implement authentication"
Created scope with ULID: 01H8XGJWBWBAQ1J3T3B8A0V0A8
Canonical alias: quiet-river-a4f7

# Custom canonical alias
$ scopes create "Fix login bug" --alias fix-login
Created scope with ULID: 01H8XGJWCDEFG2K4L5M6N7P8Q9
Canonical alias: fix-login
      ```typescript

### Listing and Viewing
```bash
# List all scopes (respects context/focus)
$ scopes list
Found 12 scopes:

quiet-river-a4f7    Implement authentication          priority=high status=ready
wise-ocean-b2k8     Redesign user dashboard          priority=high status=todo  
swift-mountain-c9x4 Fix critical security bug        priority=high status=done
brave-star-e5n3     Optimize database queries        priority=medium status=ready
gentle-cloud-f8p6   Update documentation             priority=low status=todo

# Show detailed scope information
$ scopes show quiet-river-a4f7
Scope: quiet-river-a4f7
Title: Implement authentication
Status: ready
Created: 2025-01-15 10:30:00
Updated: 2025-01-15 14:22:00

Aliases:
- quiet-river-a4f7 (canonical)
- auth-feature (custom)
- sprint-42 (custom)

Aspects:
- priority: high
- status: ready
- complexity: medium
- estimate: 8h

Parent: None
Children: 2 scopes
    ├── login-ui: Design login interface
    └── password-val: Add password validation

# Display scope hierarchy
$ scopes tree
quiet-river-a4f7    Implement authentication          priority=high
    ├── login-ui      Design login interface           priority=medium
    │   └── form-val  Add form validation              priority=low
    └── password-val  Add password validation          priority=high
          └── hash-impl Implement password hashing       priority=medium
      ```typescript

### Editing Scopes
```bash
# Update scope title
$ scopes update quiet-river-a4f7 --title "Enhanced authentication system"
✓ Updated scope 'quiet-river-a4f7':
    Title: Enhanced authentication system

# Update aspects
$ scopes update quiet-river-a4f7 --status completed
✓ Updated scope 'quiet-river-a4f7':
    status: ready → completed
      ```typescript

## Alias Management

### Alias Operations
```bash
# Add additional alias
$ scopes alias add quiet-river-a4f7 auth-feature
✓ Alias 'auth-feature' assigned to scope 'quiet-river-a4f7'

# List all aliases for scope
$ scopes alias list quiet-river-a4f7
Aliases for scope 01H8XGJWBWBAQ1J3T3B8A0V0A8:
- quiet-river-a4f7 (canonical)
- auth-feature (custom)
- sprint-42 (custom)

# Change canonical alias
$ scopes alias set-canonical quiet-river-a4f7 authentication
✓ 'authentication' is now the canonical alias
    'quiet-river-a4f7' remains as regular alias

# Remove alias
$ scopes alias rm sprint-42
✓ Removed alias 'sprint-42'
      ```typescript

### Resolution
- Exact match: `auth-feature` → specific scope
- Prefix match: `quiet` → `quiet-river-a4f7` (if unique)
- Disambiguation: Multiple matches show all options

## Aspect-Based Classification

### Setting Aspects
```bash
# Set single aspect
$ scopes aspect set quiet-river-a4f7 priority=high
✓ Set aspects on scope 'quiet-river-a4f7':
    priority: high

# Set multiple aspects
$ scopes aspect set quiet-river-a4f7 priority=high status=ready
✓ Set aspects on scope 'quiet-river-a4f7':
    priority: high
    status: ready
      ```typescript

### Querying by Aspects
```bash
# Simple filter
$ scopes list -a priority=high
Found 3 scopes with priority=high:

quiet-river-a4f7    Implement authentication          priority=high status=ready
wise-ocean-b2k8     Redesign user dashboard          priority=high status=todo  
swift-mountain-c9x4 Fix critical security bug        priority=high status=done

# Comparison operator
$ scopes list -a complexity>=medium
Found 5 scopes with complexity>=medium:

calm-forest-d1m2    Add user preferences             complexity=medium priority=low
quiet-river-a4f7    Implement authentication         complexity=high priority=high
wise-ocean-b2k8     Redesign user dashboard         complexity=large priority=high

# Complex query
$ scopes list -a "priority>=high AND status=ready"
Found 2 scopes matching criteria:

quiet-river-a4f7    Implement authentication         priority=high status=ready
brave-star-e5n3     Optimize database queries       priority=high status=ready
      ```typescript

### Aspect Definitions
```bash
# Define ordered aspect
$ scopes aspect define priority --type ordered --values low,medium,high
✓ Defined aspect 'priority' (ordered): low < medium < high

# List all aspect definitions
$ scopes aspect list --definitions
Defined aspects:
- priority (ordered): low < medium < high
- status (text): any text value
- complexity (ordered): low < medium < high < large
- estimate (duration): time values (1h, 2d, etc.)
      ```typescript

## Context Management (Named Views)

### Context Operations
```bash
# Create context
$ scopes context create "client-work" --filter "project=acme AND priority>=medium"
✓ Created context 'client-work' with filter: project=acme AND priority>=medium

# Create global context
$ scopes context create --global "urgent" --filter "priority=high"
✓ Created global context 'urgent'

# Apply context
$ scopes context switch client-work
✓ Switched to context 'client-work'
    Filter: project=acme AND priority>=medium
    Matching scopes: 8

# List contexts
$ scopes context list
Available contexts:
- client-work (active) - project=acme AND priority>=medium
- personal-projects - project=personal
- urgent (global) - priority=high
- default - logged!=true

# Show active context
$ scopes context current
Current context: client-work
Filter: project=acme AND priority>=medium
Level: workspace-local
Matching scopes: 8 of 247 total

# Show context details
$ scopes context show client-work
Context: client-work
Type: workspace-local
Filter: project=acme AND priority>=medium
Created: 2025-01-15 09:00:00
Last used: 2025-01-15 15:30:00
Matching scopes: 8

# Modify context
$ scopes context edit client-work --filter "project=acme AND status!=completed"
✓ Updated context 'client-work':
    Filter: project=acme AND status!=completed

# Remove context
$ scopes context rm old-context
✓ Removed context 'old-context'
      ```typescript

### Context Scopes
- **Global**: Available everywhere (`--global`)
- **Local**: Workspace-specific (`--local`)
- **Default**: System-wide fallback context

## Focus Management

### Focus Operations
```bash
# Focus on scope
$ scopes focus auth-feature
✓ Focused on scope 'auth-feature': Implement user authentication
    Including 2 child scopes

# Include all descendants
$ scopes focus auth-feature --recursive
✓ Focused on 'auth-feature' and all descendants (5 scopes total)

# Clear focus
$ scopes focus clear
✓ Focus cleared

# Set global focus
$ scopes focus --user auth-feature
✓ Set global focus on 'auth-feature'

# Set workspace focus
$ scopes focus --workspace bug-fix-ui
✓ Set workspace focus on 'bug-fix-ui'

# Show current focus
$ scopes focus current
Current focus: auth-feature (Implement user authentication)
Level: workspace-level
Children included: 2 scopes

$ scopes focus
Current focus: auth-feature (workspace-level, recursive)
Visible scopes: 5 of 247 total
Focus set: 2 minutes ago
      ```typescript

### Focus Display
- When focused, all commands respect the focus filter
- Use `--all` or `--no-focus` to bypass temporarily

## Status and Information

### System Status
```bash
# Complete system status
$ scopes status
Current focus: auth-feature (workspace-level, recursive)
Current context: client-work
Workspace: /Users/dev/projects/webapp
Visible scopes: 5 of 247 total
Focus set: 2 minutes ago
Context set: 1 hour ago

Database: ~/.scopes/scopes.db (247 scopes)
Last sync: Never (local-only)

# Version information
$ scopes version
Scopes v0.1.0
Build: 2025-01-15T10:30:00Z
Platform: darwin/arm64

# Help for specific command
$ scopes help focus
scopes-focus - Manage focus on specific scopes

USAGE:
        scopes focus [OPTIONS] [ALIAS]

ARGS:
        <ALIAS>    Scope alias to focus on

OPTIONS:
          --recursive         Include all child scopes
          --user              Set global focus
          --workspace         Set workspace-specific focus
          --clear             Clear current focus
          -h, --help          Print help information
      ```typescript

## Common Workflows

### Daily Development Flow
```bash
# Start work on specific feature
$ scopes context switch client-work
✓ Switched to context 'client-work'
    Filter: project=acme AND priority>=medium
    Matching scopes: 8

$ scopes focus auth-feature --recursive
✓ Focused on 'auth-feature' and all descendants (5 scopes total)

# Work on specific tasks
$ scopes list                           # See focused tasks
[FOCUS: auth-feature (recursive)] [CONTEXT: client-work]

auth-feature         Implement user authentication      priority=high
    ├── login-ui       Design login interface            priority=medium
    │   └── form-val   Add form validation               priority=low
    └── password-val   Add password validation           priority=high
          └── hash-impl  Implement password hashing        priority=medium

$ scopes aspect set login-ui status=in-progress
✓ Set aspects on scope 'login-ui':
    status: in-progress

$ scopes update login-ui --title "Improved login UI"
✓ Updated scope 'login-ui':
    Title: Improved login UI

# Switch context for different work
$ scopes context switch personal-projects
✓ Switched to context 'personal-projects'
    Filter: project=personal
    Matching scopes: 15

$ scopes focus blog-engine
✓ Focused on scope 'blog-engine': Build personal blog
    Including 3 child scopes
      ```typescript

### Project Organization
```bash
# Create organized scope hierarchy
scopes create "Authentication System" --alias auth-system
scopes create "Login UI" --parent auth-system --alias login-ui
scopes create "Password Validation" --parent auth-system --alias pwd-val

# Add metadata
scopes aspect set auth-system priority=high complexity=large
scopes aspect set login-ui priority=medium estimate=4h
scopes aspect set pwd-val priority=high estimate=2h

# Create focused context
scopes context create "auth-work" --filter "parent=auth-system OR alias=auth-system"
      ```typescript

### Advanced Filtering
```bash
# Complex aspect queries
scopes list -a "priority>=medium AND estimate<=8h AND status!=done"
scopes list -a "blocked=false AND (type=feature OR type=bug)"

# Combined context and focus
scopes context switch sprint-current
scopes focus high-priority-items --recursive
scopes list                           # Shows intersection of both filters
      ```typescript

## Tab Completion

All commands support dynamic tab completion:
- `scopes show qu<TAB>` → shows all aliases starting with "qu"
- `scopes focus <TAB>` → shows all available aliases
- `scopes context switch <TAB>` → shows all available contexts

## Output Conventions

### Standard Listing Format
      ```typescript
alias-name           Title/Description                   aspects...
quiet-river-a4f7     Implement authentication           priority=high status=ready
    ├── login-ui       Design login interface            priority=medium
    └── password-val   Add password validation           priority=high
      ```typescript

### Status Indicators
- `[FOCUS: alias]` - Active focus
- `[CONTEXT: name]` - Active context
- `✓` - Success confirmation
- Tree symbols (`├── └──`) for hierarchy

## Error Handling

### Common Errors
```bash
# Ambiguous alias
Error: Prefix 'quiet' matches multiple scopes:
- quiet-river-a4f7 "Implement authentication"
- quiet-mountain-b8e2 "Fix login bug"

# Unknown alias
Error: No scope found with alias 'unknown-alias'

# Invalid aspect query
Error: Invalid aspect query syntax in 'priority=>high'
      ```typescript

## Configuration

### Directory Structure
      ```typescript
~/.scopes/           # Global configuration
├── config.json     # User settings
└── focus.json      # User-level focus state

project/.scopes/     # Workspace configuration  
├── workspace.json  # Workspace settings
└── focus.json      # Workspace focus state
      ```typescript

## See Also

- [User Stories](../explanation/user-stories/) - Detailed usage scenarios
- [Domain Overview](../explanation/domain-overview.md) - Core concepts
- [Architecture Decisions](../explanation/adr/) - Design rationale
