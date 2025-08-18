package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import arrow.core.left
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import kotlin.random.Random

/**
 * Service for generating human-readable aliases using the Haikunator pattern.
 *
 * Generates aliases in the format: adjective-noun-token (e.g., "bold-tiger-x7k")
 * This provides memorable yet unique identifiers for scopes.
 *
 * The generation is deterministic based on the AliasId, making the alias
 * self-contained and not dependent on external factors like ScopeId.
 */
class HaikunatorService {

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

    /**
     * Generates a canonical alias for a given alias ID using deterministic haikunator pattern.
     *
     * Uses the alias ID as a seed to ensure the same ID always generates the same alias name.
     * This makes the alias generation self-contained and reproducible.
     *
     * @param aliasId The alias ID to generate a name for
     * @return Either an error or the generated alias name
     */
    fun generateCanonicalAlias(aliasId: AliasId): Either<Throwable, AliasName> {
        return try {
            // Use the alias ID's hash as seed for deterministic generation
            val seed = aliasId.value.hashCode().toLong()
            val random = Random(seed)

            val adjective = adjectives[random.nextInt(adjectives.size)]
            val noun = nouns[random.nextInt(nouns.size)]
            val token = generateToken(random, 3)

            val aliasString = "$adjective-$noun-$token"

            AliasName.create(aliasString)
                .mapLeft { it as Throwable }
        } catch (e: Exception) {
            e.left()
        }
    }

    /**
     * Generates a random alias using the haikunator pattern.
     *
     * This is non-deterministic and will generate different results each time.
     *
     * @return Either an error or the generated alias name
     */
    fun generateRandomAlias(): Either<Throwable, AliasName> {
        return try {
            val random = Random.Default

            val adjective = adjectives[random.nextInt(adjectives.size)]
            val noun = nouns[random.nextInt(nouns.size)]
            val token = generateToken(random, 3)

            val aliasString = "$adjective-$noun-$token"

            AliasName.create(aliasString)
                .mapLeft { it as Throwable }
        } catch (e: Exception) {
            e.left()
        }
    }

    private fun generateToken(random: Random, length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}

