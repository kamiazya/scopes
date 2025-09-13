package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.JsonDiffError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.serialization.json.JsonElement

/**
 * Strategy for merging concurrent changes.
 */
interface MergeStrategy {
    /**
     * Get the name of this merge strategy.
     */
    val name: String

    /**
     * Get a description of how this strategy works.
     */
    val description: String

    /**
     * Check if this strategy can handle the given conflicts.
     */
    fun canHandle(conflicts: List<Conflict>): Boolean

    /**
     * Merge two change sets with the given conflicts.
     *
     * @param base The base document (common ancestor)
     * @param changeSet1 The first change set
     * @param changeSet2 The second change set
     * @param conflicts The detected conflicts
     * @return Either an error or the merge result
     */
    suspend fun merge(base: JsonElement, changeSet1: ChangeSet, changeSet2: ChangeSet, conflicts: List<Conflict>): Either<JsonDiffError, MergeResult>

    /**
     * Resolve a single conflict.
     *
     * @param conflict The conflict to resolve
     * @param context The merge context
     * @return Either an error or the resolved change
     */
    fun resolveConflict(conflict: Conflict, context: MergeContext): Either<JsonDiffError, JsonChange?>
}

/**
 * Abstract base class for merge strategies.
 */
abstract class BaseMergeStrategy(override val name: String, override val description: String, protected val logger: Logger = ConsoleLogger("MergeStrategy")) :
    MergeStrategy {

    override suspend fun merge(
        base: JsonElement,
        changeSet1: ChangeSet,
        changeSet2: ChangeSet,
        conflicts: List<Conflict>,
    ): Either<JsonDiffError, MergeResult> = either {
        logger.debug(
            "Starting merge with $name strategy",
            mapOf(
                "conflicts" to conflicts.size,
                "changes1" to changeSet1.changeCount(),
                "changes2" to changeSet2.changeCount(),
            ),
        )

        ensure(canHandle(conflicts)) {
            JsonDiffError.InvalidMergeStrategy(
                strategyName = name,
                reason = "Strategy cannot handle the given conflicts",
            )
        }

        val context = MergeContext(
            base = base,
            changeSet1 = changeSet1,
            changeSet2 = changeSet2,
        )

        val mergedChanges = mutableListOf<JsonChange>()
        val unresolvedConflicts = mutableListOf<Conflict>()

        // Apply non-conflicting changes from both change sets
        val conflictingPaths = conflicts.flatMap { conflict ->
            listOf(conflict.change1, conflict.change2).flatMap { change ->
                when (change) {
                    is JsonChange.Add -> listOf(change.path)
                    is JsonChange.Remove -> listOf(change.path)
                    is JsonChange.Replace -> listOf(change.path)
                    is JsonChange.Move -> listOf(change.from, change.to)
                }
            }
        }.toSet()

        // Add non-conflicting changes from changeSet1
        changeSet1.diff.changes.forEach { change ->
            if (!isChangeConflicting(change, conflictingPaths)) {
                mergedChanges.add(change)
            }
        }

        // Add non-conflicting changes from changeSet2
        changeSet2.diff.changes.forEach { change ->
            if (!isChangeConflicting(change, conflictingPaths) &&
                !isDuplicateChange(change, mergedChanges)
            ) {
                mergedChanges.add(change)
            }
        }

        // Resolve conflicts
        conflicts.forEach { conflict ->
            when (val resolved = resolveConflict(conflict, context)) {
                is Either.Right -> {
                    resolved.value?.let { mergedChanges.add(it) }
                }
                is Either.Left -> {
                    unresolvedConflicts.add(conflict)
                }
            }
        }

        // Create merged diff
        val mergedDiff = JsonDiff(
            changes = optimizeChanges(mergedChanges),
            structuralChanges = mergeStructuralChanges(
                changeSet1.diff.structuralChanges,
                changeSet2.diff.structuralChanges,
            ),
            metadata = mapOf(
                "mergeStrategy" to name,
                "mergedAt" to SystemTimeProvider().now().toString(),
            ),
        )

        // Apply merged changes to base to get result
        val mergedDocument = mergedDiff.apply(base).bind()

        logger.debug(
            "Merge completed",
            mapOf(
                "mergedChanges" to mergedDiff.changeCount(),
                "unresolvedConflicts" to unresolvedConflicts.size,
            ),
        )

        MergeResult(
            mergedDocument = mergedDocument,
            mergedChangeSet = ChangeSet.create(
                diff = mergedDiff,
                source = base,
                target = mergedDocument,
                metadata = mapOf(
                    "mergeStrategy" to name,
                    "parent1" to changeSet1.id.toString(),
                    "parent2" to changeSet2.id.toString(),
                ),
            ).mapLeft { error ->
                JsonDiffError.MergeOperationFailed(
                    reason = "Failed to create merged changeset: $error",
                )
            }.bind(),
            resolvedConflicts = conflicts - unresolvedConflicts.toSet(),
            unresolvedConflicts = unresolvedConflicts,
            metadata = mapOf(
                "strategy" to name,
                "totalConflicts" to conflicts.size.toString(),
                "resolved" to (conflicts.size - unresolvedConflicts.size).toString(),
            ),
        )
    }

    protected fun isChangeConflicting(change: JsonChange, conflictingPaths: Set<JsonPath>): Boolean {
        val changePaths = when (change) {
            is JsonChange.Add -> setOf(change.path)
            is JsonChange.Remove -> setOf(change.path)
            is JsonChange.Replace -> setOf(change.path)
            is JsonChange.Move -> setOf(change.from, change.to)
        }

        return changePaths.any { it in conflictingPaths }
    }

    protected fun isDuplicateChange(change: JsonChange, existingChanges: List<JsonChange>): Boolean = existingChanges.any { existing ->
        change == existing
    }

    protected fun optimizeChanges(changes: List<JsonChange>): List<JsonChange> {
        // Remove redundant changes and optimize
        val optimized = mutableListOf<JsonChange>()
        val processed = mutableSetOf<String>()

        changes.forEach { change ->
            val key = changeKey(change)
            if (key !in processed) {
                optimized.add(change)
                processed.add(key)
            }
        }

        return optimized
    }

    private fun changeKey(change: JsonChange): String = when (change) {
        is JsonChange.Add -> "add:${change.path}"
        is JsonChange.Remove -> "remove:${change.path}"
        is JsonChange.Replace -> "replace:${change.path}"
        is JsonChange.Move -> "move:${change.from}:${change.to}"
    }

    protected fun mergeStructuralChanges(changes1: List<StructuralChange>, changes2: List<StructuralChange>): List<StructuralChange> {
        // Merge and deduplicate structural changes
        return (changes1 + changes2).distinctBy { change ->
            when (change) {
                is StructuralChange.FieldAdded -> "add:${change.path}"
                is StructuralChange.FieldRemoved -> "remove:${change.path}"
                is StructuralChange.TypeChanged -> "type:${change.path}"
                is StructuralChange.ArraySizeChanged -> "size:${change.path}"
            }
        }
    }
}

