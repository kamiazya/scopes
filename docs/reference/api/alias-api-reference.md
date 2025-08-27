# Alias System API Reference

This document provides a comprehensive API reference for the alias system in Scopes, covering domain objects, application services, and infrastructure components.

## Domain Layer

### Value Objects

#### AliasName

Represents a validated alias name with normalization.

```kotlin
package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

@JvmInline
value class AliasName private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<ScopeInputError.AliasError, AliasName>
    }
    
    override fun toString(): String = value
}
```

**Validation Rules:**
- Length: 2-64 characters
- Pattern: `^[a-z][a-z0-9-_]{1,63}$`
- Normalized to lowercase
- No consecutive special characters (`--` or `__`)

**Example:**
```kotlin
// Valid
AliasName.create("auth-system")     // Right(AliasName("auth-system"))
AliasName.create("AUTH-SYSTEM")     // Right(AliasName("auth-system"))
AliasName.create("user_mgmt_v2")    // Right(AliasName("user_mgmt_v2"))

// Invalid
AliasName.create("a")               // Left(TooShort)
AliasName.create("1start")          // Left(InvalidFormat)
AliasName.create("test--alias")     // Left(InvalidFormat)
```

#### AliasId

Unique identifier for alias entities using ULID format.

```kotlin
@JvmInline
value class AliasId private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<ScopeInputError.IdError, AliasId>
        fun generate(): AliasId
    }
    
    fun toAggregateId(): Either<AggregateIdError, AggregateId>
    override fun toString(): String = value
}
```

**Example:**
```kotlin
// Generate new ID
val aliasId = AliasId.generate()  // AliasId("01ARZ3NDEKTSV4RRFFQ69G5FAV")

// Create from existing ULID
val result = AliasId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV")  // Right(AliasId(...))

// Convert to AggregateId
val aggregateId = aliasId.toAggregateId()  // Right(AggregateId("gid://scopes/Alias/..."))
```

#### AliasType

Enumeration distinguishing alias types.

```kotlin
enum class AliasType {
    CANONICAL,  // Primary, auto-generated alias
    CUSTOM      // User-defined additional alias
}
```

### Entities

#### ScopeAlias

Represents the relationship between a scope and its alias.

```kotlin
data class ScopeAlias(
    val id: AliasId,
    val scopeId: ScopeId,
    val aliasName: AliasName,
    val aliasType: AliasType,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun createCanonical(
            scopeId: ScopeId,
            aliasName: AliasName,
            createdAt: Instant = Clock.System.now()
        ): ScopeAlias
        
        fun createCustom(
            scopeId: ScopeId,
            aliasName: AliasName,
            createdAt: Instant = Clock.System.now()
        ): ScopeAlias
    }
}
```

**Example:**
```kotlin
// Create canonical alias
val canonical = ScopeAlias.createCanonical(
    scopeId = ScopeId.generate(),
    aliasName = AliasName.create("quiet-river-x7k").getOrNull()!!
)

// Create custom alias
val custom = ScopeAlias.createCustom(
    scopeId = scopeId,
    aliasName = AliasName.create("auth-system").getOrNull()!!
)
```

### Domain Services

#### AliasGenerationService

Service for generating alias names.

```kotlin
interface AliasGenerationService {
    suspend fun generateCanonicalAlias(
        aliasId: AliasId
    ): Either<ScopeInputError.AliasError, AliasName>
    
    suspend fun generateRandomAlias(): Either<ScopeInputError.AliasError, AliasName>
}
```

#### AliasGenerationStrategy

Strategy interface for alias generation algorithms.

```kotlin
interface AliasGenerationStrategy {
    fun generate(seed: Long, wordProvider: WordProvider): String
    fun generateRandom(wordProvider: WordProvider): String
    fun getName(): String
}
```

#### WordProvider

Interface for providing words for alias generation.

```kotlin
interface WordProvider {
    fun getAdjectives(): List<String>
    fun getNouns(): List<String>
    fun getAdditionalWords(category: String): List<String>
    fun getAvailableCategories(): List<String>
}
```

### Repository Interfaces

#### ScopeAliasRepository

Repository for alias persistence operations.

