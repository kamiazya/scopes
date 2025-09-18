# Getting Started with Scopes

This tutorial will guide you through your first steps with Scopes, from installation to creating your first task hierarchy. By the end of this tutorial, you'll understand the core concepts and be ready to manage your projects with Scopes.

## What You'll Learn

- How to install Scopes
- Basic commands and concepts
- Creating your first scope
- Organizing with hierarchies
- Using aliases effectively
- Working with the MCP server for AI integration

## Prerequisites

- A Unix-like operating system (Linux, macOS, or WSL on Windows)
- Basic command-line familiarity
- (Optional) An AI assistant that supports MCP for enhanced features

## Step 1: Installation

### Quick Install (Recommended)

Download and run the installation script:

```bash
curl -fsSL https://github.com/kamiazya/scopes/releases/latest/download/install.sh | bash
```

### Manual Installation

If you prefer manual installation:

1. Download the latest release:
```bash
wget https://github.com/kamiazya/scopes/releases/latest/download/scopes-linux-x64.tar.gz
```

2. Extract to your preferred location:
```bash
tar -xzf scopes-linux-x64.tar.gz -C ~/.local/bin/
```

3. Add to your PATH if needed:
```bash
export PATH="$HOME/.local/bin:$PATH"
```

### Verify Installation

Check that Scopes is installed correctly:

```bash
scopes --version
```

You should see output like:
```
Scopes v0.1.0
Platform: linux/x64
```

## Step 2: Your First Scope

Let's create your first scope - a simple task:

```bash
scopes create "Learn Scopes basics"
```

Output:
```
Created scope with canonical alias: quiet-river-x7k
Title: Learn Scopes basics
```

Notice the auto-generated alias `quiet-river-x7k`. This is a human-friendly identifier that Scopes creates automatically.

### Understanding the Output

- **Canonical alias**: The primary identifier for your scope
- **Title**: The human-readable description
- Every scope gets a unique ULID internally, but you work with aliases

## Step 3: Viewing Your Scopes

List all your scopes:

```bash
scopes list
```

Output:
```
Found 1 scope:

quiet-river-x7k    Learn Scopes basics
```

Get detailed information about a specific scope:

```bash
scopes get quiet-river-x7k
```

Output:
```
Scope Details:
  Canonical Alias: quiet-river-x7k
  Title: Learn Scopes basics
  Created: 2025-01-18 10:30:00
  Parent: None
  Children: 0
```

## Step 4: Creating a Hierarchy

Scopes shine when organizing hierarchical tasks. Let's create a project with subtasks:

```bash
# Create a main project
scopes create "Build personal website" --alias website

# Create subtasks under it
scopes create "Design homepage" --parent website
scopes create "Setup hosting" --parent website
scopes create "Write content" --parent website
```

View the hierarchy:

```bash
scopes list
```

Output:
```
Found 4 scopes:

website             Build personal website
├── gentle-wind-a3m    Design homepage
├── brave-mountain-b7x Setup hosting
└── swift-ocean-c9k    Write content
```

## Step 5: Using Custom Aliases

Auto-generated aliases are unique but not always memorable. Add custom aliases:

```bash
# Add a custom alias to a scope
scopes alias add gentle-wind-a3m homepage-design

# Now you can use either alias
scopes get homepage-design
```

You can even set a custom alias when creating:

```bash
scopes create "Configure DNS" --parent website --alias dns-setup
```

## Step 6: Working with Aspects

Aspects are flexible metadata for classification. Add priority to your tasks:

```bash
# Set priority on tasks
scopes aspect set homepage-design priority=high
scopes aspect set dns-setup priority=medium
scopes aspect set swift-ocean-c9k priority=low
```

Filter scopes by aspects:

```bash
scopes list -a priority=high
```

Output:
```
Found 1 scope with priority=high:

homepage-design     Design homepage              priority=high
```

## Step 7: Quick Updates

As you work, update your scopes:

```bash
# Update the title
scopes update homepage-design --title "Design and implement homepage"

# Mark as complete using aspects
scopes aspect set homepage-design status=completed
```

## Step 8: Using Prefix Matching

Scopes supports smart prefix matching for convenience:

```bash
# Instead of typing the full alias
scopes get quiet-river-x7k

# You can use a unique prefix
scopes get quiet

# Works with custom aliases too
scopes get home  # matches homepage-design
```

## Step 9: MCP Server for AI Integration (Optional)

If you use an AI assistant like Claude, enable the MCP server:

### Start the MCP Server

```bash
scopes mcp
```

### Configure Your AI Assistant

For Claude Desktop, add to your configuration:

```json
{
  "mcpServers": {
    "scopes": {
      "command": "scopes",
      "args": ["mcp"]
    }
  }
}
```

Now your AI assistant can:
- Create and manage scopes directly
- Understand your project structure
- Help organize and plan tasks

## Step 10: Practical Workflow Example

Let's put it all together with a real workflow:

```bash
# 1. Create a project for learning a new programming language
scopes create "Learn Rust programming" --alias learn-rust

# 2. Break it down into chapters
scopes create "Setup development environment" --parent learn-rust --alias rust-setup
scopes create "Learn basic syntax" --parent learn-rust --alias rust-basics
scopes create "Build first project" --parent learn-rust --alias rust-project

# 3. Add more detail to setup
scopes create "Install Rust toolchain" --parent rust-setup
scopes create "Configure VS Code" --parent rust-setup
scopes create "Setup debugging" --parent rust-setup

# 4. Set priorities and estimates
scopes aspect set rust-setup priority=high estimate=2h
scopes aspect set rust-basics priority=high estimate=8h
scopes aspect set rust-project priority=medium estimate=16h

# 5. View your learning path
scopes list -a priority=high
```

## Next Steps

Congratulations! You've learned the basics of Scopes. Here's what to explore next:

### Immediate Next Steps

1. **[Aliases Tutorial](./getting-started-with-aliases.md)** - Deep dive into the alias system
2. **[Using Aliases Guide](../guides/using-aliases.md)** - Best practices and workflows
3. **[CLI Quick Reference](../reference/cli-quick-reference.md)** - All commands at your fingertips

### Advanced Features

Once comfortable with basics:

- **Contexts**: Create named views for different work modes
- **Focus Management**: Concentrate on specific scope branches
- **MCP Integration**: Leverage AI for task management
- **Aspects**: Build complex classification systems

### Tips for Success

1. **Start Simple**: Begin with flat lists, add hierarchy as needed
2. **Use Meaningful Aliases**: Create custom aliases for important scopes
3. **Leverage Aspects**: Use them for status, priority, estimates
4. **Explore Prefix Matching**: Save typing with smart prefixes
5. **Integrate with Your Workflow**: Use MCP for AI-assisted management

## Getting Help

- Run `scopes --help` for general help
- Run `scopes <command> --help` for command-specific help
- Check the [CLI Quick Reference](../reference/cli-quick-reference.md)
- Report issues at the [GitHub repository](https://github.com/kamiazya/scopes)

## Summary

You've learned how to:
- ✅ Install and verify Scopes
- ✅ Create scopes with auto-generated aliases
- ✅ Build hierarchical task structures
- ✅ Use custom aliases for easier access
- ✅ Apply aspects for classification
- ✅ Update and manage scopes
- ✅ Integrate with AI through MCP

Welcome to the Scopes community! Start organizing your work in a more intuitive, AI-friendly way.