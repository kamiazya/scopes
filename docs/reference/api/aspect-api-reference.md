# Aspect API Reference

This document provides technical reference for the Aspect Management API in Scopes. All aspect operations use the domain-driven design patterns with clean architecture.

## Core Types

### AspectType

Sealed class hierarchy representing the supported aspect types:

```kotlin
sealed class AspectType {
    data object Text : AspectType()
    data object Numeric : AspectType()  
    data object BooleanType : AspectType()
    data class Ordered(val allowedValues: List<AspectValue>) : AspectType()
    data object Duration : AspectType()
}
```

#### Type Validation Rules

| Type | Valid Values | Examples |
|------|-------------|----------|
| `Text` | Any non-empty string | `"in-progress"`, `"feature"`, `"John Doe"` |
| `Numeric` | Valid numeric strings | `"42"`, `"3.14"`, `"-10"`, `"0"` |
| `BooleanType` | Boolean-like strings | `"true"`, `"false"`, `"yes"`, `"no"`, `"1"`, `"0"` |
| `Ordered` | Values from defined list | Defined order: `"low" < "medium" < "high"` |
| `Duration` | ISO 8601 duration format | `"P1D"`, `"PT2H30M"`, `"P1W"` |

### AspectValue

Value object for aspect values with type-aware parsing:

```kotlin
class AspectValue private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<ScopesError, AspectValue>
    }
    
    // Type checking methods
    fun isNumeric(): Boolean
    fun isBoolean(): Boolean
    fun isDuration(): Boolean
    
    // Type conversion methods
    fun toNumericValue(): Double?
    fun toBooleanValue(): Boolean?
    fun parseDuration(): Duration
}
```

### AspectDefinition

Domain entity representing aspect definitions:

```kotlin
class AspectDefinition private constructor(
    val key: AspectKey,
    val type: AspectType,
    val description: String? = null,
    val allowMultiple: Boolean = false
) {
    companion object {
        // Factory methods for each type
        fun createText(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition
        fun createNumeric(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition
        fun createBoolean(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition
        fun createOrdered(key: AspectKey, allowedValues: List<AspectValue>, description: String? = null, allowMultiple: Boolean = false): Either<ScopesError, AspectDefinition>
        fun createDuration(key: AspectKey, description: String? = null, allowMultiple: Boolean = false): AspectDefinition
    }
    
    // Validation methods
    fun isValidValue(value: AspectValue): Boolean
    fun compareValues(value1: AspectValue, value2: AspectValue): Int
}
```

## Use Cases

### DefineAspectUseCase

Creates new aspect definitions with type validation:

```kotlin
class DefineAspectUseCase(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val transactionManager: TransactionManager
) {
    suspend fun execute(
        key: String,
        description: String?,
        type: AspectType
    ): Either<ScopesError, AspectDefinition>
}
```

**Usage Examples:**
```kotlin
// Define text aspect
val result = defineAspectUseCase.execute(
    key = "status",
    description = "Task status",
    type = AspectType.Text
)

// Define ordered aspect
val priorityValues = listOf("low", "medium", "high").map { 
    AspectValue.create(it).getOrNull()!! 
}
val result = defineAspectUseCase.execute(
    key = "priority", 
    description = "Task priority",
    type = AspectType.Ordered(priorityValues)
)

// Define duration aspect
val result = defineAspectUseCase.execute(
    key = "estimatedTime",
    description = "Estimated completion time", 
    type = AspectType.Duration
)
```

### ValidateAspectValueUseCase

Validates aspect values against their definitions:

```kotlin
class ValidateAspectValueUseCase(
    private val aspectDefinitionRepository: AspectDefinitionRepository
) {
    suspend fun execute(key: String, value: String): Either<ScopesError, AspectValue>
    
    suspend fun executeMultiple(
        aspects: Map<String, List<String>>
    ): Either<ScopesError, Map<String, List<AspectValue>>>
}
```

**Validation Rules:**

| Type | Validation Logic |
|------|-----------------|
| Text | Any non-empty string after trimming |
| Numeric | Must parse as valid `Double` |
| Boolean | Must be one of: `true`, `false`, `yes`, `no`, `1`, `0` (case-insensitive) |
| Ordered | Must exist in the defined `allowedValues` list |
| Duration | Must be valid ISO 8601 duration format |

