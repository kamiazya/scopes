package io.github.kamiazya.scopes.domain.error

/**
 * Domain-rich recovery configuration system replacing primitive-obsessed RecoveryConfiguration.
 * 
 * This file implements a functional DDD approach with immutable value objects that encapsulate
 * recovery behavior and validation. Each configuration type focuses on a specific domain concern
 * with built-in domain methods for common operations.
 */

/**
 * Configuration for Scope title recovery operations.
 * 
 * Encapsulates all title-related recovery behavior including default generation,
 * truncation, and cleaning logic. This replaces primitive string fields with
 * domain-rich behavior.
 */
data class ScopeTitleRecoveryConfig(
    val defaultTemplate: String = "Untitled Scope",
    val maxLength: Int = 200,
    val truncationSuffix: String = "..."
) {
    
    init {
        require(maxLength > 0) { "maxLength must be positive, got: $maxLength" }
        require(defaultTemplate.isNotBlank()) { "defaultTemplate cannot be blank" }
    }
    
    /**
     * Generates a default title using the configured template.
     * Pure function with no side effects.
     */
    fun generateDefaultTitle(): String = defaultTemplate
    
    /**
     * Truncates a title to fit within maxLength, adding suffix if needed.
     * Pure function that preserves meaning while enforcing length constraints.
     */
    fun truncateTitle(title: String): String {
        if (title.length <= maxLength) {
            return title
        }
        
        val availableLength = maxLength - truncationSuffix.length
        return if (availableLength > 0) {
            title.take(availableLength) + truncationSuffix
        } else {
            // Edge case: suffix is longer than max length
            title.take(maxLength)
        }
    }
    
    /**
     * Cleans title by removing newlines and normalizing whitespace.
     * Pure function that ensures titles are single-line and properly formatted.
     */
    fun cleanTitle(title: String): String {
        return title
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Configuration for Scope description recovery operations.
 * 
 * Encapsulates all description-related recovery behavior including truncation
 * and extraction logic. Focuses on preserving meaning while enforcing constraints.
 */
data class ScopeDescriptionRecoveryConfig(
    val maxLength: Int = 1000,
    val truncationSuffix: String = "..."
) {
    
    init {
        require(maxLength >= 0) { "maxLength must be non-negative, got: $maxLength" }
    }
    
    /**
     * Truncates a description to fit within maxLength, adding suffix if needed.
     * Pure function that preserves meaning while enforcing length constraints.
     */
    fun truncateDescription(description: String): String {
        if (description.length <= maxLength) {
            return description
        }
        
        val availableLength = maxLength - truncationSuffix.length
        return if (availableLength > 0) {
            description.take(availableLength) + truncationSuffix
        } else {
            // Edge case: suffix is longer than max length
            description.take(maxLength)
        }
    }
    
    /**
     * Extracts the first sentence from a description.
     * Pure function that helps summarize long descriptions.
     */
    fun extractFirstSentence(description: String): String {
        if (description.isBlank()) {
            return description
        }
        
        val firstPeriodIndex = description.indexOf('.')
        return if (firstPeriodIndex != -1) {
            description.substring(0, firstPeriodIndex).trim()
        } else {
            description
        }
    }
}

/**
 * Configuration for Scope duplication recovery operations.
 * 
 * Encapsulates all duplication-related recovery behavior including suffix generation
 * and variant creation. Focuses on generating unique title variations when duplicates occur.
 */
data class ScopeDuplicationRecoveryConfig(
    val suffixTemplate: String = " ({number})",
    val maxRetryAttempts: Int = 10
) {
    
    init {
        require(maxRetryAttempts > 0) { "maxRetryAttempts must be positive, got: $maxRetryAttempts" }
        require(suffixTemplate.contains("{number}")) { 
            "suffixTemplate must contain {number} placeholder, got: '$suffixTemplate'" 
        }
    }
    
    /**
     * Generates a single variant by appending the numbered suffix to the base title.
     * Pure function that creates predictable title variations.
     */
    fun generateVariant(baseTitle: String, number: Int): String {
        return baseTitle + suffixTemplate.replace("{number}", number.toString())
    }
    
    /**
     * Generates the suffix part for a given number.
     * Pure function that creates consistent suffix formatting.
     */
    fun generateSuffix(number: Int): String {
        return suffixTemplate.replace("{number}", number.toString())
    }
    
    /**
     * Generates multiple title variants up to the requested count.
     * Pure function that respects maxRetryAttempts constraint.
     */
    fun generateVariants(baseTitle: String, count: Int): List<String> {
        if (count <= 0) {
            return emptyList()
        }
        
        val actualCount = minOf(count, maxRetryAttempts)
        return (1..actualCount).map { number ->
            generateVariant(baseTitle, number)
        }
    }
}

/**
 * Configuration for Scope hierarchy recovery operations.
 * 
 * Encapsulates all hierarchy-related recovery guidance including depth and children
 * limit violations. Focuses on providing contextual guidance messages that help
 * users understand and resolve hierarchy constraints.
 */
data class ScopeHierarchyRecoveryConfig(
    val maxDepthGuidance: String = "The hierarchy is too deep (maximum {maxDepth} levels). " +
        "Consider restructuring to reduce nesting.",
    val maxChildrenGuidance: String = "Too many child scopes (maximum {maxChildren} allowed). " +
        "Consider organizing them into logical groups."
) {
    
    /**
     * Generates contextual guidance for depth limit violations.
     * Pure function that substitutes placeholders with actual values.
     */
    fun getDepthGuidance(maxDepth: Int, currentDepth: Int): String {
        return maxDepthGuidance
            .replace("{maxDepth}", maxDepth.toString())
            .replace("{currentDepth}", currentDepth.toString())
    }
    
    /**
     * Generates contextual guidance for children limit violations.
     * Pure function that substitutes placeholders with actual values.
     */
    fun getChildrenGuidance(maxChildren: Int, currentChildren: Int): String {
        return maxChildrenGuidance
            .replace("{maxChildren}", maxChildren.toString())
            .replace("{currentChildren}", currentChildren.toString())
    }
}

/**
 * Sealed class hierarchy for scope recovery configurations.
 * 
 * Provides type-safe access to all recovery configuration types while maintaining
 * functional domain-driven design principles. Uses the sealed class pattern to
 * ensure exhaustive handling and future extensibility.
 */
sealed class ScopeRecoveryConfiguration {
    
    /**
     * Complete recovery configuration containing all specialized config types.
     * This is the primary implementation that provides access to all recovery behaviors.
     */
    data class Complete(
        val title: ScopeTitleRecoveryConfig,
        val description: ScopeDescriptionRecoveryConfig,
        val duplication: ScopeDuplicationRecoveryConfig,
        val hierarchy: ScopeHierarchyRecoveryConfig
    ) : ScopeRecoveryConfiguration() {
        
        /**
         * Type-safe accessor for title configuration.
         */
        fun titleConfig(): ScopeTitleRecoveryConfig = title
        
        /**
         * Type-safe accessor for description configuration.
         */
        fun descriptionConfig(): ScopeDescriptionRecoveryConfig = description
        
        /**
         * Type-safe accessor for duplication configuration.
         */
        fun duplicationConfig(): ScopeDuplicationRecoveryConfig = duplication
        
        /**
         * Type-safe accessor for hierarchy configuration.
         */
        fun hierarchyConfig(): ScopeHierarchyRecoveryConfig = hierarchy
    }
    
    companion object {
        /**
         * Factory method for creating a default configuration with sensible defaults.
         * Pure function that creates a complete configuration ready for use.
         */
        fun default(): Complete = Complete(
            title = ScopeTitleRecoveryConfig(),
            description = ScopeDescriptionRecoveryConfig(),
            duplication = ScopeDuplicationRecoveryConfig(),
            hierarchy = ScopeHierarchyRecoveryConfig()
        )
    }
}
