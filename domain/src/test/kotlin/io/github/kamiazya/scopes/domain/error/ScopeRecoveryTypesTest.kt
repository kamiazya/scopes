package io.github.kamiazya.scopes.domain.error

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain

/**
 * Test-Driven Development for the new ScopeRecoveryConfiguration system.
 *
 * This test suite drives the design of domain-rich configuration entities
 * replacing the primitive-obsessed RecoveryConfiguration.
 */
class ScopeRecoveryTypesTest : StringSpec({

    "ScopeTitleRecoveryConfig should enforce positive maxTitleLength" {
        // Test setup
        shouldThrow<IllegalArgumentException> {
            ScopeTitleRecoveryConfig(
                defaultTemplate = "Untitled Scope",
                maxLength = 0, // Invalid
                truncationSuffix = "..."
            )
        }
    }

    "ScopeTitleRecoveryConfig should enforce non-blank defaultTemplate" {
        // Test setup
        shouldThrow<IllegalArgumentException> {
            ScopeTitleRecoveryConfig(
                defaultTemplate = "", // Invalid
                maxLength = 200,
                truncationSuffix = "..."
            )
        }
    }

    "ScopeTitleRecoveryConfig should create valid configuration with proper values" {
        // Test setup
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "Untitled Scope",
            maxLength = 200,
            truncationSuffix = "..."
        )

        config.defaultTemplate shouldBe "Untitled Scope"
        config.maxLength shouldBe 200
        config.truncationSuffix shouldBe "..."
    }

    "ScopeTitleRecoveryConfig generateDefaultTitle should return configured template" {
        // Test setup
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "New Task",
            maxLength = 200,
            truncationSuffix = "..."
        )

        config.generateDefaultTitle() shouldBe "New Task"
    }

    "ScopeTitleRecoveryConfig truncateTitle should truncate long titles properly" {
        // Test setup
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "Default",
            maxLength = 10,
            truncationSuffix = "..."
        )

        val longTitle = "This is a very long title"
        val truncated = config.truncateTitle(longTitle)

        truncated.length shouldBe 10 // Should equal max length
        truncated shouldEndWith "..."
        truncated shouldBe "This is..."
    }

    "ScopeTitleRecoveryConfig truncateTitle should not modify short titles" {
        // Test setup
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "Default",
            maxLength = 50,
            truncationSuffix = "..."
        )

        val shortTitle = "Short"
        config.truncateTitle(shortTitle) shouldBe shortTitle
    }

    "ScopeTitleRecoveryConfig cleanTitle should remove newlines and excess whitespace" {
        // Test setup
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "Default",
            maxLength = 200,
            truncationSuffix = "..."
        )

        val dirtyTitle = "Title with\nnewlines\r\n  and   excess   spaces  "
        val cleaned = config.cleanTitle(dirtyTitle)

        cleaned shouldBe "Title with newlines and excess spaces"
        cleaned shouldNotContain "\n"
        cleaned shouldNotContain "\r"
    }

    "ScopeTitleRecoveryConfig cleanTitle should handle empty and whitespace-only input" {
        // Test setup
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "Default",
            maxLength = 200,
            truncationSuffix = "..."
        )

        config.cleanTitle("") shouldBe ""
        config.cleanTitle("   ") shouldBe ""
        config.cleanTitle("\n\r\t") shouldBe ""
    }

    "ScopeTitleRecoveryConfig should have sensible defaults" {
        // Test setup
        val config = ScopeTitleRecoveryConfig()

        config.defaultTemplate shouldBe "Untitled Scope"
        config.maxLength shouldBe 200
        config.truncationSuffix shouldBe "..."
    }

    // =====  ScopeDescriptionRecoveryConfig Tests =====

    "ScopeDescriptionRecoveryConfig should enforce positive maxDescriptionLength" {
        // Test setup
        shouldThrow<IllegalArgumentException> {
            ScopeDescriptionRecoveryConfig(
                maxLength = -1, // Invalid
                truncationSuffix = "..."
            )
        }
    }

    "ScopeDescriptionRecoveryConfig should create valid configuration with proper values" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )

        config.maxLength shouldBe 1000
        config.truncationSuffix shouldBe "..."
    }

    "ScopeDescriptionRecoveryConfig truncateDescription should truncate long descriptions properly" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 20,
            truncationSuffix = "..."
        )

        val longDescription = "This is a very long description that exceeds the limit"
        val truncated = config.truncateDescription(longDescription)

        truncated.length shouldBe 20 // Should equal max length
        truncated shouldEndWith "..."
        truncated shouldBe "This is a very lo..."
    }

    "ScopeDescriptionRecoveryConfig truncateDescription should not modify short descriptions" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 100,
            truncationSuffix = "..."
        )

        val shortDescription = "Short description"
        config.truncateDescription(shortDescription) shouldBe shortDescription
    }

    "ScopeDescriptionRecoveryConfig truncateDescription should handle empty descriptions" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 100,
            truncationSuffix = "..."
        )

        config.truncateDescription("") shouldBe ""
        config.truncateDescription("   ") shouldBe "   " // Preserves whitespace-only
    }

    "ScopeDescriptionRecoveryConfig extractFirstSentence should extract first sentence" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )

        val multiSentence = "First sentence. Second sentence. Third sentence."
        config.extractFirstSentence(multiSentence) shouldBe "First sentence"
    }

    "ScopeDescriptionRecoveryConfig extractFirstSentence should handle descriptions without periods" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )

        val noPeriod = "This has no period"
        config.extractFirstSentence(noPeriod) shouldBe noPeriod
    }

    "ScopeDescriptionRecoveryConfig extractFirstSentence should handle empty descriptions" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )

        config.extractFirstSentence("") shouldBe ""
        config.extractFirstSentence("   ") shouldBe "   "
    }

    "ScopeDescriptionRecoveryConfig should have sensible defaults" {
        // Test setup
        val config = ScopeDescriptionRecoveryConfig()

        config.maxLength shouldBe 1000
        config.truncationSuffix shouldBe "..."
    }

    // =====  ScopeDuplicationRecoveryConfig Tests =====

    "ScopeDuplicationRecoveryConfig should enforce valid suffix template with placeholder" {
        // Test setup
        shouldThrow<IllegalArgumentException> {
            ScopeDuplicationRecoveryConfig(
                suffixTemplate = " (copy)", // Invalid - no {number} placeholder
                maxRetryAttempts = 10
            )
        }
    }

    "ScopeDuplicationRecoveryConfig should enforce positive maxRetryAttempts" {
        // Test setup
        shouldThrow<IllegalArgumentException> {
            ScopeDuplicationRecoveryConfig(
                suffixTemplate = " ({number})",
                maxRetryAttempts = 0 // Invalid
            )
        }
    }

    "ScopeDuplicationRecoveryConfig should create valid configuration with proper values" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 5
        )

        config.suffixTemplate shouldBe " ({number})"
        config.maxRetryAttempts shouldBe 5
    }

    "ScopeDuplicationRecoveryConfig generateVariant should create variant with number substitution" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 10
        )

        config.generateVariant("Original Title", 1) shouldBe "Original Title (1)"
        config.generateVariant("Task", 42) shouldBe "Task (42)"
    }

    "ScopeDuplicationRecoveryConfig generateVariant should handle different template formats" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = "_{number}_copy",
            maxRetryAttempts = 10
        )

        config.generateVariant("Document", 3) shouldBe "Document_3_copy"
    }

    "ScopeDuplicationRecoveryConfig generateSuffix should create suffix with number" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " - Copy {number}",
            maxRetryAttempts = 10
        )

        config.generateSuffix(5) shouldBe " - Copy 5"
    }

    "ScopeDuplicationRecoveryConfig generateVariants should create multiple unique variants" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 10
        )

        val variants = config.generateVariants("Base Title", 3)

        variants.size shouldBe 3
        variants shouldBe listOf(
            "Base Title (1)",
            "Base Title (2)",
            "Base Title (3)"
        )
    }

    "ScopeDuplicationRecoveryConfig generateVariants should respect maxRetryAttempts" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 2
        )

        val variants = config.generateVariants("Title", 5) // Request more than max

        variants.size shouldBe 2 // Should be limited to maxRetryAttempts
        variants shouldBe listOf(
            "Title (1)",
            "Title (2)"
        )
    }

    "ScopeDuplicationRecoveryConfig generateVariants should handle zero count request" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 10
        )

        config.generateVariants("Title", 0) shouldBe emptyList<String>()
    }

    "ScopeDuplicationRecoveryConfig should have sensible defaults" {
        // Test setup
        val config = ScopeDuplicationRecoveryConfig()

        config.suffixTemplate shouldBe " ({number})"
        config.maxRetryAttempts shouldBe 10
    }

    // =====  ScopeHierarchyRecoveryConfig Tests =====

    "ScopeHierarchyRecoveryConfig should create valid configuration with proper values" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Consider reorganizing hierarchy",
            maxChildrenGuidance = "Try grouping related items"
        )

        config.maxDepthGuidance shouldBe "Consider reorganizing hierarchy"
        config.maxChildrenGuidance shouldBe "Try grouping related items"
    }

    "ScopeHierarchyRecoveryConfig getDepthGuidance should return configured guidance" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Hierarchy too deep",
            maxChildrenGuidance = "Too many children"
        )

        config.getDepthGuidance(10, 8) shouldBe "Hierarchy too deep"
    }

    "ScopeHierarchyRecoveryConfig getDepthGuidance should include context in message" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Current depth: {currentDepth}, Max allowed: {maxDepth}. Consider restructuring.",
            maxChildrenGuidance = "Too many children"
        )

        val guidance = config.getDepthGuidance(5, 8)
        guidance shouldBe "Current depth: 8, Max allowed: 5. Consider restructuring."
    }

    "ScopeHierarchyRecoveryConfig getChildrenGuidance should return configured guidance" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Hierarchy too deep",
            maxChildrenGuidance = "Too many child scopes"
        )

        config.getChildrenGuidance(5, 7) shouldBe "Too many child scopes"
    }

    "ScopeHierarchyRecoveryConfig getChildrenGuidance should include context in message" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Hierarchy too deep",
            maxChildrenGuidance = "Current children: {currentChildren}, Max allowed: {maxChildren}. Try grouping."
        )

        val guidance = config.getChildrenGuidance(3, 6)
        guidance shouldBe "Current children: 6, Max allowed: 3. Try grouping."
    }

    "ScopeHierarchyRecoveryConfig should handle guidance without placeholders" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Reduce nesting levels",
            maxChildrenGuidance = "Create sub-categories"
        )

        config.getDepthGuidance(3, 5) shouldBe "Reduce nesting levels"
        config.getChildrenGuidance(10, 15) shouldBe "Create sub-categories"
    }

    "ScopeHierarchyRecoveryConfig should have sensible defaults" {
        // Test setup
        val config = ScopeHierarchyRecoveryConfig()

        config.maxDepthGuidance shouldBe "The hierarchy is too deep (maximum {maxDepth} levels, " +
            "currently {currentDepth}). Consider restructuring to reduce nesting."
        config.maxChildrenGuidance shouldBe "Too many child scopes (maximum {maxChildren} allowed, " +
            "currently {currentChildren}). Consider organizing them into logical groups."
    }

    // =====  ScopeRecoveryConfiguration Tests =====

    "ScopeRecoveryConfiguration.Complete should provide access to all configuration types" {
        // Test setup
        val config = ScopeRecoveryConfiguration.Complete(
            title = ScopeTitleRecoveryConfig(),
            description = ScopeDescriptionRecoveryConfig(),
            duplication = ScopeDuplicationRecoveryConfig(),
            hierarchy = ScopeHierarchyRecoveryConfig()
        )

        config.title shouldBe ScopeTitleRecoveryConfig()
        config.description shouldBe ScopeDescriptionRecoveryConfig()
        config.duplication shouldBe ScopeDuplicationRecoveryConfig()
        config.hierarchy shouldBe ScopeHierarchyRecoveryConfig()
    }

    "ScopeRecoveryConfiguration.Complete should be accessible through sealed class polymorphism" {
        // Test setup
        val config: ScopeRecoveryConfiguration = ScopeRecoveryConfiguration.Complete(
            title = ScopeTitleRecoveryConfig(defaultTemplate = "Custom Task"),
            description = ScopeDescriptionRecoveryConfig(maxLength = 500),
            duplication = ScopeDuplicationRecoveryConfig(suffixTemplate = " - Copy {number}"),
            hierarchy = ScopeHierarchyRecoveryConfig(maxDepthGuidance = "Custom depth guidance")
        )

        when (config) {
            is ScopeRecoveryConfiguration.Complete -> {
                config.title.defaultTemplate shouldBe "Custom Task"
                config.description.maxLength shouldBe 500
                config.duplication.suffixTemplate shouldBe " - Copy {number}"
                config.hierarchy.maxDepthGuidance shouldBe "Custom depth guidance"
            }
        }
    }

    "ScopeRecoveryConfiguration.Complete should provide default configuration factory" {
        // Test setup
        val config = ScopeRecoveryConfiguration.default()

        config shouldBe ScopeRecoveryConfiguration.Complete(
            title = ScopeTitleRecoveryConfig(),
            description = ScopeDescriptionRecoveryConfig(),
            duplication = ScopeDuplicationRecoveryConfig(),
            hierarchy = ScopeHierarchyRecoveryConfig()
        )
    }

    "ScopeRecoveryConfiguration should support type-safe access to specific configurations" {
        // Test setup
        val config = ScopeRecoveryConfiguration.Complete(
            title = ScopeTitleRecoveryConfig(defaultTemplate = "Test"),
            description = ScopeDescriptionRecoveryConfig(maxLength = 800),
            duplication = ScopeDuplicationRecoveryConfig(maxRetryAttempts = 5),
            hierarchy = ScopeHierarchyRecoveryConfig()
        )

        config.titleConfig().defaultTemplate shouldBe "Test"
        config.descriptionConfig().maxLength shouldBe 800
        config.duplicationConfig().maxRetryAttempts shouldBe 5
        config.hierarchyConfig().maxDepthGuidance shouldBe "The hierarchy is too deep " +
            "(maximum {maxDepth} levels, currently {currentDepth}). Consider restructuring to reduce nesting."
    }
})