```kotlin
interface ScopeAliasRepository {
    suspend fun save(alias: ScopeAlias): Either<PersistenceError, Unit>
    suspend fun findByAliasName(aliasName: AliasName): Either<PersistenceError, ScopeAlias?>
    suspend fun findById(id: AliasId): Either<PersistenceError, ScopeAlias?>
    suspend fun findByScopeId(scopeId: ScopeId): Either<PersistenceError, List<ScopeAlias>>
    suspend fun findCanonicalByScopeId(scopeId: ScopeId): Either<PersistenceError, ScopeAlias?>
    suspend fun findByScopeIdAndType(
        scopeId: ScopeId,
        type: AliasType
    ): Either<PersistenceError, List<ScopeAlias>>
    suspend fun findByAliasNamePrefix(
        prefix: String,
        limit: Int
    ): Either<PersistenceError, List<ScopeAlias>>
    suspend fun existsByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean>
    suspend fun removeByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean>
    suspend fun removeByScopeId(scopeId: ScopeId): Either<PersistenceError, Int>
    suspend fun update(alias: ScopeAlias): Either<PersistenceError, Boolean>
    suspend fun count(): Either<PersistenceError, Long>
    suspend fun listAll(offset: Long, limit: Int): Either<PersistenceError, List<ScopeAlias>>
}
```

### Domain Errors

```kotlin
sealed class ScopeAliasError : DomainError() {
    data class DuplicateAlias(
        val aliasName: String,
        val existingScopeId: ScopeId,
        val attemptedScopeId: ScopeId,
        override val occurredAt: Instant = Clock.System.now()
    ) : ScopeAliasError()
    
    data class AliasNotFound(
        val aliasName: String,
        override val occurredAt: Instant = Clock.System.now()
    ) : ScopeAliasError()
    
    data class CannotRemoveCanonicalAlias(
        val scopeId: ScopeId,
        val aliasName: String,
        override val occurredAt: Instant = Clock.System.now()
    ) : ScopeAliasError()
    
    data class AliasGenerationFailed(
        val scopeId: ScopeId,
        val retryCount: Int,
        override val occurredAt: Instant = Clock.System.now()
    ) : ScopeAliasError()
}
```

## Application Layer

### Commands

#### AddCustomAliasCommand
```kotlin
data class AddCustomAliasCommand(
    val scopeId: ScopeId,
    val aliasName: String
)
```

#### RemoveAliasCommand
```kotlin
data class RemoveAliasCommand(
    val aliasName: String
)
```

#### GenerateCanonicalAliasCommand
```kotlin
data class GenerateCanonicalAliasCommand(
    val scopeId: ScopeId
)
```

### Queries

#### GetScopeByAliasQuery
```kotlin
data class GetScopeByAliasQuery(
    val aliasName: String
)
```

#### GetAliasesByScopeIdQuery
```kotlin
data class GetAliasesByScopeIdQuery(
    val scopeId: ScopeId
)
```

#### SearchAliasesQuery
```kotlin
data class SearchAliasesQuery(
    val prefix: String,
    val limit: Int = 10
)
```

### Command Handlers

#### AddCustomAliasCommandHandler

```kotlin
class AddCustomAliasCommandHandler(
    private val scopeRepository: ScopeRepository,
    private val aliasRepository: ScopeAliasRepository,
    private val transactionManager: TransactionManager
) : CommandHandler<AddCustomAliasCommand, ScopeAlias> {
    
    override suspend fun handle(
        command: AddCustomAliasCommand
    ): Either<ApplicationError, ScopeAlias> = either {
        // 1. Validate alias format
        val aliasName = AliasName.create(command.aliasName)
            .mapLeft { it.toApplicationError() }
            .bind()
            
        // 2. Check scope exists
        val scope = scopeRepository.findById(command.scopeId)
            .mapLeft { it.toApplicationError() }
            .bind()
            ?: raise(ApplicationError.NotFound.Scope(command.scopeId.toString()))
            
        // 3. Check for duplicates
        val exists = aliasRepository.existsByAliasName(aliasName)
            .mapLeft { it.toApplicationError() }
            .bind()
            
        if (exists) {
            raise(ApplicationError.AliasDuplicate(command.aliasName))
        }
        
        // 4. Create and save alias
        val alias = ScopeAlias.createCustom(command.scopeId, aliasName)
        
        transactionManager.inTransaction {
            aliasRepository.save(alias)
                .mapLeft { it.toApplicationError() }
        }.bind()
        
        alias
    }
}
```

