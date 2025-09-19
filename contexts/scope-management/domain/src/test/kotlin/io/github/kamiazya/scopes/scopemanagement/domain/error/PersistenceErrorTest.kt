package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for PersistenceError and its nested error types.
 */
class PersistenceErrorTest :
    StringSpec({

        "should create ConcurrencyConflict error with all parameters" {
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "Scope",
                entityId = "scope-123",
                expectedVersion = "5",
                actualVersion = "7",
            )

            error.shouldBeInstanceOf<PersistenceError>()
            error.shouldBeInstanceOf<ScopesError>()
            error.entityType shouldBe "Scope"
            error.entityId shouldBe "scope-123"
            error.expectedVersion shouldBe "5"
            error.actualVersion shouldBe "7"
        }

        "should support version numbers as strings" {
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "ScopeAggregate",
                entityId = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                expectedVersion = "42",
                actualVersion = "43",
            )

            error.expectedVersion shouldBe "42"
            error.actualVersion shouldBe "43"
        }

        "should support version hashes as strings" {
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "EventStream",
                entityId = "event-stream-456",
                expectedVersion = "a1b2c3d4e5f6",
                actualVersion = "f6e5d4c3b2a1",
            )

            error.expectedVersion shouldBe "a1b2c3d4e5f6"
            error.actualVersion shouldBe "f6e5d4c3b2a1"
        }

        "should handle optimistic locking scenarios" {
            // Scenario 1: User A and User B both read version 1
            val userAReadsVersion = "1"
            val userBReadsVersion = "1"

            // User B saves first, incrementing to version 2
            val userBSavesVersion = "2"

            // User A tries to save but fails due to version mismatch
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "Scope",
                entityId = "shared-scope",
                expectedVersion = userAReadsVersion,
                actualVersion = userBSavesVersion,
            )

            error.expectedVersion shouldBe "1"
            error.actualVersion shouldBe "2"
            error.entityType shouldBe "Scope"
            error.entityId shouldBe "shared-scope"
        }

        "should maintain proper inheritance hierarchy" {
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "TestEntity",
                entityId = "test-id",
                expectedVersion = "old",
                actualVersion = "new",
            )

            error.shouldBeInstanceOf<PersistenceError>()
            error.shouldBeInstanceOf<ScopesError>()
        }

        "should properly implement equals and hashCode" {
            val error1 = PersistenceError.ConcurrencyConflict(
                entityType = "Scope",
                entityId = "scope-123",
                expectedVersion = "1",
                actualVersion = "2",
            )

            val error2 = PersistenceError.ConcurrencyConflict(
                entityType = "Scope",
                entityId = "scope-123",
                expectedVersion = "1",
                actualVersion = "2",
            )

            val error3 = PersistenceError.ConcurrencyConflict(
                entityType = "Scope",
                entityId = "scope-456",
                expectedVersion = "1",
                actualVersion = "2",
            )

            error1 shouldBe error2
            error1.hashCode() shouldBe error2.hashCode()
            error1 shouldNotBe error3
            error1.hashCode() shouldNotBe error3.hashCode()
        }

        "should handle event sourcing version conflicts" {
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "ScopeAggregate",
                entityId = "aggregate-789",
                expectedVersion = "event-sequence-100",
                actualVersion = "event-sequence-105",
            )

            error.entityType shouldBe "ScopeAggregate"
            error.entityId shouldBe "aggregate-789"
            // In event sourcing, versions might be event sequence numbers
            error.expectedVersion shouldBe "event-sequence-100"
            error.actualVersion shouldBe "event-sequence-105"
        }

        "should handle distributed system version vectors" {
            // Version vectors in distributed systems might be complex strings
            val error = PersistenceError.ConcurrencyConflict(
                entityType = "DistributedScope",
                entityId = "dist-scope-001",
                expectedVersion = "node1:5,node2:3,node3:7",
                actualVersion = "node1:6,node2:3,node3:8",
            )

            error.expectedVersion shouldBe "node1:5,node2:3,node3:7"
            error.actualVersion shouldBe "node1:6,node2:3,node3:8"
        }
    })
