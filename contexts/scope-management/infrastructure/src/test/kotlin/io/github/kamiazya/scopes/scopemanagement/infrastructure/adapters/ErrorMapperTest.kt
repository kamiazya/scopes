package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.*
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
                it("should map ScopeInputError.IdError.Blank to InvalidId") {
                    val domainError = ScopeInputError.IdError.Blank("")
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidId>()
                    result.id shouldBe ""
                    result.expectedFormat shouldBe "Non-empty ULID format"
                }

                it("should map ScopeInputError.IdError.InvalidFormat to InvalidId") {
                    val domainError = ScopeInputError.IdError.InvalidFormat(
                        attemptedValue = "invalid-id",
                        formatType = ScopeInputError.IdError.InvalidFormat.IdFormatType.ULID,
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidId>()
                    result.id shouldBe "invalid-id"
                    result.expectedFormat shouldBe "ULID format"
                }

                it("should map ScopeInputError.TitleError.Empty to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.Empty("")
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map ScopeInputError.TitleError.TooShort to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.TooShort(attemptedValue = "a", minimumLength = 3)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "a"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                }

                it("should map ScopeInputError.TitleError.TooLong to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.TooLong(attemptedValue = "very long title", maximumLength = 10)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "very long title"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooLong>()
                }

                it("should map ScopeInputError.TitleError.ContainsProhibitedCharacters to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.ContainsProhibitedCharacters(
                        attemptedValue = "title<>",
                        prohibitedCharacters = listOf('<', '>'),
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "title<>"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.InvalidCharacters>()
                }

                it("should map ScopeInputError.DescriptionError.TooLong to InvalidDescription") {
                    val domainError = ScopeInputError.DescriptionError.TooLong(attemptedValue = "very long description", maximumLength = 10)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidDescription>()
                    result.descriptionText shouldBe "very long description"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.DescriptionValidationFailure.TooLong>()
                }
            }

            context("Alias validation errors") {
                it("should map ScopeInputError.AliasError.Empty to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.Empty(attemptedValue = "")
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe ""
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map ScopeInputError.AliasError.TooShort to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.TooShort(attemptedValue = "a", minimumLength = 3)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "a"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                }

                it("should map ScopeInputError.AliasError.TooLong to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.TooLong(attemptedValue = "very long alias", maximumLength = 10)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "very long alias"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooLong>()
                }

                it("should map ScopeInputError.AliasError.InvalidFormat to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.InvalidFormat(
                        attemptedValue = "invalid-alias",
                        patternType = ScopeInputError.AliasError.InvalidFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    result.title shouldBe "invalid-alias"
                    result.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.InvalidCharacters>()
                }
            }

            context("Business rule violations") {
                it("should map ScopeError.NotFound to BusinessError.NotFound") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.NotFound(scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    result.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.ParentNotFound to BusinessError.NotFound") {
                    val parentId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.ParentNotFound(parentId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    result.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.AlreadyDeleted to BusinessError.AlreadyDeleted") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.AlreadyDeleted(scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.AlreadyDeleted>()
                    result.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.AlreadyArchived to BusinessError.ArchivedScope") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.AlreadyArchived(scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.ArchivedScope>()
                    result.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.DuplicateTitle to BusinessError.DuplicateTitle") {
                    val parentId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()
                    val domainError = ScopeError.DuplicateTitle("My Title", parentId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateTitle>()
                    result.title shouldBe "My Title"
                    result.parentId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.VersionMismatch to SystemError.ConcurrentModification") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.VersionMismatch(scopeId, expectedVersion = 1, actualVersion = 2)
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ConcurrentModification>()
                    result.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                    result.expectedVersion shouldBe 1L
                    result.actualVersion shouldBe 2L
                }
            }

            context("Structured errors") {
                it("should map ScopesError.NotFound with alias type to BusinessError.AliasNotFound") {
                    val domainError = ScopesError.NotFound(
                        entityType = "Scope",
                        identifierType = "alias",
                        identifier = "my-alias",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.AliasNotFound>()
                    result.alias shouldBe "my-alias"
                }

                it("should map ScopesError.NotFound with non-alias type to BusinessError.NotFound") {
                    val domainError = ScopesError.NotFound(
                        entityType = "Scope",
                        identifierType = "id",
                        identifier = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    result.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopesError.AlreadyExists to BusinessError.DuplicateAlias") {
                    val domainError = ScopesError.AlreadyExists(
                        entityType = "AspectDefinition",
                        identifier = "priority",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateAlias>()
                    result.alias shouldBe "priority"
                }

                it("should map ScopesError.SystemError to SystemError.ServiceUnavailable") {
                    val domainError = ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE,
                        service = "database",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    val result = contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                    result.service shouldBe "database"
                }
            }

            context("Unmapped errors") {
                it("should handle unmapped errors and log them") {
                    // Create a custom error that's not handled by the mapper - use a real ScopesError subclass
                    val domainError = ScopesError.RepositoryError(
                        repositoryName = "TestRepository",
                        operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                        cause = RuntimeException("test"),
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    // Should get the fallback ServiceUnavailable error
                    val result = contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                    result.service shouldBe "scope-management"
                }
            }
        }
    })
