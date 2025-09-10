package io.github.kamiazya.scopes.collaborativeversioning.application.integration

import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.application.command.*
import io.github.kamiazya.scopes.collaborativeversioning.application.handler.command.*
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewComment
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewCommentType
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ResourceState
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.datetime.Clock

class ProposalWorkflowIntegrationTest :
    DescribeSpec({

        describe("Proposal Workflow Integration") {

            it("should handle complete proposal lifecycle from creation to merge") {
                // Setup
                val mockChangeProposalRepository = mockk<ChangeProposalRepository>()
                val mockTrackedResourceRepository = mockk<TrackedResourceRepository>()
                val mockResourceState = mockk<ResourceState>()

                val proposeHandler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository)
                val reviewHandler = ReviewProposalHandler(mockChangeProposalRepository)
                val approveHandler = ApproveProposalHandler(mockChangeProposalRepository)
                val mergeHandler = MergeProposalHandler(mockChangeProposalRepository)

                // Test data
                val author = Author.agent(AgentId.from("author-agent"))
                val reviewer = Author.agent(AgentId.from("reviewer-agent"))
                val approver = Author.agent(AgentId.from("approver-agent"))
                val resourceId = ResourceId.generate()

                val proposalSlot = slot<ChangeProposal>()

                // Mock repository behavior
                coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                coEvery { mockChangeProposalRepository.save(capture(proposalSlot)) } answers { proposalSlot.captured.right() }
                coEvery { mockChangeProposalRepository.findById(any()) } answers { proposalSlot.captured.right() }

                // Mock resource state for conflict detection
                coEvery { mockResourceState.getValueAtPath(any()) } returns null
                coEvery { mockResourceState.canApplyChangeset(any()) } returns true

                // Step 1: Create proposal
                val createCommand = ProposeChangeCommand(
                    author = author,
                    targetResourceId = resourceId,
                    title = "Add new feature",
                    description = "This proposal adds a new feature to the system",
                )

                val createResult = proposeHandler(createCommand)
                createResult.isRight() shouldBe true

                val createdDto = createResult.getOrNull()!!
                createdDto.state shouldBe ProposalState.DRAFT
                createdDto.title shouldBe "Add new feature"

                val proposalId = createdDto.proposalId

                // Step 2: Submit proposal for review
                val submitCommand = SubmitProposalCommand(proposalId)
                val submitResult = mergeHandler.submit(submitCommand)

                submitResult.isRight() shouldBe true
                val submittedDto = submitResult.getOrNull()!!
                submittedDto.state shouldBe ProposalState.SUBMITTED
                submittedDto.submittedAt shouldNotBe null

                // Step 3: Start review process
                val startReviewCommand = StartReviewCommand(proposalId)
                val startReviewResult = reviewHandler.startReview(startReviewCommand)

                startReviewResult.isRight() shouldBe true
                val reviewingDto = startReviewResult.getOrNull()!!
                reviewingDto.state shouldBe ProposalState.REVIEWING

                // Step 4: Add review comments
                val comment = ReviewComment.create(
                    author = reviewer,
                    content = "This looks good, please make a minor change in line 10",
                    commentType = ReviewCommentType.GENERAL,
                    timestamp = Clock.System.now(),
                )

                val addCommentCommand = ReviewProposalCommand(proposalId, reviewer, comment)
                val commentResult = reviewHandler.addComment(addCommentCommand)

                commentResult.isRight() shouldBe true
                val commentedDto = commentResult.getOrNull()!!
                commentedDto.reviewComments.size shouldBe 1
                commentedDto.statistics.totalCommentCount shouldBe 1

                // Step 5: Approve proposal
                val approveCommand = ApproveProposalCommand(
                    proposalId = proposalId,
                    approver = approver,
                    approvalMessage = "Approved for merge",
                )

                val approveResult = approveHandler.approve(approveCommand)
                approveResult.isRight() shouldBe true

                val approvedDto = approveResult.getOrNull()!!
                approvedDto.state shouldBe ProposalState.APPROVED
                approvedDto.resolvedAt shouldNotBe null

                // Step 6: Merge proposal
                val mergeCommand = MergeProposalCommand(
                    proposalId = proposalId,
                    applicator = approver,
                    currentResourceState = mockResourceState,
                )

                val mergeResult = mergeHandler.merge(mergeCommand)
                mergeResult.isRight() shouldBe true

                val mergedDto = mergeResult.getOrNull()!!
                mergedDto.state shouldBe ProposalState.APPLIED
                mergedDto.appliedAt shouldNotBe null
                mergedDto.appliedChanges shouldNotBe null
                mergedDto.conflicts shouldBe emptyList()
            }

            it("should handle proposal rejection workflow") {
                // Setup
                val mockChangeProposalRepository = mockk<ChangeProposalRepository>()
                val mockTrackedResourceRepository = mockk<TrackedResourceRepository>()

                val proposeHandler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository)
                val reviewHandler = ReviewProposalHandler(mockChangeProposalRepository)
                val approveHandler = ApproveProposalHandler(mockChangeProposalRepository)
                val mergeHandler = MergeProposalHandler(mockChangeProposalRepository)

                // Test data
                val author = Author.agent(AgentId.from("author-agent"))
                val reviewer = Author.agent(AgentId.from("reviewer-agent"))
                val resourceId = ResourceId.generate()

                val proposalSlot = slot<ChangeProposal>()

                // Mock repository behavior
                coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                coEvery { mockChangeProposalRepository.save(capture(proposalSlot)) } answers { proposalSlot.captured.right() }
                coEvery { mockChangeProposalRepository.findById(any()) } answers { proposalSlot.captured.right() }

                // Step 1: Create and submit proposal
                val createCommand = ProposeChangeCommand(
                    author = author,
                    targetResourceId = resourceId,
                    title = "Problematic feature",
                    description = "This proposal has issues",
                )

                val createResult = proposeHandler(createCommand)
                createResult.isRight() shouldBe true
                val proposalId = createResult.getOrNull()!!.proposalId

                // Submit for review
                val submitResult = mergeHandler.submit(SubmitProposalCommand(proposalId))
                submitResult.isRight() shouldBe true

                // Start review
                val startReviewResult = reviewHandler.startReview(StartReviewCommand(proposalId))
                startReviewResult.isRight() shouldBe true

                // Step 2: Reject proposal
                val rejectCommand = RejectProposalCommand(
                    proposalId = proposalId,
                    reviewer = reviewer,
                    rejectionReason = "This proposal violates security guidelines",
                )

                val rejectResult = approveHandler.reject(rejectCommand)
                rejectResult.isRight() shouldBe true

                val rejectedDto = rejectResult.getOrNull()!!
                rejectedDto.state shouldBe ProposalState.REJECTED
                rejectedDto.resolvedAt shouldNotBe null
            }

            it("should handle merge conflicts scenario") {
                // Setup
                val mockChangeProposalRepository = mockk<ChangeProposalRepository>()
                val mockTrackedResourceRepository = mockk<TrackedResourceRepository>()
                val mockResourceState = mockk<ResourceState>()

                val proposeHandler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository)
                val reviewHandler = ReviewProposalHandler(mockChangeProposalRepository)
                val approveHandler = ApproveProposalHandler(mockChangeProposalRepository)
                val mergeHandler = MergeProposalHandler(mockChangeProposalRepository)

                // Test data
                val author = Author.agent(AgentId.from("author-agent"))
                val approver = Author.agent(AgentId.from("approver-agent"))
                val resourceId = ResourceId.generate()

                val proposalSlot = slot<ChangeProposal>()

                // Mock repository behavior
                coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                coEvery { mockChangeProposalRepository.save(capture(proposalSlot)) } answers { proposalSlot.captured.right() }
                coEvery { mockChangeProposalRepository.findById(any()) } answers { proposalSlot.captured.right() }

                // Mock resource state to simulate conflicts
                coEvery { mockResourceState.getValueAtPath("conflict.path") } returns "different_value"
                coEvery { mockResourceState.canApplyChangeset(any()) } returns true

                // Create, submit, review, and approve proposal
                val createCommand = ProposeChangeCommand(
                    author = author,
                    targetResourceId = resourceId,
                    title = "Conflicting change",
                    description = "This change will conflict with current state",
                )

                proposeHandler(createCommand)
                val proposalId = proposalSlot.captured.id

                mergeHandler.submit(SubmitProposalCommand(proposalId))
                reviewHandler.startReview(StartReviewCommand(proposalId))
                approveHandler.approve(ApproveProposalCommand(proposalId, approver))

                // Step: Try to merge with conflicts
                val mergeCommand = MergeProposalCommand(
                    proposalId = proposalId,
                    applicator = approver,
                    currentResourceState = mockResourceState,
                )

                val mergeResult = mergeHandler.merge(mergeCommand)

                // Should fail due to conflicts
                mergeResult.isLeft() shouldBe true
                val error = mergeResult.leftOrNull()!!
                error.shouldBeInstanceOf<io.github.kamiazya.scopes.collaborativeversioning.application.error.MergeProposalError.ConflictsDetected>()
            }
        }
    })
