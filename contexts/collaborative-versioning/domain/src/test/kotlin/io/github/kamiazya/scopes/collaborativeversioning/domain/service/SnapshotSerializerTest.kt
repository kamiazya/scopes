package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SnapshotSerializerTest :
    DescribeSpec({
        describe("DefaultSnapshotSerializer") {
            val serializer = DefaultSnapshotSerializer()

            describe("serialize") {
                it("should serialize a valid snapshot to JSON") {
                    // Given
                    val content = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("title", "Test Scope")
                            put("description", "A test scope for serialization")
                            put("status", "active")
                        },
                    )

                    val snapshot = Snapshot(
                        id = SnapshotId.generate(),
                        resourceId = ResourceId.generate(),
                        versionId = VersionId.generate(),
                        versionNumber = VersionNumber.from(1).getOrNull()!!,
                        content = content,
                        authorId = AgentId.from("user_123").getOrNull()!!,
                        message = "Initial snapshot",
                        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                        metadata = mapOf(
                            "environment" to "test",
                            "source" to "unit-test",
                        ),
                    )

                    // When
                    val result = serializer.serialize(snapshot)

                    // Then
                    result.shouldBeRight()
                    val json = result.getOrNull()!!
                    json shouldContain "\"id\""
                    json shouldContain "\"resourceId\""
                    json shouldContain "\"versionId\""
                    json shouldContain "\"versionNumber\":1"
                    json shouldContain "\"title\":\"Test Scope\""
                    json shouldContain "\"message\":\"Initial snapshot\""
                    json shouldContain "\"environment\":\"test\""
                }

                it("should handle snapshots with empty metadata") {
                    // Given
                    val snapshot = createTestSnapshot(metadata = emptyMap())

                    // When
                    val result = serializer.serialize(snapshot)

                    // Then
                    result.shouldBeRight()
                    val json = result.getOrNull()!!
                    json shouldContain "\"metadata\":{}"
                }

                it("should handle snapshots with complex nested content") {
                    // Given
                    val content = ResourceContent.fromJsonElement(
                        buildJsonObject {
                            put("name", "Complex Resource")
                            put(
                                "nested",
                                buildJsonObject {
                                    put(
                                        "level1",
                                        buildJsonObject {
                                            put("level2", "deep value")
                                        },
                                    )
                                },
                            )
                            put("array", Json.parseToJsonElement("[1, 2, 3]"))
                        },
                    )

                    val snapshot = createTestSnapshot(content = content)

                    // When
                    val result = serializer.serialize(snapshot)

                    // Then
                    result.shouldBeRight()
                    val json = result.getOrNull()!!
                    json shouldContain "\"nested\""
                    json shouldContain "\"level2\":\"deep value\""
                    json shouldContain "\"array\":[1,2,3]"
                }
            }

            describe("deserialize") {
                it("should deserialize valid JSON to a snapshot") {
                    // Given
                    val json = """{
                    "id": "snap_01HX5K5P5XQRJC5KRFWEHH1234",
                    "resourceId": "res_01HX5K5P5XQRJC5KRFWEHH5678",
                    "versionId": "ver_01HX5K5P5XQRJC5KRFWEHH9012",
                    "versionNumber": 1,
                    "content": {
                        "title": "Deserialized Scope",
                        "status": "active"
                    },
                    "authorId": "agent_system",
                    "message": "Test deserialization",
                    "createdAt": 1704067200,
                    "metadata": {
                        "test": "true"
                    }
                }"""

                    // When
                    val result = serializer.deserialize(json)

                    // Then
                    result.shouldBeRight()
                    val snapshot = result.getOrNull()!!
                    snapshot.id.toString() shouldBe "snap_01HX5K5P5XQRJC5KRFWEHH1234"
                    snapshot.resourceId.toString() shouldBe "res_01HX5K5P5XQRJC5KRFWEHH5678"
                    snapshot.versionNumber.value shouldBe 1
                    snapshot.message shouldBe "Test deserialization"
                    snapshot.metadata["test"] shouldBe "true"
                }

                it("should fail on invalid JSON") {
                    // Given
                    val invalidJson = "{ invalid json"

                    // When
                    val result = serializer.deserialize(invalidJson)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<SnapshotServiceError.DeserializationError>()
                    error.reason shouldContain "Failed to deserialize snapshot"
                }

                it("should fail on missing required fields") {
                    // Given
                    val incompleteJson = """{
                    "id": "snap_01HX5K5P5XQRJC5KRFWEHH1234",
                    "resourceId": "res_01HX5K5P5XQRJC5KRFWEHH5678"
                }"""

                    // When
                    val result = serializer.deserialize(incompleteJson)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<SnapshotServiceError.DeserializationError>()
                }

                it("should fail on invalid ID formats") {
                    // Given
                    val invalidIdJson = """{
                    "id": "invalid-id",
                    "resourceId": "res_01HX5K5P5XQRJC5KRFWEHH5678",
                    "versionId": "ver_01HX5K5P5XQRJC5KRFWEHH9012",
                    "versionNumber": 1,
                    "content": {"test": "data"},
                    "authorId": "agent_system",
                    "message": "Test",
                    "createdAt": 1704067200,
                    "metadata": {}
                }"""

                    // When
                    val result = serializer.deserialize(invalidIdJson)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<SnapshotServiceError.DeserializationError>()
                }
            }

            describe("round-trip serialization") {
                it("should preserve all data through serialize/deserialize cycle") {
                    // Given
                    val original = createTestSnapshot(
                        versionNumber = 42,
                        message = "Round-trip test",
                        metadata = mapOf(
                            "key1" to "value1",
                            "key2" to "value2",
                            "nested.key" to "nested.value",
                        ),
                    )

                    // When
                    val serialized = serializer.serialize(original)
                    val deserialized = serialized.flatMap { json ->
                        serializer.deserialize(json)
                    }

                    // Then
                    deserialized.shouldBeRight()
                    val result = deserialized.getOrNull()!!

                    result.id shouldBe original.id
                    result.resourceId shouldBe original.resourceId
                    result.versionId shouldBe original.versionId
                    result.versionNumber shouldBe original.versionNumber
                    result.content shouldBe original.content
                    result.authorId shouldBe original.authorId
                    result.message shouldBe original.message
                    result.createdAt.epochSeconds shouldBe original.createdAt.epochSeconds
                    result.metadata shouldBe original.metadata
                }
            }

            describe("serializeToElement and deserializeFromElement") {
                it("should serialize to JsonElement") {
                    // Given
                    val snapshot = createTestSnapshot()

                    // When
                    val result = serializer.serializeToElement(snapshot)

                    // Then
                    result.shouldBeRight()
                    val element = result.getOrNull()!!
                    element.toString() shouldContain "\"id\""
                }

                it("should deserialize from JsonElement") {
                    // Given
                    val element = buildJsonObject {
                        put("id", "snap_01HX5K5P5XQRJC5KRFWEHH1234")
                        put("resourceId", "res_01HX5K5P5XQRJC5KRFWEHH5678")
                        put("versionId", "ver_01HX5K5P5XQRJC5KRFWEHH9012")
                        put("versionNumber", 1)
                        put("content", buildJsonObject { put("test", "data") })
                        put("authorId", "agent_test")
                        put("message", "Test message")
                        put("createdAt", 1704067200L)
                        put("metadata", buildJsonObject {})
                    }

                    // When
                    val result = serializer.deserializeFromElement(element)

                    // Then
                    result.shouldBeRight()
                    val snapshot = result.getOrNull()!!
                    snapshot.message shouldBe "Test message"
                }
            }
        }
    })

// Helper function to create test snapshots
private fun createTestSnapshot(
    versionNumber: Int = 1,
    content: ResourceContent = ResourceContent.fromJsonElement(
        buildJsonObject { put("test", "data") },
    ),
    message: String = "Test snapshot",
    metadata: Map<String, String> = mapOf("test" to "true"),
): Snapshot = Snapshot(
    id = SnapshotId.generate(),
    resourceId = ResourceId.generate(),
    versionId = VersionId.generate(),
    versionNumber = VersionNumber.from(versionNumber).getOrNull()!!,
    content = content,
    authorId = AgentId.from("test_author"),
    message = message,
    createdAt = Instant.parse("2024-01-01T00:00:00Z"),
    metadata = metadata,
)
