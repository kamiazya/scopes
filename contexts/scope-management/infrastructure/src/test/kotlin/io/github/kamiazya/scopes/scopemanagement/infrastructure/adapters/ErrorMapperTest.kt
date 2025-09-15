package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk

class ErrorMapperTest :
    DescribeSpec({
        val mockLogger = mockk<Logger>(relaxed = true)
        val errorMapper = ErrorMapper(mockLogger)

        describe("ErrorMapper") {
            context("Input validation errors") {
                it("should map ScopeInputError.IdError.EmptyId to InvalidId") {
                    val domainError = ScopeInputError.IdError.EmptyId
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidId>()
                    result.id shouldBe ""
                    result.expectedFormat shouldBe "Non-empty ULID format"
                }

                it("should map ScopeInputError.IdError.InvalidIdFormat to InvalidId") {
                    val domainError = ScopeInputError.IdError.InvalidIdFormat(
                        id = "invalid-id",
                        expectedFormat = ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID,
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidId>()
                    result.id shouldBe "invalid-id"
                    result.expectedFormat shouldBe "ULID format"
                }

                it("should map ScopeInputError.TitleError.EmptyTitle to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.EmptyTitle
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map ScopeInputError.TitleError.TitleTooShort to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.TitleTooShort(minLength = 3)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                    failure.minimumLength shouldBe 3
                }

                it("should map ScopeInputError.TitleError.TitleTooLong to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.TitleTooLong(maxLength = 100)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooLong>()
                    failure.maximumLength shouldBe 100
                }

                it("should map ScopeInputError.TitleError.InvalidTitleFormat to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.InvalidTitleFormat(title = "Title@#$")
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "Title@#$"
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.InvalidCharacters>()
                    failure.prohibitedCharacters shouldBe emptyList<Char>()
                }

                it("should map ScopeInputError.DescriptionError.DescriptionTooLong to InvalidDescription") {
                    val domainError = ScopeInputError.DescriptionError.DescriptionTooLong(maxLength = 500)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidDescription>()
                    result.descriptionText shouldBe ""
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.DescriptionValidationFailure.TooLong>()
                    failure.maximumLength shouldBe 500
                }

                it("should map ScopeInputError.AliasError.EmptyAlias to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.EmptyAlias
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map ScopeInputError.AliasError.AliasTooShort to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.AliasTooShort(minLength = 3)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                    failure.minimumLength shouldBe 3
                }

                it("should map ScopeInputError.AliasError.AliasTooLong to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.AliasTooLong(maxLength = 50)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooLong>()
                    failure.maximumLength shouldBe 50
                }

                it("should map ScopeInputError.AliasError.InvalidAliasFormat to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.InvalidAliasFormat(
                        alias = "invalid@alias",
                        expectedPattern = ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "invalid@alias"
                    val failure = result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.InvalidCharacters>()
                    failure.prohibitedCharacters shouldBe emptyList<Char>()
                }
            }

            context("Business logic errors") {
                it("should map ScopeError.NotFound to BusinessError.NotFound") {
                    val scopeId = ScopeId.generate()
                    val domainError = ScopeError.NotFound(scopeId = scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    result.scopeId shouldBe scopeId.value
                }

                it("should map ScopeError.DuplicateTitle to BusinessError.DuplicateTitle") {
                    val domainError = ScopeError.DuplicateTitle(
                        title = "Duplicate Title",
                        parentId = null,
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateTitle>()
                    result.title shouldBe "Duplicate Title"
                    result.parentId shouldBe null
                }

                it("should map ScopeError.AlreadyDeleted to BusinessError.AlreadyDeleted") {
                    val scopeId = ScopeId.generate()
                    val domainError = ScopeError.AlreadyDeleted(scopeId = scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.AlreadyDeleted>()
                    result.scopeId shouldBe scopeId.value
                }

                it("should map ScopeError.AlreadyArchived to BusinessError.ArchivedScope") {
                    val scopeId = ScopeId.generate()
                    val domainError = ScopeError.AlreadyArchived(scopeId = scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.ArchivedScope>()
                    result.scopeId shouldBe scopeId.value
                }

                it("should map ScopeError.NotArchived to BusinessError.NotArchived") {
                    val scopeId = ScopeId.generate()
                    val domainError = ScopeError.NotArchived(scopeId = scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotArchived>()
                    result.scopeId shouldBe scopeId.value
                }
            }

            context("Hierarchy errors - unified error model") {
                it("should map ScopesError.Conflict with HAS_DEPENDENCIES to HasChildren") {
                    val parentId = "parent-123"
                    val domainError = ScopesError.Conflict(
                        resourceType = "Scope",
                        resourceId = parentId,
                        conflictType = ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES,
                        details = mapOf("usage_count" to "5"),
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.HasChildren>()
                    result.scopeId shouldBe parentId
                    result.childrenCount shouldBe null
                }

                it("should map ScopesError.InvalidOperation with INVALID_STATE to ArchivedScope") {
                    val scopeId = "scope-123"
                    val domainError = ScopesError.InvalidOperation(
                        operation = "update",
                        entityType = "Scope",
                        entityId = scopeId,
                        reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.ArchivedScope>()
                    result.scopeId shouldBe scopeId
                }
            }

            context("System errors") {
                it("should map ScopesError.ConcurrencyError to SystemError.ConcurrentModification") {
                    val domainError = ScopesError.ConcurrencyError(
                        aggregateId = "scope-123",
                        aggregateType = "Scope",
                        expectedVersion = 1,
                        actualVersion = 2,
                        operation = "update",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ConcurrentModification>()
                    result.scopeId shouldBe "scope-123"
                    result.expectedVersion shouldBe 1L
                    result.actualVersion shouldBe 2L
                }

                it("should map ScopesError.SystemError to ServiceUnavailable") {
                    val domainError = ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE,
                        service = "scope-management",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                    result.service shouldBe "scope-management"
                }
            }

            context("Alias errors - unified error model") {
                it("should map ScopesError.AlreadyExists to BusinessError.DuplicateAlias") {
                    val domainError = ScopesError.AlreadyExists(
                        entityType = "Alias",
                        identifier = "duplicate-alias",
                        identifierType = "alias",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateAlias>()
                    result.alias shouldBe "duplicate-alias"
                }

                it("should map ScopesError.NotFound with alias type to BusinessError.AliasNotFound") {
                    val domainError = ScopesError.NotFound(
                        entityType = "Scope",
                        identifier = "missing-alias",
                        identifierType = "alias",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.AliasNotFound>()
                    result.alias shouldBe "missing-alias"
                }

                it("should map ScopesError.NotFound with id type to BusinessError.NotFound") {
                    val domainError = ScopesError.NotFound(
                        entityType = "Scope",
                        identifier = "missing-scope-id",
                        identifierType = "id",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    result.scopeId shouldBe "missing-scope-id"
                }
            }

            context("System errors - fallback") {
                it("should map repository errors to ServiceUnavailable") {
                    val domainError = ScopesError.RepositoryError(
                        repositoryName = "TestRepository",
                        operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                        entityType = "Test",
                        entityId = "test-123",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                    result.service shouldBe "scope-management"
                }
            }
        }
    })
