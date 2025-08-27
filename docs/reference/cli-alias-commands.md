# CLI Alias Commands Reference

This document provides comprehensive documentation for all alias-related commands in the Scopes CLI.

## Overview

The alias system in Scopes provides human-readable identifiers for scopes. Every scope has:
- **One canonical alias**: Auto-generated or custom, always unique
- **Zero or more custom aliases**: Additional names for convenience

## Core Concepts

### Alias Types
- **Canonical Alias**: The primary identifier, automatically generated using the Haikunator pattern (e.g., `quiet-river-x7k`)
- **Custom Alias**: User-defined additional aliases for easier reference

### Alias Rules
- Must be 2-64 characters long
- Can contain lowercase letters, numbers, hyphens, and underscores
- Must start with a letter
- Case-insensitive (automatically normalized to lowercase)
- Must be globally unique across all scopes

## Command Reference

### Creating Scopes with Aliases

#### Auto-Generated Canonical Alias
```bash
$ scopes create "Implement authentication"
Created scope with ULID: 01H8XGJWBWBAQ1J3T3B8A0V0A8
Canonical alias: quiet-river-x7k
```

The system automatically generates a memorable canonical alias using the pattern: `adjective-noun-token`

#### Custom Canonical Alias
```bash
$ scopes create "Fix login bug" --alias fix-login
Created scope with ULID: 01H8XGJWCDEFG2K4L5M6N7P8Q9
Canonical alias: fix-login
```

If you provide a custom alias during creation, it becomes the canonical alias instead of an auto-generated one.

#### Invalid Alias Examples
```bash
# Too short
$ scopes create "Task" --alias a
Error: Alias must be between 2 and 64 characters

# Invalid characters
$ scopes create "Task" --alias "my task"
Error: Alias can only contain letters, numbers, hyphens, and underscores

# Starts with number
$ scopes create "Task" --alias 1task
Error: Alias must start with a letter

# Already exists
$ scopes create "New task" --alias fix-login
Error: Alias 'fix-login' already exists for another scope
```

### Managing Aliases

#### Add Custom Alias
```bash
$ scopes alias add quiet-river-x7k auth-feature
✓ Alias 'auth-feature' assigned to scope 'quiet-river-x7k'

# Case normalization
$ scopes alias add quiet-river-x7k AUTH-SYSTEM
✓ Alias 'auth-system' assigned to scope 'quiet-river-x7k'
```

#### List Aliases for a Scope
```bash
$ scopes alias list quiet-river-x7k
Aliases for scope 01H8XGJWBWBAQ1J3T3B8A0V0A8:
- quiet-river-x7k (canonical)
- auth-feature (custom)
- auth-system (custom)
- sprint-42 (custom)
```

#### Remove Alias
```bash
# Remove custom alias
$ scopes alias rm sprint-42
✓ Removed alias 'sprint-42'

# Cannot remove canonical alias
$ scopes alias rm quiet-river-x7k
Error: Cannot remove canonical alias. Use 'alias regenerate' to change it.
```

#### Regenerate Canonical Alias
```bash
$ scopes alias regenerate quiet-river-x7k
✓ Canonical alias changed from 'quiet-river-x7k' to 'bold-eagle-m3n'
  Previous canonical alias 'quiet-river-x7k' is now a custom alias
```

This generates a new canonical alias and converts the old one to a custom alias for backward compatibility.

### Finding Scopes by Alias

#### Show Scope by Alias
```bash
$ scopes show auth-feature
Scope: auth-feature
Title: Implement authentication
Status: ready
Created: 2025-01-15 10:30:00
Updated: 2025-01-15 14:22:00

Aliases:
- bold-eagle-m3n (canonical)
- auth-feature (custom)
- auth-system (custom)

Parent: None
Children: 2 scopes
```

#### Alias Resolution
The CLI supports flexible alias resolution:

