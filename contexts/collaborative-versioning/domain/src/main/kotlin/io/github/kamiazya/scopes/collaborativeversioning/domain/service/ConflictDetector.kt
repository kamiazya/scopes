package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.JsonDiffError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Service for detecting conflicts between concurrent changes.
 *
 * This service identifies various types of conflicts that can occur
 * when multiple users modify the same resource concurrently.
 */
interface ConflictDetector {
    /**
     * Detect conflicts between two change sets.
     *
     * @param baseChangeSet The common ancestor change set
     * @param changeSet1 The first set of changes
     * @param changeSet2 The second set of changes
     * @return Either an error or the detected conflicts
     */
    suspend fun detectConflicts(baseChangeSet: ChangeSet?, changeSet1: ChangeSet, changeSet2: ChangeSet): Either<JsonDiffError, ConflictDetectionResult>

    /**
     * Detect conflicts between multiple change sets.
     *
     * @param baseChangeSet The common ancestor change set
     * @param changeSets The list of change sets to check
     * @return Either an error or the detected conflicts
     */
    suspend fun detectMultiWayConflicts(baseChangeSet: ChangeSet?, changeSets: List<ChangeSet>): Either<JsonDiffError, MultiWayConflictResult>

    /**
     * Check if two changes are conflicting.
     *
     * @param change1 First change
     * @param change2 Second change
     * @return true if changes conflict, false otherwise
     */
    fun areChangesConflicting(change1: JsonChange, change2: JsonChange): Boolean

    /**
     * Classify the type of conflict between two changes.
     *
     * @param change1 First change
     * @param change2 Second change
     * @return The conflict type if conflicting, null otherwise
     */
    fun classifyConflict(change1: JsonChange, change2: JsonChange): ConflictType?
}

/**
 * Default implementation of ConflictDetector.
 */
