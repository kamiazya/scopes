# Setup and Configuration

This directory contains technical setup guides and configuration documentation for various development environments and tools.

## Available Guides

### Development Environment Setup
- **[GraalVM Setup](./graalvm-setup.md)** - Complete guide for setting up GraalVM for native compilation
- **[Shell Completion](./shell-completion.md)** - Setting up command-line completion for various shells

### Build and Packaging
- **[Packaging Conventions](./packaging-conventions.md)** - Project packaging standards and conventions

## When to Use These Guides

These guides are **technical references** for specific setup scenarios:

- **GraalVM Setup**: Only needed if you plan to compile native binaries locally
- **Shell Completion**: Optional enhancement for better CLI experience  
- **Packaging Conventions**: For contributors working on build/packaging improvements

## For Regular Development

Most developers won't need these guides. For regular development work, see:

- [Development Guidelines](../../guides/development/) - Core development practices
- [Getting Started Tutorial](../../tutorials/getting-started.md) - Basic usage
- [Architecture Testing Guide](../../guides/architecture-testing-guide.md) - Running tests

## Contributing to Setup Guides

When adding new setup documentation:

1. Focus on specific, technical configuration steps
2. Include troubleshooting sections
3. Provide platform-specific instructions where needed
4. Keep guides focused on one tool/environment per file
