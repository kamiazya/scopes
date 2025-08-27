# Alias Usage Guide

This guide explains how to use aliases in Scopes for easier scope management and navigation.

## Overview

Every scope in the system can have multiple aliases - human-readable names that make it easier to reference scopes without remembering their ULIDs. This guide covers how to work with aliases effectively.

## Types of Aliases

### Canonical Aliases
- **Auto-generated**: Created automatically when a scope is created
- **Format**: Memorable phrases like `witty-penguin-42`, `clever-dolphin-123`
- **Purpose**: Provide a default human-readable name
- **Uniqueness**: Guaranteed to be unique across the system
- **Special property**: Only one canonical alias per scope

### Custom Aliases
- **User-defined**: Created manually by users
- **Format**: Any valid alias format (alphanumeric, hyphens, underscores)
- **Purpose**: Meaningful names for your workflow
- **Examples**: `my-project`, `feature-auth`, `bug-fix-123`
- **Multiple allowed**: A scope can have many custom aliases

## Alias Naming Rules

Valid aliases must follow these rules:
- **Length**: 2-64 characters
- **Characters**: Letters (a-z, A-Z), numbers (0-9), hyphens (-), underscores (_)
- **Start/End**: Must start and end with alphanumeric characters
- **No consecutive special characters**: `--` or `__` are not allowed

### Valid Examples
- `my-project`
- `feature_123`
- `BugFix2024`
- `sprint-15-backend`

### Invalid Examples
- `a` (too short)
- `-project` (starts with hyphen)
- `project-` (ends with hyphen)
- `my--project` (consecutive hyphens)
- `my project` (contains space)
- `project@123` (contains special character)

## Using Aliases in Commands

All scope commands accept either a ULID or an alias:

```bash
# Get scope by ULID
scopes get 01HZQB5QKM0WDG7ZBHSPKT3N2Y

# Get scope by alias (same scope)
scopes get my-project

# Update scope using alias
scopes update my-project --title "Updated Title"

# Delete scope using alias
scopes delete old-feature
```

## Managing Aliases

### Creating a Scope with Custom Alias

```bash
# Create with auto-generated canonical alias
scopes create "My New Project" --description "Project description"

# Create with custom alias (no auto-generated alias)
scopes create "My New Project" --alias my-project

# The custom alias becomes the canonical alias for the scope
```

### Adding Aliases to Existing Scope

```bash
# Add a custom alias to a scope (using its canonical alias)
scopes alias add witty-penguin-42 my-project

# Add another alias using the custom alias
scopes alias add my-project project-v2

# Now the scope has three aliases: witty-penguin-42, my-project, project-v2
```

### Listing Aliases

```bash
# List all aliases for a scope
scopes alias list 01HZQB5QKM0WDG7ZBHSPKT3N2Y

# Or using any of its aliases
scopes alias list my-project
```

### Renaming Aliases

```bash
# Rename an alias
scopes alias rename old-name new-name

# The scope remains accessible through all other aliases
```

### Removing Aliases

```bash
# Remove a custom alias
scopes alias remove old-alias

# Note: Cannot remove the canonical alias
```

### Changing Canonical Alias

```bash
# Make a custom alias the new canonical alias
scopes alias set-canonical my-project preferred-name

# Now preferred-name is canonical, my-project becomes a regular custom alias
```

## Best Practices

### Naming Conventions

1. **Projects**: Use descriptive names
   - Good: `website-redesign`, `mobile-app-v2`
   - Avoid: `proj1`, `thing`

2. **Features/Epics**: Include context
   - Good: `auth-oauth-integration`, `payment-stripe-api`
   - Avoid: `feature1`, `epic2`

3. **Tasks**: Be specific
   - Good: `fix-login-validation`, `add-user-avatar`
   - Avoid: `fix`, `update`

### Alias Organization

1. **Use prefixes** for grouping:
   ```bash
   backend-auth
   backend-api
   frontend-ui
   frontend-components
   ```

2. **Version aliases** for iterations:
   ```bash
   project-v1
   project-v2
   project-latest
   ```

3. **Environment aliases** for deployment:
   ```bash
   api-dev
   api-staging
   api-prod
   ```

### Workflow Tips

1. **Start with meaningful aliases**: When creating a scope, use the `--alias` flag to set a meaningful name immediately

2. **Add context aliases**: As a scope evolves, add aliases that reflect its current state or purpose

3. **Clean up old aliases**: Remove aliases that are no longer relevant to avoid confusion

4. **Document important aliases**: Keep a list of critical aliases in your project documentation

## Troubleshooting

### Common Issues

**"Alias already exists"**
- Each alias must be unique across all scopes
- Try a more specific name or add a suffix

**"Invalid alias format"**
- Check the naming rules above
- Ensure no special characters or spaces

**"Cannot remove canonical alias"**
- Set a different alias as canonical first
- Then the old canonical becomes removable

**"Alias not found"**
- Verify the exact spelling
- Use `scopes alias list` to see all aliases for a scope

## Integration with AI Assistants

When working with AI assistants through MCP:
- AI can resolve natural language references using aliases
- "this" and "that" pronouns work with focused scopes
- Aliases provide stable references across conversations

Example:
```
Human: "Show me the status of the auth feature"
AI: [Resolves 'auth feature' to 'feature-auth' alias]
```

## Summary

Aliases make working with scopes more intuitive and efficient. By following the naming conventions and best practices in this guide, you can create a well-organized and easily navigable scope hierarchy that works seamlessly with both human developers and AI assistants.
