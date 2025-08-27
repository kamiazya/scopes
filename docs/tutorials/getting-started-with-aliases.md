# Tutorial: Getting Started with Aliases

This tutorial introduces the alias system in Scopes through hands-on examples. By the end, you'll understand how to use aliases effectively for managing your projects and tasks.

## Prerequisites

- Scopes CLI installed
- Basic command-line familiarity
- About 15 minutes

## What You'll Learn

- How aliases work in Scopes
- Creating scopes with automatic and custom aliases
- Managing multiple aliases per scope
- Finding scopes using aliases
- Best practices for naming

## Part 1: Understanding Aliases

Let's start by creating your first scope and seeing how aliases work.

### Step 1: Create a Scope with Auto-Generated Alias

```bash
$ scopes create "My First Project"
```

Output:
```
Created scope with ULID: 01H8XGJWBWBAQ1J3T3B8A0V0A8
Canonical alias: quiet-river-x7k
```

**What happened?**
- Scopes created a new scope with your title
- It assigned a unique ID (the ULID)
- It generated a memorable canonical alias: `quiet-river-x7k`

The canonical alias follows the pattern `adjective-noun-token`, making it easy to remember and type.

### Step 2: View Your Scope Using the Alias

Instead of using the long ID, use the alias:

```bash
$ scopes show quiet-river-x7k
```

Output:
```
Scope: quiet-river-x7k
Title: My First Project
Created: 2025-01-15 10:30:00
Updated: 2025-01-15 10:30:00

Aliases:
- quiet-river-x7k (canonical)

Parent: None
Children: None
```

### Step 3: Try Partial Matching

You don't always need to type the full alias:

```bash
$ scopes show quiet
```

If `quiet-river-x7k` is the only alias starting with "quiet", this will work!

## Part 2: Custom Aliases

Auto-generated aliases are great, but sometimes you want your own names.

### Step 4: Create a Scope with a Custom Alias

```bash
$ scopes create "Authentication Module" --alias auth-module
```

Output:
```
Created scope with ULID: 01H8XGJWCDEFG2K4L5M6N7P8Q9
Canonical alias: auth-module
```

This time, your custom alias `auth-module` became the canonical alias.

### Step 5: Add Additional Aliases

One scope can have multiple aliases. Let's add more:

```bash
$ scopes alias add auth-module auth
$ scopes alias add auth-module security
```

Now check the aliases:

```bash
$ scopes alias list auth-module
```

Output:
```
Aliases for scope 01H8XGJWCDEFG2K4L5M6N7P8Q9:
- auth-module (canonical)
- auth (custom)
- security (custom)
```

### Step 6: Access the Scope with Any Alias

All these commands show the same scope:

```bash
$ scopes show auth-module
$ scopes show auth
$ scopes show security
```

## Part 3: Working with Multiple Scopes

Let's create a more realistic project structure.

### Step 7: Create Related Scopes

```bash
# Main project
$ scopes create "E-Commerce Website" --alias ecommerce

# Features
$ scopes create "Shopping Cart" --alias cart --parent ecommerce
$ scopes create "User Accounts" --alias users --parent ecommerce
$ scopes create "Product Catalog" --alias catalog --parent ecommerce
```

### Step 8: View the Hierarchy

```bash
$ scopes tree ecommerce
```

Output:
```
ecommerce         E-Commerce Website
├── cart          Shopping Cart
├── users         User Accounts
└── catalog       Product Catalog
```

### Step 9: Add Descriptive Aliases

Make scopes easier to find:

```bash
# Add version info
$ scopes alias add ecommerce v2-platform

# Add team reference
$ scopes alias add cart team-frontend

# Add sprint reference
$ scopes alias add users sprint-42
```

## Part 4: Finding Scopes

As projects grow, finding the right scope becomes important.

### Step 10: Search for Aliases

Find all aliases starting with a prefix:

```bash
$ scopes alias search e
```

Output:
```
Found 2 aliases starting with 'e':
- ecommerce (canonical) → "E-Commerce Website"
```

Search for all cart-related aliases:

```bash
$ scopes alias search cart
```

### Step 11: List All Scopes with Their Aliases

```bash
$ scopes list
```

Output:
```
quiet-river-x7k   My First Project            
auth-module       Authentication Module       
ecommerce         E-Commerce Website          
├── cart          Shopping Cart               
├── users         User Accounts              
└── catalog       Product Catalog
```

## Part 5: Managing Aliases

### Step 12: Handle Naming Conflicts

Try to create a duplicate alias:

```bash
$ scopes create "New Cart Feature" --alias cart
```

Output:
```
Error: Alias 'cart' already exists for scope 'Shopping Cart'
```

Solution - use a different name:

```bash
$ scopes create "New Cart Feature" --alias cart-v2
```

### Step 13: Rename by Regenerating

If you don't like an auto-generated name:

```bash
$ scopes alias regenerate quiet-river-x7k
```

