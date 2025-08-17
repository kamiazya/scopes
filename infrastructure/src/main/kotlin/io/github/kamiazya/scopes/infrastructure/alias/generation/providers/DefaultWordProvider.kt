package io.github.kamiazya.scopes.infrastructure.alias.generation.providers

import io.github.kamiazya.scopes.domain.service.WordProvider

/**
 * Default implementation of WordProvider with hardcoded word lists.
 *
 * This implementation provides the default vocabulary for alias generation,
 * using the same word lists as the original HaikunatorService.
 * It serves as a fallback when no custom word provider is configured.
 */
class DefaultWordProvider : WordProvider {

    private val adjectives = listOf(
        "bold", "brave", "calm", "clever", "cool", "epic", "fast", "great", "happy", "kind",
        "light", "noble", "quick", "smart", "swift", "wise", "young", "bright", "clear", "deep",
        "fresh", "good", "high", "keen", "live", "new", "pure", "real", "rich", "safe",
        "sure", "true", "warm", "wild", "fine", "free", "full", "glad", "hot", "open"
    )

    private val nouns = listOf(
        "tiger", "eagle", "wolf", "bear", "lion", "shark", "hawk", "fox", "deer", "owl",
        "cat", "dog", "bird", "fish", "tree", "star", "moon", "sun", "rock", "wave",
        "fire", "wind", "ice", "storm", "cloud", "river", "mountain", "forest", "ocean", "valley",
        "flower", "garden", "bridge", "tower", "castle", "sword", "shield", "arrow", "crown", "gem"
    )

    override fun getAdjectives(): List<String> = adjectives

    override fun getNouns(): List<String> = nouns

    override fun getAdditionalWords(category: String): List<String> {
        return when (category) {
            "adjectives" -> adjectives
            "nouns" -> nouns
            else -> emptyList()
        }
    }

    override fun getAvailableCategories(): List<String> = listOf("adjectives", "nouns")
}
