# Testing Patterns and Guidelines

This guide covers testing strategies, patterns, and best practices used in the Scopes project.

## Table of Contents
- [Testing Philosophy](#testing-philosophy)
- [Test Structure](#test-structure)
- [Testing Patterns](#testing-patterns)
- [Architecture Testing with Konsist](#architecture-testing-with-konsist)
- [Error Testing](#error-testing)
- [Property-Based Testing](#property-based-testing)
- [Integration Testing](#integration-testing)
- [Best Practices](#best-practices)

## Testing Philosophy

### Core Principles

- **No Mock Services**: Do not use mock services for anything ever
- **Fail-Fast**: Do not move on to the next test until the current test is complete
- **Verbose Testing**: Tests should be verbose for debugging purposes
- **Test Structure First**: If a test fails, check if the test is structured correctly before refactoring
- **Comprehensive Coverage**: Test both success and failure paths

## Test Structure

### Project Test Organization

```
contexts/scope-management/
├── domain/src/test/kotlin/
│   ├── entity/                 # Entity tests
│   ├── valueobject/           # Value object tests
│   └── service/               # Domain service tests
├── application/src/test/kotlin/
│   ├── handler/               # Use case handler tests
│   ├── service/               # Application service tests
│   └── mapper/                # DTO mapper tests
└── infrastructure/src/test/kotlin/
    ├── repository/            # Repository implementation tests
    └── adapter/               # Adapter tests
```

### Test Naming Conventions

```kotlin
// Test class naming
class CreateScopeHandlerTest : DescribeSpec({...})
class ScopeTitleTest : FunSpec({...})
class ScopeRepositoryIntegrationTest : DescribeSpec({...})

// Test case naming
describe("CreateScopeHandler") {
    context("when creating a scope with valid input") {
        it("should return success result") {...}
    }
    
    context("when title validation fails") {
        it("should return TitleValidationFailed error") {...}
    }
}
```

## Testing Patterns

### Use Case Handler Testing

Test handler logic with real implementations:

```kotlin
class CreateScopeHandlerTest : DescribeSpec({
    // Use real implementations, not mocks
    val repository = InMemoryScopeRepository()
    val validationService = ApplicationScopeValidationService(repository)
    val handler = CreateScopeHandler(repository, validationService)
    
    describe("successful scope creation") {
        it("should create scope and return result") {
            val command = CreateScope(
                title = "Valid Title",
                description = "Description",
                parentId = null,
                metadata = emptyMap()
            )
            
            val result = handler.invoke(command)
            
            result.shouldBeRight { scopeResult ->
                scopeResult.title shouldBe "Valid Title"
                scopeResult.description shouldBe "Description"
                scopeResult.parentId shouldBe null
            }
            
            // Verify persistence
            val saved = repository.findById(ScopeId.from(scopeResult.id))
            saved.shouldBeRight { scope ->
                scope.shouldNotBeNull()
                scope.title.value shouldBe "Valid Title"
            }
        }
    }
})
```

### Service-Specific Error Testing

Comprehensive error translation verification:

```kotlin
class CreateScopeHandlerServiceErrorIntegrationTest : DescribeSpec({
    describe("title validation error translation") {
        it("should translate TitleTooShort to ValidationFailed") {
            val command = CreateScope(
                title = "ab",
                description = "Test description", 
                parentId = null,
                metadata = emptyMap()
            )

            // Create service that will return specific error
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            coEvery { 
                mockValidationService.validateTitleFormat("ab") 
            } returns TitleValidationError.TitleTooShort(3, 2, "ab").left()
            
            val handler = CreateScopeHandler(repository, mockValidationService)
            val result = handler.invoke(command)
            
            result.isLeft() shouldBe true
            val error = result.leftOrNull()
                .shouldBeInstanceOf<CreateScopeError.TitleValidationFailed>()
            val titleError = error.titleError
                .shouldBeInstanceOf<TitleValidationError.TitleTooShort>()
            titleError.minLength shouldBe 3
            titleError.actualLength shouldBe 2
        }
    }
    
    describe("business rule error translation") {
        it("should translate MaxDepthExceeded to BusinessRuleViolationFailed") {
            val parentId = ScopeId.generate()
            val command = CreateScope(
                title = "Valid Title",
                description = "Test description",
                parentId = parentId.value,
                metadata = emptyMap()
            )

            // Setup validation service to return specific error
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            coEvery { 
                mockValidationService.validateHierarchyConstraints(parentId) 
            } returns ScopeBusinessRuleError.MaxDepthExceeded(
                10, 11, parentId, listOf(parentId)
            ).left()
            
            val handler = CreateScopeHandler(repository, mockValidationService)
            val result = handler.invoke(command)
            
            result.isLeft() shouldBe true
            val error = result.leftOrNull()
                .shouldBeInstanceOf<CreateScopeError.BusinessRuleViolationFailed>()
            val businessError = error.businessRuleError
                .shouldBeInstanceOf<ScopeBusinessRuleError.MaxDepthExceeded>()
            businessError.maxDepth shouldBe 10
            businessError.actualDepth shouldBe 11
            businessError.scopeId shouldBe parentId
        }
    }
})
```

### Repository Testing

Test repository implementations with edge cases:

```kotlin
class InMemoryScopeRepositoryTest : DescribeSpec({
    describe("findById") {
        context("when scope exists") {
            it("should return the scope") {
                val repository = InMemoryScopeRepository()
                val scope = createTestScope()
                repository.save(scope).shouldBeRight()
                
                val result = repository.findById(scope.id)
                
                result.shouldBeRight { found ->
                    found shouldBe scope
                }
            }
        }
        
        context("when scope does not exist") {
            it("should return null") {
                val repository = InMemoryScopeRepository()
                val result = repository.findById(ScopeId.generate())
                
                result.shouldBeRight { found ->
                    found shouldBe null
                }
            }
        }
    }
    
    describe("concurrent access") {
        it("should handle concurrent saves") {
            val repository = InMemoryScopeRepository()
            val scopes = (1..100).map { createTestScope() }
            
            runBlocking {
                scopes.map { scope ->
                    async {
                        repository.save(scope)
                    }
                }.awaitAll()
            }
            
            scopes.forEach { scope ->
                repository.findById(scope.id).shouldBeRight { 
                    it shouldBe scope 
                }
            }
        }
    }
})
```

## Architecture Testing with Konsist

### Clean Architecture Validation

```kotlin
class CleanArchitectureTest : StringSpec({
    "domain layer should not depend on application or infrastructure" {
        Konsist
            .scopeFromDirectory("contexts/scope-management/domain")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains(".application.") ||
                    import.name.contains(".infrastructure.")
                }
            }
    }
    
    "application layer should not depend on infrastructure" {
        Konsist
            .scopeFromDirectory("contexts/scope-management/application")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains(".infrastructure.")
                }
            }
    }
})
```

### Naming Convention Tests

```kotlin
class NamingConventionTest : StringSpec({
    "use case handlers should end with Handler" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .withNameEndingWith("Handler")
            .assert { it.name.endsWith("Handler") }
    }
    
    "DTOs should use appropriate suffixes" {
        Konsist
            .scopeFromDirectory("contexts/*/application")
            .classes()
            .withPackage("..dto..")
            .assert { 
                it.name.endsWith("Dto") || 
                it.name.endsWith("Result") || 
                it.name.endsWith("Input") ||
                it.name == "DTO"
            }
    }
    
    "domain errors should use strongly-typed identifiers" {
        Konsist
            .scopeFromModule("domain")
            .classes()
            .withNameEndingWith("Error")
            .functions()
            .parameters
            .assert { parameter ->
                if (parameter.name.contains("scopeId") || 
                    parameter.name.contains("parentId")) {
                    parameter.type.name == "ScopeId"
                } else {
                    true
                }
            }
    }
})
```

### DDD Pattern Tests

```kotlin
class DddPatternTest : StringSpec({
    "value objects should be immutable" {
        Konsist
            .scopeFromPackage("..valueobject..")
            .classes()
            .assertFalse { clazz ->
                clazz.properties().any { it.hasModifier(KoModifier.VAR) }
            }
    }
    
    "repositories should have interfaces in domain" {
        val implementations = Konsist
            .scopeFromPackage("..infrastructure.repository..")
            .classes()
            .filter { it.name.endsWith("Repository") }
        
        implementations.assertTrue { impl ->
            Konsist
                .scopeFromPackage("..domain.repository..")
                .interfaces()
                .any { it.name == impl.name.removeSuffix("Impl") }
        }
    }
})
```

## Error Testing

### Error Mapping Tests

```kotlin
class ErrorMappingSpecificationTest : DescribeSpec({
    describe("Error mapping specifications") {
        it("should preserve error context during mapping") {
            val domainError = DomainScopeInputError.TitleError.TooShort(
                occurredAt = Clock.System.now(),
                attemptedValue = "ab",
                minimumLength = 3
            )
            
            val applicationError = domainError.toApplicationError() 
                as AppScopeInputError.TitleTooShort
            
            applicationError.attemptedValue shouldBe "ab"
            applicationError.minimumLength shouldBe 3
        }
        
        it("should fail fast for unmapped error types") {
            val unmappedError = object : DomainError() {
                override val message = "Unmapped"
            }
            
            shouldThrow<IllegalStateException> {
                unmappedError.toApplicationError()
            }
        }
    }
})
```

## Property-Based Testing

### Value Object Invariants

```kotlin
class ScopeTitlePropertyTest : FunSpec({
    test("title should always be trimmed") {
        checkAll(Arb.string()) { input ->
            ScopeTitle.create(input).fold(
                { true }, // Error case is valid
                { it?.value == input.trim() }
            )
        }
    }
    
    test("title length should not exceed maximum") {
        checkAll(Arb.string(201..500)) { input ->
            ScopeTitle.create(input).isLeft()
        }
    }
    
    test("valid titles should be accepted") {
        checkAll(
            Arb.string(1..200)
                .filter { it.trim().isNotEmpty() }
        ) { input ->
            ScopeTitle.create(input).isRight()
        }
    }
})
```

### Business Rule Properties

```kotlin
class HierarchyPropertyTest : FunSpec({
    test("hierarchy depth should never exceed maximum") {
        checkAll(
            Arb.list(Arb.uuid().map { ScopeId.from(it.toString()) }, 1..20)
        ) { scopeIds ->
            val hierarchy = buildHierarchy(scopeIds)
            val depth = calculateDepth(hierarchy)
            
            if (depth > MAX_HIERARCHY_DEPTH) {
                validateHierarchy(hierarchy).isLeft()
            } else {
                validateHierarchy(hierarchy).isRight()
            }
        }
    }
})
```

## Integration Testing

### End-to-End Tests

```kotlin
class ScopeManagementIntegrationTest : DescribeSpec({
    val facade = ScopeManagementFacade(
        createHandler = CreateScopeHandler(repository, validationService),
        updateHandler = UpdateScopeHandler(repository, validationService),
        deleteHandler = DeleteScopeHandler(repository)
    )
    
    describe("complete scope lifecycle") {
        it("should create, update, and delete scope") {
            // Create
            val createCommand = CreateScopeCommand(
                title = "Integration Test",
                description = "Testing lifecycle"
            )
            val created = facade.createScope(createCommand).shouldBeRight()
            
            // Update
            val updateCommand = UpdateScopeCommand(
                id = created.id,
                title = "Updated Title"
            )
            val updated = facade.updateScope(updateCommand).shouldBeRight()
            updated.title shouldBe "Updated Title"
            
            // Delete
            val deleteCommand = DeleteScopeCommand(id = created.id)
            facade.deleteScope(deleteCommand).shouldBeRight()
            
            // Verify deletion
            facade.getScope(GetScopeQuery(id = created.id))
                .shouldBeRight { it shouldBe null }
        }
    }
})
```

## Best Practices

### 1. Test Data Builders

Create builders for complex test data:

```kotlin
object TestDataBuilders {
    fun scope(
        id: ScopeId = ScopeId.generate(),
        title: String = "Test Scope",
        description: String? = null,
        parentId: ScopeId? = null
    ): Scope = Scope(
        id = id,
        title = ScopeTitle.create(title).getOrElse { 
            throw IllegalArgumentException("Invalid test title") 
        },
        description = description?.let { 
            ScopeDescription.create(it).getOrElse { null } 
        },
        parentId = parentId,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        aspects = emptyMap()
    )
}
```

### 2. Test Fixtures

Organize common test data:

```kotlin
object TestFixtures {
    val validTitle = "Valid Test Title"
    val tooShortTitle = "ab"
    val tooLongTitle = "a".repeat(201)
    val emptyTitle = ""
    val blankTitle = "   "
    
    val rootScopeId = ScopeId.from("test-root-scope")
    val childScopeId = ScopeId.from("test-child-scope")
}
```

### 3. Assertion Extensions

Create domain-specific assertions:

```kotlin
fun Either<*, CreateScopeResult>.shouldSucceedWith(
    block: CreateScopeResult.() -> Unit
) {
    this.shouldBeRight()
    this.getOrNull()!!.block()
}

fun Either<CreateScopeError, *>.shouldFailWith(
    expectedError: KClass<out CreateScopeError>
) {
    this.shouldBeLeft()
    this.leftOrNull()!!.shouldBeInstanceOf(expectedError)
}
```

### 4. Test Organization

- Group related tests using `describe` and `context`
- Use descriptive test names that explain the scenario
- Test one behavior per test case
- Arrange-Act-Assert pattern for clarity

### 5. Performance Testing

Include performance tests for critical paths:

```kotlin
class PerformanceTest : StringSpec({
    "repository should handle large datasets efficiently" {
        val repository = InMemoryScopeRepository()
        val scopes = (1..10000).map { createTestScope() }
        
        val saveTime = measureTimeMillis {
            scopes.forEach { repository.save(it) }
        }
        
        saveTime shouldBeLessThan 5000 // 5 seconds
        
        val findTime = measureTimeMillis {
            scopes.forEach { scope ->
                repository.findById(scope.id)
            }
        }
        
        findTime shouldBeLessThan 1000 // 1 second
    }
})
```

## Related Documentation

- [Error Handling](./error-handling.md) - Error testing strategies
- [Clean Architecture Patterns](./clean-architecture-patterns.md) - Architectural testing
- [Architecture Testing Guide](../architecture-testing-guide.md) - Konsist usage
