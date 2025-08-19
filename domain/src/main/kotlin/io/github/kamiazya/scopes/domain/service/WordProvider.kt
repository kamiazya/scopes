package io.github.kamiazya.scopes.domain.service

/**
 * Provider interface for word lists used in alias generation.
 *
 * This interface allows for flexible word list management, enabling
 * different sources (hardcoded, configuration files, databases, APIs)
 * to provide vocabulary for alias generation.
 *
 * The word lists can be extended or customized without modifying
 * the generation algorithms, following the Open/Closed Principle.
 */
interface WordProvider {

    /**
     * Gets the list of adjectives.
     *
     * @return List of adjective words
     */
    fun getAdjectives(): List<String>

    /**
     * Gets the list of nouns.
     *
     * @return List of noun words
     */
    fun getNouns(): List<String>

    /**
     * Gets additional words by category.
     *
     * This allows for extensibility by adding new word categories
     * without modifying the interface.
     *
     * @param category The category name (e.g., "verbs", "colors", "animals")
     * @return List of words in the specified category, or empty list if category doesn't exist
     */
    fun getAdditionalWords(category: String): List<String> = emptyList()

    /**
     * Gets all available word categories.
     *
     * @return List of available category names
     */
    fun getAvailableCategories(): List<String> = listOf("adjectives", "nouns")
}

