package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
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

class VersionSnapshotServiceTest :
    DescribeSpec({

        val mockRepository = mockk<TrackedResourceRepository>()
        val mockSerializer = mockk<SnapshotSerializer>()
        val mockMetadataValidator = mockk<SnapshotMetadataValidator>()
        val logger = ConsoleLogger("VersionSnapshotServiceTest")

        val service = DefaultVersionSnapshotService(
            repository = mockRepository,
            serializer = mockSerializer,
            metadataValidator = mockMetadataValidator,
            logger = logger,
        )

        val resourceId = ResourceId.generate()
        val authorId = AgentId.generate()
        val timestamp = Clock.System.now()
        val resourceType = ResourceType.from("TestResource").getOrNull()!!

        describe("VersionSnapshotService") {

            context("createSnapshot") {
                it("should create snapshot successfully") {
                    // Given
                    val content = ResourceContent.fromJsonElement(JsonPrimitive("test content"))
                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    )

                    coEvery { mockMetadataValidator.validate(any(), any()) } returns Unit.right()
                    coEvery { mockRepository.save(any()) } returns trackedResource.right()

                    // When
                    val result = service.createSnapshot(
                        resource = trackedResource,
                        content = content,
                        authorId = authorId,
                        message = "Test snapshot",
                        metadata = mapOf("user_key" to "user_value"),
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeRight()
                    val snapshot = result.getOrNull()!!
                    snapshot.resourceId shouldBe resourceId
                    snapshot.content shouldBe content
                    snapshot.authorId shouldBe authorId
                    snapshot.message shouldBe "Test snapshot"
                    snapshot.metadata["created_via"] shouldBe "VersionSnapshotService"
                    snapshot.metadata["user_key"] shouldBe "user_value"

                    coVerify(exactly = 1) { mockRepository.save(any()) }
                }

                it("should fail when content exceeds size limit") {
                    // Given
                    val largeContent = ResourceContent.fromJsonElement(
                        JsonPrimitive("x".repeat(11_000_000)), // 11MB
                    )
                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    )

                    // When
                    val result = service.createSnapshot(
                        resource = trackedResource,
                        content = largeContent,
                        authorId = authorId,
                        message = "Test snapshot",
                        metadata = emptyMap(),
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.StorageLimitExceeded>()

                    coVerify(exactly = 0) { mockRepository.save(any()) }
                }

                it("should fail when metadata validation fails") {
                    // Given
                    val content = ResourceContent.fromJsonElement(JsonPrimitive("test content"))
                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    )

                    val validationError = SnapshotServiceError.MetadataValidationError(
                        key = "invalid_key",
                        value = "value",
                        reason = "Key contains invalid characters",
                    )

                    coEvery { mockMetadataValidator.validate("invalid_key", any()) } returns validationError.left()

                    // When
                    val result = service.createSnapshot(
                        resource = trackedResource,
                        content = content,
                        authorId = authorId,
                        message = "Test snapshot",
                        metadata = mapOf("invalid_key" to "value"),
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error shouldBe validationError

                    coVerify(exactly = 0) { mockRepository.save(any()) }
                }
            }

            context("restoreSnapshot") {
                it("should restore to snapshot successfully") {
                    // Given
                    val content = ResourceContent.fromJsonElement(JsonPrimitive("test content"))
                    val targetSnapshotId = SnapshotId.generate()

                    val targetSnapshot = Snapshot(
                        id = targetSnapshotId,
                        resourceId = resourceId,
                        versionId = VersionId.generate(),
                        versionNumber = VersionNumber.from(1).getOrNull()!!,
                        content = content,
                        authorId = authorId,
                        message = "Original snapshot",
                        createdAt = timestamp,
                        metadata = emptyMap(),
                    )

                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    ).let { resource ->
                        // Add the target snapshot to the resource
                        resource.copy(
                            snapshots = listOf(targetSnapshot),
                            currentVersion = targetSnapshot.versionNumber,
                        )
                    }

                    val restoredResource = trackedResource.restoreToVersion(
                        targetVersionNumber = targetSnapshot.versionNumber,
                        authorId = authorId,
                        message = "Restored",
                        timestamp = timestamp,
                    ).getOrNull()!!

                    coEvery { mockRepository.save(any()) } returns restoredResource.right()

                    // When
                    val result = service.restoreSnapshot(
                        resource = trackedResource,
                        targetSnapshotId = targetSnapshotId,
                        authorId = authorId,
                        message = "Restore test",
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeRight()
                    val restoredSnapshot = result.getOrNull()!!
                    restoredSnapshot.content shouldBe content

                    coVerify(exactly = 1) { mockRepository.save(any()) }
                }

                it("should fail when target snapshot not found") {
                    // Given
                    val targetSnapshotId = SnapshotId.generate()
                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    )

                    // When
                    val result = service.restoreSnapshot(
                        resource = trackedResource,
                        targetSnapshotId = targetSnapshotId,
                        authorId = authorId,
                        message = "Restore test",
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.SnapshotNotFound>()
                    error.snapshotId shouldBe targetSnapshotId

                    coVerify(exactly = 0) { mockRepository.save(any()) }
                }
            }

            context("validateSnapshot") {
                it("should pass validation for valid content") {
                    // Given
                    val content = ResourceContent.fromJsonElement(JsonPrimitive("test content"))
                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    )

                    // When
                    val result = service.validateSnapshot(trackedResource, content)

                    // Then
                    result.shouldBeRight()
                }

                it("should fail validation for oversized content") {
                    // Given
                    val largeContent = ResourceContent.fromJsonElement(
                        JsonPrimitive("x".repeat(11_000_000)), // 11MB
                    )
                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    )

                    // When
                    val result = service.validateSnapshot(trackedResource, largeContent)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.StorageLimitExceeded>()
                }
            }

            context("getSnapshots") {
                it("should retrieve all snapshots for a resource") {
                    // Given
                    val snapshot1 = Snapshot(
                        id = SnapshotId.generate(),
                        resourceId = resourceId,
                        versionId = VersionId.generate(),
                        versionNumber = VersionNumber.from(1).getOrNull()!!,
                        content = ResourceContent.fromJsonElement(JsonPrimitive("v1")),
                        authorId = authorId,
                        message = "Version 1",
                        createdAt = timestamp,
                        metadata = emptyMap(),
                    )

                    val snapshot2 = Snapshot(
                        id = SnapshotId.generate(),
                        resourceId = resourceId,
                        versionId = VersionId.generate(),
                        versionNumber = VersionNumber.from(2).getOrNull()!!,
                        content = ResourceContent.fromJsonElement(JsonPrimitive("v2")),
                        authorId = authorId,
                        message = "Version 2",
                        createdAt = timestamp.plus(kotlinx.datetime.Duration.minutes(1)),
                        metadata = emptyMap(),
                    )

                    val trackedResource = TrackedResource.create(
                        id = resourceId,
                        resourceType = resourceType,
                        createdAt = timestamp,
                    ).copy(
                        snapshots = listOf(snapshot1, snapshot2),
                        currentVersion = snapshot2.versionNumber,
                    )

                    coEvery { mockRepository.findById(resourceId) } returns trackedResource.right()

                    // When
                    val result = service.getSnapshots(resourceId)

                    // Then
                    result.shouldBeRight()
                    val snapshots = result.getOrNull()!!
                    snapshots.size shouldBe 2
                    snapshots[0] shouldBe snapshot1
                    snapshots[1] shouldBe snapshot2
                }

                it("should return empty list when resource not found") {
                    // Given
                    coEvery { mockRepository.findById(resourceId) } returns null.right()

                    // When
                    val result = service.getSnapshots(resourceId)

                    // Then
                    result.shouldBeRight()
                    val snapshots = result.getOrNull()!!
                    snapshots shouldBe emptyList()
                }
            }
        }
    })
