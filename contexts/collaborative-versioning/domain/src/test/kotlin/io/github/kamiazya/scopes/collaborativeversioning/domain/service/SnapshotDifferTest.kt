package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class SnapshotDifferTest :
    DescribeSpec({
        describe("DefaultSnapshotDiffer") {
            val differ = DefaultSnapshotDiffer()

            describe("calculateDiff") {
                it("should detect no changes when snapshots are identical") {
                    // Given
                    val content = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test Resource")
                            put("status", "active")
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content)
                    val snapshot2 = createTestSnapshot(content = content)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe false
                    diff.contentDiff.operations shouldHaveSize 0
                    diff.metadataChanges shouldHaveSize 0
                }

                it("should detect added fields") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test Resource")
                        },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test Resource")
                            put("description", "New description")
                            put("status", "active")
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe true
                    diff.contentDiff.addedPaths shouldContain "description"
                    diff.contentDiff.addedPaths shouldContain "status"
                    diff.contentDiff.changeCount() shouldBe 2
                }

                it("should detect removed fields") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test Resource")
                            put("description", "To be removed")
                            put("status", "active")
                        },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test Resource")
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe true
                    diff.contentDiff.removedPaths shouldContain "description"
                    diff.contentDiff.removedPaths shouldContain "status"
                }

                it("should detect modified fields") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Original Title")
                            put("status", "draft")
                        },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Updated Title")
                            put("status", "active")
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe true
                    diff.contentDiff.modifiedPaths shouldContain "title"
                    diff.contentDiff.modifiedPaths shouldContain "status"
                }

                it("should detect nested object changes") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test")
                            put(
                                "config",
                                buildJsonObject {
                                    put("enabled", true)
                                    put("value", 42)
                                },
                            )
                        },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test")
                            put(
                                "config",
                                buildJsonObject {
                                    put("enabled", false)
                                    put("value", 100)
                                    put("newField", "added")
                                },
                            )
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe true
                    diff.contentDiff.modifiedPaths shouldContain "config.enabled"
                    diff.contentDiff.modifiedPaths shouldContain "config.value"
                    diff.contentDiff.addedPaths shouldContain "config.newField"
                }

                it("should detect array changes") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test")
                            putJsonArray("items") {
                                add("item1")
                                add("item2")
                            }
                        },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test")
                            putJsonArray("items") {
                                add("item1")
                                add("modified")
                                add("item3")
                            }
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe true
                    diff.contentDiff.modifiedPaths shouldContain "items[1]"
                    diff.contentDiff.addedPaths shouldContain "items[2]"
                }

                it("should detect metadata changes") {
                    // Given
                    val content = createTestContent()
                    val snapshot1 = createTestSnapshot(
                        content = content,
                        metadata = mapOf(
                            "env" to "development",
                            "version" to "1.0",
                            "toRemove" to "value",
                        ),
                    )
                    val snapshot2 = createTestSnapshot(
                        content = content,
                        metadata = mapOf(
                            "env" to "production", // modified
                            "version" to "1.0", // unchanged
                            "newKey" to "newValue", // added
                            // "toRemove" was removed
                        ),
                    )

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.hasChanges() shouldBe true
                    diff.contentDiff.hasChanges() shouldBe false // Content unchanged
                    diff.metadataChanges shouldHaveSize 3

                    diff.metadataChanges["env"].shouldBeInstanceOf<MetadataChange.Modified>()
                    diff.metadataChanges["newKey"].shouldBeInstanceOf<MetadataChange.Added>()
                    diff.metadataChanges["toRemove"].shouldBeInstanceOf<MetadataChange.Removed>()
                }

                it("should calculate size change") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject { put("small", "data") },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("large", "x".repeat(1000))
                        },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.calculateDiff(snapshot1, snapshot2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.sizeChange.fromSize shouldBe content1.sizeInBytes().toLong()
                    diff.sizeChange.toSize shouldBe content2.sizeInBytes().toLong()
                    diff.sizeChange.difference shouldBe (content2.sizeInBytes() - content1.sizeInBytes()).toLong()
                    diff.sizeChange.percentageChange shouldBe
                        ((content2.sizeInBytes() - content1.sizeInBytes()).toDouble() / content1.sizeInBytes()) * 100
                }
            }

            describe("calculateContentDiff") {
                it("should handle complex nested structures") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put(
                                "level1",
                                buildJsonObject {
                                    put(
                                        "level2",
                                        buildJsonObject {
                                            put(
                                                "level3",
                                                buildJsonObject {
                                                    put("value", "original")
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put(
                                "level1",
                                buildJsonObject {
                                    put(
                                        "level2",
                                        buildJsonObject {
                                            put(
                                                "level3",
                                                buildJsonObject {
                                                    put("value", "modified")
                                                    put("newField", "added")
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )

                    // When
                    val result = differ.calculateContentDiff(content1, content2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.modifiedPaths shouldContain "level1.level2.level3.value"
                    diff.addedPaths shouldContain "level1.level2.level3.newField"
                }

                it("should handle empty content") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(buildJsonObject {})
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("field", "value")
                        },
                    )

                    // When
                    val result = differ.calculateContentDiff(content1, content2)

                    // Then
                    result.shouldBeRight()
                    val diff = result.getOrNull()!!
                    diff.addedPaths shouldContain "field"
                    diff.operations shouldHaveSize 1
                    diff.operations.first().shouldBeInstanceOf<DiffOperation.Add>()
                }
            }

            describe("areContentsIdentical") {
                it("should return true for identical contents") {
                    // Given
                    val content = createTestContent()
                    val snapshot1 = createTestSnapshot(content = content)
                    val snapshot2 = createTestSnapshot(content = content)

                    // When
                    val result = differ.areContentsIdentical(snapshot1, snapshot2)

                    // Then
                    result shouldBe true
                }

                it("should return false for different contents") {
                    // Given
                    val content1 = ResourceContent.fromJsonElement(
                        buildJsonObject { put("a", "1") },
                    )
                    val content2 = ResourceContent.fromJsonElement(
                        buildJsonObject { put("a", "2") },
                    )
                    val snapshot1 = createTestSnapshot(content = content1)
                    val snapshot2 = createTestSnapshot(content = content2)

                    // When
                    val result = differ.areContentsIdentical(snapshot1, snapshot2)

                    // Then
                    result shouldBe false
                }
            }
        }
    })

// Helper functions
private fun createTestSnapshot(content: ResourceContent = createTestContent(), metadata: Map<String, String> = emptyMap()): Snapshot = Snapshot(
    id = SnapshotId.generate(),
    resourceId = ResourceId.generate(),
    versionId = VersionId.generate(),
    versionNumber = VersionNumber.from(1).getOrNull()!!,
    content = content,
    authorId = AgentId.from("test_author"),
    message = "Test snapshot",
    createdAt = Clock.System.now(),
    metadata = metadata,
)

private fun createTestContent(): ResourceContent = ResourceContent.fromJsonElement(
    buildJsonObject {
        put("title", "Test Resource")
        put("status", "active")
    },
)