```bash
# Exact match
$ scopes show auth-feature
✓ Found scope with alias 'auth-feature'

# Prefix match (if unique)
$ scopes show auth-f
✓ Found scope with alias 'auth-feature' (matched prefix)

# Ambiguous prefix
$ scopes show auth
Error: Prefix 'auth' matches multiple scopes:
- auth-feature "Implement authentication"
- auth-system "Authentication system module"
- auth-tests "Authentication test suite"
Please provide a more specific alias.

# Case insensitive
$ scopes show AUTH-FEATURE
✓ Found scope with alias 'auth-feature' (normalized from 'AUTH-FEATURE')
```

### Searching Aliases

#### Search by Prefix
```bash
$ scopes alias search auth
Found 5 aliases starting with 'auth':
- auth-feature (custom) → "Implement authentication"
- auth-system (custom) → "Authentication system module"
- auth-tests (canonical) → "Authentication test suite"
- auth-ui (custom) → "Authentication UI components"
- auth-api (canonical) → "Authentication API endpoints"

# Limit results
$ scopes alias search auth --limit 3
Found 3 aliases starting with 'auth' (showing first 3):
- auth-feature (custom) → "Implement authentication"
- auth-system (custom) → "Authentication system module"
- auth-tests (canonical) → "Authentication test suite"
```

#### List All Aliases
```bash
$ scopes alias list --all
Total aliases: 47 (23 canonical, 24 custom)

Canonical aliases:
- quiet-river-x7k → "Implement authentication"
- bold-eagle-m3n → "Database optimization"
- swift-tiger-k9p → "UI redesign"
...

Custom aliases:
- auth-feature → "Implement authentication"
- db-perf → "Database optimization"
- new-ui → "UI redesign"
...
```

## Advanced Usage

### Bulk Alias Operations

#### Import Aliases from File
```bash
$ cat aliases.txt
auth-feature=quiet-river-x7k
db-perf=bold-eagle-m3n
new-ui=swift-tiger-k9p

$ scopes alias import aliases.txt
✓ Imported 3 aliases:
  - auth-feature → quiet-river-x7k
  - db-perf → bold-eagle-m3n
  - new-ui → swift-tiger-k9p
```

#### Export Aliases
```bash
$ scopes alias export > my-aliases.txt
✓ Exported 47 aliases to my-aliases.txt
```

### Alias Validation

#### Check Alias Availability
```bash
$ scopes alias check my-new-alias
✓ Alias 'my-new-alias' is available

$ scopes alias check auth-feature
✗ Alias 'auth-feature' is already in use by scope 'quiet-river-x7k'
```

#### Validate Alias Format
```bash
$ scopes alias validate "my alias"
✗ Invalid alias format:
  - Cannot contain spaces
  - Valid pattern: [a-z][a-z0-9-_]{1,63}

$ scopes alias validate my-valid-alias
✓ Valid alias format
```

## Integration with Other Commands

### Using Aliases in Commands
All scope-referencing commands accept aliases:

```bash
# Update using alias
$ scopes update auth-feature --title "Enhanced authentication"

# Focus using alias
$ scopes focus auth-feature --recursive

# Show hierarchy using alias
$ scopes tree auth-feature

# Add child using parent alias
$ scopes create "Login form" --parent auth-feature
```

### Tab Completion
The CLI provides intelligent tab completion for aliases:

```bash
# Complete alias
$ scopes show auth<TAB>
auth-api       auth-feature   auth-system    auth-tests     auth-ui

# Complete command options after alias
$ scopes alias add quiet-river-x7k <TAB>
<Enter custom alias name>
```

## Error Messages and Troubleshooting

### Common Errors

#### Duplicate Alias
```bash
$ scopes alias add some-scope auth-feature
Error: Alias 'auth-feature' already exists for scope 'quiet-river-x7k'
Hint: Use 'scopes alias list auth-feature' to see current assignment
```