/**
 * Strategy that always takes changes from the first change set (ours).
 */
class OursStrategy :
    BaseMergeStrategy(
        name = "ours",
        description = "Always prefer changes from the first change set",
    ) {
    override fun canHandle(conflicts: List<Conflict>): Boolean = true

    override fun resolveConflict(conflict: Conflict, context: MergeContext): Either<JsonDiffError, JsonChange?> = Either.Right(conflict.change1)
}

/**
 * Strategy that always takes changes from the second change set (theirs).
 */
class TheirsStrategy :
    BaseMergeStrategy(
        name = "theirs",
        description = "Always prefer changes from the second change set",
    ) {
    override fun canHandle(conflicts: List<Conflict>): Boolean = true

    override fun resolveConflict(conflict: Conflict, context: MergeContext): Either<JsonDiffError, JsonChange?> = Either.Right(conflict.change2)
}

/**
 * Strategy that attempts automatic resolution based on conflict type.
 */
class AutomaticStrategy :
    BaseMergeStrategy(
        name = "automatic",
        description = "Automatically resolve conflicts where possible",
    ) {
    override fun canHandle(conflicts: List<Conflict>): Boolean {
        // Can handle if all conflicts have low or medium severity
        return conflicts.all { it.severity != ConflictSeverity.High }
    }

    override fun resolveConflict(conflict: Conflict, context: MergeContext): Either<JsonDiffError, JsonChange?> = either {
        when (conflict.type) {
            ConflictType.DoubleDelete -> {
                // Both want to delete - accept the deletion
                conflict.change1
            }
            ConflictType.UpdateConflict -> {
                // For updates, could use timestamps or other heuristics
                // For now, fail and require manual resolution
                raise(
                    JsonDiffError.MergeOperationFailed(
                        reason = "Cannot automatically resolve update conflict",
                        conflictPaths = listOf(conflict.path.toString()),
                    ),
                )
            }
            ConflictType.AddConflict -> {
                // For additions, could merge arrays or create unique keys
                // For now, take the first one
                conflict.change1
            }
            else -> {
                raise(
                    JsonDiffError.MergeOperationFailed(
                        reason = "Cannot automatically resolve ${conflict.type} conflict",
                        conflictPaths = listOf(conflict.path.toString()),
                    ),
                )
            }
        }
    }
}

