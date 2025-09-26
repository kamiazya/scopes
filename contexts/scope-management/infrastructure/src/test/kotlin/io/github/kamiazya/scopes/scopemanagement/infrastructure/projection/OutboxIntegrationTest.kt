package io.github.kamiazya.scopes.scopemanagement.infrastructure.projection
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.metrics.DefaultProjectionMetrics
import io.github.kamiazya.scopes.platform.observability.metrics.InMemoryMetricsRegistry
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightEventOutboxRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializersModule
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

// Domain imports

class OutboxIntegrationTest :
    DescribeSpec({
        describe("Outbox publisher immediate processing") {
            it("enqueues and processes a ScopeCreated event -> PROCESSED and projection present") {
                // Setup in-memory DB and repositories
                val db = SqlDelightDatabaseProvider.createDatabase(":memory:")
                val scopeRepo = SqlDelightScopeRepository(db)
                val aliasRepo = SqlDelightScopeAliasRepository(db)

                val logger = ConsoleLogger("OutboxTest")
                val metrics = DefaultProjectionMetrics(InMemoryMetricsRegistry())
                val projectionService = EventProjectionService(
                    scopeRepository = scopeRepo,
                    scopeAliasRepository = aliasRepo,
                    logger = logger,
                    projectionMetrics = metrics,
                )

                val outboxRepo = SqlDelightEventOutboxRepository(db)

                val json = Json {
                    serializersModule = ScopeEventSerializersModule.create()
                    ignoreUnknownKeys = true
                    isLenient = true
                    classDiscriminator = "type"
                }

                val projector = OutboxProjectionService(
                    outboxRepository = outboxRepo,
                    projectionService = projectionService,
                    json = json,
                    logger = logger,
                )

                val publisher = OutboxEventProjectionService(
                    outboxRepository = outboxRepo,
                    projector = projector,
                    json = json,
                    logger = logger,
                    processImmediately = true,
                )

                // Build a ScopeCreated event
                val scopeId = ScopeId.generate()
                val aggregateId = scopeId.toAggregateId().fold(
                    { e -> error("aggregateId conversion failed: $e") },
                    { it },
                )

                val now = Clock.System.now()
                val event = ScopeCreated(
                    aggregateId = aggregateId,
                    eventId = EventId.generate(),
                    occurredAt = now,
                    aggregateVersion = AggregateVersion.initial().increment(),
                    scopeId = scopeId,
                    title = ScopeTitle.create("Test Scope").getOrNull()!!,
                    description = ScopeDescription.create("desc").getOrNull(),
                    parentId = null,
                )

                // Enqueue and process immediately
                publisher.projectEvent(event).shouldBeRight()

                // Outbox should have no pending records (processed immediately)
                outboxRepo.fetchPending(10).shouldBeEmpty()

                // Projection: scope should exist
                val loadedEither = scopeRepo.findById(scopeId)
                loadedEither.shouldBeRight()
                loadedEither.getOrNull() shouldNotBe null
            }
        }
    })
