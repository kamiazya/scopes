package io.github.kamiazya.scopes.scopemanagement.eventstore.sqlite

import co.touchlab.kermit.Logger
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject.DeviceId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

class SqliteEventStoreTest :
    DescribeSpec({

        // Test database setup
        fun createTestDatabase(): Database {
            // Use in-memory database for tests
            return Database.connect(
                url = "jdbc:sqlite::memory:",
                driver = "org.sqlite.JDBC",
            )
        }

        fun createEventStore(database: Database): SqliteEventStore {
            val json = Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

            return SqliteEventStore(
                database = database,
                json = json,
                logger = Logger.withTag("TestEventStore"),
            )
        }

        // Simple test domain event
        data class TestEvent(
            override val eventId: EventId,
            override val aggregateId: AggregateId,
            override val aggregateVersion: AggregateVersion,
            override val occurredAt: kotlinx.datetime.Instant,
            val data: String,
        ) : DomainEvent

        describe("SqliteEventStore basic operations") {
            it("should create event store without errors") {
                val database = createTestDatabase()
                val eventStore = createEventStore(database)

                // Basic smoke test - just verify the event store can be created
                // Full database tests will be added later
            }

            it("should generate device IDs") {
                val id1 = DeviceId.generate()
                val id2 = DeviceId.generate()

                (id1.value != id2.value) shouldBe true
            }
        }
    })
