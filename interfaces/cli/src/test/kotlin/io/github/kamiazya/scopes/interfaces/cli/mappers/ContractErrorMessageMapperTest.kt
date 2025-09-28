package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ContractErrorMessageMapperTest :
    DescribeSpec({

        describe("ContractErrorMessageMapper") {

            describe("getMessage for ScopeContractError") {

                describe("InputError messages") {
                    it("should format InvalidTitle with Empty validation failure") {
                        val error = ScopeContractError.InputError.InvalidTitle(
                            title = "",
                            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
                        )

                        ContractErrorMessageMapper.getMessage(error) shouldBe "Title cannot be empty"
                    }

                    it("should format InvalidTitle with TooShort validation failure") {
                        val error = ScopeContractError.InputError.InvalidTitle(
                            title = "ab",
                            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                                minimumLength = 3,
                                actualLength = 2,
                            ),
                        )

                        ContractErrorMessageMapper.getMessage(error) shouldBe
                            "Title is too short (minimum 3 characters, got 2)"
                    }
                }

                describe("BusinessError messages") {
                    it("should format NotFound error") {
                        val error = ScopeContractError.BusinessError.NotFound(scopeId = "scope-123")

                        ContractErrorMessageMapper.getMessage(error) shouldBe "Scope not found: scope-123"
                        ContractErrorMessageMapper.getMessage(error, debug = true) shouldBe
                            "Scope not found: 'scope-123' [ULID/alias searched: scope-123]"
                    }

                    it("should format DuplicateTitle error") {
                        val error = ScopeContractError.BusinessError.DuplicateTitle(
                            title = "Existing Title",
                            parentId = "parent-123",
                            existingScopeId = "existing-456",
                        )

                        ContractErrorMessageMapper.getMessage(error) shouldBe
                            "A scope with title 'Existing Title' already exists under parent parent-123"
                        ContractErrorMessageMapper.getMessage(error, debug = true) shouldBe
                            "A scope with title 'Existing Title' already exists under parent parent-123 [parent ULID: parent-123]"
                    }

                    it("should format HierarchyViolation with MaxDepthExceeded") {
                        val error = ScopeContractError.BusinessError.HierarchyViolation(
                            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                                scopeId = "scope-123",
                                maximumDepth = 10,
                                attemptedDepth = 11,
                            ),
                        )

                        ContractErrorMessageMapper.getMessage(error) shouldBe
                            "Maximum hierarchy depth exceeded: attempted 11, maximum 10"
                        ContractErrorMessageMapper.getMessage(error, debug = true) shouldBe
                            "Maximum hierarchy depth exceeded: attempted 11, maximum 10 [violation type: MaxDepthExceeded]"
                    }

                    it("should format HierarchyViolation with CircularReference") {
                        val error = ScopeContractError.BusinessError.HierarchyViolation(
                            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                                scopeId = "scope-123",
                                parentId = "parent-456",
                                cyclePath = listOf("scope-123", "parent-456", "grandparent-789", "scope-123"),
                            ),
                        )

                        ContractErrorMessageMapper.getMessage(error) shouldBe
                            "Circular reference detected: scope scope-123 cannot have parent parent-456 (cycle: scope-123 -> parent-456 -> grandparent-789 -> scope-123)"
                    }

                    it("should format ContextNotFound error") {
                        val error = ScopeContractError.BusinessError.ContextNotFound(contextKey = "missing-context")

                        ContractErrorMessageMapper.getMessage(error) shouldBe
                            "Context view not found: missing-context"
                    }
                }

                describe("SystemError messages") {
                    it("should format ServiceUnavailable error") {
                        val error = ScopeContractError.SystemError.ServiceUnavailable(service = "daemon")

                        ContractErrorMessageMapper.getMessage(error) shouldBe
                            "Service unavailable: daemon"
                    }
                }
            }

            describe("ValidationMessageFormatter") {

                describe("formatHierarchyViolation") {
                    it("should format MaxDepthExceeded") {
                        val violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                            scopeId = "scope-123",
                            maximumDepth = 5,
                            attemptedDepth = 6,
                        )

                        ValidationMessageFormatter.formatHierarchyViolation(violation) shouldBe
                            "Maximum hierarchy depth exceeded: attempted 6, maximum 5"
                    }

                    it("should format CircularReference without cycle path") {
                        val violation = ScopeContractError.HierarchyViolationType.CircularReference(
                            scopeId = "scope-123",
                            parentId = "parent-456",
                            cyclePath = null,
                        )

                        ValidationMessageFormatter.formatHierarchyViolation(violation) shouldBe
                            "Circular reference detected: scope scope-123 cannot have parent parent-456"
                    }
                }

                describe("formatContextFilterValidationFailure") {
                    it("should format InvalidSyntax with position") {
                        val failure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                            expression = "priority=>high",
                            errorType = "Invalid operator",
                            position = 8,
                        )

                        // Note: ContractErrorMessageMapper overrides this to include expression
                        ValidationMessageFormatter.formatContextFilterValidationFailure(failure) shouldBe
                            "Invalid filter syntax: Invalid operator at position 8"
                    }

                    it("should format InvalidSyntax without position") {
                        val failure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                            expression = "incomplete AND",
                            errorType = "Incomplete expression",
                            position = null,
                        )

                        ValidationMessageFormatter.formatContextFilterValidationFailure(failure) shouldBe
                            "Invalid filter syntax: Incomplete expression"
                    }
                }
            }
        }
    })
