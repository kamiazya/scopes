package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.*
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DefaultVersionSnapshotServiceTest :
    DescribeSpec({
        describe("DefaultVersionSnapshotService") {
            lateinit var mockRepository: TrackedResourceRepository
            lateinit var mockSerializer: SnapshotSerializer
            lateinit var mockMetadataValidator: SnapshotMetadataValidator
            lateinit var service: DefaultVersionSnapshotService

            beforeEach {
                mockRepository = mockk()
                mockSerializer = mockk()
                mockMetadataValidator = mockk()
                service = DefaultVersionSnapshotService(
                    repository = mockRepository,
                    serializer = mockSerializer,
                    metadataValidator = mockMetadataValidator,
                    logger = ConsoleLogger("test"),
                )
            }

            describe("createSnapshot") {
                it("should create a snapshot successfully") {
                    // Given
                    val resource = createTestTrackedResource()
                    val content = createTestContent()
                    val authorId = AgentId.from("user_123").getOrNull()!!
                    val message = "Test snapshot"
                    val metadata = mapOf("env" to "test")
                    val timestamp = Clock.System.now()

                    coEvery { mockMetadataValidator.validate(any(), any()) } returns Unit.right()
                    coEvery { mockRepository.save(any()) } returns resource.right()

                    // When
                    val result = service.createSnapshot(
                        resource = resource,
                        content = content,
                        authorId = authorId,
                        message = message,
                        metadata = metadata,
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeRight()
                    val snapshot = result.getOrNull()!!
                    snapshot.content shouldBe content
                    snapshot.authorId shouldBe authorId
                    snapshot.message shouldBe message
                    snapshot.metadata shouldContainKey "env"
                    snapshot.metadata shouldContainKey "created_via"
                    snapshot.metadata shouldContainKey "content_size_bytes"

                    coVerify { mockRepository.save(resource) }
                }

                it("should fail when content exceeds size limit") {
                    // Given
                    val resource = createTestTrackedResource()
                    val largeContent = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("data", "x".repeat(11_000_000)) // Over 10MB
                        },
                    )
                    val authorId = AgentId.from("user_123").getOrNull()!!

                    // When
                    val result = service.createSnapshot(
                        resource = resource,
                        content = largeContent,
                        authorId = authorId,
                        message = "Large snapshot",
                        timestamp = Clock.System.now(),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.StorageLimitExceeded>()
                }

                it("should fail when metadata validation fails") {
                    // Given
                    val resource = createTestTrackedResource()
                    val content = createTestContent()
                    val authorId = AgentId.from("user_123").getOrNull()!!
                    val invalidMetadata = mapOf("invalid_key!!!" to "value")

                    coEvery { mockMetadataValidator.validate(any(), any()) } returns
                        SnapshotServiceError.MetadataValidationError(
                            key = "invalid_key!!!",
                            value = "value",
                            reason = "Invalid key format",
                        ).left()

                    // When
                    val result = service.createSnapshot(
                        resource = resource,
                        content = content,
                        authorId = authorId,
                        message = "Test",
                        metadata = invalidMetadata,
                        timestamp = Clock.System.now(),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.MetadataValidationError>()
                }

                it("should fail when repository save fails") {
                    // Given
                    val resource = createTestTrackedResource()
                    val content = createTestContent()
                    val authorId = AgentId.from("user_123").getOrNull()!!

                    coEvery { mockMetadataValidator.validate(any(), any()) } returns Unit.right()
                    coEvery { mockRepository.save(any()) } returns
                        SaveTrackedResourceError.NetworkError(
                            message = "Connection failed",
                            cause = null,
                        ).left()

                    // When
                    val result = service.createSnapshot(
                        resource = resource,
                        content = content,
                        authorId = authorId,
                        message = "Test",
                        timestamp = Clock.System.now(),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.SerializationError>()
                }
            }

            describe("restoreSnapshot") {
                it("should restore from a specific snapshot") {
                    // Given
                    val targetSnapshotId = SnapshotId.generate()
                    val targetSnapshot = createTestSnapshot(
                        id = targetSnapshotId,
                        versionNumber = 2,
                    )
                    val resource = createTestTrackedResource(
                        snapshots = listOf(
                            createTestSnapshot(versionNumber = 1),
                            targetSnapshot,
                            createTestSnapshot(versionNumber = 3),
                        ),
                    )
                    val authorId = AgentId.from("user_123").getOrNull()!!
                    val message = "Restore to version 2"
                    val timestamp = Clock.System.now()

                    val restoredSnapshot = createTestSnapshot(versionNumber = 4)
                    val updatedResource = createTestTrackedResource(
                        currentVersion = 4,
                        snapshots = resource.getAllSnapshots() + restoredSnapshot,
                    )

                    coEvery { mockRepository.save(any()) } returns updatedResource.right()

                    // When
                    val result = service.restoreSnapshot(
                        resource = resource,
                        targetSnapshotId = targetSnapshotId,
                        authorId = authorId,
                        message = message,
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeRight()
                    val snapshot = result.getOrNull()!!
                    snapshot shouldNotBe null
                    snapshot.versionNumber.value shouldBe 4
                }

                it("should fail when target snapshot not found") {
                    // Given
                    val resource = createTestTrackedResource()
                    val nonExistentId = SnapshotId.generate()
                    val authorId = AgentId.from("user_123").getOrNull()!!

                    // When
                    val result = service.restoreSnapshot(
                        resource = resource,
                        targetSnapshotId = nonExistentId,
                        authorId = authorId,
                        message = "Restore",
                        timestamp = Clock.System.now(),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.SnapshotNotFound>()
                    error.snapshotId shouldBe nonExistentId
                }
            }

            describe("restoreToVersion") {
                it("should restore to a specific version number") {
                    // Given
                    val targetVersion = VersionNumber.from(2).getOrNull()!!
                    val resource = createTestTrackedResource(currentVersion = 3)
                    val authorId = AgentId.from("user_123").getOrNull()!!
                    val message = "Restore to v2"
                    val timestamp = Clock.System.now()

                    coEvery { mockRepository.save(any()) } returns resource.right()

                    // When
                    val result = service.restoreToVersion(
                        resource = resource,
                        targetVersion = targetVersion,
                        authorId = authorId,
                        message = message,
                        timestamp = timestamp,
                    )

                    // Then
                    result.shouldBeRight()
                    coVerify { mockRepository.save(resource) }
                }

                it("should fail when target version doesn't exist") {
                    // Given
                    val resource = createTestTrackedResource(
                        currentVersion = 3,
                        snapshots = listOf(
                            createTestSnapshot(versionNumber = 1),
                            createTestSnapshot(versionNumber = 3),
                        ),
                    )
                    val nonExistentVersion = VersionNumber.from(2).getOrNull()!!
                    val authorId = AgentId.from("user_123").getOrNull()!!

                    // When
                    val result = service.restoreToVersion(
                        resource = resource,
                        targetVersion = nonExistentVersion,
                        authorId = authorId,
                        message = "Restore",
                        timestamp = Clock.System.now(),
                    )

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.VersionNotFound>()
                }
            }

            describe("getSnapshot") {
                it("should retrieve a specific snapshot") {
                    // Given
                    val resourceId = ResourceId.generate()
                    val snapshotId = SnapshotId.generate()
                    val targetSnapshot = createTestSnapshot(id = snapshotId)
                    val resource = createTestTrackedResource(
                        id = resourceId,
                        snapshots = listOf(
                            createTestSnapshot(),
                            targetSnapshot,
                            createTestSnapshot(),
                        ),
                    )

                    coEvery { mockRepository.findById(resourceId) } returns resource.right()

                    // When
                    val result = service.getSnapshot(resourceId, snapshotId)

                    // Then
                    result.shouldBeRight()
                    val snapshot = result.getOrNull()
                    snapshot shouldBe targetSnapshot
                }

                it("should return null when resource not found") {
                    // Given
                    val resourceId = ResourceId.generate()
                    val snapshotId = SnapshotId.generate()

                    coEvery { mockRepository.findById(resourceId) } returns null.right()

                    // When
                    val result = service.getSnapshot(resourceId, snapshotId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe null
                }

                it("should return null when snapshot not found in resource") {
                    // Given
                    val resourceId = ResourceId.generate()
                    val snapshotId = SnapshotId.generate()
                    val resource = createTestTrackedResource(id = resourceId)

                    coEvery { mockRepository.findById(resourceId) } returns resource.right()

                    // When
                    val result = service.getSnapshot(resourceId, snapshotId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe null
                }
            }

            describe("getSnapshots") {
                it("should retrieve all snapshots for a resource") {
                    // Given
                    val resourceId = ResourceId.generate()
                    val snapshots = listOf(
                        createTestSnapshot(versionNumber = 1),
                        createTestSnapshot(versionNumber = 2),
                        createTestSnapshot(versionNumber = 3),
                    )
                    val resource = createTestTrackedResource(
                        id = resourceId,
                        snapshots = snapshots,
                    )

                    coEvery { mockRepository.findById(resourceId) } returns resource.right()

                    // When
                    val result = service.getSnapshots(resourceId)

                    // Then
                    result.shouldBeRight()
                    val retrievedSnapshots = result.getOrNull()!!
                    retrievedSnapshots shouldHaveSize 3
                    retrievedSnapshots shouldBe snapshots
                }

                it("should return empty list when resource not found") {
                    // Given
                    val resourceId = ResourceId.generate()

                    coEvery { mockRepository.findById(resourceId) } returns null.right()

                    // When
                    val result = service.getSnapshots(resourceId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe emptyList()
                }
            }

            describe("calculateSnapshotSize") {
                it("should calculate the total size of a snapshot") {
                    // Given
                    val content = createTestContent()
                    val metadata = mapOf(
                        "key1" to "value1",
                        "key2" to "value2",
                    )
                    val snapshot = createTestSnapshot(
                        content = content,
                        metadata = metadata,
                    )

                    // When
                    val size = service.calculateSnapshotSize(snapshot)

                    // Then
                    size shouldBe (
                        content.sizeInBytes() +
                            metadata.entries.sumOf { it.key.length + it.value.length } +
                            1024L
                        )
                }
            }

            describe("validateSnapshot") {
                it("should validate snapshot successfully") {
                    // Given
                    val resource = createTestTrackedResource()
                    val content = createTestContent()

                    // When
                    val result = service.validateSnapshot(resource, content)

                    // Then
                    result.shouldBeRight()
                }

                it("should fail when content is too large") {
                    // Given
                    val resource = createTestTrackedResource()
                    val largeContent = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("data", "x".repeat(11_000_000)) // Over 10MB
                        },
                    )

                    // When
                    val result = service.validateSnapshot(resource, largeContent)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.StorageLimitExceeded>()
                }
            }
        }
    })

// Helper functions
private fun createTestTrackedResource(
    id: ResourceId = ResourceId.generate(),
    currentVersion: Int = 3,
    snapshots: List<Snapshot> = listOf(
        createTestSnapshot(versionNumber = 1),
        createTestSnapshot(versionNumber = 2),
        createTestSnapshot(versionNumber = 3),
    ),
): TrackedResource {
    val resource = mockk<TrackedResource>(relaxed = true)
    every { resource.id } returns id
    every { resource.currentVersion } returns VersionNumber.from(currentVersion).getOrNull()!!
    every { resource.getAllSnapshots() } returns snapshots
    every { resource.getSnapshot(any()) } answers {
        val version = firstArg<VersionNumber>()
        snapshots.find { it.versionNumber == version }
    }
    every { resource.getCurrentSnapshot() } returns snapshots.lastOrNull()
    every { resource.getHistorySizeInBytes() } returns
        snapshots.sumOf { it.contentSizeInBytes().toLong() }

    // Mock createSnapshot to return a new snapshot
    every {
        resource.createSnapshot(any(), any(), any(), any())
    } returns createTestSnapshot(versionNumber = currentVersion + 1).right()

    // Mock restoreToVersion
    every {
        resource.restoreToVersion(any(), any(), any(), any())
    } answers {
        val targetVersion = firstArg<VersionNumber>()
        val snapshot = snapshots.find { it.versionNumber == targetVersion }
        if (snapshot != null) {
            createTestSnapshot(versionNumber = currentVersion + 1).right()
        } else {
            TrackedResourceError.VersionNotFound(id, targetVersion).left()
        }
    }

    return resource
}

private fun createTestSnapshot(
    id: SnapshotId = SnapshotId.generate(),
    versionNumber: Int = 1,
    content: ResourceContent = createTestContent(),
    metadata: Map<String, String> = emptyMap(),
): Snapshot = Snapshot(
    id = id,
    resourceId = ResourceId.generate(),
    versionId = VersionId.generate(),
    versionNumber = VersionNumber.from(versionNumber).getOrNull()!!,
    content = content,
    authorId = AgentId.from("test_author"),
    message = "Test snapshot v$versionNumber",
    createdAt = Clock.System.now(),
    metadata = metadata,
)

private fun createTestContent(): ResourceContent = ResourceContent.fromJsonElement(
    buildJsonObject {
        put("title", "Test Resource")
        put("description", "A test resource for unit tests")
        put("status", "active")
    },
)
