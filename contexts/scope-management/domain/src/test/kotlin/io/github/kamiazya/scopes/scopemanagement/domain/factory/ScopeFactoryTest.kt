package io.github.kamiazya.scopes.scopemanagement.domain.factory

import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

class ScopeFactoryTest :
    DescribeSpec({
        describe("ScopeFactory") {
            val hierarchyService = mockk<ScopeHierarchyService>()
            val scopeRepository = mockk<ScopeRepository>()
            val factory = ScopeFactory(hierarchyService, scopeRepository)

            describe("createScope") {
                describe("when parent does not exist") {
                    it("should return ParentNotFound error with correct scopeId and parentId") {
                        // Arrange
                        val parentId = ScopeId.generate()
                        val title = "Child Scope"
                        val hierarchyPolicy = HierarchyPolicy.default()

                        // Mock repository to return false for parent existence check
                        coEvery { scopeRepository.existsById(parentId) } returns false.right()

                        // Act
                        val result = factory.createScope(
                            title = title,
                            parentId = parentId,
                            hierarchyPolicy = hierarchyPolicy,
                        )

                        // Assert
                        result.shouldBeLeft()
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<ScopeHierarchyError.ParentNotFound>()

                        // Verify that scopeId is different from parentId
                        error.scopeId shouldNotBe parentId
                        error.parentId shouldBe parentId

                        // Verify that scopeId is a valid generated ID
                        error.scopeId.shouldBeInstanceOf<ScopeId>()
                    }
                }
            }
        }
    })
