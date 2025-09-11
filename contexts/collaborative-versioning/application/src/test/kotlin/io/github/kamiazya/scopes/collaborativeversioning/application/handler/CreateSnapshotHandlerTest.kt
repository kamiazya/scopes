package io.github.kamiazya.scopes.collaborativeversioning.application.handler

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.application.command.CreateSnapshotCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.application.handler.command.CreateSnapshotHandler
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonPrimitive

class CreateSnapshotHandlerTest :
    DescribeSpec({

        val mockRepository = mockk<TrackedResourceRepository>()
        val mockSnapshotService = mockk<VersionSnapshotService>()
        val logger = ConsoleLogger("CreateSnapshotHandlerTest")

        val handler = CreateSnapshotHandler(
            trackedResourceRepository = mockRepository,
            snapshotService = mockSnapshotService,
            logger = logger,
        )

        val resourceId = ResourceId.generate()
        val authorId = AgentId.generate()
        val content = ResourceContent.fromJsonElement(JsonPrimitive("test content"))
        val timestamp = Clock.System.now()

        val command = CreateSnapshotCommand(
            resourceId = resourceId,
            content = content,
            authorId = authorId,
            message = "Test snapshot",
            metadata = mapOf("test" to "metadata"),
            timestamp = timestamp,
        )

        describe("CreateSnapshotHandler") {

            context("successful snapshot creation") {
                it("should create snapshot and return DTO") {
                    // Given
                    val trackedResource = TrackedResource.create(
                        resourceId = resourceId,
                        resourceType = ResourceType.from("TestResource").getOrNull()!!,
                        initialContent = content,
                        authorId = authorId,
                        message = "Initial commit",
                        timestamp = timestamp,
                    )

                    val snapshot = Snapshot(
                        id = SnapshotId.generate(),
                        resourceId = resourceId,
                        versionId = VersionId.generate(),
                        versionNumber = VersionNumber.from(1).getOrNull()!!,
                        content = content,
                        authorId = authorId,
                        message = "Test snapshot",
                        createdAt = timestamp,
                        metadata = mapOf("test" to "metadata"),
                    )

                    coEvery { mockRepository.findById(resourceId) } returns trackedResource.right()
                    coEvery {
                        mockSnapshotService.createSnapshot(
                            resource = trackedResource,
                            content = content,
                            authorId = authorId,
                            message = "Test snapshot",
                            metadata = mapOf("test" to "metadata"),
                            timestamp = timestamp,
                        )
                    } returns snapshot.right()

                    // When
                    val result = handler.invoke(command)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrNull()!!
                    dto shouldBe SnapshotDto.fromDomain(snapshot)

                    coVerify(exactly = 1) { mockRepository.findById(resourceId) }
                    coVerify(exactly = 1) {
                        mockSnapshotService.createSnapshot(any(), any(), any(), any(), any(), any())
                    }
                }
            }

            context("resource not found") {
                it("should return TrackedResourceNotFound error") {
                    // Given
                    coEvery { mockRepository.findById(resourceId) } returns null.right()

                    // When
                    val result = handler.invoke(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotApplicationError.TrackedResourceNotFound>()
                    error.resourceId shouldBe resourceId

                    coVerify(exactly = 1) { mockRepository.findById(resourceId) }
                    coVerify(exactly = 0) { mockSnapshotService.createSnapshot(any(), any(), any(), any(), any(), any()) }
                }
            }

            context("repository operation failure") {
                it("should return RepositoryOperationFailed error") {
                    // Given
                    val repositoryError = io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindTrackedResourceError.ResourceNotFound(
                        resourceId = resourceId,
                    )

                    coEvery { mockRepository.findById(resourceId) } returns repositoryError.left()

                    // When
                    val result = handler.invoke(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotApplicationError.RepositoryOperationFailed>()
                    error.operation shouldBe "findById"

                    coVerify(exactly = 1) { mockRepository.findById(resourceId) }
                    coVerify(exactly = 0) { mockSnapshotService.createSnapshot(any(), any(), any(), any(), any(), any()) }
                }
            }

            context("snapshot service failure") {
                it("should return SnapshotCreationFailed error") {
                    // Given
                    val trackedResource = TrackedResource.create(
                        resourceId = resourceId,
                        resourceType = ResourceType.from("TestResource").getOrNull()!!,
                        initialContent = content,
                        authorId = authorId,
                        message = "Initial commit",
                        timestamp = timestamp,
                    )

                    val domainError = SnapshotServiceError.StorageLimitExceeded(
                        currentSize = 10_000_000L,
                        maxSize = 5_000_000L,
                    )

                    coEvery { mockRepository.findById(resourceId) } returns trackedResource.right()
                    coEvery {
                        mockSnapshotService.createSnapshot(any(), any(), any(), any(), any(), any())
                    } returns domainError.left()

                    // When
                    val result = handler.invoke(command)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotApplicationError.SnapshotCreationFailed>()
                    error.resourceId shouldBe resourceId
                    error.domainError shouldBe domainError

                    coVerify(exactly = 1) { mockRepository.findById(resourceId) }
                    coVerify(exactly = 1) {
                        mockSnapshotService.createSnapshot(any(), any(), any(), any(), any(), any())
                    }
                }
            }

            context("command validation") {
                it("should reject command with blank message") {
                    // Given
                    val invalidCommand = command.copy(message = "")

                    // When/Then - Command creation should throw
                    runCatching {
                        CreateSnapshotCommand(
                            resourceId = resourceId,
                            content = content,
                            authorId = authorId,
                            message = "",
                            metadata = emptyMap(),
                            timestamp = timestamp,
                        )
                    }.isFailure shouldBe true
                }

                it("should reject command with too many metadata entries") {
                    // Given
                    val tooManyMetadata = (1..100).associate { "key$it" to "value$it" }

                    // When/Then - Command creation should throw
                    runCatching {
                        CreateSnapshotCommand(
                            resourceId = resourceId,
                            content = content,
                            authorId = authorId,
                            message = "Test",
                            metadata = tooManyMetadata,
                            timestamp = timestamp,
                        )
                    }.isFailure shouldBe true
                }
            }
        }
    })
