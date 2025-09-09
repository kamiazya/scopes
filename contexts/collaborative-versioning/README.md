# Collaborative Versioning Bounded Context

This bounded context is responsible for managing version control and changesets in a collaborative environment where both human users and AI agents can make changes.

## Overview

The collaborative versioning context implements a distributed version control system inspired by Git and lix, enabling:

- **Multi-agent collaboration**: Support for both human and AI agents
- **Changeset management**: Track and merge changes from multiple sources
- **Version history**: Maintain complete history of all changes
- **Conflict resolution**: Handle conflicts between concurrent changes

## Architecture

This context follows Clean Architecture and DDD principles:

### Domain Layer
- **Value Objects**: `ChangesetId`, `VersionId`, `ChangeType`
- **Entities**: (To be implemented)
- **Aggregates**: (To be implemented)
- **Domain Services**: (To be implemented)
- **Repository Interfaces**: `ChangesetRepository`

### Application Layer
- **Command Handlers**: (To be implemented)
- **Query Handlers**: (To be implemented)
- **Use Cases**: (To be implemented)
- **DTOs**: (To be implemented)

### Infrastructure Layer
- **Repository Implementations**: (To be implemented)
- **Persistence**: SQLDelight-based storage
- **Adapters**: (To be implemented)

## Integration

This context integrates with:
- **Agent Management**: To track who made changes
- **Scope Management**: To version scope changes
- **Event Store**: For event sourcing support
