package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies

import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import kotlin.random.Random

/**
 * Haikunator strategy for generating human-readable aliases.
 *
 * Generates aliases in the format: adjective-noun-token (e.g., "bold-tiger-x7k")
 * This provides memorable yet unique identifiers for scopes.
 *
 * The generation can be deterministic (using a seed) or random,
 * making the aliases both reproducible and unique.
 */
class HaikunatorStrategy : AliasGenerationStrategy {

    override fun generate(seed: Long, wordProvider: WordProvider): String {
        val random = Random(seed)

        val adjectives = wordProvider.getAdjectives()
        val nouns = wordProvider.getNouns()

        val adjective = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]
        val token = generateToken(random, 3)

        return "$adjective-$noun-$token"
    }

    override fun generateRandom(wordProvider: WordProvider): String {
        val random = Random.Default

        val adjectives = wordProvider.getAdjectives()
        val nouns = wordProvider.getNouns()

        val adjective = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]
        val token = generateToken(random, 3)

        return "$adjective-$noun-$token"
    }

    override fun getName(): String = "haikunator"

    private fun generateToken(random: Random, length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
