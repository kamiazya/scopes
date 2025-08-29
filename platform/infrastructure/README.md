# Platform Infrastructure

This module provides shared infrastructure components for the Scopes application, including database connection management and transaction handling.

## Database Management

### Single Database Instance

The platform infrastructure ensures that only one SQLite database connection exists across the entire application. This is crucial for:

1. **Transaction Integrity**: All operations participate in the same transaction context
2. **Resource Efficiency**: Reduces memory usage and connection overhead
3. **Data Consistency**: Prevents conflicts from multiple connections

### Usage

#### Application Startup

```kotlin
import io.github.kamiazya.scopes.platform.infrastructure.database.DatabaseBootstrap
import io.github.kamiazya.scopes.eventstore.infrastructure.factory.EventStoreFactory
import io.github.kamiazya.scopes.devicesync.infrastructure.factory.DeviceSyncFactory

fun main() {
    // Initialize database at startup
    val transactionManager = DatabaseBootstrap.initializeProduction()
    
    // Create services using shared connection
    val eventStore = EventStoreFactory.createEventStore(serializersModule)
    val deviceSync = DeviceSyncFactory.createDeviceSync(eventStore)
    
    // Use services...
    
    // Shutdown gracefully
    DatabaseBootstrap.shutdown()
}
```

#### Testing

```kotlin
import io.github.kamiazya.scopes.platform.infrastructure.database.DatabaseBootstrap

class MyTest {
    @BeforeTest
    fun setup() {
        // Use in-memory database for tests
        val transactionManager = DatabaseBootstrap.initializeTest()
    }
    
    @AfterTest
    fun teardown() {
        DatabaseBootstrap.shutdown()
    }
}
```

### Configuration Options

The `DatabaseConfiguration` class provides various options for SQLite:

- **WAL Mode**: Enabled by default for better concurrency
- **Busy Timeout**: Configurable timeout for locked database
- **Synchronous Mode**: Balance between performance and durability
- **Foreign Keys**: Enforced by default

### Transaction Management

Use `TransactionManager` for all database operations:

```kotlin
suspend fun myUseCase(transactionManager: TransactionManager): Either<Error, Result> {
    return transactionManager.inTransaction {
        // All database operations here share the same transaction
        val event = eventRepository.store(event)
        val state = syncRepository.updateState(state)
        
        // Return success or error
        Either.Right(Result(event, state))
    }
}
```

## Architecture Benefits

1. **Clean Architecture**: Infrastructure details are isolated from business logic
2. **Testability**: Easy to mock TransactionManager for unit tests
3. **Flexibility**: Can swap database implementations without changing business code
4. **Performance**: Optimized SQLite settings for local-first applications

## Migration from Direct Connections

If you have code that creates database connections directly:

```kotlin
// Old approach - DON'T DO THIS
val database = Database.connect("jdbc:sqlite:mydb.db", "org.sqlite.JDBC")
```

Replace with:

```kotlin
// New approach - USE THIS
val transactionManager = DatabaseBootstrap.initializeProduction()
val eventStore = EventStoreFactory.createEventStore(serializersModule)
```
