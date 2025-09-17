package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectError

/**
 * Domain service for managing aspects.
 *
 * This service provides centralized logic for parsing, validating,
 * and formatting aspects, removing this responsibility from the interface layer.
 */
class AspectManagementService {

    /**
     * Parse aspect entry from string format.
     * Supports both "key:value" and "key=value" formats.
     *
     * @param entry The aspect entry string to parse
     * @return Parsed key-value pair or null if invalid
     */
    fun parseAspectEntry(entry: String): Pair<String, String>? {
        if (entry.isBlank()) return null

        // Try colon first, then equals
        val colonIndex = entry.indexOf(':')
        val equalsIndex = entry.indexOf('=')

        val splitIndex = when {
            colonIndex > 0 && (equalsIndex < 0 || colonIndex < equalsIndex) -> colonIndex
            equalsIndex > 0 -> equalsIndex
            else -> return null
        }

        val key = entry.substring(0, splitIndex).trim()
        val value = entry.substring(splitIndex + 1).trim()

        return if (key.isNotEmpty() && value.isNotEmpty()) {
            key to value
        } else {
            null
        }
    }

    /**
     * Parse multiple aspect entries into a map.
     * Groups multiple values for the same key.
     *
     * @param entries List of aspect entry strings
     * @return Map of aspect keys to lists of values
     */
    fun parseAspectFilters(entries: List<String>): Map<String, List<String>> = entries.mapNotNull { parseAspectEntry(it) }
        .groupBy({ it.first }, { it.second })

    /**
     * Validate aspect key-value pairs.
     *
     * @param aspects Map of aspect key-value pairs
     * @return Either validation error or validated aspects
     */
    fun validateAspects(aspects: Map<String, String>): Either<AspectError, Map<String, String>> {
        // Basic validation for now
        // TODO: Add more sophisticated validation based on aspect definitions

        if (aspects.isEmpty()) {
            return Either.Left(AspectError.NoAspectsProvided)
        }

        for ((key, value) in aspects) {
            if (key.isBlank()) {
                return Either.Left(
                    AspectError.InvalidAspectKey(
                        key,
                        AspectError.InvalidAspectKey.KeyError.EMPTY,
                    ),
                )
            }
            if (value.isBlank()) {
                return Either.Left(
                    AspectError.InvalidAspectValue(
                        key,
                        value,
                        AspectError.InvalidAspectValue.ValueError.EMPTY,
                    ),
                )
            }
        }

        return Either.Right(aspects)
    }

    /**
     * Format aspects for JSON representation.
     *
     * @param aspects Map of aspect key-value pairs
     * @return JSON-formatted string representation
     */
    fun generateJsonFormat(aspects: Map<String, String>): String = aspects.entries.joinToString(", ") { (key, value) ->
        "\"$key\": \"$value\""
    }.let { "{$it}" }

    /**
     * Format aspects for human-readable display.
     *
     * @param aspects Map of aspect key-value pairs
     * @return Human-readable string representation
     */
    fun generateDisplayFormat(aspects: Map<String, String>): String = aspects.entries.joinToString(", ") { (key, value) ->
        "$key=$value"
    }

    /**
     * Check if a scope matches the given aspect filters.
     * A scope matches if for each filter key, at least one of the scope's values
     * for that key matches one of the filter's expected values.
     *
     * @param scopeAspects The aspects of the scope to check (multi-valued)
     * @param filters The aspect filters to match against
     * @return true if the scope matches all filters
     */
    fun checkAspectFilters(scopeAspects: Map<String, List<String>>?, filters: Map<String, List<String>>): Boolean {
        if (filters.isEmpty()) return true
        if (scopeAspects.isNullOrEmpty()) return false

        return filters.all { (key, expectedValues) ->
            val actualValues = scopeAspects[key]
            actualValues != null &&
                actualValues.any { actualValue ->
                    expectedValues.contains(actualValue)
                }
        }
    }
}
