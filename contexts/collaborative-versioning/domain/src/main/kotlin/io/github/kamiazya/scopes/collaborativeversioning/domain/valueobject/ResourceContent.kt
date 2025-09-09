package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ResourceContentError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Value object representing the content of a versioned resource.
 *
 * ResourceContent encapsulates the actual data of a resource at a specific
 * point in time. It is stored as JSON to provide flexibility while maintaining
 * structure. The content format may vary based on the ResourceType.
 */
@JvmInline
value class ResourceContent private constructor(val value: JsonElement) {

    companion object {
        private const val MAX_CONTENT_SIZE = 1_048_576 // 1MB

        /**
         * Create ResourceContent from a JSON string.
         *
         * @param jsonString The JSON string representing the content
         * @return Either<ResourceContentError, ResourceContent>
         */
        fun fromJson(jsonString: String): Either<ResourceContentError, ResourceContent> = either {
            ensure(jsonString.isNotBlank()) {
                ResourceContentError.EmptyContent()
            }

            ensure(jsonString.length <= MAX_CONTENT_SIZE) {
                ResourceContentError.ContentTooLarge(
                    actualSize = jsonString.length,
                    maxSize = MAX_CONTENT_SIZE,
                )
            }

            val jsonElement = try {
                Json.parseToJsonElement(jsonString)
            } catch (e: Exception) {
                raise(
                    ResourceContentError.InvalidJson(
                        reason = e.message ?: "Failed to parse JSON",
                        cause = e,
                    ),
                )
            }

            ResourceContent(jsonElement)
        }

        /**
         * Create ResourceContent from a JsonElement.
         *
         * @param jsonElement The JSON element representing the content
         * @return ResourceContent
         */
        fun fromJsonElement(jsonElement: JsonElement): ResourceContent = ResourceContent(jsonElement)
    }

    /**
     * Get the content as a JSON string.
     */
    fun toJsonString(): String = Json.encodeToString(JsonElement.serializer(), value)

    /**
     * Get the size of the content in bytes (as JSON string).
     */
    fun sizeInBytes(): Int = toJsonString().length

    override fun toString(): String = toJsonString()
}
