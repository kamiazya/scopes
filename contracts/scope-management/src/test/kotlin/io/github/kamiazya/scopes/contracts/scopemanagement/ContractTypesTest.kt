package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class ContractTypesTest :
    DescribeSpec({
        describe("Contract Types") {
            describe("Commands") {
                it("CreateScopeCommand should use primitive types") {
                    val command = CreateScopeCommand(
                        title = "Test Scope",
                        description = "Test Description",
                        parentId = "01HX3BQXYZ123456789ABCDEF",
                        generateAlias = true,
                        customAlias = "test-alias",
                    )

                    command.title shouldBe "Test Scope"
                    command.parentId shouldBe "01HX3BQXYZ123456789ABCDEF"
                }

                it("UpdateScopeCommand should use primitive types") {
                    val command = UpdateScopeCommand(
                        id = "01HX3BQXYZ123456789ABCDEF",
                        title = "Updated Title",
                        description = "Updated Description",
                        parentId = "01HX3BQXYZ123456789ABCDEG",
                    )

                    command.id shouldBe "01HX3BQXYZ123456789ABCDEF"
                    command.title shouldBe "Updated Title"
                }

                it("DeleteScopeCommand should use primitive types") {
                    val command = DeleteScopeCommand(
                        id = "01HX3BQXYZ123456789ABCDEF",
                    )

                    command.id shouldBe "01HX3BQXYZ123456789ABCDEF"
                }
            }

            describe("Queries") {
                it("GetScopeQuery should use primitive types") {
                    val query = GetScopeQuery(
                        id = "01HX3BQXYZ123456789ABCDEF",
                    )

                    query.id shouldBe "01HX3BQXYZ123456789ABCDEF"
                }

                it("GetChildrenQuery should use primitive types") {
                    val query = GetChildrenQuery(
                        parentId = "01HX3BQXYZ123456789ABCDEF",
                    )

                    query.parentId shouldBe "01HX3BQXYZ123456789ABCDEF"
                }
            }

            describe("Results") {
                val now = Clock.System.now()

                it("CreateScopeResult should contain all necessary fields") {
                    val result = CreateScopeResult(
                        id = "01HX3BQXYZ123456789ABCDEF",
                        title = "Created Scope",
                        description = "Description",
                        parentId = null,
                        canonicalAlias = "created-scope",
                        createdAt = now,
                        updatedAt = now,
                    )

                    result.id shouldBe "01HX3BQXYZ123456789ABCDEF"
                    result.canonicalAlias shouldBe "created-scope"
                }

                it("UpdateScopeResult should contain all necessary fields") {
                    val result = UpdateScopeResult(
                        id = "01HX3BQXYZ123456789ABCDEF",
                        title = "Updated Scope",
                        description = "Updated Description",
                        parentId = null,
                        canonicalAlias = "updated-scope",
                        createdAt = now,
                        updatedAt = now,
                    )

                    result.id shouldBe "01HX3BQXYZ123456789ABCDEF"
                    result.canonicalAlias shouldBe "updated-scope"
                }

                it("ScopeResult should contain all necessary fields") {
                    val result = ScopeResult(
                        id = "01HX3BQXYZ123456789ABCDEF",
                        title = "Test Scope",
                        description = "Description",
                        parentId = null,
                        canonicalAlias = "test-scope",
                        createdAt = now,
                        updatedAt = now,
                        isArchived = false,
                        aspects = mapOf("category" to listOf("test")),
                    )

                    result.id shouldBe "01HX3BQXYZ123456789ABCDEF"
                    result.isArchived shouldBe false
                    result.aspects["category"] shouldBe listOf("test")
                }
            }

            describe("Errors") {
                it("InputError types should have proper structure") {
                    val idError = ScopeContractError.InputError.InvalidId("bad-id", "ULID format")
                    idError.id shouldBe "bad-id"
                    idError.expectedFormat shouldBe "ULID format"

                    val titleError = ScopeContractError.InputError.InvalidTitle(
                        "",
                        ScopeContractError.TitleValidationFailure.Empty,
                    )
                    titleError.title shouldBe ""
                    titleError.validationFailure shouldBe ScopeContractError.TitleValidationFailure.Empty
                }

                it("BusinessError types should have proper structure") {
                    val notFoundError = ScopeContractError.BusinessError.NotFound("01HX3BQXYZ123456789ABCDEF")
                    notFoundError.scopeId shouldBe "01HX3BQXYZ123456789ABCDEF"

                    val duplicateError = ScopeContractError.BusinessError.DuplicateTitle("Duplicate", null, "01HX3BQXYZ123456789ABCDEG")
                    duplicateError.title shouldBe "Duplicate"
                    duplicateError.parentId shouldBe null
                    duplicateError.existingScopeId shouldBe "01HX3BQXYZ123456789ABCDEG"

                    val hierarchyError = ScopeContractError.BusinessError.HierarchyViolation(
                        ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                            scopeId = "01HX3BQXYZ123456789ABCDEF",
                            attemptedDepth = 10,
                            maximumDepth = 5,
                        ),
                    )
                    val violation = hierarchyError.violation as ScopeContractError.HierarchyViolationType.MaxDepthExceeded
                    violation.scopeId shouldBe "01HX3BQXYZ123456789ABCDEF"
                    violation.attemptedDepth shouldBe 10
                    violation.maximumDepth shouldBe 5
                }

                it("SystemError types should have proper structure") {
                    val unavailableError = ScopeContractError.SystemError.ServiceUnavailable("database")
                    unavailableError.service shouldBe "database"

                    val timeoutError = ScopeContractError.SystemError.Timeout("createScope", 5.seconds)
                    timeoutError.operation shouldBe "createScope"
                    timeoutError.timeout shouldBe 5.seconds
                }
            }
        }
    })
