package io.github.kamiazya.scopes.userpreferences.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.AggregateId
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.AggregateVersion
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchySettings
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceKey
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceValue
import io.github.kamiazya.scopes.userpreferences.infrastructure.config.HierarchySettingsConfig
import io.github.kamiazya.scopes.userpreferences.infrastructure.config.UserPreferencesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText

class FileBasedUserPreferencesRepository(private val configPath: Path, private val logger: Logger, private val clock: Clock = Clock.System) :
    UserPreferencesRepository {

    private val configFile = configPath.resolve(UserPreferencesConfig.CONFIG_FILE_NAME)
    private var cachedAggregate: UserPreferencesAggregate? = null
    private val currentUserAggregateId = AggregateId.generate()

    init {
        configPath.createDirectories()
    }

    override suspend fun save(aggregate: UserPreferencesAggregate): Either<UserPreferencesError, Unit> = either {
        withContext(Dispatchers.IO) {
            try {
                val preferences = aggregate.preferences
                    ?: raise(UserPreferencesError.PreferencesNotInitialized())

                val config = UserPreferencesConfig(
                    version = UserPreferencesConfig.CURRENT_VERSION,
                    hierarchySettings = HierarchySettingsConfig(
                        maxDepth = preferences.hierarchySettings.maxDepth,
                        maxChildrenPerScope = preferences.hierarchySettings.maxChildrenPerScope,
                    ),
                    customPreferences = preferences.customPreferences
                        .mapKeys { it.key.value }
                        .mapValues { it.value.asString() },
                )

                val json = UserPreferencesConfig.json.encodeToString(
                    UserPreferencesConfig.serializer(),
                    config,
                )

                Files.write(configFile, json.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                cachedAggregate = aggregate

                logger.info("Saved user preferences to ${configFile.toAbsolutePath()}")
            } catch (e: Exception) {
                logger.error("Failed to save user preferences: ${e.message}")
                raise(
                    UserPreferencesError.InvalidPreferenceValue(
                        key = "save",
                        value = aggregate.id.value,
                        reason = "Failed to write preferences: ${e.message}",
                    ),
                )
            }
        }
    }

    override suspend fun findById(id: AggregateId): Either<UserPreferencesError, UserPreferencesAggregate?> = if (id == currentUserAggregateId) {
        findForCurrentUser()
    } else {
        null.right()
    }

    override suspend fun findForCurrentUser(): Either<UserPreferencesError, UserPreferencesAggregate?> = either {
        cachedAggregate?.let { return@either it }

        withContext(Dispatchers.IO) {
            if (!configFile.exists()) {
                logger.debug("No preferences file found at ${configFile.toAbsolutePath()}")
                return@withContext null
            }

            try {
                val json = configFile.readText()
                val config = UserPreferencesConfig.json.decodeFromString(
                    UserPreferencesConfig.serializer(),
                    json,
                )

                val hierarchySettings = HierarchySettings.create(
                    maxDepth = config.hierarchySettings.maxDepth,
                    maxChildrenPerScope = config.hierarchySettings.maxChildrenPerScope,
                ).bind()

                val customPreferences = config.customPreferences
                    .mapNotNull { (key, value) ->
                        PreferenceKey.create(key).fold(
                            {
                                logger.warn("Skipping invalid preference key: $key")
                                null
                            },
                            { validKey -> validKey to PreferenceValue.fromString(value) },
                        )
                    }
                    .toMap()

                val now = clock.now()
                val preferences = UserPreferences(
                    hierarchySettings = hierarchySettings,
                    customPreferences = customPreferences,
                    createdAt = now,
                    updatedAt = now,
                )

                val aggregate = UserPreferencesAggregate(
                    id = currentUserAggregateId,
                    version = AggregateVersion.initial(),
                    preferences = preferences,
                    createdAt = now,
                    updatedAt = now,
                )

                cachedAggregate = aggregate
                logger.info("Loaded user preferences from ${configFile.toAbsolutePath()}")

                aggregate
            } catch (e: Exception) {
                logger.error("Failed to load user preferences: ${e.message}")
                raise(
                    UserPreferencesError.InvalidPreferenceValue(
                        key = "load",
                        value = configFile.toString(),
                        reason = "Failed to read preferences: ${e.message}",
                    ),
                )
            }
        }
    }
}
