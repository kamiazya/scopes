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

### Quality Assurance
- Run `./gradlew konsistTest` to verify architectural compliance
- Architecture tests validate Clean Architecture and DDD principles
- All changes must pass Konsist architecture validation

### Asynchronous Collaboration
- Use comments for non-blocking AI interaction
- Maintain context across sessions
- Support handoff between different AI assistants

Remember: This is a greenfield project inheriting the best ideas from Project Manager while introducing the revolutionary unified Scope concept for true AI-developer symbiosis.

## USE SUB-AGENTS FOR CONTEXT OPTIMIZATION

### 1. Always use the file-analyzer sub-agent when asked to read files.
The file-analyzer agent is an expert in extracting and summarizing critical information from files, particularly log files and verbose outputs. It provides concise, actionable summaries that preserve essential information while dramatically reducing context usage.

### 2. Always use the code-analyzer sub-agent when asked to search code, analyze code, research bugs, or trace logic flow.

The code-analyzer agent is an expert in code analysis, logic tracing, and vulnerability detection. It provides concise, actionable summaries that preserve essential information while dramatically reducing context usage.

### 3. Always use the test-runner sub-agent to run tests and analyze the test results.

Using the test-runner agent ensures:

- Full test output is captured for debugging
- Main conversation stays clean and focused
- Context usage is optimized
- All issues are properly surfaced
- No approval dialogs interrupt the workflow

## Philosophy

> Think carefully and implement the most concise solution that changes as little code as possible.

### Error Handling

- **Fail fast** for critical configuration (missing text model)
- **Log and continue** for optional features (extraction model)
- **Graceful degradation** when external services unavailable
- **User-friendly messages** through resilience layer

### Testing

- Always use the test-runner agent to execute tests.
- Do not use mock services for anything ever.
- Do not move on to the next test until the current test is complete.
- If the test fails, consider checking if the test is structured correctly before deciding we need to refactor the codebase.
- Tests to be verbose so we can use them for debugging.

## Tone and Behavior

- Criticism is welcome. Please tell me when I am wrong or mistaken, or even when you think I might be wrong or mistaken.
- Please tell me if there is a better approach than the one I am taking.
- Please tell me if there is a relevant standard or convention that I appear to be unaware of.
- Be skeptical.
- Be concise.
- Short summaries are OK, but don't give an extended breakdown unless we are working through the details of a plan.
- Do not flatter, and do not give compliments unless I am specifically asking for your judgement.
- Occasional pleasantries are fine.
- Feel free to ask many questions. If you are in doubt of my intent, don't guess. Ask.

## ABSOLUTE RULES:

- NO PARTIAL IMPLEMENTATION
- NO SIMPLIFICATION : no "//This is simplified stuff for now, complete implementation would blablabla"
- NO CODE DUPLICATION : check existing codebase to reuse functions and constants Read files before writing new functions. Use common sense function name to find them easily.
- NO DEAD CODE : either use or delete from codebase completely
- IMPLEMENT TEST FOR EVERY FUNCTIONS
- NO CHEATER TESTS : test must be accurate, reflect real usage and be designed to reveal flaws. No useless tests! Design tests to be verbose so we can use them for debuging.
- NO INCONSISTENT NAMING - read existing codebase naming patterns.
- NO OVER-ENGINEERING - Don't add unnecessary abstractions, factory patterns, or middleware when simple functions would work. Don't think "enterprise" when you need "working"
- NO MIXED CONCERNS - Don't put validation logic inside API handlers, database queries inside UI components, etc. instead of proper separation
- NO RESOURCE LEAKS - Don't forget to close database connections, clear timeouts, remove event listeners, or clean up file handles
