# Error Handling Guidelines

## Overview

This document defines the error handling patterns and best practices for the Scopes project.

## Core Principles

1. **Functional Error Handling**: Use Arrow's `Either` type for expected errors
2. **Fail-Fast for Data Integrity**: Use Kotlin's error functions for data corruption
3. **Type Safety**: Leverage Kotlin's type system to make errors explicit
4. **No Silent Failures**: Never use "unknown" or default values to mask errors

## Error Handling Patterns

### Domain Layer

Use `Either` for business logic errors:

```kotlin
fun create(title: String): Either<ValidationError, ScopeTitle> =
    if (title.isBlank()) {
        Left(ValidationError.BlankTitle)
    } else {
        Right(ScopeTitle(title))
    }
```

### Application Layer

Propagate errors using `Either` and map between error types:

```kotlin
fun execute(command: CreateScopeCommand): Either<ApplicationError, Scope> =
    either {
        val title = ScopeTitle.create(command.title)
            .mapLeft { it.toApplicationError() }
            .bind()
        // ...
    }
```

### Infrastructure Layer

Use Kotlin's error functions for data integrity violations:

```kotlin
// ✅ Good - Use Kotlin's standard functions
fun rowToEntity(row: ScopeRow): Scope {
    val id = ScopeId.create(row.id).fold(
        ifLeft = { error("Invalid id in database: $it") },
        ifRight = { it }
    )
    // ...
}

// ❌ Bad - Don't use Java-style exceptions
fun rowToEntity(row: ScopeRow): Scope {
    val id = ScopeId.create(row.id).fold(
        ifLeft = { throw IllegalStateException("Invalid id") },  // Don't do this
        ifRight = { it }
    )
    // ...
}

// ❌ Bad - Never use fallback values
fun rowToEntity(row: ScopeRow): Scope {
    val type = when (row.type) {
        "TEXT" -> AspectType.Text
        else -> AspectType.Text  // Never use silent fallbacks!
    }
    // ...
}
```

### CLI Layer

Use `CliktError` for user-facing errors. The CLI layer is the **only exception** where throwing is acceptable because:
1. Clikt framework expects exceptions for error handling
2. It's the final layer before user interaction
3. CliktError provides consistent error formatting

**Recommended pattern using extension functions:**

```kotlin
// Define extension function
fun <E, A> Either<E, A>.toCliktResult(errorMapper: (E) -> String): A =
    fold(
        ifLeft = { error -> throw CliktError(errorMapper(error)) },
        ifRight = { it }
    )

// Use in commands
override fun run() {
    val result = adapter.createScope(title, description)
        .toCliktResult { "Error: ${getMessage(it)}" }
    echo(result)
}
```

**Acceptable patterns in CLI layer:**

```kotlin
// Direct fold with throw (current pattern - acceptable)
result.fold(
    { error -> throw CliktError("Error: ${getMessage(error)}") },
    { success -> echo(success) }
)

// Using extension function (recommended for consistency)
val output = result.toCliktResult { getMessage(it) }
echo(output)
```

## Kotlin Standard Error Functions

### When to use each function:

| Function | Use Case | Example |
|----------|----------|---------|
| `error()` | Illegal state that should never happen | `error("Unmapped enum value: $value")` |
| `check()` | Post-condition validation | `check(result.isValid) { "Result must be valid" }` |
| `require()` | Pre-condition validation | `require(input.isNotEmpty()) { "Input cannot be empty" }` |
| `checkNotNull()` | Null safety with custom message | `checkNotNull(value) { "Value was unexpectedly null" }` |

### Examples:

```kotlin
// Pre-condition check
fun processData(data: List<String>) {
    require(data.isNotEmpty()) { "Data list cannot be empty" }
    // ...
}

// Post-condition check
fun calculateTotal(): Int {
    val result = complexCalculation()
    check(result >= 0) { "Total cannot be negative: $result" }
    return result
}

// State validation
fun mapDatabaseType(type: String): AspectType = when (type) {
    "TEXT" -> AspectType.Text
    "NUMERIC" -> AspectType.Numeric
    else -> error("Unknown database type: $type")  // Fail-fast
}
```

## Anti-Patterns to Avoid

### ❌ Using "unknown" fallbacks
```kotlin
// BAD
val type = row.type ?: "unknown"
```

### ❌ Silently catching exceptions
```kotlin
// BAD
try {
    riskyOperation()
} catch (e: Exception) {
    // Silently ignore
}
```

### ❌ Using Java-style exception throwing
```kotlin
// BAD
throw IllegalStateException("Error message")
// GOOD
error("Error message")
```

### ❌ Mixing error handling patterns inconsistently
```kotlin
// BAD - Inconsistent mix
fun process(): Result {
    val a = operation1() ?: throw Exception()  // Nullable
    val b = operation2().getOrThrow()          // Result type
    val c = operation3()                       // Throws directly
}

// GOOD - Consistent Either pattern
fun process(): Either<Error, Result> = either {
    val a = operation1().bind()
    val b = operation2().bind()
    val c = operation3().bind()
}
```

## Testing Error Cases

Always test error paths:

```kotlin
@Test
fun `should fail with validation error for blank title`() {
    val result = ScopeTitle.create("")
    
    result.shouldBeLeft().also { error ->
        error.shouldBeInstanceOf<ValidationError.BlankTitle>()
    }
}

@Test
fun `should throw error for invalid database data`() {
    val invalidRow = ScopeRow(id = "invalid-ulid", ...)
    
    shouldThrow<IllegalStateException> {
        repository.rowToScope(invalidRow)
    }.message.shouldContain("Invalid id in database")
}
```

## Migration Path

When updating existing code:

1. Replace `throw IllegalStateException(...)` with `error(...)`
2. Replace `throw IllegalArgumentException(...)` with `require(...) { ... }`
3. Replace nullable returns with `Either` for explicit error handling
4. Remove all "unknown" or default fallback values

## Decision Rationale

This approach balances:
- **Type Safety**: Errors are explicit in function signatures
- **Performance**: Either is lightweight compared to exceptions
- **Debugging**: Stack traces are preserved when needed (via error/check/require)
- **User Experience**: CLI errors are clear and actionable

As a local CLI tool without monitoring infrastructure, fail-fast behavior for data integrity issues is critical to prevent silent data corruption.