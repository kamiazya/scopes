package io.github.kamiazya.scopes.interfaces.cli.helpers

import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Test data builders for interface layer integration tests.
 * Provides convenient methods to create test data without mock dependencies.
 */
object TestDataBuilders {

    /**
     * Creates a test CreateScopeCommand with sensible defaults
     */
    fun createScopeCommand(
        title: String = "Test Scope",
        description: String? = "Test Description",
        parentId: String? = null,
        generateAlias: Boolean = true,
        customAlias: String? = null,
    ): CreateScopeCommand = CreateScopeCommand(
        title = title,
        description = description,
        parentId = parentId,
        generateAlias = generateAlias,
        customAlias = customAlias,
    )

    /**
     * Creates a test UpdateScopeCommand
     */
    fun updateScopeCommand(id: String, title: String? = null, description: String? = null): UpdateScopeCommand = UpdateScopeCommand(
        id = id,
        title = title,
        description = description,
    )

    /**
     * Creates a test ScopeResult with sensible defaults
     */
    fun scopeResult(
        id: String = "test-scope-id",
        title: String = "Test Scope",
        description: String? = "Test Description",
        parentId: String? = null,
        canonicalAlias: String = "test-alias",
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now(),
        isArchived: Boolean = false,
        aspects: Map<String, List<String>> = emptyMap(),
    ): ScopeResult = ScopeResult(
        id = id,
        title = title,
        description = description,
        parentId = parentId,
        canonicalAlias = canonicalAlias,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        aspects = aspects,
    )

    /**
     * Creates a test CreateScopeResult
     */
    fun createScopeResult(
        id: String = "new-scope-id",
        title: String = "New Scope",
        description: String? = "New Description",
        parentId: String? = null,
        canonicalAlias: String = "new-alias",
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now(),
    ): CreateScopeResult = CreateScopeResult(
        id = id,
        title = title,
        description = description,
        parentId = parentId,
        canonicalAlias = canonicalAlias,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /**
     * Creates a test UpdateScopeResult
     */
    fun updateScopeResult(
        id: String = "updated-scope-id",
        title: String = "Updated Scope",
        description: String? = "Updated Description",
        parentId: String? = null,
        canonicalAlias: String = "updated-alias",
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now(),
    ): UpdateScopeResult = UpdateScopeResult(
        id = id,
        title = title,
        description = description,
        parentId = parentId,
        canonicalAlias = canonicalAlias,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /**
     * Creates a test AliasInfo
     */
    fun aliasInfo(
        aliasName: String = "test-alias",
        aliasType: String = "canonical",
        isCanonical: Boolean = true,
        createdAt: Instant = Clock.System.now(),
    ): AliasInfo = AliasInfo(
        aliasName = aliasName,
        aliasType = aliasType,
        isCanonical = isCanonical,
        createdAt = createdAt,
    )

    /**
     * Creates test aspects map for common scenarios
     */
    object Aspects {
        val empty: Map<String, List<String>> = emptyMap()

        val withPriority: Map<String, List<String>> = mapOf(
            "priority" to listOf("high"),
        )

        val withStatus: Map<String, List<String>> = mapOf(
            "status" to listOf("active"),
        )

        val withMultiple: Map<String, List<String>> = mapOf(
            "priority" to listOf("high"),
            "status" to listOf("active"),
            "type" to listOf("feature", "important"),
        )
    }

    /**
     * Common test scope IDs for consistency
     */
    object ScopeIds {
        const val ROOT = "root-scope-id"
        const val PARENT = "parent-scope-id"
        const val CHILD1 = "child-scope-id-1"
        const val CHILD2 = "child-scope-id-2"
        const val ORPHAN = "orphan-scope-id"
    }

    /**
     * Common test aliases for consistency
     */
    object Aliases {
        const val ROOT = "root-alias"
        const val PARENT = "parent-alias"
        const val CHILD1 = "child-alias-1"
        const val CHILD2 = "child-alias-2"
        const val CUSTOM = "custom-alias"
        const val GENERATED = "quiet-river-x7k"
    }

    /**
     * Common error scenarios
     */
    object ErrorScenarios {
        const val EMPTY_TITLE = ""
        val LONG_TITLE = "A".repeat(201) // Changed to val because repeat() is not a compile-time constant
        const val INVALID_ID = "invalid-id-format"
        const val NON_EXISTENT_ID = "non-existent-id"
        const val DUPLICATE_ALIAS = "already-exists"
    }
}