Output:
```
✓ Canonical alias changed from 'quiet-river-x7k' to 'bold-falcon-m3n'
  Previous canonical alias 'quiet-river-x7k' is now a custom alias
```

The old name still works but is now a custom alias!

### Step 14: Clean Up Aliases

Remove aliases you no longer need:

```bash
$ scopes alias rm security
✓ Removed alias 'security'
```

Note: You cannot remove canonical aliases, only regenerate them.

## Part 6: Real-World Patterns

### Step 15: Sprint-Based Work

Organize current sprint tasks:

```bash
# Create sprint scope
$ scopes create "Sprint 42" --alias sprint-42

# Create tasks with sprint prefix
$ scopes create "Fix login bug" --alias sprint-42-login --parent sprint-42
$ scopes create "Update API docs" --alias sprint-42-docs --parent sprint-42

# Find all sprint work
$ scopes list --filter "alias:starts-with:sprint-42"
```

### Step 16: Feature Branches

Mirror your git workflow:

```bash
# Create feature scope matching branch
$ scopes create "New payment gateway" --alias feature/payment-gateway

# Add ticket reference
$ scopes alias add feature/payment-gateway JIRA-1234
```

### Step 17: Quick Access Patterns

Create short aliases for frequently accessed scopes:

```bash
# Current work
$ scopes alias add sprint-42-login current
$ scopes alias add feature/payment-gateway wip

# Quick status check
$ scopes show current
$ scopes show wip
```

## Practice Exercises

### Exercise 1: Project Setup
Create a structure for a blog platform with:
- Main blog scope with alias `blog`
- Three features: posts, comments, admin
- Custom aliases for each feature

<details>
<summary>Solution</summary>

```bash
$ scopes create "Blog Platform" --alias blog
$ scopes create "Post Management" --alias posts --parent blog
$ scopes create "Comment System" --alias comments --parent blog
$ scopes create "Admin Panel" --alias admin --parent blog

# Add custom aliases
$ scopes alias add posts blog-posts
$ scopes alias add comments blog-comments
$ scopes alias add admin blog-admin
```
</details>

### Exercise 2: Alias Search
Using the blog structure from Exercise 1:
1. Find all blog-related aliases
2. Show the admin panel using a prefix
3. List all custom aliases

<details>
<summary>Solution</summary>

```bash
# 1. Find blog aliases
$ scopes alias search blog

# 2. Show admin with prefix (if unique)
$ scopes show adm

# 3. List custom aliases
$ scopes alias list --all | grep "(custom)"
```
</details>

### Exercise 3: Refactoring
Your "posts" feature grew too large. Reorganize it:
1. Create sub-features for "editor" and "publishing"
2. Update aliases to reflect the new structure
3. Remove the old generic "posts" alias

<details>
<summary>Solution</summary>

```bash
# Create sub-features
$ scopes create "Post Editor" --alias post-editor --parent posts
$ scopes create "Publishing System" --alias post-publish --parent posts

# Add descriptive aliases
$ scopes alias add post-editor editor
$ scopes alias add post-publish publish

# Update parent scope alias
$ scopes alias add posts post-system

# Remove old generic alias
$ scopes alias rm blog-posts
```
</details>

## Tips and Tricks

### 1. Naming Conventions
- **Be consistent**: `feature-login`, `feature-payment`, not `login` and `payment-feat`
- **Use prefixes**: `api-users`, `api-products` for grouping
- **Include context**: `acme-api` vs just `api`

### 2. Alias Length
- Short for frequent use: `auth`, `api`, `ui`
- Descriptive for clarity: `user-authentication`, `payment-processing`
- Balance typing speed with clarity

### 3. Case Sensitivity
- Aliases are case-insensitive: `AUTH` = `auth`
- Always stored as lowercase
- Mixed case input is normalized

### 4. Special Characters
- Allowed: letters, numbers, hyphens (-), underscores (_)
- Not allowed: spaces, dots, slashes
- Must start with a letter

## Next Steps

Now that you understand aliases, explore:

1. **[Using Contexts](./working-with-contexts.md)** - Filter scopes for focused work
2. **[Managing Hierarchies](./organizing-with-hierarchies.md)** - Build complex project structures
3. **[Team Collaboration](./collaborating-with-teams.md)** - Share and sync aliases

## Quick Reference Card

| Command | Purpose | Example |
|---------|---------|---------|
| `create --alias` | Create with custom alias | `scopes create "Task" --alias my-task` |
| `alias add` | Add custom alias | `scopes alias add scope-id new-name` |
| `alias list` | Show scope's aliases | `scopes alias list my-task` |
| `alias rm` | Remove custom alias | `scopes alias rm old-name` |
| `alias regenerate` | Change canonical alias | `scopes alias regenerate scope-id` |
| `alias search` | Find aliases by prefix | `scopes alias search proj` |
| `show` | Display scope by alias | `scopes show my-task` |

Remember: Aliases are your friends - use them liberally to make your work easier!
