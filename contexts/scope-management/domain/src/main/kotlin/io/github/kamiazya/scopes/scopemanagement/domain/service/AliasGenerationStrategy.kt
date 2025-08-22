package io.github.kamiazya.scopes.scopemanagement.domain.service

/**
 * Strategy interface for alias generation algorithms.
 *
 * This interface allows different algorithms to be plugged in for generating
 * alias names. Each strategy can implement its own naming pattern while
 * utilizing a WordProvider for vocabulary when needed.
 *
 * Examples of strategies:
 * - Haikunator: adjective-noun-token pattern
 * - UUID: UUID-based naming
 * - Sequential: numbered sequence pattern
 * - Custom: user-defined patterns
 */
interface AliasGenerationStrategy {

    /**
     * Generates an alias name using the strategy's algorithm.
     *
     * @param seed A seed value for deterministic generation
     * @param wordProvider Provider for word lists (adjectives, nouns, etc.)
     * @return The generated alias name string
     */
    fun generate(seed: Long, wordProvider: WordProvider): String

    /**
     * Generates a random alias name using the strategy's algorithm.
     *
     * @param wordProvider Provider for word lists (adjectives, nouns, etc.)
     * @return The generated alias name string
     */
    fun generateRandom(wordProvider: WordProvider): String

    /**
     * Gets the name of this strategy.
     * Used for configuration and identification.
     *
     * @return The strategy name (e.g., "haikunator", "uuid", "sequential")
     */
    fun getName(): String
}
