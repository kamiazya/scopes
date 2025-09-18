# Arrow Style Guide for Scopes

This guide defines the standard patterns for using Arrow library in the Scopes codebase.

## Core Principles

1. **Type Safety First**: Use Either for all operations that can fail
2. **Explicit Error Handling**: No exceptions in business logic
3. **Functional Composition**: Leverage Arrow's functional operators
4. **Consistency**: Follow the same patterns across all layers

## Either Usage Patterns

### Basic Construction

```kotlin
// ✅ GOOD - Use extension functions
val success = value.right()
val failure = error.left()

// ❌ BAD - Don't use constructors directly
val success = Either.Right(value)
val failure = Either.Left(error)
```

### Either Blocks

Use `either{}` blocks for composing multiple operations:

```kotlin
// ✅ GOOD - Clean composition with either block
suspend fun createUser(request: CreateUserRequest): Either<DomainError, User> = either {
    val validatedEmail = Email.create(request.email).bind()
    val validatedName = Name.create(request.name).bind()
    val user = User(
        id = UserId.generate(),
        email = validatedEmail,
        name = validatedName
    )
    userRepository.save(user).bind()
}

// ❌ BAD - Manual chaining
suspend fun createUser(request: CreateUserRequest): Either<DomainError, User> {
    return Email.create(request.email).flatMap { email ->
        Name.create(request.name).flatMap { name ->
            val user = User(id = UserId.generate(), email = email, name = name)
            userRepository.save(user)
        }
    }
}
```

### Error Transformation

Use `mapLeft` when transforming errors between layers:

```kotlin
// ✅ GOOD - Clear error transformation
override suspend fun execute(command: Command): Either<ApplicationError, Result> = 
    domainService.process(command)
        .mapLeft { domainError -> 
            ApplicationError.from(domainError) 
        }

// ❌ BAD - Using fold for simple transformation
override suspend fun execute(command: Command): Either<ApplicationError, Result> = 
    domainService.process(command).fold(
        { error -> ApplicationError.from(error).left() },
        { result -> result.right() }
    )
```

## Validation Patterns

### Using ensure

Prefer `ensure` for validation within either blocks:

```kotlin
// ✅ GOOD - Clean validation with ensure
suspend fun updateBalance(accountId: String, amount: Double): Either<Error, Account> = either {
    val account = accountRepository.findById(accountId).bind()
    
    ensure(amount > 0) { 
        Error.InvalidAmount("Amount must be positive") 
    }
    
    ensure(account.balance + amount >= 0) { 
        Error.InsufficientFunds("Insufficient balance") 
    }
    
    account.copy(balance = account.balance + amount)
}

// ❌ BAD - Manual validation
suspend fun updateBalance(accountId: String, amount: Double): Either<Error, Account> = either {
    val account = accountRepository.findById(accountId).bind()
    
    if (amount <= 0) {
        raise(Error.InvalidAmount("Amount must be positive"))
    }
    
    if (account.balance + amount < 0) {
        raise(Error.InsufficientFunds("Insufficient balance"))
    }
    
    account.copy(balance = account.balance + amount)
}
```

### Factory Methods with Validation

```kotlin
// ✅ GOOD - Factory method returning Either
data class Email private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<ValidationError, Email> = either {
            ensure(value.isNotBlank()) { 
                ValidationError.BlankField("email") 
            }
            ensure(value.contains("@")) { 
                ValidationError.InvalidFormat("email", "must contain @") 
            }
            Email(value)
        }
    }
}
```

## Error Handling in Infrastructure

### Exception Handling

Use Arrow's `catch` for exception handling:

```kotlin
// ✅ GOOD - Using Arrow's catch
override suspend fun findById(id: UserId): Either<PersistenceError, User?> = 
    catch {
        database.users.findOne(id.value)?.toDomainModel()
    }.mapLeft { throwable ->
        PersistenceError.DatabaseError(
            message = "Failed to find user",
            cause = throwable
        )
    }

// ⚠️ ACCEPTABLE - Try-catch for complex scenarios
override suspend fun save(user: User): Either<PersistenceError, User> = 
    withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Complex multi-table operations
            }
            user.right()
        } catch (e: SQLException) {
            PersistenceError.DatabaseError("Failed to save user", e).left()
        } catch (e: Exception) {
            PersistenceError.UnexpectedError("Unexpected error", e).left()
        }
    }
```

