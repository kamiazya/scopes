# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the Scopes project.

## Project Overview

**Scopes** is a next-generation local-first task and project management tool designed for symbiotic collaboration between developers and AI assistants.

### Vision

Create a unified, recursive task management system where AI and human developers work together seamlessly, eliminating context loss and maximizing productivity through intelligent workspace management.

### Core Concepts

#### 1. Unified "Scope" Entity
- **Recursive Structure**: Projects, epics, and tasks are all "Scopes"
- **Unlimited Hierarchy**: No depth restrictions
- **Consistent Operations**: Same features at every level

#### 2. AI-Native Architecture
- **Comment-Based AI Integration**: Asynchronous AI collaboration through comments
- **Workspace + Focus Management**: Automatic context switching
- **MCP (Model Context Protocol)**: Standard AI integration
- **Natural Language Context**: "this", "that" resolve to focused scope

#### 3. Local-First Design
- **Offline-First**: All features work without internet
- **Selective Sync**: Choose what to share with external tools
- **Cross-Platform**: Native support for Windows, macOS, Linux
- **Privacy by Design**: Local-only data stays local

### Target Users

- **AI-Driven Developers**: Using AI as primary development partner
- **Tech Leads**: Managing design quality before team presentation
- **OSS Contributors**: Coordinating across multiple projects
- **Multi-Device Developers**: Seamless work across machines
- **International Engineers**: Breaking language barriers with AI

## Language and Communication Policy

### Official Project Language: English
- All documentation, code, and public content in English
- Ensures international accessibility
- Exception: Local configuration files (e.g., CLAUDE.local.md)

### AI Interaction Language: User Preference
- Claude interactions in user's preferred language (日本語OK)
- Local notes can be in any language
- Balances accessibility with productivity

## Documentation Framework

We adopt the **Diátaxis framework** for organizing documentation:

### Documentation Categories
- **Tutorials** @/docs/tutorials/ - Learning-oriented guides for newcomers
- **How-to Guides** @/docs/guides/ - Task-oriented recipes for specific goals
- **Reference** @/docs/reference/ - Information-oriented technical descriptions
- **Explanation** @/docs/explanation/ - Understanding-oriented discussions

### ADR (Architecture Decision Records)
- Location: @/docs/explanation/adr/
- Purpose: Document architectural decisions with context and rationale
- Review: Include ADR review in design validation phase

### Important Notes
- Technology stack is being evaluated (considering Kotlin)
- Specific implementation details will come later
- Focus on "what" not "how" during requirements
- Maintain clear separation between requirements and design

## AI Collaboration Guidelines

### Context Awareness
- Always check current workspace and focus state
- Support natural language pronouns when focus is set
- Maintain conversation history within scope context

### Safety and Privacy
- Destructive operations require user confirmation
- Local-only data never syncs without permission
- All AI operations record co-authorship
- System must work without AI (AI-optional)

### Asynchronous Collaboration
- Use comments for non-blocking AI interaction
- Maintain context across sessions
- Support handoff between different AI assistants

Remember: This is a greenfield project inheriting the best ideas from Project Manager while introducing the revolutionary unified Scope concept for true AI-developer symbiosis.

