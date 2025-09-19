package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

/**
 * Tests for AliasOperation sealed class and ConflictResolution data class.
 *
 * Business rules:
 * - AliasOperation represents different types of alias operations with their context
 * - ConflictResolution handles scenarios where aliases already exist
 * - All operation types should be immutable and properly typed
 */
class AliasOperationResultTest :
    StringSpec({

        "should create Create operation with alias" {
            val alias = ScopeAlias.createCanonical(
                scopeId = ScopeId.generate(),
                aliasName = AliasName.create("test-alias").getOrNull()!!,
                timestamp = Clock.System.now(),
            )
            val operation = AliasOperation.Create(alias)

            operation.alias shouldBe alias
            operation.shouldBeInstanceOf<AliasOperation.Create>()
        }

        "should create Replace operation with old and new aliases" {
            val scopeId = ScopeId.generate()
            val timestamp = Clock.System.now()
            val oldAlias = ScopeAlias.createCanonical(
                scopeId = scopeId,
                aliasName = AliasName.create("old-alias").getOrNull()!!,
                timestamp = timestamp,
            )
            val newAlias = ScopeAlias.createCanonical(
                scopeId = scopeId,
                aliasName = AliasName.create("new-alias").getOrNull()!!,
                timestamp = timestamp,
            )
            val operation = AliasOperation.Replace(oldAlias, newAlias)

            operation.oldAlias shouldBe oldAlias
            operation.newAlias shouldBe newAlias
            operation.shouldBeInstanceOf<AliasOperation.Replace>()
        }

        "should create Promote operation with existing alias" {
            val existingAlias = ScopeAlias.createCustom(
                scopeId = ScopeId.generate(),
                aliasName = AliasName.create("existing-alias").getOrNull()!!,
                timestamp = Clock.System.now(),
            )
            val operation = AliasOperation.Promote(existingAlias)

            operation.existingAlias shouldBe existingAlias
            operation.shouldBeInstanceOf<AliasOperation.Promote>()
        }

        "should create NoChange operation with reason" {
            val reason = "Alias already exists and is canonical"
            val operation = AliasOperation.NoChange(reason)

            operation.reason shouldBe reason
            operation.shouldBeInstanceOf<AliasOperation.NoChange>()
        }

        "should create Failure operation with error" {
            val error = ScopesError.ValidationFailed(
                field = "alias",
                value = "invalid-alias",
                constraint = ScopesError.ValidationConstraintType.InvalidValue("Invalid characters"),
            )
            val operation = AliasOperation.Failure(error)

            operation.error shouldBe error
            operation.shouldBeInstanceOf<AliasOperation.Failure>()
        }

        "should maintain equality for Create operations" {
            val alias = ScopeAlias.createCanonical(
                scopeId = ScopeId.generate(),
                aliasName = AliasName.create("test-alias").getOrNull()!!,
                timestamp = Clock.System.now(),
            )
            val operation1 = AliasOperation.Create(alias)
            val operation2 = AliasOperation.Create(alias)

            (operation1 == operation2) shouldBe true
            operation1.hashCode() shouldBe operation2.hashCode()
        }

        "should maintain equality for Replace operations" {
            val scopeId = ScopeId.generate()
            val timestamp = Clock.System.now()
            val oldAlias = ScopeAlias.createCanonical(
                scopeId = scopeId,
                aliasName = AliasName.create("old").getOrNull()!!,
                timestamp = timestamp,
            )
            val newAlias = ScopeAlias.createCanonical(
                scopeId = scopeId,
                aliasName = AliasName.create("new").getOrNull()!!,
                timestamp = timestamp,
            )
            val operation1 = AliasOperation.Replace(oldAlias, newAlias)
            val operation2 = AliasOperation.Replace(oldAlias, newAlias)

            (operation1 == operation2) shouldBe true
            operation1.hashCode() shouldBe operation2.hashCode()
        }

        "should maintain equality for NoChange operations" {
            val reason = "Same reason"
            val operation1 = AliasOperation.NoChange(reason)
            val operation2 = AliasOperation.NoChange(reason)
            val operation3 = AliasOperation.NoChange("Different reason")

            (operation1 == operation2) shouldBe true
            (operation1 == operation3) shouldBe false
            operation1.hashCode() shouldBe operation2.hashCode()
        }

        "should differentiate between different operation types" {
            val alias = ScopeAlias.createCanonical(
                scopeId = ScopeId.generate(),
                aliasName = AliasName.create("test").getOrNull()!!,
                timestamp = Clock.System.now(),
            )
            val create = AliasOperation.Create(alias)
            val promote = AliasOperation.Promote(alias)
            val noChange = AliasOperation.NoChange("test")

            (create == promote) shouldBe false
            (create == noChange) shouldBe false
            (promote == noChange) shouldBe false
        }

        "should support polymorphic usage through sealed class" {
            val alias = ScopeAlias.createCanonical(
                scopeId = ScopeId.generate(),
                aliasName = AliasName.create("test").getOrNull()!!,
                timestamp = Clock.System.now(),
            )
            val operations: List<AliasOperation> = listOf(
                AliasOperation.Create(alias),
                AliasOperation.Promote(alias),
                AliasOperation.NoChange("test"),
                AliasOperation.Failure(ScopesError.NotFound("entity", "id", "type")),
            )

            operations.forEach { operation ->
                operation.shouldBeInstanceOf<AliasOperation>()
            }

            operations[0].shouldBeInstanceOf<AliasOperation.Create>()
            operations[1].shouldBeInstanceOf<AliasOperation.Promote>()
            operations[2].shouldBeInstanceOf<AliasOperation.NoChange>()
            operations[3].shouldBeInstanceOf<AliasOperation.Failure>()
        }

        "should create ConflictResolution with all lists" {
            val toCreate = listOf(
                AliasName.create("new-alias-1").getOrNull()!!,
                AliasName.create("new-alias-2").getOrNull()!!,
            )
            val alreadyExists = listOf(
                AliasName.create("existing-alias-1").getOrNull()!!,
                AliasName.create("existing-alias-2").getOrNull()!!,
            )
            val timestamp = Clock.System.now()
            val toKeep = listOf(
                ScopeAlias.createCustom(
                    scopeId = ScopeId.generate(),
                    aliasName = AliasName.create("keep-alias-1").getOrNull()!!,
                    timestamp = timestamp,
                ),
                ScopeAlias.createCustom(
                    scopeId = ScopeId.generate(),
                    aliasName = AliasName.create("keep-alias-2").getOrNull()!!,
                    timestamp = timestamp,
                ),
            )

            val resolution = ConflictResolution(toCreate, alreadyExists, toKeep)

            resolution.toCreate shouldBe toCreate
            resolution.alreadyExists shouldBe alreadyExists
            resolution.toKeep shouldBe toKeep
        }

        "should create ConflictResolution with empty lists" {
            val resolution = ConflictResolution(
                toCreate = emptyList(),
                alreadyExists = emptyList(),
                toKeep = emptyList(),
            )

            resolution.toCreate shouldBe emptyList()
            resolution.alreadyExists shouldBe emptyList()
            resolution.toKeep shouldBe emptyList()
        }

        "should maintain equality for ConflictResolution" {
            val toCreate = listOf(AliasName.create("new").getOrNull()!!)
            val alreadyExists = listOf(AliasName.create("existing").getOrNull()!!)
            val toKeep = listOf(
                ScopeAlias.createCustom(
                    scopeId = ScopeId.generate(),
                    aliasName = AliasName.create("keep").getOrNull()!!,
                    timestamp = Clock.System.now(),
                ),
            )

            val resolution1 = ConflictResolution(toCreate, alreadyExists, toKeep)
            val resolution2 = ConflictResolution(toCreate, alreadyExists, toKeep)
            val resolution3 = ConflictResolution(emptyList(), alreadyExists, toKeep)

            (resolution1 == resolution2) shouldBe true
            (resolution1 == resolution3) shouldBe false
            resolution1.hashCode() shouldBe resolution2.hashCode()
        }

        "should handle ConflictResolution with different list sizes" {
            val singleItem = ConflictResolution(
                toCreate = listOf(AliasName.create("single").getOrNull()!!),
                alreadyExists = emptyList(),
                toKeep = emptyList(),
            )

            val multipleItems = ConflictResolution(
                toCreate = listOf(
                    AliasName.create("first").getOrNull()!!,
                    AliasName.create("second").getOrNull()!!,
                    AliasName.create("third").getOrNull()!!,
                ),
                alreadyExists = listOf(
                    AliasName.create("existing-1").getOrNull()!!,
                    AliasName.create("existing-2").getOrNull()!!,
                ),
                toKeep = listOf(
                    ScopeAlias.createCustom(
                        scopeId = ScopeId.generate(),
                        aliasName = AliasName.create("keep-1").getOrNull()!!,
                        timestamp = Clock.System.now(),
                    ),
                ),
            )

            singleItem.toCreate.size shouldBe 1
            singleItem.alreadyExists.size shouldBe 0
            singleItem.toKeep.size shouldBe 0

            multipleItems.toCreate.size shouldBe 3
            multipleItems.alreadyExists.size shouldBe 2
            multipleItems.toKeep.size shouldBe 1
        }

        "should maintain immutability of ConflictResolution lists" {
            val originalToCreate = listOf(AliasName.create("original").getOrNull()!!)
            val resolution = ConflictResolution(
                toCreate = originalToCreate,
                alreadyExists = emptyList(),
                toKeep = emptyList(),
            )

            // Lists should be immutable copies
            val retrievedList = resolution.toCreate
            retrievedList shouldBe originalToCreate

            // Original list and retrieved list should be separate instances
            // (data classes with immutable lists provide this behavior)
            resolution.toCreate.size shouldBe 1
            resolution.toCreate[0] shouldBe AliasName.create("original").getOrNull()!!
        }

        "should handle complex AliasOperation scenarios" {
            // Test realistic operation scenarios
            val scopeId = ScopeId.generate()

            val timestamp = Clock.System.now()

            // Scenario 1: Creating a new canonical alias
            val createOp = AliasOperation.Create(
                ScopeAlias.createCanonical(
                    scopeId = scopeId,
                    aliasName = AliasName.create("new-canonical").getOrNull()!!,
                    timestamp = timestamp,
                ),
            )

            // Scenario 2: Replacing old canonical with new one
            val replaceOp = AliasOperation.Replace(
                oldAlias = ScopeAlias.createCanonical(
                    scopeId = scopeId,
                    aliasName = AliasName.create("old-canonical").getOrNull()!!,
                    timestamp = timestamp,
                ),
                newAlias = ScopeAlias.createCanonical(
                    scopeId = scopeId,
                    aliasName = AliasName.create("new-canonical").getOrNull()!!,
                    timestamp = timestamp,
                ),
            )

            // Scenario 3: Promoting existing custom alias to canonical
            val promoteOp = AliasOperation.Promote(
                ScopeAlias.createCustom(
                    scopeId = scopeId,
                    aliasName = AliasName.create("custom-to-promote").getOrNull()!!,
                    timestamp = timestamp,
                ),
            )

            createOp.shouldBeInstanceOf<AliasOperation.Create>()
            replaceOp.shouldBeInstanceOf<AliasOperation.Replace>()
            promoteOp.shouldBeInstanceOf<AliasOperation.Promote>()

            // Each operation should reference the same scope
            createOp.alias.scopeId shouldBe scopeId
            replaceOp.oldAlias.scopeId shouldBe scopeId
            replaceOp.newAlias.scopeId shouldBe scopeId
            promoteOp.existingAlias.scopeId shouldBe scopeId
        }

        "should handle edge case NoChange reasons" {
            val edgeCaseReasons = listOf(
                "",
                "   ", // whitespace
                "Very long reason message that explains why no change is needed in great detail with multiple clauses",
                "Reason with special characters: @#$%^&*()_+-=[]{}|;':\",./<>?",
                "Unicode reason: 测试 テスト тест",
            )

            edgeCaseReasons.forEach { reason ->
                val operation = AliasOperation.NoChange(reason)
                operation.reason shouldBe reason
                operation.shouldBeInstanceOf<AliasOperation.NoChange>()
            }
        }

        "should handle different ScopesError types in Failure operations" {
            val errorTypes = listOf(
                ScopesError.NotFound("entity", "id", "type"),
                ScopesError.ValidationFailed("field", "value", ScopesError.ValidationConstraintType.InvalidValue("message")),
                ScopesError.SystemError(
                    errorType = ScopesError.SystemError.SystemErrorType.CONFIGURATION_ERROR,
                    service = "alias-service",
                    context = mapOf("error" to "test"),
                ),
            )

            errorTypes.forEach { error ->
                val operation = AliasOperation.Failure(error)
                operation.error shouldBe error
                operation.shouldBeInstanceOf<AliasOperation.Failure>()
            }
        }
    })
