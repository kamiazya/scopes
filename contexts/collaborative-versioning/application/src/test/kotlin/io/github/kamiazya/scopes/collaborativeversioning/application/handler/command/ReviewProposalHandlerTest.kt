package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ReviewProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.StartReviewCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ReviewProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewComment
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewCommentType
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ProposalStatistics
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock

class ReviewProposalHandlerTest :
    DescribeSpec({

        val mockChangeProposalRepository = mockk<ChangeProposalRepository>()
        val handler = ReviewProposalHandler(mockChangeProposalRepository)

        describe("ReviewProposalHandler") {

            describe("start review") {
                it("should start review process successfully") {
                    // Given
                    val proposalId = ProposalId.generate()
                    val command = StartReviewCommand(proposalId)

                    val submittedProposal = mockk<ChangeProposal> {
                        every { state } returns ProposalState.SUBMITTED
                        every { startReview(any()) } returns mockk<ChangeProposal> {
                            every { id } returns proposalId
                            every { state } returns ProposalState.REVIEWING
                            every { reviewComments } returns emptyList()
                            every { getStatistics() } returns ProposalStatistics(0, 0, 0, 0, 0)
                            every { updatedAt } returns Clock.System.now()
                        }.right()
                    }

                    val reviewingProposal = submittedProposal.startReview(Clock.System.now()).getOrNull()!!

                    coEvery { mockChangeProposalRepository.findById(proposalId) } returns submittedProposal.right()
                    coEvery { mockChangeProposalRepository.save(reviewingProposal) } returns reviewingProposal.right()

                    // When
                    val result = handler.startReview(command)

                    // Then
                    result.isRight() shouldBe true
                    val dto = result.getOrNull()!!
                    dto.proposalId shouldBe proposalId
                    dto.state shouldBe ProposalState.REVIEWING

                    coVerify { mockChangeProposalRepository.findById(proposalId) }
                    coVerify { mockChangeProposalRepository.save(reviewingProposal) }
                }

                it("should fail when proposal is not found") {
                    // Given
                    val proposalId = ProposalId.generate()
                    val command = StartReviewCommand(proposalId)

                    coEvery { mockChangeProposalRepository.findById(proposalId) } returns null.right()

                    // When
                    val result = handler.startReview(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ReviewProposalError.ProposalNotFound>()
                    (error as ReviewProposalError.ProposalNotFound).proposalId shouldBe proposalId

                    coVerify { mockChangeProposalRepository.findById(proposalId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }

                it("should fail when repository find operation fails") {
                    // Given
                    val proposalId = ProposalId.generate()
                    val command = StartReviewCommand(proposalId)

                    val findError = FindChangeProposalError.DatabaseError("Connection failed")
                    coEvery { mockChangeProposalRepository.findById(proposalId) } returns findError.left()

                    // When
                    val result = handler.startReview(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ReviewProposalError.FindFailure>()
                    (error as ReviewProposalError.FindFailure).findError shouldBe findError

                    coVerify { mockChangeProposalRepository.findById(proposalId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }
            }

            describe("add comment") {
                it("should add review comment successfully") {
                    // Given
                    val proposalId = ProposalId.generate()
                    val reviewer = Author.agent(AgentId.from("reviewer-123"))
                    val comment = ReviewComment.create(
                        author = reviewer,
                        content = "This looks good, but needs minor changes",
                        commentType = ReviewCommentType.GENERAL,
                        timestamp = Clock.System.now(),
                    )
                    val command = ReviewProposalCommand(proposalId, reviewer, comment)

                    val reviewingProposal = mockk<ChangeProposal> {
                        every { state } returns ProposalState.REVIEWING
                        every { addReviewComment(comment, any()) } returns mockk<ChangeProposal> {
                            every { id } returns proposalId
                            every { state } returns ProposalState.REVIEWING
                            every { reviewComments } returns listOf(comment)
                            every { getStatistics() } returns ProposalStatistics(1, 1, 0, 0, 0)
                            every { updatedAt } returns Clock.System.now()
                        }.right()
                    }

                    val proposalWithComment = reviewingProposal.addReviewComment(comment, Clock.System.now()).getOrNull()!!

                    coEvery { mockChangeProposalRepository.findById(proposalId) } returns reviewingProposal.right()
                    coEvery { mockChangeProposalRepository.save(proposalWithComment) } returns proposalWithComment.right()

                    // When
                    val result = handler.addComment(command)

                    // Then
                    result.isRight() shouldBe true
                    val dto = result.getOrNull()!!
                    dto.proposalId shouldBe proposalId
                    dto.state shouldBe ProposalState.REVIEWING
                    dto.reviewComments.size shouldBe 1

                    coVerify { mockChangeProposalRepository.findById(proposalId) }
                    coVerify { mockChangeProposalRepository.save(proposalWithComment) }
                }

                it("should fail when proposal is not found") {
                    // Given
                    val proposalId = ProposalId.generate()
                    val reviewer = Author.agent(AgentId.from("reviewer-123"))
                    val comment = ReviewComment.create(
                        author = reviewer,
                        content = "This looks good",
                        commentType = ReviewCommentType.GENERAL,
                        timestamp = Clock.System.now(),
                    )
                    val command = ReviewProposalCommand(proposalId, reviewer, comment)

                    coEvery { mockChangeProposalRepository.findById(proposalId) } returns null.right()

                    // When
                    val result = handler.addComment(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ReviewProposalError.ProposalNotFound>()
                    (error as ReviewProposalError.ProposalNotFound).proposalId shouldBe proposalId

                    coVerify { mockChangeProposalRepository.findById(proposalId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }

                it("should fail when proposal is in wrong state for comments") {
                    // Given
                    val proposalId = ProposalId.generate()
                    val reviewer = Author.agent(AgentId.from("reviewer-123"))
                    val comment = ReviewComment.create(
                        author = reviewer,
                        content = "This looks good",
                        commentType = ReviewCommentType.GENERAL,
                        timestamp = Clock.System.now(),
                    )
                    val command = ReviewProposalCommand(proposalId, reviewer, comment)

                    val draftProposal = mockk<ChangeProposal> {
                        every { state } returns ProposalState.DRAFT
                        every { addReviewComment(comment, any()) } returns ChangeProposalError.InvalidStateTransition(
                            ProposalState.DRAFT,
                            "add review comment",
                        ).left()
                    }

                    coEvery { mockChangeProposalRepository.findById(proposalId) } returns draftProposal.right()

                    // When
                    val result = handler.addComment(command)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ReviewProposalError.DomainRuleViolation>()

                    coVerify { mockChangeProposalRepository.findById(proposalId) }
                    coVerify(exactly = 0) { mockChangeProposalRepository.save(any()) }
                }
            }
        }
    })