#### Invalid Alias Format
```bash
$ scopes alias add some-scope "My Alias"
Error: Invalid alias format 'My Alias'
- Must be 2-64 characters long
- Can only contain lowercase letters, numbers, hyphens, and underscores
- Must start with a letter
- Automatically normalized to lowercase
Example: my-alias, feature_1, test-123
```

#### Scope Not Found
```bash
$ scopes show unknown-alias
Error: No scope found with alias 'unknown-alias'
Hint: Use 'scopes alias search unknown' to find similar aliases
```

#### Cannot Remove Canonical Alias
```bash
$ scopes alias rm bold-eagle-m3n
Error: Cannot remove canonical alias 'bold-eagle-m3n'
Hint: Use 'scopes alias regenerate' to change the canonical alias
```

### Troubleshooting Tips

1. **Finding Lost Scopes**: If you can't remember an alias, use search:
   ```bash
   $ scopes list --all | grep -i "authentication"
   $ scopes alias search auth
   ```

2. **Resolving Conflicts**: When adding an alias that might exist:
   ```bash
   $ scopes alias check my-alias && scopes alias add scope-id my-alias
   ```

3. **Cleaning Up Unused Aliases**: List orphaned aliases (after scope deletion):
   ```bash
   $ scopes alias list --orphaned
   Found 2 orphaned aliases (scope was deleted):
   - old-feature
   - deprecated-module
   
   $ scopes alias cleanup
   ✓ Removed 2 orphaned aliases
   ```

## Best Practices

### Naming Conventions
1. **Use descriptive names**: `user-auth` instead of `ua`
2. **Include context**: `client-acme-api` instead of just `api`
3. **Version suffixes**: `parser-v2` for major revisions
4. **Team prefixes**: `team-alpha-sprint-1`

### Alias Organization
1. **Hierarchical naming**: 
   - Parent: `auth-system`
   - Children: `auth-system-login`, `auth-system-token`

2. **Project prefixes**:
   - `web-homepage`
   - `web-admin`
   - `api-public`
   - `api-internal`

3. **Status suffixes**:
   - `feature-x-wip` (work in progress)
   - `bugfix-y-done`
   - `hotfix-z-urgent`

### Performance Considerations
- Alias lookups are optimized for speed (< 20ms)
- Search operations use indexed prefix matching
- Custom aliases have no performance penalty
- Bulk operations are batched for efficiency

## Configuration

### Alias Generation Settings
Configure in `~/.scopes/config.json`:

```json
{
  "alias": {
    "generation": {
      "strategy": "haikunator",
      "wordLists": {
        "adjectives": ["bold", "swift", "quiet", "wise"],
        "nouns": ["river", "mountain", "ocean", "forest"]
      }
    },
    "validation": {
      "minLength": 2,
      "maxLength": 64,
      "pattern": "^[a-z][a-z0-9-_]{1,63}$"
    }
  }
}
```

### Import/Export Formats
Aliases can be imported/exported in multiple formats:

```bash
# Simple format (default)
auth-feature=quiet-river-x7k
db-perf=bold-eagle-m3n

# JSON format
$ scopes alias export --format json
{
  "aliases": [
    {
      "alias": "auth-feature",
      "scopeId": "01H8XGJWBWBAQ1J3T3B8A0V0A8",
      "type": "custom"
    }
  ]
}

# CSV format
$ scopes alias export --format csv
alias,scopeId,type,scopeTitle
auth-feature,01H8XGJWBWBAQ1J3T3B8A0V0A8,custom,"Implement authentication"
```

## See Also

- [Quick Reference](./cli-quick-reference.md) - General CLI command reference
- [Scope Management](../guides/scope-management.md) - Managing scopes effectively
- [Architecture Decision: Alias System](../explanation/adr/00XX-alias-system.md) - Design rationale
- [API Reference: Alias Domain](../reference/api/alias-domain.md) - Technical API details
