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
```

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
```

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
```

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
```

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
```

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
```

### Aspect Definitions
```bash
# Define text aspect (default)
$ scopes aspect define description --type text
✓ Defined aspect 'description' (text): any text value

# Define numeric aspect
$ scopes aspect define estimate --type numeric
✓ Defined aspect 'estimate' (numeric): numeric values only

# Define boolean aspect
$ scopes aspect define completed --type boolean
✓ Defined aspect 'completed' (boolean): true/false, yes/no, 1/0

# Define ordered aspect with custom values
$ scopes aspect define priority --type ordered --values low,medium,high,critical
✓ Defined aspect 'priority' (ordered): low < medium < high < critical

# Define duration aspect (ISO 8601 format)
$ scopes aspect define timeSpent --type duration
✓ Defined aspect 'timeSpent' (duration): ISO 8601 durations (P1D, PT2H30M, etc.)

# List all aspect definitions
$ scopes aspect definitions
Aspect Definitions:

• priority - Task priority level
  Type: Ordered (4 values)

• status - Task status
  Type: Text

• type - Task type classification
  Type: Text

• completed - Completion status
  Type: Boolean (true/false, yes/no, 1/0)

• estimate - Estimated effort
  Type: Numeric

• timeSpent - Time spent on task
  Type: Duration (ISO 8601)

# Show specific aspect definition
$ scopes aspect show priority
Aspect Definition: priority
Description: Task priority level
Type: Ordered
Values: low < medium < high < critical
Allow Multiple: No
Created: 2025-01-15 10:30:00

# Update aspect definition
$ scopes aspect update priority --description "Updated priority levels"
✓ Updated aspect definition 'priority':
    Description: Updated priority levels

# Delete aspect definition (with confirmation)
$ scopes aspect delete old-aspect
Warning: This will delete the aspect definition 'old-aspect' and remove it from all scopes.
Type 'yes' to confirm: yes
✓ Deleted aspect definition 'old-aspect'
```

## Context Management (Named Views)

### Context Operations
```bash
# Create context with filter
$ scopes context create my-work "My Work" --filter "assignee=me AND status!=closed" -d "Active tasks assigned to me"
✓ Context view 'my-work' created successfully
Key: my-work
Name: My Work
Description: Active tasks assigned to me
Filter: assignee=me AND status!=closed

# Create context for client project
$ scopes context create client-a "Client A Project" --filter "project=client-a"
✓ Context view 'client-a' created successfully

# List all contexts
$ scopes context list
Context Views (3):

• my-work [CURRENT] - My Work
  Active tasks assigned to me
  Filter: assignee=me AND status!=closed

• client-a - Client A Project
  Filter: project=client-a

• urgent - Urgent Items
  High priority tasks
  Filter: priority=high OR priority=critical

# Show specific context details
$ scopes context show my-work
Context View Details
===================

Key: my-work
Name: My Work
Description: Active tasks assigned to me

Filter Expression:
  assignee=me AND status!=closed

Timestamps:
  Created: 2025-01-15 10:30:00
  Updated: 2025-01-15 14:22:00

# Edit existing context
$ scopes context edit my-work --name "My Active Tasks" --filter "assignee=me AND status=active"
✓ Context view 'my-work' updated successfully

# Switch to a different context
$ scopes context switch client-a
✓ Switched to context 'client-a'
Active filter: project=client-a

# Show current active context
$ scopes context current
Current context: client-a
Key: client-a
Name: Client A Project
Filter: project=client-a

# Clear current context
$ scopes context current --clear
✓ Current context cleared. All scopes will be visible.

# Delete a context
$ scopes context delete old-context
✓ Context view 'old-context' deleted successfully

# List scopes with active context filter applied
$ scopes list
Using context filter: project=client-a
Found 8 scopes:
...

# List scopes ignoring the active context
$ scopes list --no-context
Found 42 scopes:
...

# Combine context filter with additional query
$ scopes list --query "priority=high"
Using context filter: project=client-a
Found 3 scopes:
... (shows only high priority items within client-a project)
```

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
```

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
```

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
```

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
```

### Advanced Filtering

#### Query Operators
- **Comparison**: `=`, `!=`, `<`, `>`, `<=`, `>=`
- **Logical**: `AND`, `OR`, `NOT`
- **Grouping**: `(` and `)` for precedence

#### Type-Specific Queries
```bash
# Text comparisons (case-sensitive)
$ scopes list -a 'status="in-progress"'
$ scopes list -a 'description!="legacy code"'

# Numeric comparisons
$ scopes list -a "estimate>=5"
$ scopes list -a "storyPoints<8"

# Boolean comparisons
$ scopes list -a "completed=true"
$ scopes list -a "blocked!=yes"

# Ordered aspect comparisons (based on defined order)
$ scopes list -a "priority>=medium"        # medium, high, critical
$ scopes list -a "priority<high"           # low, medium

# Duration comparisons (ISO 8601 format)
$ scopes list -a "timeSpent>PT2H"          # More than 2 hours
$ scopes list -a "estimatedTime<=P1D"      # Less than or equal to 1 day
$ scopes list -a "actualTime>=P1W"         # Greater than or equal to 1 week
```