### DTOs

#### ScopeDto with Aliases
```kotlin
data class ScopeDto(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val customAliases: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### AliasDto
```kotlin
data class AliasDto(
    val aliasName: String,
    val scopeId: String,
    val scopeTitle: String,
    val aliasType: String,
    val createdAt: Instant
)
```

### Application Errors

```kotlin
sealed class ScopeAliasError(
    recoverable: Boolean = true
) : ApplicationError(recoverable) {
    
    data class AliasDuplicate(
        val aliasName: String,
        val existingScopeId: String,
        val attemptedScopeId: String
    ) : ScopeAliasError()
    
    data class AliasNotFound(
        val aliasName: String
    ) : ScopeAliasError()
    
    data class CannotRemoveCanonicalAlias(
        val scopeId: String,
        val aliasName: String
    ) : ScopeAliasError()
    
    data class AliasGenerationFailed(
        val scopeId: String,
        val retryCount: Int
    ) : ScopeAliasError(recoverable = false)
}
```

## Infrastructure Layer

### DefaultAliasGenerationService

```kotlin
class DefaultAliasGenerationService(
    private val strategy: AliasGenerationStrategy,
    private val wordProvider: WordProvider
) : AliasGenerationService {
    
    override suspend fun generateCanonicalAlias(
        aliasId: AliasId
    ): Either<ScopeInputError.AliasError, AliasName> = try {
        val seed = aliasId.value.hashCode().toLong()
        val aliasString = strategy.generate(seed, wordProvider)
        AliasName.create(aliasString)
    } catch (e: Exception) {
        ScopeInputError.AliasError.InvalidFormat(
            occurredAt = currentTimestamp(),
            attemptedValue = e.message ?: "generation failed",
            expectedPattern = "[a-z][a-z0-9-_]{1,63}"
        ).left()
    }
}
```

### HaikunatorStrategy

```kotlin
class HaikunatorStrategy : AliasGenerationStrategy {
    
    override fun generate(seed: Long, wordProvider: WordProvider): String {
        val random = Random(seed)
        val adjective = wordProvider.getAdjectives()[
            random.nextInt(wordProvider.getAdjectives().size)
        ]
        val noun = wordProvider.getNouns()[
            random.nextInt(wordProvider.getNouns().size)
        ]
        val token = generateToken(random, 3)
        return "$adjective-$noun-$token"
    }
    
