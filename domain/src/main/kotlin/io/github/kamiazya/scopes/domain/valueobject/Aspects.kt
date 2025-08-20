package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull

/**
 * Value Object representing a collection of aspects for a Scope.
 * Encapsulates the domain logic for aspect management while hiding implementation details.
 *
 * Business rules:
 * - Each aspect key can have one or more values (NonEmptyList)
 * - Aspect keys must be unique within a scope
 * - All operations return new instances (immutable)
 */
@ConsistentCopyVisibility
data class Aspects private constructor(
    private val map: Map<AspectKey, NonEmptyList<AspectValue>> = emptyMap()
) {
    companion object {
        /**
         * Create an empty Aspects collection.
         */
        fun empty(): Aspects = Aspects()

        /**
         * Create Aspects from a map of AspectKey to NonEmptyList<AspectValue>.
         */
        fun from(map: Map<AspectKey, NonEmptyList<AspectValue>>): Aspects = Aspects(map)

        /**
         * Create Aspects from a list of key-value pairs.
         */
        fun of(vararg pairs: Pair<AspectKey, NonEmptyList<AspectValue>>): Aspects =
            Aspects(pairs.toMap())
    }

    /**
     * Get all values for a specific aspect key.
     * Returns null if the key doesn't exist.
     */
    fun get(key: AspectKey): NonEmptyList<AspectValue>? = map[key]

    /**
     * Get the first value for a specific aspect key.
     * Returns null if the key doesn't exist.
     */
    fun getFirst(key: AspectKey): AspectValue? = map[key]?.head

    /**
     * Set aspect values for a specific key.
     * Pure function that returns a new instance.
     */
    fun set(key: AspectKey, values: NonEmptyList<AspectValue>): Aspects =
        copy(map = map + (key to values))

    /**
     * Set a single aspect value for a specific key (convenience method).
     * Pure function that returns a new instance.
     */
    fun set(key: AspectKey, value: AspectValue): Aspects =
        set(key, nonEmptyListOf(value))

    /**
     * Add a value to an existing aspect key.
     * If the key doesn't exist, creates a new aspect with the single value.
     * Pure function that returns a new instance.
     */
    fun add(key: AspectKey, value: AspectValue): Aspects {
        val existingValues = map[key]
        return if (existingValues != null) {
            copy(map = map + (key to (existingValues + value)))
        } else {
            set(key, value)
        }
    }

    /**
     * Remove an aspect key entirely.
     * Pure function that returns a new instance.
     */
    fun remove(key: AspectKey): Aspects =
        copy(map = map - key)

    /**
     * Remove multiple aspect keys.
     * Pure function that returns a new instance.
     */
    fun remove(keys: Set<AspectKey>): Aspects =
        copy(map = map - keys)

    /**
     * Remove a specific value from an aspect key.
     * If this was the last value, the key is removed entirely.
     * Pure function that returns a new instance.
     */
    fun remove(key: AspectKey, value: AspectValue): Aspects {
        val currentValues = map[key] ?: return this
        val newValues = currentValues.filter { it != value }
        return if (newValues.isEmpty()) {
            copy(map = map - key)
        } else {
            val nonEmptyValues = newValues.toNonEmptyListOrNull()
            if (nonEmptyValues != null) {
                copy(map = map + (key to nonEmptyValues))
            } else {
                copy(map = map - key)
            }
        }
    }

    /**
     * Check if an aspect key exists.
     */
    fun contains(key: AspectKey): Boolean = map.containsKey(key)

    /**
     * Get all aspect keys.
     */
    fun keys(): Set<AspectKey> = map.keys

    /**
     * Get all aspects as a map.
     * Returns a copy to maintain immutability.
     */
    fun toMap(): Map<AspectKey, NonEmptyList<AspectValue>> = map.toMap()

    /**
     * Merge with another Aspects collection.
     * Values from the other collection override values in this collection.
     * Pure function that returns a new instance.
     */
    fun merge(other: Aspects): Aspects =
        copy(map = map + other.map)

    /**
     * Check if this collection is empty.
     */
    fun isEmpty(): Boolean = map.isEmpty()

    /**
     * Get the number of aspect keys.
     */
    fun size(): Int = map.size

    /**
     * Filter aspects by a predicate.
     * Pure function that returns a new instance.
     */
    fun filter(predicate: (AspectKey, NonEmptyList<AspectValue>) -> Boolean): Aspects =
        copy(map = map.filter { (key, values) -> predicate(key, values) })

    /**
     * Transform aspect values while keeping the same keys.
     * Pure function that returns a new instance.
     */
    fun mapValues(transform: (AspectKey, NonEmptyList<AspectValue>) -> NonEmptyList<AspectValue>): Aspects =
        copy(map = map.mapValues { (key, values) -> transform(key, values) })

    override fun toString(): String = "Aspects($map)"
}
