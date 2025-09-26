package io.github.kamiazya.scopes.scopemanagement.infrastructure.integration

import arrow.core.getOrElse
import arrow.core.flatMap
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.metrics.DefaultProjectionMetrics
import io.github.kamiazya.scopes.platform.observability.metrics.InMemoryMetricsRegistry
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.EventStoreContractErrorMapper
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.InMemoryEventStoreAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.policy.DefaultHierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.projection.EventProjectionService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.projection.OutboxEventProjectionService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.projection.OutboxProjectionService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.ContractBasedScopeEventSourcingRepository
import io.github.kamiazya.scopes.platform.infrastructure.transaction.NoOpTransactionManager
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightEventOutboxRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializersModule
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent

/**
 * End-to-end integration test for the Event Sourcing flow.
 * 
 * Tests the complete flow:
 * 1. Command execution (Create/Update/Delete)
 * 2. Event saving to Event Store
 * 3. Outbox enqueueing  
 * 4. Event projection to read model
 * 5. RDB retrieval
 */
class EventSourcingE2EIntegrationTest : DescribeSpec({
    
    describe("Event Sourcing E2E Flow") {
        
        // Setup test infrastructure
        val logger = ConsoleLogger("E2E-Test")
        val json = Json {
            serializersModule = ScopeEventSerializersModule.create()
            ignoreUnknownKeys = true
            isLenient = true
            classDiscriminator = "type"
        }
        
        fun createTestEnvironment(): TestEnvironment {
            // In-memory database for testing
            val db = SqlDelightDatabaseProvider.createDatabase(":memory:")
            
            // Repositories
            val scopeRepo = SqlDelightScopeRepository(db)
            val aliasRepo = SqlDelightScopeAliasRepository(db)
            val outboxRepo = SqlDelightEventOutboxRepository(db)
            
            // Event Store
            val eventStoreAdapter = InMemoryEventStoreAdapter(json)
            val eventStoreCommandPort: EventStoreCommandPort = eventStoreAdapter
            val eventStoreQueryPort: EventStoreQueryPort = eventStoreAdapter
            
            // Event Sourcing Repository
            val errorMapper = EventStoreContractErrorMapper(logger)
            val eventSourcingRepo = ContractBasedScopeEventSourcingRepository(
                eventStoreCommandPort = eventStoreCommandPort,
                eventStoreQueryPort = eventStoreQueryPort,
                eventStoreContractErrorMapper = errorMapper,
                json = json
            )
            
            // Projection services
            val metrics = DefaultProjectionMetrics(InMemoryMetricsRegistry())
            val projectionService = EventProjectionService(
                scopeRepository = scopeRepo,
                scopeAliasRepository = aliasRepo,
                logger = logger,
                projectionMetrics = metrics
            )
            
            val outboxProjector = OutboxProjectionService(
                outboxRepository = outboxRepo,
                projectionService = projectionService,
                json = json,
                logger = logger
            )
            
            // Event Publisher that enqueues to outbox and processes immediately
            val eventPublisher = OutboxEventProjectionService(
                outboxRepository = outboxRepo,
                projector = outboxProjector,
                json = json,
                logger = logger,
                processImmediately = true // Process events immediately for testing
            )
            
            // Application services
            val hierarchyPolicyProvider = DefaultHierarchyPolicyProvider()
            val hierarchyService = ScopeHierarchyService()
            val hierarchyAppService = ScopeHierarchyApplicationService(scopeRepo, hierarchyService)
            val wordProvider = DefaultWordProvider()
            val aliasGenerationService = DefaultAliasGenerationService(
                strategy = HaikunatorStrategy(),
                wordProvider = wordProvider
            )
            val transactionManager = NoOpTransactionManager()
            val appErrorMapper = ApplicationErrorMapper(logger)
            
            // Command handlers
            val createHandler = CreateScopeHandler(
                eventSourcingRepository = eventSourcingRepo,
                scopeRepository = scopeRepo,
                hierarchyApplicationService = hierarchyAppService,
                hierarchyService = hierarchyService,
                transactionManager = transactionManager,
                hierarchyPolicyProvider = hierarchyPolicyProvider,
                eventPublisher = eventPublisher,
                aliasGenerationService = aliasGenerationService,
                applicationErrorMapper = appErrorMapper,
                logger = logger
            )
            
            val updateHandler = UpdateScopeHandler(
                eventSourcingRepository = eventSourcingRepo,
                scopeRepository = scopeRepo,
                transactionManager = transactionManager,
                eventPublisher = eventPublisher,
                logger = logger,
                applicationErrorMapper = appErrorMapper
            )
            
            val deleteHandler = DeleteScopeHandler(
                eventSourcingRepository = eventSourcingRepo,
                scopeRepository = scopeRepo,
                scopeHierarchyService = hierarchyService,
                transactionManager = transactionManager,
                eventPublisher = eventPublisher,
                logger = logger,
                applicationErrorMapper = appErrorMapper
            )
            
            return TestEnvironment(
                createHandler = createHandler,
                updateHandler = updateHandler,
                deleteHandler = deleteHandler,
                scopeRepository = scopeRepo,
                aliasRepository = aliasRepo,
                eventSourcingRepository = eventSourcingRepo,
                outboxRepository = outboxRepo,
                eventStoreAdapter = eventStoreAdapter
            )
        }
        
        context("Create Scope Flow") {
            it("should complete the full event sourcing flow for scope creation") {
                val env = createTestEnvironment()
                
                // 1. Execute Create command
                val createCommand = CreateScopeCommand.WithAutoAlias(
                    title = "Test Scope",
                    description = "A test scope for E2E validation",
                    parentId = null
                )
                
                val createResult = env.createHandler(createCommand)
                createResult.shouldBeRight()
                
                val scopeId = createResult.getOrElse { error("Create failed") }.id
                
                // 2. Verify events were saved to Event Store
                val aggregateId = ScopeId.create(scopeId)
                    .flatMap { it.toAggregateId() }
                    .getOrElse { error("Invalid scope ID") }
                    
                val events = env.eventSourcingRepository.getEvents(aggregateId)
                events.shouldBeRight()
                events.getOrNull()?.shouldHaveSize(2) // ScopeCreated + AliasAssigned
                
                // 3. Verify Outbox was processed (processImmediately = true)
                val pendingOutbox = env.outboxRepository.fetchPending(10)
                pendingOutbox.shouldBeEmpty()
                
                // 4. Verify projection was created in RDB
                val retrievedScope = env.scopeRepository.findById(ScopeId.create(scopeId).getOrElse { error("Invalid ID") })
                retrievedScope.shouldBeRight()
                retrievedScope.getOrNull().shouldNotBeNull()
                retrievedScope.getOrNull()?.title?.value shouldBe "Test Scope"
                
                // 5. Verify alias was created
                val aliases = env.aliasRepository.findByScopeId(ScopeId.create(scopeId).getOrElse { error("Invalid ID") })
                aliases.shouldBeRight()
                aliases.getOrNull()?.shouldHaveSize(1) // Should have generated canonical alias
            }
        }
        
        context("Update Scope Flow") {
            it("should complete the full event sourcing flow for scope update") {
                val env = createTestEnvironment()
                
                // First create a scope
                val createCommand = CreateScopeCommand.WithAutoAlias(
                    title = "Original Title",
                    description = "Original description",
                    parentId = null
                )
                
                val createResult = env.createHandler(createCommand)
                createResult.shouldBeRight()
                val scopeId = createResult.getOrElse { error("Create failed") }.id
                
                // Wait a bit to ensure projection completes
                delay(100)
                
                // 2. Execute Update command
                val updateCommand = UpdateScopeCommand(
                    id = scopeId,
                    title = "Updated Title",
                    description = "Updated description"
                )
                
                val updateResult = env.updateHandler(updateCommand)
                updateResult.shouldBeRight()
                
                // 3. Verify events in Event Store (should have 4 events now)
                val aggregateId = ScopeId.create(scopeId)
                    .flatMap { it.toAggregateId() }
                    .getOrElse { error("Invalid scope ID") }
                    
                val events = env.eventSourcingRepository.getEvents(aggregateId)
                events.shouldBeRight()
                events.getOrNull()?.shouldHaveSize(4) // ScopeCreated + AliasAssigned + ScopeTitleUpdated + ScopeDescriptionUpdated
                
                // 4. Verify projection was updated in RDB
                val retrievedScope = env.scopeRepository.findById(ScopeId.create(scopeId).getOrElse { error("Invalid ID") })
                retrievedScope.shouldBeRight()
                retrievedScope.getOrNull().shouldNotBeNull()
                retrievedScope.getOrNull()?.title?.value shouldBe "Updated Title"
                retrievedScope.getOrNull()?.description?.value shouldBe "Updated description"
                
                // 5. Verify outbox was processed
                val pendingOutbox = env.outboxRepository.fetchPending(10)
                pendingOutbox.shouldBeEmpty()
            }
        }
        
        context("Delete Scope Flow") {
            it("should complete the full event sourcing flow for scope deletion") {
                val env = createTestEnvironment()
                
                // First create a scope
                val createCommand = CreateScopeCommand.WithAutoAlias(
                    title = "Scope to Delete",
                    description = "This scope will be deleted",
                    parentId = null
                )
                
                val createResult = env.createHandler(createCommand)
                createResult.shouldBeRight()
                val scopeId = createResult.getOrElse { error("Create failed") }.id
                
                // Wait a bit to ensure projection completes
                delay(100)
                
                // 2. Execute Delete command
                val deleteCommand = DeleteScopeCommand(
                    id = scopeId
                )
                
                val deleteResult = env.deleteHandler(deleteCommand)
                deleteResult.shouldBeRight()
                
                // 3. Verify events in Event Store (should have 3 events: Create + Alias + Delete)
                val aggregateId = ScopeId.create(scopeId)
                    .flatMap { it.toAggregateId() }
                    .getOrElse { error("Invalid scope ID") }
                    
                val events = env.eventSourcingRepository.getEvents(aggregateId)
                events.shouldBeRight()
                events.getOrNull()?.shouldHaveSize(3) // ScopeCreated + AliasAssigned + ScopeDeleted
                
                // 4. Verify projection was removed from RDB
                val retrievedScope = env.scopeRepository.findById(ScopeId.create(scopeId).getOrElse { error("Invalid ID") })
                retrievedScope.shouldBeRight()
                retrievedScope.getOrNull() shouldBe null // Should be deleted
                
                // 5. Verify aliases were removed
                val aliases = env.aliasRepository.findByScopeId(ScopeId.create(scopeId).getOrElse { error("Invalid ID") })
                aliases.shouldBeRight()
                aliases.getOrNull()?.shouldBeEmpty() // Aliases should be removed
                
                // 6. Verify outbox was processed
                val pendingOutbox = env.outboxRepository.fetchPending(10)
                pendingOutbox.shouldBeEmpty()
            }
        }
        
        context("Complex Scenario") {
            it("should handle multiple operations in sequence") {
                val env = createTestEnvironment()
                
                // 1. Create parent scope
                val parentCommand = CreateScopeCommand.WithCustomAlias(
                    title = "Parent Project",
                    description = "Main project scope",
                    parentId = null,
                    alias = "parent-project"
                )
                
                val parentResult = env.createHandler(parentCommand)
                parentResult.shouldBeRight()
                val parentId = parentResult.getOrElse { error("Parent create failed") }.id
                
                // 2. Create child scope
                val childCommand = CreateScopeCommand.WithAutoAlias(
                    title = "Child Task",
                    description = "Sub-task under parent",
                    parentId = parentId
                )
                
                val childResult = env.createHandler(childCommand)
                childResult.shouldBeRight()
                val childId = childResult.getOrElse { error("Child create failed") }.id
                
                // 3. Update parent
                val updateParentCommand = UpdateScopeCommand(
                    id = parentId,
                    title = "Updated Parent Project",
                    description = null // Keep existing description
                )
                
                env.updateHandler(updateParentCommand).shouldBeRight()
                
                // 4. Verify final state
                val parentScopeId = ScopeId.create(parentId).getOrElse { error("Invalid parent ID") }
                val parentScope = env.scopeRepository.findById(parentScopeId)
                parentScope.shouldBeRight()
                parentScope.getOrNull()?.title?.value shouldBe "Updated Parent Project"
                
                val childScopeId = ScopeId.create(childId).getOrElse { error("Invalid child ID") }
                val childScope = env.scopeRepository.findById(childScopeId)
                childScope.shouldBeRight()
                childScope.getOrNull()?.parentId shouldBe parentScopeId
                
                // 5. Verify all outbox events were processed
                env.outboxRepository.fetchPending(100).shouldBeEmpty()
                
                // 6. Verify event counts
                val parentAggregateId = parentScopeId.toAggregateId().getOrElse { error("Invalid aggregate ID") }
                val parentEvents = env.eventSourcingRepository.getEvents(parentAggregateId)
                parentEvents.shouldBeRight()
                parentEvents.getOrNull()?.shouldHaveSize(3) // ScopeCreated + AliasAssigned + ScopeTitleUpdated
                
                val childAggregateId = childScopeId.toAggregateId().getOrElse { error("Invalid aggregate ID") }
                val childEvents = env.eventSourcingRepository.getEvents(childAggregateId)
                childEvents.shouldBeRight()
                childEvents.getOrNull()?.shouldHaveSize(2) // ScopeCreated + AliasAssigned
            }
        }
    }
})

/**
 * Test environment container with all necessary components
 */
data class TestEnvironment(
    val createHandler: CreateScopeHandler,
    val updateHandler: UpdateScopeHandler,
    val deleteHandler: DeleteScopeHandler,
    val scopeRepository: ScopeRepository,
    val aliasRepository: SqlDelightScopeAliasRepository,
    val eventSourcingRepository: EventSourcingRepository<*>,
    val outboxRepository: SqlDelightEventOutboxRepository,
    val eventStoreAdapter: InMemoryEventStoreAdapter
)