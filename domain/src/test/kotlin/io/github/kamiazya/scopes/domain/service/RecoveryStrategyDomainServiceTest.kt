package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ScopeError
import io.github.kamiazya.scopes.domain.error.ScopeValidationError
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleViolation
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.RecoveryApproach
import io.github.kamiazya.scopes.domain.service.RecoveryStrategyDomainService
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldContainAll

class RecoveryStrategyDomainServiceTest : StringSpec({

    val service = RecoveryStrategyDomainService()

    "RecoveryStrategyDomainService should be creatable" {
        val service = RecoveryStrategyDomainService()
        service.shouldBeInstanceOf<RecoveryStrategyDomainService>()
    }

    "determineRecoveryStrategy should return DEFAULT_VALUE for empty scope title error" {
        val error = ScopeValidationError.EmptyScopeTitle

        val strategy = service.determineRecoveryStrategy(error)

        strategy shouldBe RecoveryStrategy.DEFAULT_VALUE
    }

    "determineRecoveryStrategy should return TRUNCATE for title too long error" {
        val error = ScopeValidationError.ScopeTitleTooLong(
            maxLength = 50,
            actualLength = 75
        )

        val strategy = service.determineRecoveryStrategy(error)

        strategy shouldBe RecoveryStrategy.TRUNCATE
    }

    "determineRecoveryStrategy should return CLEAN_FORMAT for title contains newline error" {
        val error = ScopeValidationError.ScopeTitleContainsNewline

        val strategy = service.determineRecoveryStrategy(error)

        strategy shouldBe RecoveryStrategy.CLEAN_FORMAT
    }

    "determineRecoveryStrategy should return GENERATE_VARIANTS for duplicate title error" {
        val error = ScopeBusinessRuleViolation.ScopeDuplicateTitle(
            title = "Duplicate Title",
            parentId = ScopeId.from("01234567890123456789012345")
        )

        val strategy = service.determineRecoveryStrategy(error)

        strategy shouldBe RecoveryStrategy.GENERATE_VARIANTS
    }

    "getStrategyApproach should return AUTOMATIC_SUGGESTION for empty title error" {
        val error = ScopeValidationError.EmptyScopeTitle

        val approach = service.getStrategyApproach(error)

        approach shouldBe RecoveryApproach.AUTOMATIC_SUGGESTION
    }

    "getStrategyApproach should return USER_INPUT_REQUIRED for title too long error" {
        val error = ScopeValidationError.ScopeTitleTooLong(
            maxLength = 50,
            actualLength = 75
        )

        val approach = service.getStrategyApproach(error)

        approach shouldBe RecoveryApproach.USER_INPUT_REQUIRED
    }

    "getStrategyApproach should return MANUAL_INTERVENTION for scope error" {
        val error = ScopeError.ScopeNotFound

        val approach = service.getStrategyApproach(error)

        approach shouldBe RecoveryApproach.MANUAL_INTERVENTION
    }

    "isStrategyComplex should return false for simple strategies" {
        service.isStrategyComplex(RecoveryStrategy.DEFAULT_VALUE) shouldBe false
        service.isStrategyComplex(RecoveryStrategy.TRUNCATE) shouldBe false
        service.isStrategyComplex(RecoveryStrategy.CLEAN_FORMAT) shouldBe false
    }

    "isStrategyComplex should return true for complex strategies" {
        service.isStrategyComplex(RecoveryStrategy.GENERATE_VARIANTS) shouldBe true
        service.isStrategyComplex(RecoveryStrategy.RESTRUCTURE_HIERARCHY) shouldBe true
    }
})
