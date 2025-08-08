package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.RecoveryApproach
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldContainAll

/**
 * Test for RecoveryStrategyDomainService.
 * 
 * This test suite verifies that the domain service correctly determines recovery
 * strategies and approaches based purely on domain error types, without any
 * application or infrastructure concerns.
 */
class RecoveryStrategyDomainServiceTest : StringSpec({

    val service = RecoveryStrategyDomainService()

    // ===== SERVICE CREATION TEST =====

    "RecoveryStrategyDomainService should be creatable" {
        val service = RecoveryStrategyDomainService()
        service.shouldBeInstanceOf<RecoveryStrategyDomainService>()
    }

    // ===== RECOVERY STRATEGY DETERMINATION TESTS =====

    "determineRecoveryStrategy should return DEFAULT_VALUE for empty title error" {
        // Arrange
        val error = DomainError.ScopeValidationError.EmptyScopeTitle

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.DEFAULT_VALUE
    }

    "determineRecoveryStrategy should return DEFAULT_VALUE for title too short error" {
        // Arrange
        val error = DomainError.ScopeValidationError.ScopeTitleTooShort

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.DEFAULT_VALUE
    }

    "determineRecoveryStrategy should return TRUNCATE for title too long error" {
        // Arrange
        val error = DomainError.ScopeValidationError.ScopeTitleTooLong(
            maxLength = 200,
            actualLength = 300
        )

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.TRUNCATE
    }

    "determineRecoveryStrategy should return CLEAN_FORMAT for title contains newline error" {
        // Arrange
        val error = DomainError.ScopeValidationError.ScopeTitleContainsNewline

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.CLEAN_FORMAT
    }

    "determineRecoveryStrategy should return TRUNCATE for description too long error" {
        // Arrange
        val error = DomainError.ScopeValidationError.ScopeDescriptionTooLong(
            maxLength = 1000,
            actualLength = 2000
        )

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.TRUNCATE
    }

    "determineRecoveryStrategy should return GENERATE_VARIANTS for duplicate title error" {
        // Arrange
        val error = DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(
            title = "Duplicate Title",
            parentId = ScopeId.generate()
        )

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.GENERATE_VARIANTS
    }

    "determineRecoveryStrategy should return RESTRUCTURE_HIERARCHY for max depth exceeded error" {
        // Arrange
        val error = DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(
            maxDepth = 10,
            actualDepth = 12
        )

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.RESTRUCTURE_HIERARCHY
    }

    "determineRecoveryStrategy should return RESTRUCTURE_HIERARCHY for max children exceeded error" {
        // Arrange
        val error = DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(
            maxChildren = 100,
            actualChildren = 150
        )

        // Act
        val strategy = service.determineRecoveryStrategy(error)

        // Assert
        strategy shouldBe RecoveryStrategy.RESTRUCTURE_HIERARCHY
    }

    // ===== RECOVERY APPROACH DETERMINATION TESTS =====

    "getStrategyApproach should return AUTOMATIC_SUGGESTION for empty title error" {
        // Arrange
        val error = DomainError.ScopeValidationError.EmptyScopeTitle

        // Act
        val approach = service.getStrategyApproach(error)

        // Assert
        approach shouldBe RecoveryApproach.AUTOMATIC_SUGGESTION
    }

    "getStrategyApproach should return AUTOMATIC_SUGGESTION for title too short error" {
        // Arrange
        val error = DomainError.ScopeValidationError.ScopeTitleTooShort

        // Act
        val approach = service.getStrategyApproach(error)

        // Assert
        approach shouldBe RecoveryApproach.AUTOMATIC_SUGGESTION
    }

    "getStrategyApproach should return USER_INPUT_REQUIRED for title too long error" {
        // Arrange
        val error = DomainError.ScopeValidationError.ScopeTitleTooLong(200, 300)

        // Act
        val approach = service.getStrategyApproach(error)

        // Assert
        approach shouldBe RecoveryApproach.USER_INPUT_REQUIRED
    }

    "getStrategyApproach should return USER_INPUT_REQUIRED for duplicate title error" {
        // Arrange
        val error = DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(
            title = "Duplicate Title",
            parentId = ScopeId.generate()
        )

        // Act
        val approach = service.getStrategyApproach(error)

        // Assert
        approach shouldBe RecoveryApproach.USER_INPUT_REQUIRED
    }

    "getStrategyApproach should return MANUAL_INTERVENTION for max depth exceeded error" {
        // Arrange
        val error = DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(10, 12)

        // Act
        val approach = service.getStrategyApproach(error)

        // Assert
        approach shouldBe RecoveryApproach.MANUAL_INTERVENTION
    }

    "getStrategyApproach should return MANUAL_INTERVENTION for max children exceeded error" {
        // Arrange
        val error = DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(100, 150)

        // Act
        val approach = service.getStrategyApproach(error)

        // Assert
        approach shouldBe RecoveryApproach.MANUAL_INTERVENTION
    }

    // ===== STRATEGY COMPLEXITY ASSESSMENT TESTS =====

    "isStrategyComplex should return false for simple strategies" {
        // Arrange & Act & Assert
        service.isStrategyComplex(RecoveryStrategy.DEFAULT_VALUE) shouldBe false
        service.isStrategyComplex(RecoveryStrategy.TRUNCATE) shouldBe false
        service.isStrategyComplex(RecoveryStrategy.CLEAN_FORMAT) shouldBe false
    }

    "isStrategyComplex should return true for complex strategies" {
        // Arrange & Act & Assert
        service.isStrategyComplex(RecoveryStrategy.GENERATE_VARIANTS) shouldBe true
        service.isStrategyComplex(RecoveryStrategy.RESTRUCTURE_HIERARCHY) shouldBe true
    }

    // ===== CONSISTENCY AND INVARIANTS TESTS =====

    "service methods should be consistent for the same error type" {
        // Arrange
        val error = DomainError.ScopeValidationError.EmptyScopeTitle

        // Act
        val strategy = service.determineRecoveryStrategy(error)
        val approach = service.getStrategyApproach(error)
        val isComplex = service.isStrategyComplex(strategy)

        // Assert
        strategy shouldBe RecoveryStrategy.DEFAULT_VALUE
        approach shouldBe RecoveryApproach.AUTOMATIC_SUGGESTION
        isComplex shouldBe false
    }

    "all RecoveryStrategy values should have known complexity mapping" {
        // Arrange & Act & Assert
        RecoveryStrategy.values().forEach { strategy ->
            // Should not throw exception - all strategies must have complexity mapping
            val isComplex = service.isStrategyComplex(strategy)
            // isComplex can be true or false, but method should not fail
        }
    }

    "all DomainError types should have recovery strategy mapping" {
        // Arrange
        val errors = listOf(
            DomainError.ScopeValidationError.EmptyScopeTitle,
            DomainError.ScopeValidationError.ScopeTitleTooShort,
            DomainError.ScopeValidationError.ScopeTitleTooLong(200, 300),
            DomainError.ScopeValidationError.ScopeTitleContainsNewline,
            DomainError.ScopeValidationError.ScopeDescriptionTooLong(1000, 1500),
            DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle(
                "Duplicate", ScopeId.generate()
            ),
            DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(10, 12),
            DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(100, 150)
        )

        // Act & Assert
        errors.forEach { error ->
            val strategy = service.determineRecoveryStrategy(error)
            val approach = service.getStrategyApproach(error)
            
            // Verify strategy and approach are valid
            RecoveryStrategy.values().shouldContainAll(listOf(strategy))
            RecoveryApproach.values().shouldContainAll(listOf(approach))
        }
    }
})