    private fun generateToken(random: Random, length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
```

### InMemoryScopeAliasRepository

```kotlin
class InMemoryScopeAliasRepository : ScopeAliasRepository {
    private val aliases = mutableMapOf<AliasName, ScopeAlias>()
    private val mutex = Mutex()
    
    override suspend fun save(alias: ScopeAlias): Either<PersistenceError, Unit> = 
        either {
            mutex.withLock {
                aliases[alias.aliasName] = alias
            }
        }
        
    override suspend fun findByAliasName(
        aliasName: AliasName
    ): Either<PersistenceError, ScopeAlias?> = 
        either {
            mutex.withLock {
                aliases[aliasName]
            }
        }
    
    // ... other methods
}
```

## Usage Examples

### Creating a Scope with Custom Alias

```kotlin
// In command handler
val createCommand = CreateScopeCommand(
    title = "Authentication System",
    canonicalAlias = "auth-system"
)

val result = createScopeHandler.handle(createCommand)
result.fold(
    { error -> println("Failed: $error") },
    { scope -> println("Created scope with alias: ${scope.canonicalAlias}") }
)
```

### Adding Multiple Aliases

```kotlin
// Add custom aliases
val commands = listOf(
    AddCustomAliasCommand(scopeId, "auth"),
    AddCustomAliasCommand(scopeId, "authentication"),
    AddCustomAliasCommand(scopeId, "security-module")
)

commands.forEach { command ->
    addAliasHandler.handle(command).fold(
        { error -> println("Failed to add alias: $error") },
        { alias -> println("Added alias: ${alias.aliasName}") }
    )
}
```

### Searching for Aliases

```kotlin
val searchQuery = SearchAliasesQuery(prefix = "auth", limit = 10)
val results = searchAliasesHandler.handle(searchQuery)

results.fold(
    { error -> println("Search failed: $error") },
    { aliases -> 
        aliases.forEach { dto ->
            println("${dto.aliasName} -> ${dto.scopeTitle}")
        }
    }
)
```

### Alias Resolution

```kotlin
suspend fun resolveAlias(input: String): Either<ApplicationError, Scope> {
    // Try exact match first
    val exactQuery = GetScopeByAliasQuery(input)
    val exactResult = getScopeByAliasHandler.handle(exactQuery)
    
    if (exactResult.isRight()) {
        return exactResult
    }
    
    // Try prefix match
    val searchQuery = SearchAliasesQuery(input, limit = 2)
    val searchResult = searchAliasesHandler.handle(searchQuery)
    
    return searchResult.flatMap { aliases ->
        when (aliases.size) {
            0 -> ApplicationError.NotFound.Alias(input).left()
            1 -> getScopeByAliasHandler.handle(
                GetScopeByAliasQuery(aliases.first().aliasName)
            )
            else -> ApplicationError.AmbiguousAlias(
                input, 
                aliases.map { it.aliasName }
            ).left()
        }
    }
}
```

## Testing

### Unit Test Example

```kotlin
class AliasNameTest : DescribeSpec({
    describe("AliasName creation") {
        it("should normalize to lowercase") {
            val result = AliasName.create("AUTH-SYSTEM")
            result.shouldBeRight()
            result.getOrNull()!!.value shouldBe "auth-system"
        }
        
        it("should reject invalid patterns") {
            val invalidNames = listOf("", "a", "1start", "-start", "test--alias")
            invalidNames.forEach { name ->
                AliasName.create(name).shouldBeLeft()
            }
        }
    }
})
```

### Integration Test Example

```kotlin
class AliasIntegrationTest : DescribeSpec({
    lateinit var context: IntegrationTestContext
    
    beforeSpec {
        IntegrationTestFixture.setupTestDependencies()
        context = IntegrationTestFixture.createTestContext()
    }
    
    describe("Full alias workflow") {
        it("should create scope with custom alias and find it") {
            // Create scope
            val createResult = context.createScopeHandler.handle(
                CreateScopeCommand(
                    title = "Test Scope",
                    canonicalAlias = "test-scope"
                )
            )
            createResult.shouldBeRight()
            
            // Find by alias
            val findResult = context.getScopeByAliasHandler.handle(
                GetScopeByAliasQuery("test-scope")
            )
            findResult.shouldBeRight()
            findResult.getOrNull()!!.title shouldBe "Test Scope"
        }
    }
})
```

## Performance Considerations

### Indexing
- Primary index on `aliasName` for O(1) lookups
- Secondary index on `scopeId` for listing aliases
- Prefix index for search operations

### Caching
- LRU cache for frequently accessed aliases
- Cache invalidation on updates
- TTL-based expiration

### Batch Operations
```kotlin
// Efficient bulk import
suspend fun importAliases(
    mappings: List<Pair<String, String>>
): Either<ApplicationError, Int> = either {
    var successCount = 0
    
    transactionManager.inTransaction {
        mappings.forEach { (aliasName, scopeId) ->
            // Batch validation and insertion
            successCount++
        }
        successCount.right()
    }.bind()
}
```

## Security

### Input Validation
- Pattern matching prevents injection attacks
- Length limits prevent DoS
- Character restrictions ensure URL safety

### Authorization
```kotlin
// Example authorization check
suspend fun authorizedAliasOperation(
    userId: UserId,
    scopeId: ScopeId,
    operation: AliasOperation
): Either<AuthorizationError, Unit> = either {
    val scope = scopeRepository.findById(scopeId).bind()
    val hasPermission = authService.checkPermission(userId, scope, operation)
    
    if (!hasPermission) {
        raise(AuthorizationError.InsufficientPermissions(operation))
    }
}
```

## Versioning

The alias system API follows semantic versioning:
- **1.0.0**: Initial release with core functionality
- **1.1.0**: Added bulk import/export
- **1.2.0**: Added search capabilities
- **2.0.0**: Breaking change - lowercase normalization

## Migration

See [Migrating to Aliases](../../guides/migrating-to-aliases.md) for upgrade paths.
