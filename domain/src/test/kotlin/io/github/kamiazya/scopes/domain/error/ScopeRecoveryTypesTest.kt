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
        // RED: This should fail because ScopeTitleRecoveryConfig doesn't exist yet
        shouldThrow<IllegalArgumentException> {
            ScopeTitleRecoveryConfig(
                defaultTemplate = "Untitled Scope",
                maxLength = 0, // Invalid
                truncationSuffix = "..."
            )
        }
    }

    "ScopeTitleRecoveryConfig should enforce non-blank defaultTemplate" {
        // RED: This should fail because ScopeTitleRecoveryConfig doesn't exist yet
        shouldThrow<IllegalArgumentException> {
            ScopeTitleRecoveryConfig(
                defaultTemplate = "", // Invalid
                maxLength = 200,
                truncationSuffix = "..."
            )
        }
    }

    "ScopeTitleRecoveryConfig should create valid configuration with proper values" {
        // RED: This should fail because ScopeTitleRecoveryConfig doesn't exist yet
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
        // RED: This should fail - testing domain behavior
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "New Task",
            maxLength = 200,
            truncationSuffix = "..."
        )
        
        config.generateDefaultTitle() shouldBe "New Task"
    }

    "ScopeTitleRecoveryConfig truncateTitle should truncate long titles properly" {
        // RED: This should fail - testing truncation logic
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
        // RED: This should fail - testing edge case
        val config = ScopeTitleRecoveryConfig(
            defaultTemplate = "Default",
            maxLength = 50,
            truncationSuffix = "..."
        )
        
        val shortTitle = "Short"
        config.truncateTitle(shortTitle) shouldBe shortTitle
    }

    "ScopeTitleRecoveryConfig cleanTitle should remove newlines and excess whitespace" {
        // RED: This should fail - testing title cleaning
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
        // RED: This should fail - testing edge cases
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
        // RED: This should fail - testing default constructor
        val config = ScopeTitleRecoveryConfig()
        
        config.defaultTemplate shouldBe "Untitled Scope"
        config.maxLength shouldBe 200
        config.truncationSuffix shouldBe "..."
    }

    // =====  ScopeDescriptionRecoveryConfig Tests =====

    "ScopeDescriptionRecoveryConfig should enforce positive maxDescriptionLength" {
        // RED: This should fail because ScopeDescriptionRecoveryConfig doesn't exist yet
        shouldThrow<IllegalArgumentException> {
            ScopeDescriptionRecoveryConfig(
                maxLength = -1, // Invalid
                truncationSuffix = "..."
            )
        }
    }

    "ScopeDescriptionRecoveryConfig should create valid configuration with proper values" {
        // RED: This should fail because ScopeDescriptionRecoveryConfig doesn't exist yet
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )
        
        config.maxLength shouldBe 1000
        config.truncationSuffix shouldBe "..."
    }

    "ScopeDescriptionRecoveryConfig truncateDescription should truncate long descriptions properly" {
        // RED: This should fail - testing truncation logic
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
        // RED: This should fail - testing edge case
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 100,
            truncationSuffix = "..."
        )
        
        val shortDescription = "Short description"
        config.truncateDescription(shortDescription) shouldBe shortDescription
    }

    "ScopeDescriptionRecoveryConfig truncateDescription should handle empty descriptions" {
        // RED: This should fail - testing edge case
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 100,
            truncationSuffix = "..."
        )
        
        config.truncateDescription("") shouldBe ""
        config.truncateDescription("   ") shouldBe "   " // Preserves whitespace-only
    }

    "ScopeDescriptionRecoveryConfig extractFirstSentence should extract first sentence" {
        // RED: This should fail - testing domain behavior
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )
        
        val multiSentence = "First sentence. Second sentence. Third sentence."
        config.extractFirstSentence(multiSentence) shouldBe "First sentence"
    }

    "ScopeDescriptionRecoveryConfig extractFirstSentence should handle descriptions without periods" {
        // RED: This should fail - testing edge case
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )
        
        val noPeriod = "This has no period"
        config.extractFirstSentence(noPeriod) shouldBe noPeriod
    }

    "ScopeDescriptionRecoveryConfig extractFirstSentence should handle empty descriptions" {
        // RED: This should fail - testing edge case
        val config = ScopeDescriptionRecoveryConfig(
            maxLength = 1000,
            truncationSuffix = "..."
        )
        
        config.extractFirstSentence("") shouldBe ""
        config.extractFirstSentence("   ") shouldBe "   "
    }

    "ScopeDescriptionRecoveryConfig should have sensible defaults" {
        // RED: This should fail - testing default constructor
        val config = ScopeDescriptionRecoveryConfig()
        
        config.maxLength shouldBe 1000
        config.truncationSuffix shouldBe "..."
    }

    // =====  ScopeDuplicationRecoveryConfig Tests =====

    "ScopeDuplicationRecoveryConfig should enforce valid suffix template with placeholder" {
        // RED: This should fail because ScopeDuplicationRecoveryConfig doesn't exist yet
        shouldThrow<IllegalArgumentException> {
            ScopeDuplicationRecoveryConfig(
                suffixTemplate = " (copy)", // Invalid - no {number} placeholder
                maxRetryAttempts = 10
            )
        }
    }

    "ScopeDuplicationRecoveryConfig should enforce positive maxRetryAttempts" {
        // RED: This should fail because ScopeDuplicationRecoveryConfig doesn't exist yet
        shouldThrow<IllegalArgumentException> {
            ScopeDuplicationRecoveryConfig(
                suffixTemplate = " ({number})",
                maxRetryAttempts = 0 // Invalid
            )
        }
    }

    "ScopeDuplicationRecoveryConfig should create valid configuration with proper values" {
        // RED: This should fail because ScopeDuplicationRecoveryConfig doesn't exist yet
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 5
        )
        
        config.suffixTemplate shouldBe " ({number})"
        config.maxRetryAttempts shouldBe 5
    }

    "ScopeDuplicationRecoveryConfig generateVariant should create variant with number substitution" {
        // RED: This should fail - testing domain behavior
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 10
        )
        
        config.generateVariant("Original Title", 1) shouldBe "Original Title (1)"
        config.generateVariant("Task", 42) shouldBe "Task (42)"
    }

    "ScopeDuplicationRecoveryConfig generateVariant should handle different template formats" {
        // RED: This should fail - testing flexible templates
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = "_{number}_copy",
            maxRetryAttempts = 10
        )
        
        config.generateVariant("Document", 3) shouldBe "Document_3_copy"
    }

    "ScopeDuplicationRecoveryConfig generateSuffix should create suffix with number" {
        // RED: This should fail - testing domain behavior
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " - Copy {number}",
            maxRetryAttempts = 10
        )
        
        config.generateSuffix(5) shouldBe " - Copy 5"
    }

    "ScopeDuplicationRecoveryConfig generateVariants should create multiple unique variants" {
        // RED: This should fail - testing bulk generation
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
        // RED: This should fail - testing constraint enforcement
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
        // RED: This should fail - testing edge case
        val config = ScopeDuplicationRecoveryConfig(
            suffixTemplate = " ({number})",
            maxRetryAttempts = 10
        )
        
        config.generateVariants("Title", 0) shouldBe emptyList<String>()
    }

    "ScopeDuplicationRecoveryConfig should have sensible defaults" {
        // RED: This should fail - testing default constructor
        val config = ScopeDuplicationRecoveryConfig()
        
        config.suffixTemplate shouldBe " ({number})"
        config.maxRetryAttempts shouldBe 10
    }

    // =====  ScopeHierarchyRecoveryConfig Tests =====

    "ScopeHierarchyRecoveryConfig should create valid configuration with proper values" {
        // RED: This should fail because ScopeHierarchyRecoveryConfig doesn't exist yet
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Consider reorganizing hierarchy",
            maxChildrenGuidance = "Try grouping related items"
        )
        
        config.maxDepthGuidance shouldBe "Consider reorganizing hierarchy"
        config.maxChildrenGuidance shouldBe "Try grouping related items"
    }

    "ScopeHierarchyRecoveryConfig getDepthGuidance should return configured guidance" {
        // RED: This should fail - testing domain behavior
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Hierarchy too deep",
            maxChildrenGuidance = "Too many children"
        )
        
        config.getDepthGuidance(10, 8) shouldBe "Hierarchy too deep"
    }

    "ScopeHierarchyRecoveryConfig getDepthGuidance should include context in message" {
        // RED: This should fail - testing contextual guidance
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Current depth: {currentDepth}, Max allowed: {maxDepth}. Consider restructuring.",
            maxChildrenGuidance = "Too many children"
        )
        
        val guidance = config.getDepthGuidance(5, 8)
        guidance shouldBe "Current depth: 8, Max allowed: 5. Consider restructuring."
    }

    "ScopeHierarchyRecoveryConfig getChildrenGuidance should return configured guidance" {
        // RED: This should fail - testing domain behavior
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Hierarchy too deep",
            maxChildrenGuidance = "Too many child scopes"
        )
        
        config.getChildrenGuidance(5, 7) shouldBe "Too many child scopes"
    }

    "ScopeHierarchyRecoveryConfig getChildrenGuidance should include context in message" {
        // RED: This should fail - testing contextual guidance
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Hierarchy too deep",
            maxChildrenGuidance = "Current children: {currentChildren}, Max allowed: {maxChildren}. Try grouping."
        )
        
        val guidance = config.getChildrenGuidance(3, 6)
        guidance shouldBe "Current children: 6, Max allowed: 3. Try grouping."
    }

    "ScopeHierarchyRecoveryConfig should handle guidance without placeholders" {
        // RED: This should fail - testing simple guidance messages
        val config = ScopeHierarchyRecoveryConfig(
            maxDepthGuidance = "Reduce nesting levels",
            maxChildrenGuidance = "Create sub-categories"
        )
        
        config.getDepthGuidance(3, 5) shouldBe "Reduce nesting levels"
        config.getChildrenGuidance(10, 15) shouldBe "Create sub-categories"
    }

    "ScopeHierarchyRecoveryConfig should have sensible defaults" {
        // RED: This should fail - testing default constructor
        val config = ScopeHierarchyRecoveryConfig()
        
        config.maxDepthGuidance shouldBe "The hierarchy is too deep (maximum {maxDepth} levels). " +
            "Consider restructuring to reduce nesting."
        config.maxChildrenGuidance shouldBe "Too many child scopes (maximum {maxChildren} allowed). " +
            "Consider organizing them into logical groups."
    }

    // =====  ScopeRecoveryConfiguration Tests =====

    "ScopeRecoveryConfiguration.Complete should provide access to all configuration types" {
        // RED: This should fail because ScopeRecoveryConfiguration doesn't exist yet
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
        // RED: This should fail - testing sealed class hierarchy
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
        // RED: This should fail - testing factory method
        val config = ScopeRecoveryConfiguration.default()
        
        config shouldBe ScopeRecoveryConfiguration.Complete(
            title = ScopeTitleRecoveryConfig(),
            description = ScopeDescriptionRecoveryConfig(),
            duplication = ScopeDuplicationRecoveryConfig(),
            hierarchy = ScopeHierarchyRecoveryConfig()
        )
    }

    "ScopeRecoveryConfiguration should support type-safe access to specific configurations" {
        // RED: This should fail - testing accessor methods
        val config = ScopeRecoveryConfiguration.Complete(
            title = ScopeTitleRecoveryConfig(defaultTemplate = "Test"),
            description = ScopeDescriptionRecoveryConfig(maxLength = 800),
            duplication = ScopeDuplicationRecoveryConfig(maxRetryAttempts = 5),
            hierarchy = ScopeHierarchyRecoveryConfig()
        )
        
        config.titleConfig().defaultTemplate shouldBe "Test"
        config.descriptionConfig().maxLength shouldBe 800
        config.duplicationConfig().maxRetryAttempts shouldBe 5
        config.hierarchyConfig().maxDepthGuidance shouldBe "The hierarchy is too deep (maximum {maxDepth} levels). " +
            "Consider restructuring to reduce nesting."
    }
})
