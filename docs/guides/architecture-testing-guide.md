# Architecture Testing Guide with Konsist

## Overview

Scopes uses [Konsist](https://docs.konsist.lemonappdev.com/) to enforce architectural rules and maintain code quality through automated architecture tests. These tests run as part of the build process and ensure that the codebase adheres to Clean Architecture and Domain-Driven Design principles.

## What is Konsist?

Konsist is a Kotlin library that allows you to write tests for your architecture. Instead of relying on code reviews to catch architectural violations, Konsist automatically verifies that your code follows the established patterns.

## Architecture Tests Location

All architecture tests are located in:
```
quality/konsist/src/test/kotlin/io/github/kamiazya/scopes/konsist/
```

## Implemented Architecture Tests

### 1. Clean Architecture Test

Ensures proper layer separation and dependency direction:

```kotlin
// Domain layers should not depend on application or infrastructure
Konsist
    .scopeFromDirectory("contexts/$context/domain")
    .files
    .assertFalse { file ->
        file.imports.any { import ->
            import.name.contains(".application.") ||
            import.name.contains(".infrastructure.")
        }
    }
```

**Rules enforced:**
- Domain layer has no external dependencies
- Application layer doesn't depend on infrastructure
- Infrastructure layer doesn't depend on interfaces or apps
- Platform utilities are truly independent

### 2. Bounded Context Architecture Test

Validates DDD bounded context isolation:

```kotlin
// Each context should be self-contained
contexts.forEach { context ->
    Konsist
        .scopeFromDirectory("contexts/$context")
        .files
        .assertFalse { file ->
            // Should not import from other contexts directly
            otherContexts.any { other ->
                file.imports.any { it.name.contains("contexts.$other") }
            }
        }
}
```

**Rules enforced:**
- Contexts communicate only through contracts
- No direct dependencies between contexts
- Proper use of ports and adapters pattern

### 3. Contract Layer Architecture Test

Ensures contracts remain pure interfaces:

```kotlin
// Contracts should only contain interfaces and DTOs
Konsist
    .scopeFromDirectory("contracts")
    .classes()
    .assertFalse { clazz ->
        // No implementation details
        clazz.hasAnnotation("Component") ||
        clazz.hasAnnotation("Service") ||
        clazz.hasAnnotation("Repository")
    }
```

**Rules enforced:**
- Contracts contain only interfaces and data classes
- No framework dependencies in contracts
- No implementation logic in contract layer

### 4. Naming Convention Tests

Enforces consistent naming patterns:

```kotlin
// Command handlers must end with "Handler"
Konsist
    .scopeFromPackage("..command.handler..")
    .classes()
    .assertTrue { it.name.endsWith("Handler") }

// DTOs must end with "Dto"
Konsist
    .scopeFromPackage("..dto..")
    .classes()
    .assertTrue { it.name.endsWith("Dto") }
```

### 5. Package Structure Tests

Validates package organization:

```kotlin
// Domain entities must be in domain package
Konsist
    .scopeFromPackage("..domain..")
    .classes()
    .filter { it.hasAnnotation("Entity") }
    .assertTrue {
        it.packageName.contains("domain.entity") ||
        it.packageName.contains("domain.model")
    }
```

## Running Architecture Tests

### Run all Konsist tests:
```bash
./gradlew konsistTest
```

### Run specific test class:
```bash
./gradlew :quality:konsist:test --tests "*.CleanArchitectureTest"
```

### Run as part of build:
```bash
./gradlew build
# Architecture tests run automatically
```

## Test Output

When violations are detected, Konsist provides clear error messages:

```
CleanArchitectureTest > domain layers should not depend on application FAILED

    Assertion failed:
    File: contexts/scope-management/domain/src/main/kotlin/Example.kt

    The file contains forbidden import:
    - io.github.kamiazya.scopes.scopemanagement.application.SomeClass

    Domain layer must not depend on application layer.
```

## Writing Custom Architecture Tests

### Example: Ensure all use cases have tests

```kotlin
class UseCaseTestCoverage : StringSpec({

    "all use cases should have corresponding tests" {
        val useCases = Konsist
            .scopeFromProject()
            .classes()
            .filter { it.name.endsWith("UseCase") }

        val testClasses = Konsist
            .scopeFromTest()
            .classes()
            .filter { it.name.endsWith("Test") }
            .map { it.name.removeSuffix("Test") }

        useCases.assertTrue { useCase ->
            testClasses.contains(useCase.name)
        }
    }
})
```

### Example: Enforce repository pattern

```kotlin
class RepositoryPatternTest : StringSpec({

    "repositories should have interfaces" {
        val implementations = Konsist
            .scopeFromPackage("..infrastructure.repository..")
            .classes()
            .filter { it.name.endsWith("RepositoryImpl") }

        implementations.assertTrue { impl ->
            val interfaceName = impl.name.removeSuffix("Impl")
            Konsist
                .scopeFromPackage("..domain.repository..")
                .interfaces()
                .any { it.name == interfaceName }
        }
    }
})
```

## Best Practices

### 1. Run Tests Frequently
```bash
# Add to your development workflow
alias kt="./gradlew konsistTest"
```

### 2. Fix Violations Immediately
Architecture violations should be treated as build failures.

### 3. Document Exceptions
If you need to violate a rule, document why:
```kotlin
@Suppress("KonsistArchitecture") // Temporary: Migrating to new pattern
class LegacyAdapter {
    // ...
}
```

### 4. Add Tests for New Patterns
When introducing new architectural patterns, add corresponding tests:
```kotlin
"new pattern should follow convention" {
    Konsist
        .scopeFromPackage("..newpattern..")
        .classes()
        .assertTrue { /* validation logic */ }
}
```

## Common Violations and Solutions

### Violation: Domain depends on Application
```kotlin
// ❌ Bad: Domain importing application
import io.github.kamiazya.scopes.application.SomeService

// ✅ Good: Use interfaces in domain
import io.github.kamiazya.scopes.domain.port.SomePort
```

### Violation: Direct context communication
```kotlin
// ❌ Bad: Direct import from another context
import io.github.kamiazya.scopes.userpreferences.domain.UserPreference

// ✅ Good: Use contracts
import io.github.kamiazya.scopes.contracts.userpreferences.UserPreferenceDto
```

### Violation: Implementation in contracts
```kotlin
// ❌ Bad: Logic in contract layer
interface ScopeContract {
    fun calculate() = 42 // Implementation!
}

// ✅ Good: Pure interface
interface ScopeContract {
    fun calculate(): Int
}
```

## Integration with CI/CD

### GitHub Actions
```yaml
- name: Run Architecture Tests
  run: ./gradlew konsistTest

- name: Upload Test Results
  if: failure()
  uses: actions/upload-artifact@v3
  with:
    name: konsist-results
    path: quality/konsist/build/reports/tests/
```

### Pre-commit Hook
```bash
#!/bin/bash
# .git/hooks/pre-commit
echo "Running architecture tests..."
./gradlew konsistTest --quiet
if [ $? -ne 0 ]; then
    echo "Architecture tests failed. Please fix violations before committing."
    exit 1
fi
```

## Performance Considerations

### Test Execution Time
- Full suite: ~5-10 seconds
- Incremental: ~1-2 seconds (with Gradle cache)

### Optimization Tips
1. Use focused scopes:
```kotlin
// Slower: Scan entire project
Konsist.scopeFromProject()

// Faster: Scan specific module
Konsist.scopeFromModule("scope-management")
```

2. Cache test results:
```kotlin
companion object {
    // Reuse scope across tests
    private val scope = Konsist.scopeFromProject()
}
```

## Debugging Failed Tests

### Enable verbose output:
```bash
./gradlew konsistTest --info
```

### Debug specific test:
```kotlin
@Test
fun debugTest() {
    val violations = Konsist
        .scopeFromPackage("problem.package")
        .classes()
        .filter { /* your condition */ }

    violations.forEach {
        println("Violation in: ${it.filePath}")
        println("Class: ${it.name}")
        println("Imports: ${it.imports}")
    }

    violations.shouldBeEmpty()
}
```

## Benefits of Architecture Testing

### 1. Automated Enforcement
- No manual code review needed for architecture
- Instant feedback on violations
- Consistent enforcement across team

### 2. Documentation as Code
- Architecture rules are executable
- Tests serve as architecture documentation
- Examples show correct patterns

### 3. Refactoring Safety
- Catch breaking changes early
- Maintain architectural integrity
- Confidence in large-scale changes

### 4. Onboarding Aid
- New developers learn patterns quickly
- Clear error messages guide correct implementation
- Reduces architecture-related questions

## Extending Architecture Tests

To add new architecture tests:

1. Create test file in `quality/konsist/src/test/kotlin/`
2. Extend appropriate test base (StringSpec, FunSpec, etc.)
3. Write assertions using Konsist DSL
4. Run tests to verify
5. Add to CI pipeline if needed

## Related Documentation

- [Clean Architecture](../explanation/clean-architecture.md) - Architecture principles
- [Domain-Driven Design](../explanation/domain-driven-design.md) - DDD patterns
<!-- - [Testing Guide](./testing-guide.md) - General testing practices (planned) -->
- [Contributing Guide](../CONTRIBUTING.md) - Development workflow