**Error Cases:**
```kotlin
sealed class ValidationError {
    data class InvalidNumericValue(val key: String, val value: String) : ValidationError()
    data class InvalidBooleanValue(val key: String, val value: String) : ValidationError()
    data class InvalidOrderedValue(val key: String, val value: String, val allowedValues: List<String>) : ValidationError()
    data class InvalidDurationValue(val key: String, val value: String) : ValidationError()
    data class MultipleValuesNotAllowed(val key: String) : ValidationError()
    data class AspectNotFound(val key: String) : ValidationError()
}
```

### Query System

#### AspectQueryParser

Parses query strings into Abstract Syntax Tree (AST):

```kotlin
class AspectQueryParser {
    fun parse(query: String): Either<ScopesError, AspectQueryAST>
}

sealed class AspectQueryAST {
    data class Comparison(
        val key: String,
        val operator: ComparisonOperator,
        val value: String
    ) : AspectQueryAST()
    
    data class And(
        val left: AspectQueryAST,
        val right: AspectQueryAST
    ) : AspectQueryAST()
    
    data class Or(
        val left: AspectQueryAST,
        val right: AspectQueryAST  
    ) : AspectQueryAST()
    
    data class Not(
        val expression: AspectQueryAST
    ) : AspectQueryAST()
}

enum class ComparisonOperator {
    EQ,    // =
    NE,    // !=
    LT,    // <
    LE,    // <=
    GT,    // >
    GE     // >=
}
```

**Query Grammar:**
```bnf
Query ::= Expression
Expression ::= Term ((AND | OR) Term)*
Term ::= NOT? (Comparison | '(' Expression ')')
Comparison ::= Identifier Operator Value
Operator ::= '=' | '!=' | '<' | '<=' | '>' | '>='
Value ::= QuotedString | UnquotedString
Identifier ::= [a-zA-Z][a-zA-Z0-9_]*
```

#### AspectQueryEvaluator

Evaluates parsed queries against aspect collections:

```kotlin
class AspectQueryEvaluator(
    private val aspectDefinitions: Map<String, AspectDefinition>
) {
    fun evaluate(query: AspectQueryAST, aspects: Aspects): Boolean
}
```

**Type-Aware Comparison Rules:**

| Aspect Type | Comparison Logic |
|-------------|------------------|
| Text | String comparison (case-sensitive) |
| Numeric | Numeric comparison after parsing to `Double` |
| Boolean | Boolean comparison after parsing |
| Ordered | Positional comparison based on `allowedValues` order |
| Duration | Duration comparison after parsing ISO 8601 |

### FilterScopesWithQueryUseCase

Filters scopes using the query system:

```kotlin
class FilterScopesWithQueryUseCase(
    private val scopeRepository: ScopeRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val parser: AspectQueryParser
) {
    suspend fun execute(query: String): Either<ScopesError, List<Scope>>
}
```

## Error Handling

### ScopesError Hierarchy

```kotlin
sealed class ScopesError {
    // Validation errors
    data class ValidationFailed(
        val message: String,
        val details: ValidationError
    ) : ScopesError()
    
    // Query parsing errors
    data class QueryParseError(
        val message: String,
        val position: Int? = null
    ) : ScopesError()
    
    // Aspect definition errors
    data class AspectAlreadyExists(val key: String) : ScopesError()
    data class AspectNotFound(val key: String) : ScopesError()
    data class InvalidAspectDefinition(val message: String) : ScopesError()
    
    // Repository errors
    data class RepositoryError(val cause: String) : ScopesError()
    
    // Generic errors
    data class InvalidOperation(val message: String) : ScopesError()
}
```

### Error Response Patterns

All use cases follow consistent error handling:

```kotlin
// Success case
val result: Either<ScopesError, T> = useCase.execute(...)
result.fold(
    { error -> /* Handle error */ },
    { value -> /* Handle success */ }
)

// Specific error handling
when (val error = result.leftOrNull()) {
    is ScopesError.ValidationFailed -> handleValidationError(error)
    is ScopesError.AspectNotFound -> handleNotFound(error)
    is ScopesError.QueryParseError -> handleQueryError(error)
    else -> handleGenericError(error)
}
```

## Repository Contracts

### AspectDefinitionRepository

```kotlin
interface AspectDefinitionRepository {
    suspend fun save(definition: AspectDefinition): Either<Any, AspectDefinition>
    suspend fun findByKey(key: AspectKey): Either<Any, AspectDefinition?>
    suspend fun findAll(): Either<Any, List<AspectDefinition>>
    suspend fun existsByKey(key: AspectKey): Either<Any, Boolean>
    suspend fun deleteByKey(key: AspectKey): Either<Any, Boolean>
}
```

