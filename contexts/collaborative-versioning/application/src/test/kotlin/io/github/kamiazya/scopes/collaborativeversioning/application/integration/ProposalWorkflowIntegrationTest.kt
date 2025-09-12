package io.github.kamiazya.scopes.collaborativeversioning.application.integration

import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.application.command.*
import io.github.kamiazya.scopes.collaborativeversioning.application.handler.command.*
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewComment
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ResourceState
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ReviewCommentType
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Clock

class ProposalWorkflowIntegrationTest :
    DescribeSpec({

        describe("Proposal Workflow Integration") {

            it("should handle complete proposal lifecycle from creation to merge") {
                // Setup
                val mockChangeProposalRepository = mockk<ChangeProposalRepository>()
                val mockTrackedResourceRepository = mockk<TrackedResourceRepository>()
                val mockResourceState = mockk<ResourceState>()

                val mockEventPublisher = mockk<DomainEventPublisher>()
                coEvery { mockEventPublisher.publish(any()) } returns Unit.right()
                val mockLogger = mockk<Logger>(relaxed = true)
                val proposeHandler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository, mockEventPublisher, mockLogger)
                val startReviewHandler = StartReviewHandler(mockChangeProposalRepository)
                val addCommentHandler = AddReviewCommentHandler(mockChangeProposalRepository)
                val approveHandler = ApproveProposalCommandHandler(mockChangeProposalRepository)
                val mergeHandler = MergeApprovedProposalHandler(mockChangeProposalRepository)
                val submitHandler = SubmitProposalHandler(mockChangeProposalRepository)

                // Test data
                val author = Author.fromAgent(AgentId.generate(), "Author Agent").getOrNull()!!
                val reviewer = Author.fromAgent(AgentId.generate(), "Reviewer Agent").getOrNull()!!
                val approver = Author.fromAgent(AgentId.generate(), "Approver Agent").getOrNull()!!
                val resourceId = ResourceId.generate()

                // Track latest saved proposal
                var savedProposal: ChangeProposal? = null

                // Mock repository behavior
                coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                coEvery { mockChangeProposalRepository.save(any()) } answers {
                    val proposal = it.invocation.args[0] as ChangeProposal
                    savedProposal = proposal
                    proposal.right()
                }
                coEvery { mockChangeProposalRepository.findById(any()) } answers {
                    savedProposal?.right() ?: null.right()
                }

                // Mock resource state for conflict detection
                coEvery { mockResourceState.getValueAtPath(any()) } returns null
                coEvery { mockResourceState.canApplyChangeset(any()) } returns true

                // Step 1: Create proposal
                val createCommand = ProposeChangeCommand(
                    author = author,
                    targetResourceId = resourceId,
                    title = "Add new feature",
                    description = "This proposal adds a new feature to the system",
                    proposedChanges = listOf(
                        ProposedChange.Inline(
                            id = ProposedChange.generateId(),
                            resourceId = resourceId,
                            description = "Add new feature",
                            createdAt = Clock.System.now(),
                            changes = listOf(
                                Change(
                                    path = "/features/newFeature",
                                    operation = ChangeOperation.ADD,
                                    previousValue = null,
                                    newValue = "enabled"
                                )
                            )
                        )
                    )
                )

                val createResult = proposeHandler(createCommand)
                createResult.isRight() shouldBe true

                val createdDto = createResult.getOrNull()!!
                createdDto.state shouldBe ProposalState.DRAFT
                createdDto.title shouldBe "Add new feature"

                val proposalId = createdDto.proposalId

                // Step 2: Submit proposal for review
                val submitCommand = SubmitProposalCommand(proposalId)
                val submitResult = submitHandler(submitCommand)

                submitResult.isRight() shouldBe true
                val submittedDto = submitResult.getOrNull()!!
                submittedDto.state shouldBe ProposalState.SUBMITTED
                submittedDto.submittedAt shouldNotBe null

                // Step 3: Start review process
                val startReviewCommand = StartReviewCommand(proposalId)
                val startReviewResult = startReviewHandler(startReviewCommand)

                startReviewResult.isRight() shouldBe true
                val reviewingDto = startReviewResult.getOrNull()!!
                reviewingDto.state shouldBe ProposalState.REVIEWING

                // Step 4: Add review comments
                val comment = ReviewComment.create(
                    author = reviewer,
                    content = "This looks good, please make a minor change in line 10",
                    commentType = ReviewCommentType.COMMENT,
                    timestamp = Clock.System.now(),
                )

                val addCommentCommand = ReviewProposalCommand(proposalId, reviewer, comment)
                val commentResult = addCommentHandler(addCommentCommand)

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

                val approveResult = approveHandler(approveCommand)
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

                val mergeResult = mergeHandler(mergeCommand)
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

                val mockEventPublisher = mockk<DomainEventPublisher>()
                coEvery { mockEventPublisher.publish(any()) } returns Unit.right()
                val mockLogger = mockk<Logger>(relaxed = true)
                val proposeHandler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository, mockEventPublisher, mockLogger)
                val startReviewHandler = StartReviewHandler(mockChangeProposalRepository)
                val addCommentHandler = AddReviewCommentHandler(mockChangeProposalRepository)
                val rejectHandler = RejectProposalHandler(mockChangeProposalRepository)
                val submitHandler = SubmitProposalHandler(mockChangeProposalRepository)

                // Test data
                val author = Author.fromAgent(AgentId.generate(), "Author Agent").getOrNull()!!
                val reviewer = Author.fromAgent(AgentId.generate(), "Reviewer Agent").getOrNull()!!
                val resourceId = ResourceId.generate()

                // Track latest saved proposal
                var savedProposal: ChangeProposal? = null

                // Mock repository behavior
                coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                coEvery { mockChangeProposalRepository.save(any()) } answers {
                    val proposal = it.invocation.args[0] as ChangeProposal
                    savedProposal = proposal
                    proposal.right()
                }
                coEvery { mockChangeProposalRepository.findById(any()) } answers {
                    savedProposal?.right() ?: null.right()
                }

                // Step 1: Create and submit proposal
                val createCommand = ProposeChangeCommand(
                    author = author,
                    targetResourceId = resourceId,
                    title = "Problematic feature",
                    description = "This proposal has issues",
                    proposedChanges = listOf(
                        ProposedChange.Inline(
                            id = ProposedChange.generateId(),
                            resourceId = resourceId,
                            description = "Add problematic feature",
                            createdAt = Clock.System.now(),
                            changes = listOf(
                                Change(
                                    path = "/features/problematic",
                                    operation = ChangeOperation.ADD,
                                    previousValue = null,
                                    newValue = "risky"
                                )
                            )
                        )
                    )
                )

                val createResult = proposeHandler(createCommand)
                createResult.isRight() shouldBe true
                val proposalId = createResult.getOrNull()!!.proposalId

                // Submit for review
                val submitResult = submitHandler(SubmitProposalCommand(proposalId))
                submitResult.isRight() shouldBe true

                // Start review
                val startReviewResult = startReviewHandler(StartReviewCommand(proposalId))
                startReviewResult.isRight() shouldBe true

                // Step 2: Reject proposal
                val rejectCommand = RejectProposalCommand(
                    proposalId = proposalId,
                    reviewer = reviewer,
                    rejectionReason = "This proposal violates security guidelines",
                )

                val rejectResult = rejectHandler(rejectCommand)
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

                val mockEventPublisher = mockk<DomainEventPublisher>()
                coEvery { mockEventPublisher.publish(any()) } returns Unit.right()
                val mockLogger = mockk<Logger>(relaxed = true)
                val proposeHandler = ProposeChangeHandler(mockChangeProposalRepository, mockTrackedResourceRepository, mockEventPublisher, mockLogger)
                val startReviewHandler = StartReviewHandler(mockChangeProposalRepository)
                val addCommentHandler = AddReviewCommentHandler(mockChangeProposalRepository)
                val approveHandler = ApproveProposalCommandHandler(mockChangeProposalRepository)
                val submitHandler = SubmitProposalHandler(mockChangeProposalRepository)
                val mergeHandler = MergeApprovedProposalHandler(mockChangeProposalRepository)

                // Test data
                val author = Author.fromAgent(AgentId.generate(), "Author Agent").getOrNull()!!
                val approver = Author.fromAgent(AgentId.generate(), "Approver Agent").getOrNull()!!
                val resourceId = ResourceId.generate()

                // Track latest saved proposal
                var savedProposal: ChangeProposal? = null

                // Mock repository behavior
                coEvery { mockTrackedResourceRepository.existsById(resourceId) } returns true.right()
                coEvery { mockChangeProposalRepository.save(any()) } answers {
                    val proposal = it.invocation.args[0] as ChangeProposal
                    savedProposal = proposal
                    proposal.right()
                }
                coEvery { mockChangeProposalRepository.findById(any()) } answers {
                    savedProposal?.right() ?: null.right()
                }

                // Mock resource state to simulate conflicts
                coEvery { mockResourceState.getValueAtPath("conflict.path") } returns "different_value"
                coEvery { mockResourceState.canApplyChangeset(any()) } returns true

                // Create, submit, review, and approve proposal
                val createCommand = ProposeChangeCommand(
                    author = author,
                    targetResourceId = resourceId,
                    title = "Conflicting change",
                    description = "This change will conflict with current state",
                    proposedChanges = listOf(
                        ProposedChange.Inline(
                            id = ProposedChange.generateId(),
                            resourceId = resourceId,
                            description = "Update conflicting value",
                            createdAt = Clock.System.now(),
                            changes = listOf(
                                Change(
                                    path = "conflict.path",
                                    operation = ChangeOperation.MODIFY,
                                    previousValue = "old_value",
                                    newValue = "new_value"
                                )
                            )
                        )
                    )
                )

                val proposeResult = proposeHandler(createCommand)
                val proposalId = proposeResult.getOrNull()!!.proposalId

                submitHandler(SubmitProposalCommand(proposalId))
                startReviewHandler(StartReviewCommand(proposalId))
                approveHandler(ApproveProposalCommand(proposalId, approver))

                // Step: Try to merge with conflicts
                val mergeCommand = MergeProposalCommand(
                    proposalId = proposalId,
                    applicator = approver,
                    currentResourceState = mockResourceState,
                )

                val mergeResult = mergeHandler(mergeCommand)

                // Should fail due to conflicts
                mergeResult.isLeft() shouldBe true
                val error = mergeResult.leftOrNull()!!
                error.shouldBeInstanceOf<io.github.kamiazya.scopes.collaborativeversioning.application.error.MergeProposalError.ConflictsDetected>()
            }
        }
    })