## Testing Patterns

### Asserting Either Results

```kotlin
// ✅ GOOD - Safe assertion with clear error message
@Test
fun `should create valid email`() {
    val result = Email.create("user@example.com")
    
    result.fold(
        { error -> fail("Expected success but got: $error") },
        { email -> email.value shouldBe "user@example.com" }
    )
}

// Alternative with kotest-arrow
@Test
fun `should create valid email`() {
    val result = Email.create("user@example.com")
    
    result.shouldBeRight { email ->
        email.value shouldBe "user@example.com"
    }
}

// ❌ BAD - Unsafe unwrapping
@Test
fun `should create valid email`() {
    val email = Email.create("user@example.com").getOrNull()!!
    email.value shouldBe "user@example.com"
}
```

## Layer Communication

### Port Adapters

```kotlin
// ✅ GOOD - Clear layer separation with error mapping
class UserPortAdapter(
    private val userService: UserService,
    private val errorMapper: ErrorMapper
) : UserPort {
    override suspend fun createUser(
        request: CreateUserRequest
    ): Either<ContractError, UserResponse> = 
        userService.createUser(request.toDomainModel())
            .mapLeft { domainError -> 
                errorMapper.toContractError(domainError) 
            }
            .map { user -> 
                UserResponse.from(user) 
            }
}
```

## Advanced Patterns

### Parallel Operations

When you need to run multiple operations in parallel:

```kotlin
// ✅ GOOD - Parallel execution with proper error handling
suspend fun processMultipleItems(
    items: List<Item>
): Either<Error, List<ProcessedItem>> = either {
    coroutineScope {
        items.map { item ->
            async {
                processItem(item)
            }
        }.map { deferred ->
            deferred.await().bind()
        }
    }
}
```

### Resource Management

Use `use` extension for resource management:

```kotlin
// ✅ GOOD - Automatic resource cleanup
suspend fun readFile(path: Path): Either<FileError, String> = catch {
    path.toFile().bufferedReader().use { reader ->
        reader.readText()
    }
}.mapLeft { throwable ->
    FileError.ReadFailed(path, throwable)
}
```

## Anti-Patterns to Avoid

### 1. Using null as Error Signal

```kotlin
// ❌ BAD - Using null for errors
val result = operation().fold({ null }, { it }) ?: return

// ✅ GOOD - Explicit error handling
operation().fold(
    { error -> handleError(error) },
    { value -> handleSuccess(value) }
)
```

### 2. Nested Either Types

```kotlin
// ❌ BAD - Nested Either
fun process(): Either<Error, Either<Warning, Result>>

// ✅ GOOD - Sealed class for multiple outcomes
sealed class ProcessResult {
    data class Success(val result: Result) : ProcessResult()
    data class Warning(val warning: Warning, val result: Result) : ProcessResult()
}

fun process(): Either<Error, ProcessResult>
```

### 3. Throwing Exceptions in Domain

```kotlin
// ❌ BAD - Throwing exceptions
fun validateAge(age: Int): Age {
    if (age < 0) throw IllegalArgumentException("Age cannot be negative")
    return Age(age)
}

// ✅ GOOD - Returning Either
fun validateAge(age: Int): Either<ValidationError, Age> = either {
    ensure(age >= 0) { ValidationError.InvalidAge("Age cannot be negative") }
    Age(age)
}
```

## Migration Guidelines

When updating existing code:

1. Start from the domain layer and work outward
2. Replace exceptions with Either returns
3. Update tests to handle Either results
4. Add error mapping between layers
5. Document error types in KDoc

## Additional Arrow Features to Consider

- **Option**: For truly optional values without error semantics
- **Validated**: For accumulating validation errors
- **NonEmptyList**: For collections that must have at least one element
- **Ior**: For results that can be both success and warning

## References

- [Arrow Core Documentation](https://arrow-kt.io/docs/core/)
- [Error Handling Guidelines](./error-handling.md)
