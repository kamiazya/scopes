# Error Handling at Layer Boundaries

## Design Philosophy

The Scopes project follows a clear separation of concerns for error handling across architectural layers. A key principle is that **error messages are a presentation layer concern** and should only be formatted in the interface layer.

## Current Architecture

### Error Types by Layer

```
Domain Layer:       Rich errors with value objects (ScopeId, AliasName, etc.)
     ↓
Application Layer:  Simplified errors with primitive types (String, Int)
     ↓
Contract Layer:     Structured errors as stable API (no messages, just data)
     ↓
Interface Layer:    User-friendly messages (formatted for CLI/API output)
```

### Key Design Decisions

1. **No Messages in Lower Layers**: Domain, Application, and Contract errors contain structured data, not formatted messages
2. **Rich Error Types**: Each layer has its own error hierarchy appropriate to its concerns
3. **Stable Contracts**: Contract errors provide a stable API between bounded contexts
4. **Presentation Ownership**: Only the interface layer formats errors into human-readable messages

## Error Flow Example

```kotlin
// 1. Domain Layer - Rich types, no messages
sealed class ScopeError : ScopesError() {
    data class NotFound(val scopeId: ScopeId) : ScopeError()
    data class DuplicateTitle(val title: String, val parentId: ScopeId?) : ScopeError()
}

// 2. Application Layer - Simplified types, no messages
sealed class ApplicationError {
    data class ScopeNotFound(val scopeId: String) : ApplicationError()
    data class ValidationFailed(val field: String, val value: String) : ApplicationError()
}

// 3. Contract Layer - Structured data, no messages
sealed interface ScopeContractError {
    data class NotFound(val scopeId: String) : BusinessError
    data class InvalidTitle(val title: String, val validationFailure: TitleValidationFailure) : InputError
}

// 4. Interface Layer - Messages created here
object ContractErrorMessageMapper {
    fun getMessage(error: ScopeContractError, debug: Boolean = false): String = when (error) {
        is ScopeContractError.NotFound -> 
            "Scope not found: ${error.scopeId}"
        // ... message formatting logic
    }
}
```

## Benefits of This Approach

### 1. Separation of Concerns
- Business logic doesn't know about user interface concerns
- Message formatting is centralized in the presentation layer
- Each layer focuses on its specific responsibilities

### 2. Internationalization Ready
- Messages are generated at the UI layer where locale information is available
- No hardcoded messages buried in business logic
- Easy to add multi-language support in the future

### 3. Multiple Interface Support
- Different interfaces (CLI, REST API, GraphQL) can format errors differently
- Same contract error can produce different messages based on context
- Debug vs production message formatting is handled at the edge

### 4. Testability
- Domain/Application logic can be tested without message assertions
- Contract stability can be verified without UI concerns
- Message formatting can be tested independently

## Error Mapping Guidelines

### Domain → Application Mapping

Located in: Application layer
Purpose: Simplify rich domain types to primitives

```kotlin
// In application layer
internal object ScopeDomainErrorMapper {
    fun toApplicationError(domainError: ScopesError): ApplicationError = when (domainError) {
        is ScopeError.NotFound -> 
            ApplicationError.ScopeNotFound(domainError.scopeId.value) // value object → string
    }
}
```

### Application → Contract Mapping

Located in: Infrastructure/Adapter layer
Purpose: Create stable API errors

```kotlin
// In infrastructure layer
internal object ErrorMapper {
    fun mapToContractError(error: ApplicationError): ScopeContractError = when (error) {
        is ApplicationError.ScopeNotFound -> 
            ScopeContractError.NotFound(scopeId = error.scopeId)
    }
}
```

### Contract → User Message Mapping

Located in: Interface layer
Purpose: Format human-readable messages

```kotlin
// In interface layer
object ContractErrorMessageMapper {
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.NotFound -> 
            "Scope not found: ${error.scopeId}\n" +
            "💡 Tip: Use 'scopes list' to see available scopes"
    }
}
```

## Anti-Patterns to Avoid

### ❌ Messages in Domain Errors

```kotlin
// BAD - Domain layer shouldn't have user messages
data class ScopeNotFound(val id: ScopeId) : DomainError() {
    val message = "Scope not found: ${id.value}" // Don't do this!
}

// GOOD - Just data
data class ScopeNotFound(val id: ScopeId) : DomainError()
```

### ❌ Pre-formatted Messages in Contracts

```kotlin
// BAD - Contract has formatted message
data class NotFound(val message: String) : ContractError

// GOOD - Contract has structured data
data class NotFound(val scopeId: String) : ContractError
```

### ❌ Message Logic in Multiple Layers

```kotlin
// BAD - Message formatting scattered across layers
// In domain
override fun toString() = "Scope $id not found"
// In application  
fun toMessage() = "Cannot find scope: $scopeId"
// In contract
val userMessage = "Scope with ID '$scopeId' does not exist"

// GOOD - Single responsibility in interface layer
fun getMessage(error: ContractError): String = // All formatting here
```

## Testing Strategies

### Domain/Application Tests
- Test error conditions without asserting on messages
- Verify error types and data content
- Focus on business rule validation

```kotlin
test("should return NotFound error for non-existent scope") {
    val result = repository.findById(ScopeId("123"))
    result shouldBeLeft { error ->
        error.shouldBeInstanceOf<ScopeError.NotFound>()
        error.scopeId.value shouldBe "123"
    }
    // No message assertions!
}
```

### Contract Tests
- Verify error structure stability
- Test that all error cases are mapped
- Ensure backward compatibility

### Interface Tests
- Test message formatting
- Verify user-friendly output
- Check debug vs normal mode differences

```kotlin
test("should format NotFound error with helpful tip") {
    val error = ScopeContractError.NotFound("123")
    val message = ContractErrorMessageMapper.getMessage(error)
    
    message shouldContain "Scope not found: 123"
    message shouldContain "scopes list"
}
```

## Conclusion

By keeping error messages exclusively in the presentation layer, the architecture remains clean, testable, and flexible. This design supports multiple user interfaces, future internationalization, and maintains clear separation of concerns throughout the codebase.
