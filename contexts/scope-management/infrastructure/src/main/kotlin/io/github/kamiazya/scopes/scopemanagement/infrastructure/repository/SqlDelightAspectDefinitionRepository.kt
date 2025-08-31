package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.db.Aspect_definitions
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLDelight implementation of AspectDefinitionRepository.
 */
class SqlDelightAspectDefinitionRepository(private val database: ScopeManagementDatabase, private val json: Json = Json) : AspectDefinitionRepository {

    override suspend fun save(definition: AspectDefinition): Either<Any, AspectDefinition> = try {
        val (typeString, allowedValues) = when (val type = definition.type) {
            is AspectType.Text -> "TEXT" to null
            is AspectType.Numeric -> "NUMERIC" to null
            is AspectType.BooleanType -> "BOOLEAN" to null
            is AspectType.Ordered -> "ORDERED" to json.encodeToString(type.allowedValues.map { it.value })
        }

        val aspectTypeStr = if (allowedValues != null) "$typeString:$allowedValues" else typeString

        database.aspectDefinitionQueries.upsertAspectDefinition(
            key = definition.key.value,
            aspect_type = aspectTypeStr,
            description = definition.description,
            allow_multiple_values = if (definition.allowMultiple) 1L else 0L,
        )

        definition.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun findByKey(key: AspectKey): Either<Any, AspectDefinition?> = try {
        val result = database.aspectDefinitionQueries.findByKey(key.value).executeAsOneOrNull()
        result?.let { rowToAspectDefinition(it) }.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun findAll(): Either<Any, List<AspectDefinition>> = try {
        val results = database.aspectDefinitionQueries.getAllDefinitions().executeAsList()
        results.map { rowToAspectDefinition(it) }.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun existsByKey(key: AspectKey): Either<Any, Boolean> = try {
        val result = database.aspectDefinitionQueries.existsByKey(key.value).executeAsOne()
        result.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun deleteByKey(key: AspectKey): Either<Any, Boolean> = try {
        database.aspectDefinitionQueries.deleteByKey(key.value)
        true.right()
    } catch (e: Exception) {
        e.left()
    }

    private fun rowToAspectDefinition(row: Aspect_definitions): AspectDefinition {
        val key = AspectKey.create(row.key).fold(
            ifLeft = { error("Invalid key in database: $it") },
            ifRight = { it },
        )
        val typeString = row.aspect_type
        val description = row.description
        val allowMultiple = row.allow_multiple_values == 1L

        val type = when {
            typeString == "TEXT" -> AspectType.Text
            typeString == "NUMERIC" -> AspectType.Numeric
            typeString == "BOOLEAN" -> AspectType.BooleanType
            typeString.startsWith("ORDERED:") -> {
                val valuesJson = typeString.substring("ORDERED:".length)
                val values = json.decodeFromString<List<String>>(valuesJson)
                AspectType.Ordered(
                    values.mapNotNull { value ->
                        AspectValue.create(value).fold(
                            ifLeft = { null },
                            ifRight = { it },
                        )
                    },
                )
            }
            else -> error(
                "Unknown aspect type in database: '$typeString' for aspect key '$key'. " +
                    "Valid types are: TEXT, NUMERIC, BOOLEAN, or ORDERED:<json_array>",
            )
        }

        return when (type) {
            is AspectType.Text -> AspectDefinition.createText(key, description, allowMultiple)
            is AspectType.Numeric -> AspectDefinition.createNumeric(key, description, allowMultiple)
            is AspectType.BooleanType -> AspectDefinition.createBoolean(key, description, allowMultiple)
            is AspectType.Ordered -> {
                AspectDefinition.createOrdered(key, type.allowedValues, description, allowMultiple)
                    .fold(
                        ifLeft = { error("Failed to create ordered AspectDefinition: $it") },
                        ifRight = { it },
                    )
            }
        }
    }
}
