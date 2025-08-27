# How to Use Aliases in Scopes

This guide walks through common workflows and best practices for using the alias system in Scopes.

## Table of Contents
- [Understanding Aliases](#understanding-aliases)
- [Basic Alias Operations](#basic-alias-operations)
- [Advanced Workflows](#advanced-workflows)
- [Team Collaboration](#team-collaboration)
- [Migration and Refactoring](#migration-and-refactoring)
- [Troubleshooting](#troubleshooting)

## Understanding Aliases

### What are Aliases?
Aliases are human-readable identifiers for scopes. Instead of using internal IDs like `01H8XGJWBWBAQ1J3T3B8A0V0A8`, you work with names like `auth-feature` or `quiet-river-x7k`.

### Types of Aliases
1. **Canonical Alias**: Every scope has exactly one canonical alias
   - Auto-generated when scope is created
   - Uses memorable pattern: `adjective-noun-token` (e.g., `bold-tiger-x7k`)
   - Can be regenerated but not removed

2. **Custom Alias**: Optional additional names
   - User-defined for convenience
   - Can have multiple per scope
   - Fully manageable (add, remove, modify)

## Basic Alias Operations

### Creating Scopes with Meaningful Names

#### Let Scopes Generate the Alias
Best for quick tasks or when you don't have a specific name in mind:

```bash
$ scopes create "Fix login validation bug"
Created scope with canonical alias: quiet-river-x7k
```

#### Provide Your Own Alias
Best for important features or when you need a specific reference:

```bash
$ scopes create "Authentication system" --alias auth-system
Created scope with canonical alias: auth-system
```

### Adding Custom Aliases

Add aliases to make scopes easier to find:

```bash
# Add a project code
$ scopes alias add auth-system acme-123

# Add a short name
$ scopes alias add auth-system auth

# Add a version reference
$ scopes alias add auth-system auth-v2
```

### Finding Scopes by Alias

#### Using Exact Names
```bash
$ scopes show auth-system
```

#### Using Partial Names
If unique, you can use prefixes:
```bash
$ scopes show auth-s    # Works if no other alias starts with "auth-s"
```

#### Searching for Aliases
When you can't remember the exact name:
```bash
$ scopes alias search auth
Found 5 aliases starting with 'auth':
- auth-system (canonical)
- auth-api (custom)  
- auth-ui (custom)
- auth-tests (canonical)
- auth-docs (custom)
```

## Advanced Workflows

### Organizing Large Projects

#### Hierarchical Naming Convention
Create a consistent naming scheme for related scopes:

```bash
# Main project
$ scopes create "E-commerce Platform" --alias ecom

# Major components
$ scopes create "User Management" --alias ecom-users --parent ecom
$ scopes create "Product Catalog" --alias ecom-catalog --parent ecom
$ scopes create "Shopping Cart" --alias ecom-cart --parent ecom

# Sub-components
$ scopes create "User Authentication" --alias ecom-users-auth --parent ecom-users
$ scopes create "User Profiles" --alias ecom-users-profile --parent ecom-users
```

#### Using Aliases with Contexts
Create focused work environments:

```bash
# Create a context for user management work
$ scopes context create user-work --filter "alias:starts-with:ecom-users"

# Switch to the context
$ scopes context switch user-work

# Now 'scopes list' shows only user management scopes
$ scopes list
ecom-users          User Management              status=active
ecom-users-auth     User Authentication         status=in-progress  
ecom-users-profile  User Profiles              status=planned
```

### Refactoring with Aliases

#### Renaming Projects
When a project changes direction:

```bash
# Original project
$ scopes show proto-chat
Title: Chat Prototype

# Project evolved - add new alias
$ scopes alias add proto-chat messaging-platform

# Regenerate canonical to reflect new purpose
$ scopes alias regenerate proto-chat
✓ Canonical alias changed to 'swift-message-k9p'
  'proto-chat' is now a custom alias

# Update title
$ scopes update messaging-platform --title "Enterprise Messaging Platform"
```

#### Merging Related Scopes
When consolidating work:

```bash
# Find related scopes
$ scopes alias search login
- login-ui (canonical)
- login-api (canonical)  
- login-tests (custom)

# Create parent scope for consolidation
$ scopes create "Login Feature" --alias login-feature

# Move existing scopes under new parent
$ scopes update login-ui --parent login-feature
$ scopes update login-api --parent login-feature

# Add reference alias to parent
$ scopes alias add login-feature login-all
```

## Team Collaboration

### Shared Alias Conventions

#### Team Prefixes
Establish team-specific prefixes:

```bash
# Frontend team
$ scopes create "Navbar redesign" --alias fe-navbar
$ scopes create "User dashboard" --alias fe-dashboard

# Backend team  
$ scopes create "API optimization" --alias be-api-perf
$ scopes create "Database migration" --alias be-db-migrate

# QA team
$ scopes create "Login testing" --alias qa-login
$ scopes create "Performance tests" --alias qa-perf
```

#### Sprint References
Track work by sprint:

```bash
# Add sprint aliases to active work
$ scopes alias add fe-navbar sprint-42
$ scopes alias add be-api-perf sprint-42
$ scopes alias add qa-login sprint-42

# Find all sprint work
$ scopes alias search sprint-42
```

### Sharing Alias Mappings

#### Export for Team
Share your alias setup:

```bash
# Export your aliases
$ scopes alias export --format csv > team-aliases.csv

# Team member imports
$ scopes alias import team-aliases.csv
✓ Imported 23 aliases
```

#### Documentation Integration
Generate alias documentation:

```bash
# Create markdown documentation
$ scopes alias list --all --format markdown > docs/scope-aliases.md

# Example output:
## Project Aliases

| Alias | Type | Scope Title | Description |
|-------|------|-------------|-------------|
| auth-system | canonical | Authentication System | Main auth module |
| auth-api | custom | Auth API | REST endpoints |
| auth-ui | custom | Auth UI | Login/logout screens |
```

## Migration and Refactoring

### Updating Legacy References

When transitioning from old naming:

```bash
# Old system used codes like "PROJ-123"
# Add these as aliases for backward compatibility
$ scopes alias add auth-system PROJ-123
$ scopes alias add user-module PROJ-124

# Gradually transition to new names
# Then eventually remove old aliases
$ scopes alias rm PROJ-123
$ scopes alias rm PROJ-124
```

### Bulk Alias Updates

For large-scale changes:

```bash
# Export current aliases
$ scopes alias export > aliases-backup.txt

# Modify in your editor (rename prefixes, etc.)
$ sed 's/^old-prefix-/new-prefix-/g' aliases-backup.txt > aliases-updated.txt

# Clear and reimport
$ scopes alias clear --custom  # Remove all custom aliases
$ scopes alias import aliases-updated.txt
```

## Troubleshooting

### Common Issues and Solutions

#### "Alias already exists"
```bash
$ scopes alias add my-scope feature-x
Error: Alias 'feature-x' already exists

# Solution: Check current usage
$ scopes alias find feature-x
Alias 'feature-x' belongs to scope 'swift-river-a8k: Old feature'

# If outdated, reassign
$ scopes alias rm feature-x
$ scopes alias add my-scope feature-x
```

#### "Ambiguous alias" 
```bash
$ scopes show auth
Error: Multiple matches for 'auth'

# Solution: Be more specific or list options
$ scopes alias search auth
$ scopes show auth-system  # Use full name
```

#### Lost Scope References
```bash
# Can't remember any alias for a scope about "login"
$ scopes list --all | grep -i login

# Or search in titles
$ scopes list --all --format json | jq '.[] | select(.title | contains("login"))'
```

### Performance Tips

#### Efficient Alias Usage
1. **Use prefixes for quick access**: Instead of `authentication-system-module`, use `auth-sys`
2. **Avoid overly generic names**: `api` is less useful than `user-api`
3. **Clean up unused aliases**: Remove outdated references periodically

#### Batch Operations
For better performance with many aliases:

```bash
# Instead of individual adds
$ scopes alias add scope1 alias1
$ scopes alias add scope2 alias2
$ scopes alias add scope3 alias3

# Use import
$ cat << EOF > batch-aliases.txt
alias1=scope1
alias2=scope2
alias3=scope3
EOF
$ scopes alias import batch-aliases.txt
```

## Best Practices Summary

### DO:
- ✅ Use descriptive, memorable aliases
- ✅ Establish team naming conventions
- ✅ Keep aliases concise but meaningful
- ✅ Use hierarchical naming for related scopes
- ✅ Document your alias conventions
- ✅ Clean up outdated aliases periodically

### DON'T:
- ❌ Use spaces or special characters
- ❌ Create overly long aliases (keep under 30 chars ideally)
- ❌ Use ambiguous abbreviations
- ❌ Mix naming conventions within a project
- ❌ Delete canonical aliases (regenerate instead)
- ❌ Reuse aliases without checking

## Next Steps

- Read the [CLI Alias Commands Reference](../reference/cli-alias-commands.md) for complete command documentation
- Learn about [Scope Hierarchies](./scope-hierarchies.md) to organize complex projects
- Explore [Context Management](./context-management.md) to filter scopes effectively
- Check [Team Workflows](./team-workflows.md) for collaboration patterns
