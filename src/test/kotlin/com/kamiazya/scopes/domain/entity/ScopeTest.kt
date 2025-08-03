package com.kamiazya.scopes.domain.entity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ScopeTest :
    FunSpec({
        test("should create scope with valid title") {
            val scope =
                Scope(
                    id = ScopeId.generate(),
                    title = "Test Scope",
                )

            scope.title shouldBe "Test Scope"
            scope.parentId shouldBe null
            scope.description shouldBe null
            scope.metadata shouldBe emptyMap()
        }

        test("should not allow blank title") {
            shouldThrow<IllegalArgumentException> {
                Scope(
                    id = ScopeId.generate(),
                    title = "",
                )
            }

            shouldThrow<IllegalArgumentException> {
                Scope(
                    id = ScopeId.generate(),
                    title = " ",
                )
            }
        }

        test("should create scope with all parameters") {
            val id = ScopeId.generate()
            val parentId = ScopeId.generate()
            val metadata = mapOf("key" to "value")

            val scope =
                Scope(
                    id = id,
                    title = "Test Scope",
                    description = "Test Description",
                    parentId = parentId,
                    metadata = metadata,
                )

            scope.id shouldBe id
            scope.title shouldBe "Test Scope"
            scope.description shouldBe "Test Description"
            scope.parentId shouldBe parentId
            scope.metadata shouldBe metadata
            scope.createdAt shouldNotBe null
            scope.updatedAt shouldNotBe null
        }
    })
