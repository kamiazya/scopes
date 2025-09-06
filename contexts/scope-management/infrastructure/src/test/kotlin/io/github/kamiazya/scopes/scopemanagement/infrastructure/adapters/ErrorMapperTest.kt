package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.*
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.datetime.Clock

class ErrorMapperTest :
    DescribeSpec({
        val mockLogger = mockk<Logger>(relaxed = true)
        val errorMapper = ErrorMapper(mockLogger)

        describe("ErrorMapper") {
            context("Input validation errors") {
                it("should map ScopeInputError.IdError.Blank to InvalidId") {
                    val domainError = ScopeInputError.IdError.Blank(Clock.System.now(), "")
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidId>()
                    contractError as ScopeContractError.InputError.InvalidId
                    contractError.id shouldBe ""
                    contractError.expectedFormat shouldBe "Non-empty ULID format"
                }

                it("should map ScopeInputError.IdError.InvalidFormat to InvalidId") {
                    val domainError = ScopeInputError.IdError.InvalidFormat(Clock.System.now(), "invalid-id", "ULID format")
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidId>()
                    contractError as ScopeContractError.InputError.InvalidId
                    contractError.id shouldBe "invalid-id"
                    contractError.expectedFormat shouldBe "ULID format"
                }

                it("should map ScopeInputError.TitleError.Empty to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.Empty(Clock.System.now(), "")
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe ""
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map ScopeInputError.TitleError.TooShort to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.TooShort(Clock.System.now(), "a", 3)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe "a"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                }

                it("should map ScopeInputError.TitleError.TooLong to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.TooLong(Clock.System.now(), "very long title", 10)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe "very long title"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooLong>()
                }

                it("should map ScopeInputError.TitleError.ContainsProhibitedCharacters to InvalidTitle") {
                    val domainError = ScopeInputError.TitleError.ContainsProhibitedCharacters(Clock.System.now(), "title<>", listOf('<', '>'))
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe "title<>"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.InvalidCharacters>()
                }

                it("should map ScopeInputError.DescriptionError.TooLong to InvalidDescription") {
                    val domainError = ScopeInputError.DescriptionError.TooLong(Clock.System.now(), "very long description", 10)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidDescription>()
                    contractError as ScopeContractError.InputError.InvalidDescription
                    contractError.descriptionText shouldBe "very long description"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.DescriptionValidationFailure.TooLong>()
                }
            }

            context("Alias validation errors") {
                it("should map ScopeInputError.AliasError.Empty to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.Empty(Clock.System.now(), "")
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe ""
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map ScopeInputError.AliasError.TooShort to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.TooShort(Clock.System.now(), "a", 3)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe "a"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                }

                it("should map ScopeInputError.AliasError.TooLong to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.TooLong(Clock.System.now(), "very long alias", 10)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe "very long alias"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooLong>()
                }

                it("should map ScopeInputError.AliasError.InvalidFormat to InvalidTitle") {
                    val domainError = ScopeInputError.AliasError.InvalidFormat(Clock.System.now(), "invalid-alias", "pattern")
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError as ScopeContractError.InputError.InvalidTitle
                    contractError.title shouldBe "invalid-alias"
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.InvalidCharacters>()
                }
            }

            context("Business rule violations") {
                it("should map ScopeError.NotFound to BusinessError.NotFound") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.NotFound(scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    contractError as ScopeContractError.BusinessError.NotFound
                    contractError.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.ParentNotFound to BusinessError.NotFound") {
                    val parentId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.ParentNotFound(parentId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    contractError as ScopeContractError.BusinessError.NotFound
                    contractError.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.AlreadyDeleted to BusinessError.AlreadyDeleted") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.AlreadyDeleted(scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.AlreadyDeleted>()
                    contractError as ScopeContractError.BusinessError.AlreadyDeleted
                    contractError.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.AlreadyArchived to BusinessError.ArchivedScope") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.AlreadyArchived(scopeId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.ArchivedScope>()
                    contractError as ScopeContractError.BusinessError.ArchivedScope
                    contractError.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.DuplicateTitle to BusinessError.DuplicateTitle") {
                    val parentId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()
                    val domainError = ScopeError.DuplicateTitle("My Title", parentId)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateTitle>()
                    contractError as ScopeContractError.BusinessError.DuplicateTitle
                    contractError.title shouldBe "My Title"
                    contractError.parentId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopeError.VersionMismatch to SystemError.ConcurrentModification") {
                    val scopeId = ScopeId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").getOrNull()!!
                    val domainError = ScopeError.VersionMismatch(scopeId, 1, 2)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ConcurrentModification>()
                    contractError as ScopeContractError.SystemError.ConcurrentModification
                    contractError.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                    contractError.expectedVersion shouldBe 1L
                    contractError.actualVersion shouldBe 2L
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

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.AliasNotFound>()
                    contractError as ScopeContractError.BusinessError.AliasNotFound
                    contractError.alias shouldBe "my-alias"
                }

                it("should map ScopesError.NotFound with non-alias type to BusinessError.NotFound") {
                    val domainError = ScopesError.NotFound(
                        entityType = "Scope",
                        identifierType = "id",
                        identifier = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    contractError as ScopeContractError.BusinessError.NotFound
                    contractError.scopeId shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
                }

                it("should map ScopesError.AlreadyExists to BusinessError.DuplicateAlias") {
                    val domainError = ScopesError.AlreadyExists(
                        entityType = "AspectDefinition",
                        identifier = "priority",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateAlias>()
                    contractError as ScopeContractError.BusinessError.DuplicateAlias
                    contractError.alias shouldBe "priority"
                }

                it("should map ScopesError.SystemError to SystemError.ServiceUnavailable") {
                    val domainError = ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE,
                        service = "database",
                    )
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                    contractError as ScopeContractError.SystemError.ServiceUnavailable
                    contractError.service shouldBe "database"
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
                    contractError.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                    contractError as ScopeContractError.SystemError.ServiceUnavailable
                    contractError.service shouldBe "scope-management"
                }
            }
        }
    })