### Transaction Management

All write operations are transactional:

```kotlin
interface TransactionManager {
    suspend fun <T> runInTransaction(block: suspend () -> Either<ScopesError, T>): Either<ScopesError, T>
}
```

## Duration Type Implementation

### ISO 8601 Parsing

The Duration type supports ISO 8601 format with the following components:

```kotlin
// In AspectValue class
private fun parseISO8601Duration(iso8601: String): Duration {
    require(iso8601.startsWith("P")) { "ISO 8601 duration must start with 'P'" }
    
    var totalSeconds = 0.0
    var timePart = false
    var current = ""
    
    for (i in 1 until iso8601.length) {
        val char = iso8601[i]
        when (char) {
            'T' -> timePart = true
            'W' -> { totalSeconds += current.toDouble() * 7 * 24 * 3600; current = "" }
            'D' -> { totalSeconds += current.toDouble() * 24 * 3600; current = "" }
            'H' -> { totalSeconds += current.toDouble() * 3600; current = "" }
            'M' -> { 
                if (timePart) totalSeconds += current.toDouble() * 60
                else throw IllegalArgumentException("Month units not supported")
                current = ""
            }
            'S' -> { totalSeconds += current.toDouble(); current = "" }
            else -> current += char
        }
    }
    
    return totalSeconds.seconds
}
```

**Supported Formats:**
- `P1D` - 1 day
- `PT2H` - 2 hours
- `PT30M` - 30 minutes
- `PT45S` - 45 seconds
- `P1W` - 1 week
- `P1DT2H30M45S` - Complex duration
- `P0D` - Zero duration (valid)

**Unsupported Formats:**
- Month units (`P1M`) - Due to variable month lengths
- Year units (`P1Y`) - Due to leap years
- Fractional seconds with decimals

### Duration Comparison

Durations are compared by converting to total seconds:

```kotlin
private fun evaluateDurationComparison(
    actualValue: AspectValue,
    operator: ComparisonOperator, 
    expectedValue: String
): Boolean {
    val actual = actualValue.parseDuration()
    val expected = AspectValue.create(expectedValue).getOrNull()?.parseDuration() ?: return false
    
    return when (operator) {
        ComparisonOperator.EQ -> actual == expected
        ComparisonOperator.NE -> actual != expected
        ComparisonOperator.LT -> actual < expected
        ComparisonOperator.LE -> actual <= expected
        ComparisonOperator.GT -> actual > expected
        ComparisonOperator.GE -> actual >= expected
    }
}
```

## Testing Support

### Property-Based Testing

The aspect system includes property-based tests to ensure correctness:

```kotlin
// Example property test for AspectValue creation
class AspectValuePropertyTest : DescribeSpec({
    describe("AspectValue Property Tests") {
        it("should create aspect values for non-empty strings") {
            forAll(Arb.string(1, 100).filterNot { it.isBlank() }) { input ->
                val result = AspectValue.create(input)
                result.isRight()
            }
        }
        
        it("should validate duration formats correctly") {
            val validDurations = listOf("P1D", "PT2H", "PT30M", "P1DT2H30M", "P1W")
            validDurations.forEach { durationStr ->
                val aspectValue = AspectValue.create(durationStr).getOrNull()!!
                aspectValue.isDuration() shouldBe true
            }
        }
    }
})
```

## Performance Considerations

### Query Optimization

- Aspect definitions are cached during query evaluation
- Type-specific comparisons avoid unnecessary parsing
- Query AST is reusable across multiple evaluations

### Memory Management

- AspectValue uses string interning for common values
- Duration parsing results are cached within AspectValue instances
- Repository implementations support batch operations

## Migration and Compatibility

### Adding New Aspect Types

To add a new aspect type:

1. Extend the `AspectType` sealed class
2. Add validation logic to `AspectDefinition`
3. Add parsing support to `AspectValue`
4. Add comparison logic to `AspectQueryEvaluator`
5. Update CLI commands and documentation

### Breaking Changes Policy

- Aspect type additions are non-breaking
- Query syntax extensions are non-breaking
- Repository interface changes require major version bump
- Value parsing changes require careful migration

## See Also

- [CLI Quick Reference](../cli-quick-reference.md) - Command-line interface
- [Duration Aspects Guide](../../guides/duration-aspects-guide.md) - Practical usage
- [Clean Architecture](../../explanation/clean-architecture.md) - Architecture patterns
- [Domain-Driven Design](../../explanation/domain-driven-design.md) - Design principles
