package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ProposeChangeCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ProposeChangeError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindTrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SaveChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalState
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class ProposeChangeHandlerTest :
    DescribeSpec({

        val mockChangeProposalRepository = mockk<ChangeProposalRepository>()
        val mockTrackedResourceRepository = mockk<TrackedResourceRepository>()
        val handler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository)

        describe("ProposeChangeHandler") {

            describe("successful proposal creation") {
                it("should create a new change proposal successfully") {
                    // Given
                    val author = Author.agent(AgentId.from("agent-123"))
                    val resourceId = ResourceId.generate()
                    val command = ProposeChangeCommand(
                        author = author,
                        targetResourceId = resourceId,
                        title = "Add new feature",
                        description = "This proposal adds a new feature to the system",
                        proposedChanges = emptyList(),
                    )

                    // Mock repository responses
                    coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                    coEvery { mockChangeProposalRepository.save(any()) } returns mockk<ChangeProposal> {
                        every { id } returns mockk()
                        every { state } returns ProposalState.DRAFT
                        every { title } returns command.title
                        every { description } returns command.description
                        every { createdAt } returns mockk()
                        every { updatedAt } returns mockk()
                    }

                    // When
                    val result = handler(command)

                    // Then
                    result.isRight() shouldBe true
                    val dto = result.getOrNull()!!
                    dto.title shouldBe command.title
                    dto.description shouldBe command.description
                    dto.state shouldBe ProposalState.DRAFT

                    coVerify { mockTrackedResourceRepository.existsById(resourceId) }
                    coVerify { mockChangeProposalRepository.save(any()) }
                }
            }

            describe("resource validation failures") {
                it("should fail when target resource does not exist") {
                    // Given
                    val author = Author.agent(AgentId.from("agent-123"))
                    val resourceId = ResourceId.generate()
                    val command = ProposeChangeCommand(
                        author = author,
                        targetResourceId = resourceId,
                        title = "Add new feature",
                        description = "This proposal adds a new feature to the system",
                    )

                    coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns false.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ProposeChangeError.ResourceNotFound>()
                    error.resourceId shouldBe resourceId

                    coVerify { mockTrackedResourceRepository.existsById(resourceId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }

                it("should handle repository errors when checking resource existence") {
                    // Given
                    val author = Author.agent(AgentId.from("agent-123"))
                    val resourceId = ResourceId.generate()
                    val command = ProposeChangeCommand(
                        author = author,
                        targetResourceId = resourceId,
                        title = "Add new feature",
                        description = "This proposal adds a new feature to the system",
                    )

                    val repositoryError = FindTrackedResourceError.DatabaseError("Connection failed")
                    coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns repositoryError.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ProposeChangeError.ResourceNotFound>()

                    coVerify { mockTrackedResourceRepository.existsById(resourceId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }
            }

            describe("domain validation failures") {
                it("should fail when proposal title is empty") {
                    // Given
                    val author = Author.agent(AgentId.from("agent-123"))
                    val resourceId = ResourceId.generate()
                    val command = ProposeChangeCommand(
                        author = author,
                        targetResourceId = resourceId,
                        title = "", // Empty title
                        description = "This proposal adds a new feature to the system",
                    )

                    coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ProposeChangeError.DomainRuleViolation>()
                    (error as ProposeChangeError.DomainRuleViolation).domainError.shouldBeInstanceOf<ChangeProposalError.EmptyTitle>()

                    coVerify { mockTrackedResourceRepository.existsById(resourceId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }

                it("should fail when proposal description is empty") {
                    // Given
                    val author = Author.agent(AgentId.from("agent-123"))
                    val resourceId = ResourceId.generate()
                    val command = ProposeChangeCommand(
                        author = author,
                        targetResourceId = resourceId,
                        title = "Add new feature",
                        description = "", // Empty description
                    )

                    coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ProposeChangeError.DomainRuleViolation>()
                    (error as ProposeChangeError.DomainRuleViolation).domainError.shouldBeInstanceOf<ChangeProposalError.EmptyDescription>()

                    coVerify { mockTrackedResourceRepository.existsById(resourceId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }
            }

            describe("persistence failures") {
                it("should handle save failures gracefully") {
                    // Given
                    val author = Author.agent(AgentId.from("agent-123"))
                    val resourceId = ResourceId.generate()
                    val command = ProposeChangeCommand(
                        author = author,
                        targetResourceId = resourceId,
                        title = "Add new feature",
                        description = "This proposal adds a new feature to the system",
                    )

                    val saveError = SaveChangeProposalError.DatabaseError("Database connection failed")
                    coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                    coEvery { mockChangeProposalRepository.save(any()) } returns saveError.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ProposeChangeError.SaveFailure>()
                    (error as ProposeChangeError.SaveFailure).saveError shouldBe saveError

                    coVerify { mockTrackedResourceRepository.existsById(resourceId) }
                    coVerify { mockChangeProposalRepository.save(any()) }
                }
            }
        }
    })
