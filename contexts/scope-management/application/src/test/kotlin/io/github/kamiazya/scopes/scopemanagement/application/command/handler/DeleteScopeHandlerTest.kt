package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Simple unit test for the DeleteScopeHandler focusing on the validation logic
 * that was added based on AI review feedback.
 *
 * These tests verify that the critical validation from the Gemini AI review
 * is working correctly: scopes with children cannot be deleted.
 */
class DeleteScopeHandlerTest : DescribeSpec({
    describe("DeleteScopeHandler validation logic") {
        context("ScopeHierarchyService validation") {
            it("should reject deletion when scope has children") {
                // Given
                val scopeId = ScopeId.generate()
                val childCount = 2
                val hierarchyService = ScopeHierarchyService()

                // When - Validate deletion with children present
                val result = hierarchyService.validateDeletion(scopeId, childCount)

                // Then - Should return HasChildren error
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error.shouldBeInstanceOf<ScopeError.HasChildren>()
                error.scopeId shouldBe scopeId
                error.childCount shouldBe childCount
            }

            it("should allow deletion when scope has no children") {
                // Given
                val scopeId = ScopeId.generate()
                val childCount = 0
                val hierarchyService = ScopeHierarchyService()

                // When - Validate deletion with no children
                val result = hierarchyService.validateDeletion(scopeId, childCount)

                // Then - Should succeed
                result.shouldBeRight()
            }
        }
    }
})