/**
 * Strategy that requires manual resolution for all conflicts.
 */
class ManualStrategy :
    BaseMergeStrategy(
        name = "manual",
        description = "Require manual resolution for all conflicts",
    ) {
    override fun canHandle(conflicts: List<Conflict>): Boolean = true

    override fun resolveConflict(conflict: Conflict, context: MergeContext): Either<JsonDiffError, JsonChange?> {
        // Don't resolve any conflicts - leave them all unresolved
        return Either.Left(
            JsonDiffError.MergeOperationFailed(
                reason = "Manual resolution required",
                conflictPaths = listOf(conflict.path.toString()),
            ),
        )
    }
}

/**
 * Custom strategy that uses provided resolution rules.
 */
class CustomStrategy(private val rules: List<ResolutionRule>, name: String = "custom", description: String = "Custom merge strategy with user-defined rules") :
    BaseMergeStrategy(name, description) {

    override fun canHandle(conflicts: List<Conflict>): Boolean {
        // Can handle if we have rules for all conflict types present
        val conflictTypes = conflicts.map { it.type }.toSet()
        val handledTypes = rules.map { it.conflictType }.toSet()

        return conflictTypes.all { it in handledTypes }
    }

    override fun resolveConflict(conflict: Conflict, context: MergeContext): Either<JsonDiffError, JsonChange?> = either {
        val rule = rules.find { it.conflictType == conflict.type }

        if (rule == null) {
            raise(
                JsonDiffError.MergeOperationFailed(
                    reason = "No rule defined for ${conflict.type} conflicts",
                    conflictPaths = listOf(conflict.path.toString()),
                ),
            )
        }

        when (rule.resolution) {
            ResolutionAction.TakeFirst -> conflict.change1
            ResolutionAction.TakeSecond -> conflict.change2
            ResolutionAction.Skip -> null
            ResolutionAction.Fail -> raise(
                JsonDiffError.MergeOperationFailed(
                    reason = "Rule requires manual resolution for ${conflict.type}",
                    conflictPaths = listOf(conflict.path.toString()),
                ),
            )
        }
    }
}

/**
 * Factory for creating merge strategies.
 */
object MergeStrategyFactory {
    fun create(type: MergeStrategyType): MergeStrategy = when (type) {
        MergeStrategyType.Ours -> OursStrategy()
        MergeStrategyType.Theirs -> TheirsStrategy()
        MergeStrategyType.Automatic -> AutomaticStrategy()
        MergeStrategyType.Manual -> ManualStrategy()
    }

    fun createCustom(rules: List<ResolutionRule>): MergeStrategy = CustomStrategy(rules)
}