class DefaultConflictDetector(
    private val logger: Logger = ConsoleLogger("ConflictDetector"),
    private val config: ConflictDetectorConfig = ConflictDetectorConfig.default(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) : ConflictDetector {

    override suspend fun detectConflicts(
        baseChangeSet: ChangeSet?,
        changeSet1: ChangeSet,
        changeSet2: ChangeSet,
    ): Either<JsonDiffError, ConflictDetectionResult> = either {
        logger.debug(
            "Detecting conflicts between change sets",
            mapOf(
                "baseId" to baseChangeSet?.id?.toString(),
                "changeSet1Id" to changeSet1.id.toString(),
                "changeSet2Id" to changeSet2.id.toString(),
                "changes1Count" to changeSet1.changeCount(),
                "changes2Count" to changeSet2.changeCount(),
            ) as Map<String, Any>,
        )

        val conflicts = mutableListOf<Conflict>()
        val changes1 = changeSet1.diff.changes
        val changes2 = changeSet2.diff.changes

        // Check each pair of changes for conflicts
        changes1.forEach { change1 ->
            changes2.forEach { change2 ->
                if (areChangesConflicting(change1, change2)) {
                    val conflictType = classifyConflict(change1, change2)
                        ?: ConflictType.Unknown

                    conflicts.add(
                        Conflict(
                            id = ConflictId.generate(),
                            type = conflictType,
                            path = getConflictPath(change1, change2),
                            change1 = change1,
                            change2 = change2,
                            description = generateConflictDescription(
                                conflictType,
                                change1,
                                change2,
                            ),
                            detectedAt = timeProvider.now(),
                            severity = determineConflictSeverity(conflictType),
                            resolution = suggestResolution(conflictType, change1, change2),
                        ),
                    )
                }
            }
        }

        // Check for semantic conflicts
        if (config.detectSemanticConflicts) {
            val semanticConflicts = detectSemanticConflicts(
                changeSet1,
                changeSet2,
            )
            conflicts.addAll(semanticConflicts)
        }

        logger.debug(
            "Conflict detection completed",
            mapOf(
                "totalConflicts" to conflicts.size,
                "conflictsByType" to conflicts.groupBy { it.type }.mapValues { it.value.size },
            ),
        )

        ConflictDetectionResult(
            conflicts = conflicts,
            canAutoMerge = conflicts.none { it.severity == ConflictSeverity.High },
            metadata = mapOf(
                "detectionEngine" to "DefaultConflictDetector",
                "detectionVersion" to "1.0.0",
                "detectedAt" to timeProvider.now().toString(),
            ),
        )
    }

    override suspend fun detectMultiWayConflicts(baseChangeSet: ChangeSet?, changeSets: List<ChangeSet>): Either<JsonDiffError, MultiWayConflictResult> =
        either {
            ensure(changeSets.size >= 2) {
                JsonDiffError.ConflictDetectionFailed(
                    reason = "At least 2 change sets required for conflict detection",
                )
            }

            val pairwiseResults = mutableListOf<ConflictDetectionResult>()

            // Check all pairs of change sets
            for (i in changeSets.indices) {
                for (j in i + 1 until changeSets.size) {
                    val result = detectConflicts(
                        baseChangeSet,
                        changeSets[i],
                        changeSets[j],
                    ).bind()

                    pairwiseResults.add(result)
                }
            }

            // Aggregate conflicts
            val allConflicts = pairwiseResults.flatMap { it.conflicts }
            val uniqueConflicts = deduplicateConflicts(allConflicts)

            MultiWayConflictResult(
                conflicts = uniqueConflicts,
                changeSetCount = changeSets.size,
                pairwiseResults = pairwiseResults,
                canAutoMerge = uniqueConflicts.none { it.severity == ConflictSeverity.High },
            )
        }

    override fun areChangesConflicting(change1: JsonChange, change2: JsonChange): Boolean = when {
        // Same path modifications
        areSamePath(change1, change2) -> true

        // Overlapping paths (parent-child relationships)
        areOverlappingPaths(change1, change2) -> true

        // Move conflicts
        areMoveConflicting(change1, change2) -> true

        else -> false
    }

    override fun classifyConflict(change1: JsonChange, change2: JsonChange): ConflictType? = when {
        // Both modifying the same field
        change1 is JsonChange.Replace &&
            change2 is JsonChange.Replace &&
            change1.path == change2.path -> ConflictType.UpdateConflict

        // One deletes, other modifies
        (
            change1 is JsonChange.Remove &&
                change2 is JsonChange.Replace &&
                change1.path == change2.path
            ) ||
            (
                change1 is JsonChange.Replace &&
                    change2 is JsonChange.Remove &&
                    change1.path == change2.path
                ) -> ConflictType.DeleteUpdateConflict

        // Both delete the same field
        change1 is JsonChange.Remove &&
            change2 is JsonChange.Remove &&
            change1.path == change2.path -> ConflictType.DoubleDelete

        // Both add to the same location
        change1 is JsonChange.Add &&
            change2 is JsonChange.Add &&
            change1.path == change2.path -> ConflictType.AddConflict

        // Move conflicts
        areMoveConflicting(change1, change2) -> ConflictType.MoveConflict

        // Structural conflicts
        areStructurallyConflicting(change1, change2) -> ConflictType.StructuralConflict

        else -> null
    }

    private fun areSamePath(change1: JsonChange, change2: JsonChange): Boolean {
        val paths1 = getChangePaths(change1)
        val paths2 = getChangePaths(change2)

        return paths1.any { p1 -> paths2.contains(p1) }
    }

    private fun areOverlappingPaths(change1: JsonChange, change2: JsonChange): Boolean {
        val paths1 = getChangePaths(change1)
        val paths2 = getChangePaths(change2)

        return paths1.any { p1 ->
            paths2.any { p2 ->
                p1.isAncestorOf(p2) || p2.isAncestorOf(p1)
            }
        }
    }

    private fun areMoveConflicting(change1: JsonChange, change2: JsonChange): Boolean {
        if (change1 !is JsonChange.Move && change2 !is JsonChange.Move) {
            return false
        }

        // Check various move conflict scenarios
        return when {
            // Both trying to move the same item
            change1 is JsonChange.Move &&
                change2 is JsonChange.Move &&
                change1.from == change2.from -> true

            // Moving to the same destination
            change1 is JsonChange.Move &&
                change2 is JsonChange.Move &&
                change1.to == change2.to -> true

            // One moves what the other modifies
            change1 is JsonChange.Move && getChangePaths(change2).contains(change1.from) -> true
            change2 is JsonChange.Move && getChangePaths(change1).contains(change2.from) -> true

            else -> false
        }
    }

    private fun areStructurallyConflicting(change1: JsonChange, change2: JsonChange): Boolean {
        // Check if changes would result in invalid structure
        return when {
            // Adding to a removed parent
            change1 is JsonChange.Remove &&
                change2 is JsonChange.Add &&
                change1.path.isAncestorOf(change2.path) -> true

            change2 is JsonChange.Remove &&
                change1 is JsonChange.Add &&
                change2.path.isAncestorOf(change1.path) -> true

            else -> false
        }
    }

    private fun getChangePaths(change: JsonChange): Set<JsonPath> = when (change) {
        is JsonChange.Add -> setOf(change.path)
        is JsonChange.Remove -> setOf(change.path)
        is JsonChange.Replace -> setOf(change.path)
        is JsonChange.Move -> setOf(change.from, change.to)
    }

    private fun getConflictPath(change1: JsonChange, change2: JsonChange): JsonPath {
        val paths1 = getChangePaths(change1)
        val paths2 = getChangePaths(change2)

        // Return the first common path or the first path from change1
        return paths1.firstOrNull { paths2.contains(it) } ?: paths1.first()
    }

    private fun generateConflictDescription(type: ConflictType, change1: JsonChange, change2: JsonChange): String = when (type) {
        ConflictType.UpdateConflict -> "Both changes modify the same field"
        ConflictType.DeleteUpdateConflict -> "One change deletes while another updates"
        ConflictType.StructuralConflict -> "Changes affect document structure incompatibly"
        ConflictType.MoveConflict -> "Conflicting move operations"
        ConflictType.AddConflict -> "Both changes add to the same location"
        ConflictType.DoubleDelete -> "Both changes delete the same field"
        ConflictType.SemanticConflict -> "Changes violate semantic constraints"
        ConflictType.Unknown -> "Unknown conflict type"
    }

    private fun determineConflictSeverity(type: ConflictType): ConflictSeverity = when (type) {
        ConflictType.UpdateConflict -> ConflictSeverity.Medium
        ConflictType.DeleteUpdateConflict -> ConflictSeverity.High
        ConflictType.StructuralConflict -> ConflictSeverity.High
        ConflictType.MoveConflict -> ConflictSeverity.Medium
        ConflictType.AddConflict -> ConflictSeverity.Low
        ConflictType.DoubleDelete -> ConflictSeverity.Low
        ConflictType.SemanticConflict -> ConflictSeverity.High
        ConflictType.Unknown -> ConflictSeverity.Medium
    }

    private fun suggestResolution(type: ConflictType, change1: JsonChange, change2: JsonChange): ConflictResolution? = when (type) {
        ConflictType.UpdateConflict -> ConflictResolution.Manual(
            suggestion = "Review both changes and choose the correct value",
        )
        ConflictType.DeleteUpdateConflict -> ConflictResolution.Manual(
            suggestion = "Decide whether to keep or delete the field",
        )
        ConflictType.DoubleDelete -> ConflictResolution.Automatic(
            strategy = "Accept deletion",
            description = "Both changes agree on deletion",
        )
        ConflictType.AddConflict -> ConflictResolution.Manual(
            suggestion = "Choose which value to add or merge both",
        )
        else -> null
    }

    private fun detectSemanticConflicts(changeSet1: ChangeSet, changeSet2: ChangeSet): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()

        // Check for business rule violations
        // This would be extended based on domain-specific rules

        return conflicts
    }

    private fun deduplicateConflicts(conflicts: List<Conflict>): List<Conflict> {
        // Remove duplicate conflicts based on path and type
        return conflicts.distinctBy { "${it.path}_${it.type}" }
    }
}