#### Complex Logical Queries
```bash
# Multiple conditions with AND
$ scopes list -a "priority>=medium AND estimate<=8h AND status!=done"

# Alternative conditions with OR
$ scopes list -a "blocked=false OR priority=critical"

# Negation with NOT
$ scopes list -a "NOT (status=done OR status=cancelled)"

# Complex grouping with parentheses
$ scopes list -a "(priority=high OR priority=critical) AND estimate<=P3D"
$ scopes list -a "NOT (completed=true) AND (type=feature OR type=bug)"

# Nested logical expressions
$ scopes list -a "((priority>=high AND estimate<=PT4H) OR type=hotfix) AND NOT blocked=true"
```

#### Duration Format Examples
```bash
# Basic durations
"PT30M"     # 30 minutes
"PT2H"      # 2 hours  
"P1D"       # 1 day
"P1W"       # 1 week

# Combined durations
"P1DT2H30M" # 1 day, 2 hours, 30 minutes
"P2DT3H4M"  # 2 days, 3 hours, 4 minutes

# Query examples
$ scopes list -a "estimatedTime<PT4H"               # Less than 4 hours
$ scopes list -a "actualTime>=P1D AND actualTime<=P1W" # Between 1 day and 1 week
```

#### Query Validation
```bash
# Invalid syntax examples (will show helpful errors)
$ scopes list -a "priority=>high"          # Invalid operator
Error: Invalid operator '=>' in query. Use '>=' instead.

$ scopes list -a "priority=high AND"       # Incomplete expression
Error: Incomplete logical expression after 'AND'

$ scopes list -a "(priority=high"          # Unmatched parentheses
Error: Unmatched opening parenthesis in query

$ scopes list -a "unknownField=value"      # Undefined aspect
Warning: Aspect 'unknownField' is not defined. Query will match no scopes.
```

#### Combined Context and Focus
```bash
# Apply context filter first, then additional query
$ scopes context switch sprint-current
$ scopes focus high-priority-items --recursive
$ scopes list -a "NOT completed=true"      # Shows intersection of all filters
[FOCUS: high-priority-items (recursive)] [CONTEXT: sprint-current]

Found 8 scopes matching query 'NOT completed=true':
auth-feature         Implement authentication      priority=high status=in-progress
login-ui            Design login interface        priority=medium status=ready
password-val        Add password validation       priority=high status=ready
```

## Tab Completion

All commands support dynamic tab completion:
- `scopes show qu<TAB>` → shows all aliases starting with "qu"
- `scopes focus <TAB>` → shows all available aliases
- `scopes context switch <TAB>` → shows all available contexts

## Output Conventions

### Standard Listing Format
```text
alias-name           Title/Description                   aspects...
quiet-river-a4f7     Implement authentication           priority=high status=ready
    ├── login-ui       Design login interface            priority=medium
    └── password-val   Add password validation           priority=high
```

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
```

## Configuration

### Directory Structure
```text
~/.scopes/           # Global configuration
├── config.json     # User settings
└── focus.json      # User-level focus state

project/.scopes/     # Workspace configuration
├── workspace.json  # Workspace settings
└── focus.json      # Workspace focus state
```

## See Also

- [User Stories](../explanation/user-stories/) - Detailed usage scenarios
- [Domain Overview](../explanation/domain-overview.md) - Core concepts
- [Architecture Decisions](../explanation/adr/) - Design rationale
## System Information

### Show client and daemon information
```bash
$ scopes info
Client:
 Version:     0.1.0
 Config Dir:  /Users/alice/.scopes
 Platform:    macos/arm64

Server:
 Status:      Running
 Version:     0.1.0
 API Version: v1beta
 PID:         12345
 Address:     127.0.0.1:52345
 Uptime:      1h 03m 12s
 Started:     2025-09-26T13:45:35Z
```

Notes:
- If the daemon is not running, `Server: Status: Not running` is shown.
- The CLI resolves the daemon endpoint using `SCOPESD_ENDPOINT` first, then the platform endpoint file.

### Run create via gRPC gateway (experimental)
```bash
$ SCOPES_TRANSPORT=grpc scopes create "My Task" -d "Try gateway"
```
Notes:
- When `SCOPES_TRANSPORT=grpc` is set, eligible commands (e.g., `create`) call the daemon via gRPC TaskGatewayService.
- Unset the variable to use the local (in-process) path as before.
