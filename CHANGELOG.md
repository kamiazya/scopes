# scopes

## 0.0.1

### Patch Changes

- [#260](https://github.com/kamiazya/scopes/pull/260) [`a6e3e06`](https://github.com/kamiazya/scopes/commit/a6e3e065452f9dadaecfde8fb65dfc3c3c9c5858) Thanks [@kamiazya](https://github.com/kamiazya)! - ## Initial Release - Scopes v0.0.1

  **Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants. This initial release introduces the revolutionary unified "Scope" concept that treats projects, epics, and tasks as a single recursive entity type.

  ### 🚀 Core Features

  #### Unified Scope Management

  - **Recursive Hierarchy**: Unlimited depth project organization with consistent operations at every level
  - **Auto-Generated Aliases**: Human-friendly identifiers (e.g., `quiet-river-x7k`) for easy reference
  - **Custom Aliases**: User-defined aliases for important scopes
  - **Smart Prefix Matching**: Quick scope access with partial alias matching

  #### Flexible Metadata System

  - **Aspects**: Key-value metadata for multi-dimensional classification
  - **Built-in Aspects**: Priority, status, type, and more
  - **Custom Aspects**: Define your own classification system
  - **Advanced Querying**: Complex filtering with logical operators

  #### AI-Native Architecture

  - **MCP Integration**: 11 implemented MCP tools for AI assistant integration
  - **Claude Desktop Support**: Ready-to-use configuration for Claude
  - **Comment-Based Collaboration**: Asynchronous AI interaction through scope comments
  - **Context Preservation**: Maintain conversation history within scope context

  #### Local-First Design

  - **Offline-First**: All features work without internet connectivity
  - **SQLite Backend**: Reliable local data storage
  - **Zero Configuration**: Works immediately without setup
  - **Cross-Platform**: Native support for Linux, macOS, and Windows

  ### 🛠 Implementation Status

  #### ✅ Fully Implemented

  - **Core Scope CRUD**: Create, read, update, delete operations
  - **Alias Management**: Canonical and custom alias handling
  - **Aspect System**: Flexible metadata with type validation
  - **Context Views**: Named filtered views for workflow organization
  - **CLI Interface**: Complete command-line interface with 40+ commands
  - **MCP Server**: 11 tools for AI assistant integration
  - **Architecture**: Clean Architecture with DDD principles

  #### 🚧 Partial Implementation

  - **Context CLI Commands**: Backend complete, some CLI subcommands pending
  - **Complex Aspect Queries**: Advanced filtering capabilities
  - **Hierarchy Policies**: Configurable depth and children limits

  #### ❌ Planned Features (Future Releases)

  - **Focus Management**: Workspace-based scope attention
  - **Tree Visualization**: Hierarchical display commands
  - **Status Dashboard**: System overview and metrics
  - **Device Synchronization**: Multi-device data sync (infrastructure exists)
  - **Event Sourcing**: Audit trail and time-travel (infrastructure exists)

  ### 🏗 Architecture Highlights

  - **Clean Architecture**: Clear separation of concerns with dependency inversion
  - **Domain-Driven Design**: Bounded contexts with explicit contracts
  - **Functional Programming**: Arrow-based error handling and immutable data
  - **Architecture Testing**: Konsist validation of design principles
  - **Type Safety**: Strong typing with value objects and domain validation

  ### 📊 Technical Specifications

  - **Language**: Kotlin with coroutines
  - **Build System**: Gradle with GraalVM Native Image
  - **Database**: SQLite for local-first storage
  - **Error Handling**: Arrow Either for functional error management
  - **Testing**: Kotest with property-based testing
  - **Binary Size**: ~50MB native executables
  - **Startup Time**: Sub-second cold start
  - **Memory Usage**: Minimal footprint with GraalVM optimization

  ### 🎯 Target Users

  - **AI-Driven Developers**: Using AI assistants as primary development partners
  - **Tech Leads**: Managing design quality before team presentation
  - **OSS Contributors**: Contributing to multiple international projects
  - **Multi-Device Developers**: Working across laptop, desktop, and remote servers
  - **International Engineers**: Breaking language barriers with AI assistance

  ### 🚀 Getting Started

  ```bash
  # Installation
  curl -fsSL https://github.com/kamiazya/scopes/releases/latest/download/install.sh | bash

  # Create your first scope
  scopes create "My First Project"

  # Add custom alias
  scopes alias add quiet-river-x7k my-project

  # Set priority
  scopes aspect set my-project priority=high

  # List all scopes
  scopes list
  ```

  ### 🤖 AI Integration

  Configure Claude Desktop for AI-powered task management:

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

  This initial release establishes Scopes as a foundation for revolutionary human-AI collaborative task management, with a robust architecture ready for future enhancements in device synchronization, real-time collaboration, and advanced AI integration.
