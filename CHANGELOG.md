# scopes

## 0.0.5

### Patch Changes

- [#292](https://github.com/kamiazya/scopes/pull/292) [`6c03179`](https://github.com/kamiazya/scopes/commit/6c031790a100440828b31c8d5f704de52bd0f120) Thanks [@kamiazya](https://github.com/kamiazya)! - fix: resolve GitHub Actions release failure by adding redundant trigger mechanisms

  - Add push trigger to release.yml for automatic releases on tag push
  - Update tag resolution logic to handle both workflow_dispatch and push triggers
  - Remove failing manual release trigger job that was causing HTTP 422 errors
  - Ensures reliable releases by providing multiple trigger mechanisms

- [#292](https://github.com/kamiazya/scopes/pull/292) [`6c03179`](https://github.com/kamiazya/scopes/commit/6c031790a100440828b31c8d5f704de52bd0f120) Thanks [@kamiazya](https://github.com/kamiazya)! - fix: Resolved a release workflow failure by correcting heredoc syntax in `release.yml` to prevent improper variable expansion.

- [#309](https://github.com/kamiazya/scopes/pull/309) [`5cff8d5`](https://github.com/kamiazya/scopes/commit/5cff8d5101f3a67c4cd8cf9f2305feb98a60a62c) Thanks [@kamiazya](https://github.com/kamiazya)! - Migrate from GraalVM Native Image to JAR distribution

  **Breaking Change**: Distribution format changed from platform-specific native binaries to universal JAR bundle.

  ### Migration Details

  - **Distribution Format**: Changed from 6 platform-specific native binaries (~260MB total) to single universal JAR bundle (~20MB)
  - **Runtime Requirement**: Now requires Java 21+ to be installed on the target system
  - **Installation Method**: JAR bundle includes wrapper scripts (bash, batch, PowerShell) for platform-agnostic execution
  - **SBOM Structure**: Dual SBOM approach - source-level (`sbom/scopes-sbom.json`) and binary-level (`sbom/scopes-binary-sbom.json`)

  ### Technical Changes

  - Removed GraalVM Native Image build configuration and related plugins
  - Implemented Shadow JAR plugin for creating fat JARs with all dependencies
  - Updated CI/CD workflows to build universal JAR instead of platform-specific binaries
  - Removed platform-specific build jobs (linux-x64, darwin-x64, darwin-arm64, win32-x64, etc.)
  - Cleaned up GraalVM-specific configuration files (`native-image.properties`, `reflect-config.json`, etc.)

  ### Documentation Updates

  - Renamed `install-jar.ps1` to `install.ps1` for consistency with `install.sh`
  - Removed obsolete native binary documentation (`install/README.md`, `install/verify-README.md`, `install/offline/`)
  - Updated all security verification guides to reflect JAR bundle structure
  - Updated getting started guide with Java 21 requirement
  - Created ADR-0017 documenting the migration decision and rationale

  ### User Impact

  **Benefits**:

  - Smaller download size (~20MB vs ~260MB for all platforms)
  - Faster startup time and lower memory footprint
  - No platform-specific build issues
  - Easier to debug and profile with standard JVM tools

  **Migration Required**:

  - Users must have Java 21+ installed (see docs/explanation/setup/java-setup.md)
  - Update installation scripts to use new JAR bundle format
  - Replace platform-specific binaries with universal JAR + wrapper scripts

- [#292](https://github.com/kamiazya/scopes/pull/292) [`6c03179`](https://github.com/kamiazya/scopes/commit/6c031790a100440828b31c8d5f704de52bd0f120) Thanks [@kamiazya](https://github.com/kamiazya)! - feat: replace individual assets with bundled packages for easier downloads

  Replace the previous 28 individual release assets with organized bundle packages to eliminate download confusion. Users now choose from:

  - 6 platform-specific bundles (~20MB each) containing binary, installer, SBOM, and verification files
  - 1 unified offline package (~260MB) for enterprise/multi-platform deployments
  - SLSA provenance for supply chain security

  This provides 92% reduction in download size for most users while maintaining all existing security features (SLSA Level 3, SHA256 verification) and preparing for future daemon binary distribution.

- [#289](https://github.com/kamiazya/scopes/pull/289) [`33f06cf`](https://github.com/kamiazya/scopes/commit/33f06cf9c54b934046435e24fc62715d83678709) Thanks [@dependabot](https://github.com/apps/dependabot)! - deps(deps): bump org.graalvm.buildtools.native from 0.11.0 to 0.11.1

- [#301](https://github.com/kamiazya/scopes/pull/301) [`2da8ec3`](https://github.com/kamiazya/scopes/commit/2da8ec31f665e319ec1a1384ca131ab5f4504d89) Thanks [@joonseolee](https://github.com/joonseolee)! - feat: add pre-commit hooks for shellcheck, yamlint and shfmt

## 0.0.4

### Patch Changes

- [#275](https://github.com/kamiazya/scopes/pull/275) [`be39193`](https://github.com/kamiazya/scopes/commit/be39193b8a5b463fc1d1a3dcce1c0dae322070d6) Thanks [@kamiazya](https://github.com/kamiazya)! - Fix automatic release workflow triggering after Version PR merges

  - Integrated release triggering into version-and-release.yml to work around GitHub Actions limitations
  - GitHub Actions security prevents workflows from triggering other workflows when using GITHUB_TOKEN
  - Modified release.yml to only support manual workflow_dispatch
  - Updated permissions to `actions: write` to allow workflow dispatch via gh CLI
  - Fixed gh CLI authentication by using GH_TOKEN environment variable instead of GITHUB_TOKEN
  - Automatic releases now work correctly after Version PR merges without requiring Personal Access Tokens

- [#278](https://github.com/kamiazya/scopes/pull/278) [`146ed17`](https://github.com/kamiazya/scopes/commit/146ed175df36f28c8362df86b3d9d46fcb39b669) Thanks [@kamiazya](https://github.com/kamiazya)! - Fix medium and low severity security vulnerabilities in dependencies

  - Update Apache Commons Lang3 to 3.18.0 to fix an uncontrolled recursion vulnerability
  - Verify Logback 1.5.18 includes fixes for CVE-2024-12801 (SSRF) and CVE-2024-12798 (Expression Language injection)
  - Add explicit commons-lang3 dependency to ensure secure version is used across all modules

  This patch resolves all 3 open Dependabot security alerts (2 medium severity, 1 low severity) without breaking changes to the public API.

## 0.0.3

### Patch Changes

- [#273](https://github.com/kamiazya/scopes/pull/273) [`59d2a68`](https://github.com/kamiazya/scopes/commit/59d2a68fee47af32df2780afe64e8978f96b7fc5) Thanks [@kamiazya](https://github.com/kamiazya)! - Clarify application-to-contracts import policy and add enforcement

  - Document that Application boundary components (handlers, mappers, error mappers) may import contract types
  - Add Konsist test to automatically enforce import rules
  - Update architecture diagrams to reflect allowed dependencies
  - Maintain domain purity while avoiding duplicate DTOs at boundaries

- [#267](https://github.com/kamiazya/scopes/pull/267) [`d0c82e9`](https://github.com/kamiazya/scopes/commit/d0c82e9bc389c12032ed276790ddc14ae97639fc) Thanks [@kamiazya](https://github.com/kamiazya)! - feat: Upgrade to Clikt 5.0.3 and migrate deprecated APIs

  - Update Clikt version from 4.4.0 to 5.0.3 for improved CLI functionality
  - Migrate all CLI commands to property-based configuration (Clikt 5.x requirement)
  - Fix deprecated echo(err=true) usage across multiple command files
  - Update Native Image configuration for Mordant terminal interface compatibility
  - Improve exit handling by using parse() instead of main() for proper error handling

- [#272](https://github.com/kamiazya/scopes/pull/272) [`042f732`](https://github.com/kamiazya/scopes/commit/042f73206699a7b52ca8a758930e1d512e8a459e) Thanks [@kamiazya](https://github.com/kamiazya)! - Fix version-and-release workflow to handle GitHub API changes

  The workflow was checking for a 'merged' field that no longer exists in the GitHub API response.
  Updated to check 'merged_at' field instead to properly detect merged Version PRs.

  This fixes the issue where release tags were not being created automatically after merging Version PRs.

## 0.0.2

### Patch Changes

- [#268](https://github.com/kamiazya/scopes/pull/268) [`eca6b0c`](https://github.com/kamiazya/scopes/commit/eca6b0c784bef585ce8c27e77daea4e5993e4bff) Thanks [@kamiazya](https://github.com/kamiazya)! - Fix release workflow to prevent incorrect tag creation

  - Fixed Version and Release workflow to only create tags when Version PR is merged
  - Added tag format validation to Release workflow to prevent branch names being used as tags
  - Improved error messages to guide users on correct tag format

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
