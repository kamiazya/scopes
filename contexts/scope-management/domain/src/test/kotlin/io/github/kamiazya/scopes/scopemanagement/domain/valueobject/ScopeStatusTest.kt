package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for ScopeStatus value object.
 *
 * Business rules:
 * - Four states: Draft, Active, Completed, Archived
 * - Specific transition rules between states
 * - Different capabilities per state (editing, adding children)
 * - String parsing and serialization
 */
class ScopeStatusTest :
    StringSpec({

        "should have correct state transitions from Draft" {
            val draft = ScopeStatus.Draft

            // Valid transitions
            draft.canTransitionTo(ScopeStatus.Active) shouldBe true
            draft.canTransitionTo(ScopeStatus.Archived) shouldBe true

            // Invalid transitions
            draft.canTransitionTo(ScopeStatus.Completed) shouldBe false
            draft.canTransitionTo(ScopeStatus.Draft) shouldBe false
        }

        "should have correct state transitions from Active" {
            val active = ScopeStatus.Active

            // Valid transitions
            active.canTransitionTo(ScopeStatus.Completed) shouldBe true
            active.canTransitionTo(ScopeStatus.Archived) shouldBe true

            // Invalid transitions
            active.canTransitionTo(ScopeStatus.Draft) shouldBe false
            active.canTransitionTo(ScopeStatus.Active) shouldBe false
        }

        "should have correct state transitions from Completed" {
            val completed = ScopeStatus.Completed

            // Valid transitions
            completed.canTransitionTo(ScopeStatus.Active) shouldBe true
            completed.canTransitionTo(ScopeStatus.Archived) shouldBe true

            // Invalid transitions
            completed.canTransitionTo(ScopeStatus.Draft) shouldBe false
            completed.canTransitionTo(ScopeStatus.Completed) shouldBe false
        }

        "should have correct state transitions from Archived" {
            val archived = ScopeStatus.Archived

            // Valid transitions
            archived.canTransitionTo(ScopeStatus.Active) shouldBe true

            // Invalid transitions
            archived.canTransitionTo(ScopeStatus.Draft) shouldBe false
            archived.canTransitionTo(ScopeStatus.Completed) shouldBe false
            archived.canTransitionTo(ScopeStatus.Archived) shouldBe false
        }

        "should transition successfully for valid transitions" {
            // Draft -> Active
            val draft = ScopeStatus.Draft
            val result1 = draft.transitionTo(ScopeStatus.Active)
            result1.shouldBeRight(ScopeStatus.Active)

            // Active -> Completed
            val active = ScopeStatus.Active
            val result2 = active.transitionTo(ScopeStatus.Completed)
            result2.shouldBeRight(ScopeStatus.Completed)

            // Completed -> Archived
            val completed = ScopeStatus.Completed
            val result3 = completed.transitionTo(ScopeStatus.Archived)
            result3.shouldBeRight(ScopeStatus.Archived)

            // Archived -> Active
            val archived = ScopeStatus.Archived
            val result4 = archived.transitionTo(ScopeStatus.Active)
            result4.shouldBeRight(ScopeStatus.Active)
        }

        "should return error for invalid transitions" {
            // Draft -> Completed (invalid)
            val draft = ScopeStatus.Draft
            val result1 = draft.transitionTo(ScopeStatus.Completed)
            val error1 = result1.shouldBeLeft()
            error1.shouldBeInstanceOf<ScopesError.ScopeStatusTransitionError>()
            error1.from shouldBe "DRAFT"
            error1.to shouldBe "COMPLETED"
            error1.reason shouldBe "Invalid state transition from DRAFT to COMPLETED"

            // Active -> Draft (invalid)
            val active = ScopeStatus.Active
            val result2 = active.transitionTo(ScopeStatus.Draft)
            val error2 = result2.shouldBeLeft()
            error2.shouldBeInstanceOf<ScopesError.ScopeStatusTransitionError>()
            error2.from shouldBe "ACTIVE"
            error2.to shouldBe "DRAFT"
            error2.reason shouldBe "Invalid state transition from ACTIVE to DRAFT"
        }

        "should identify terminal states correctly" {
            ScopeStatus.Draft.isTerminal() shouldBe false
            ScopeStatus.Active.isTerminal() shouldBe false
            ScopeStatus.Completed.isTerminal() shouldBe false
            ScopeStatus.Archived.isTerminal() shouldBe true
        }

        "should identify states that can add children" {
            ScopeStatus.Draft.canAddChildren() shouldBe true
            ScopeStatus.Active.canAddChildren() shouldBe true
            ScopeStatus.Completed.canAddChildren() shouldBe false
            ScopeStatus.Archived.canAddChildren() shouldBe false
        }

        "should identify states that can be edited" {
            ScopeStatus.Draft.canBeEdited() shouldBe true
            ScopeStatus.Active.canBeEdited() shouldBe true
            ScopeStatus.Completed.canBeEdited() shouldBe false
            ScopeStatus.Archived.canBeEdited() shouldBe false
        }

        "should identify working states" {
            ScopeStatus.Draft.isWorking() shouldBe true
            ScopeStatus.Active.isWorking() shouldBe true
            ScopeStatus.Completed.isWorking() shouldBe false
            ScopeStatus.Archived.isWorking() shouldBe false
        }

        "should identify finished states" {
            ScopeStatus.Draft.isFinished() shouldBe false
            ScopeStatus.Active.isFinished() shouldBe false
            ScopeStatus.Completed.isFinished() shouldBe true
            ScopeStatus.Archived.isFinished() shouldBe true
        }

        "should identify states that can be completed" {
            ScopeStatus.Draft.canBeCompleted() shouldBe false
            ScopeStatus.Active.canBeCompleted() shouldBe true
            ScopeStatus.Completed.canBeCompleted() shouldBe false
            ScopeStatus.Archived.canBeCompleted() shouldBe false
        }

        "should identify states that can be activated" {
            ScopeStatus.Draft.canBeActivated() shouldBe true
            ScopeStatus.Active.canBeActivated() shouldBe false
            ScopeStatus.Completed.canBeActivated() shouldBe true
            ScopeStatus.Archived.canBeActivated() shouldBe true
        }

        "should return valid transitions for each state" {
            ScopeStatus.Draft.validTransitions() shouldContainExactly listOf(
                ScopeStatus.Active,
                ScopeStatus.Archived,
            )

            ScopeStatus.Active.validTransitions() shouldContainExactly listOf(
                ScopeStatus.Completed,
                ScopeStatus.Archived,
            )

            ScopeStatus.Completed.validTransitions() shouldContainExactly listOf(
                ScopeStatus.Active,
                ScopeStatus.Archived,
            )

            ScopeStatus.Archived.validTransitions() shouldContainExactly listOf(
                ScopeStatus.Active,
            )
        }

        "should parse valid status strings" {
            // Uppercase
            ScopeStatus.fromString("DRAFT").shouldBeRight(ScopeStatus.Draft)
            ScopeStatus.fromString("ACTIVE").shouldBeRight(ScopeStatus.Active)
            ScopeStatus.fromString("COMPLETED").shouldBeRight(ScopeStatus.Completed)
            ScopeStatus.fromString("ARCHIVED").shouldBeRight(ScopeStatus.Archived)

            // Lowercase
            ScopeStatus.fromString("draft").shouldBeRight(ScopeStatus.Draft)
            ScopeStatus.fromString("active").shouldBeRight(ScopeStatus.Active)
            ScopeStatus.fromString("completed").shouldBeRight(ScopeStatus.Completed)
            ScopeStatus.fromString("archived").shouldBeRight(ScopeStatus.Archived)

            // Mixed case
            ScopeStatus.fromString("Draft").shouldBeRight(ScopeStatus.Draft)
            ScopeStatus.fromString("Active").shouldBeRight(ScopeStatus.Active)
            ScopeStatus.fromString("Completed").shouldBeRight(ScopeStatus.Completed)
            ScopeStatus.fromString("Archived").shouldBeRight(ScopeStatus.Archived)
        }

        "should reject invalid status strings" {
            val invalidStrings = listOf(
                "INVALID",
                "PENDING",
                "DONE",
                "TODO",
                "IN_PROGRESS",
                "",
                " ",
                "123",
                "draft draft",
            )

            invalidStrings.forEach { invalid ->
                val result = ScopeStatus.fromString(invalid)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error.shouldBeInstanceOf<IllegalArgumentException>()
                error.message shouldBe "Invalid scope status: $invalid"
            }
        }

        "should have default status as Draft" {
            ScopeStatus.default() shouldBe ScopeStatus.Draft
        }

        "should serialize to uppercase string" {
            ScopeStatus.Draft.toString() shouldBe "DRAFT"
            ScopeStatus.Active.toString() shouldBe "ACTIVE"
            ScopeStatus.Completed.toString() shouldBe "COMPLETED"
            ScopeStatus.Archived.toString() shouldBe "ARCHIVED"
        }

        "should maintain singleton behavior for status objects" {
            // Objects should be the same instance
            val draft1 = ScopeStatus.Draft
            val draft2 = ScopeStatus.Draft
            (draft1 === draft2) shouldBe true

            val active1 = ScopeStatus.Active
            val active2 = ScopeStatus.Active
            (active1 === active2) shouldBe true
        }

        "should handle complete state machine flow" {
            // Draft -> Active -> Completed -> Archived
            var status: ScopeStatus = ScopeStatus.Draft

            // Start as Draft
            status.isWorking() shouldBe true
            status.canAddChildren() shouldBe true
            status.canBeEdited() shouldBe true

            // Move to Active
            status = status.transitionTo(ScopeStatus.Active).shouldBeRight()
            status shouldBe ScopeStatus.Active
            status.canBeCompleted() shouldBe true

            // Complete the scope
            status = status.transitionTo(ScopeStatus.Completed).shouldBeRight()
            status shouldBe ScopeStatus.Completed
            status.isFinished() shouldBe true
            status.canAddChildren() shouldBe false
            status.canBeEdited() shouldBe false

            // Archive it
            status = status.transitionTo(ScopeStatus.Archived).shouldBeRight()
            status shouldBe ScopeStatus.Archived
            status.isTerminal() shouldBe true

            // Can reactivate from archived
            status = status.transitionTo(ScopeStatus.Active).shouldBeRight()
            status shouldBe ScopeStatus.Active
            status.isWorking() shouldBe true
        }

        "should handle alternative workflow paths" {
            // Path 1: Draft -> Archived (cancelled draft)
            var status1: ScopeStatus = ScopeStatus.Draft
            status1 = status1.transitionTo(ScopeStatus.Archived).shouldBeRight()
            status1 shouldBe ScopeStatus.Archived

            // Path 2: Active -> Archived (abandoned work)
            var status2: ScopeStatus = ScopeStatus.Active
            status2 = status2.transitionTo(ScopeStatus.Archived).shouldBeRight()
            status2 shouldBe ScopeStatus.Archived

            // Path 3: Completed -> Active (reopened)
            var status3: ScopeStatus = ScopeStatus.Completed
            status3 = status3.transitionTo(ScopeStatus.Active).shouldBeRight()
            status3 shouldBe ScopeStatus.Active
        }

        "should verify state capabilities matrix" {
            data class StateCapabilities(
                val canEdit: Boolean,
                val canAddChildren: Boolean,
                val isWorking: Boolean,
                val isFinished: Boolean,
                val isTerminal: Boolean,
            )

            val expectations = mapOf(
                ScopeStatus.Draft to StateCapabilities(
                    canEdit = true,
                    canAddChildren = true,
                    isWorking = true,
                    isFinished = false,
                    isTerminal = false,
                ),
                ScopeStatus.Active to StateCapabilities(
                    canEdit = true,
                    canAddChildren = true,
                    isWorking = true,
                    isFinished = false,
                    isTerminal = false,
                ),
                ScopeStatus.Completed to StateCapabilities(
                    canEdit = false,
                    canAddChildren = false,
                    isWorking = false,
                    isFinished = true,
                    isTerminal = false,
                ),
                ScopeStatus.Archived to StateCapabilities(
                    canEdit = false,
                    canAddChildren = false,
                    isWorking = false,
                    isFinished = true,
                    isTerminal = true,
                ),
            )

            expectations.forEach { (status, expected) ->
                status.canBeEdited() shouldBe expected.canEdit
                status.canAddChildren() shouldBe expected.canAddChildren
                status.isWorking() shouldBe expected.isWorking
                status.isFinished() shouldBe expected.isFinished
                status.isTerminal() shouldBe expected.isTerminal
            }
        }
    })
