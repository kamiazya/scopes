# Development Guidelines

> **Note**: This document has been restructured for better context efficiency. Please refer to the modular guides in the `development/` directory for detailed information.

## Quick Links

For comprehensive development guidelines, please see:

- **[Development Guide Index](./development/README.md)** - Start here for an overview
- **[Error Handling](./development/error-handling.md)** - Error patterns and best practices
- **[Testing Patterns](./development/testing.md)** - Testing strategies and guidelines
- **[DTO Guidelines](./development/dto-guidelines.md)** - DTO naming and placement conventions
- **[Clean Architecture Patterns](./development/clean-architecture-patterns.md)** - Layer responsibilities (coming soon)
- **[Repository Patterns](./development/repository-patterns.md)** - Repository design patterns (coming soon)

## Overview

Scopes follows **Clean Architecture** principles with **Domain-Driven Design (DDD)** and **Functional Programming** paradigms. The implementation emphasizes:

1. **Strongly-Typed Domain Identifiers** - ScopeId value objects replace raw strings
2. **Service-Specific Error Hierarchies** - Rich error context for validation scenarios
3. **Clean Architecture with Functional DDD** - Domain layer isolation
4. **Error Translation Patterns** - Service errors mapped to use case errors
5. **ValidationResult for Error Accumulation** - Comprehensive validation feedback

## Development Workflow

```bash
# 1. Architecture validation
./gradlew konsistTest

# 2. Run tests
./gradlew test

# 3. Code formatting
./gradlew ktlintFormat
./gradlew detekt

# 4. Full build  
./gradlew build
```

## Related Documentation

- [Clean Architecture](../explanation/clean-architecture.md) - Architecture principles
- [Domain-Driven Design](../explanation/domain-driven-design.md) - DDD patterns
- [Architecture Testing Guide](./architecture-testing-guide.md) - Konsist usage
- [Use Case Style Guide](./use-case-style-guide.md) - UseCase pattern implementation
