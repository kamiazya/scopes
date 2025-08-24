package io.github.kamiazya.scopes.userpreferences.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import io.github.kamiazya.scopes.userpreferences.infrastructure.config.HierarchyPreferencesConfig
import io.github.kamiazya.scopes.userpreferences.infrastructure.config.UserPreferencesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileBasedUserPreferencesRepository(configPathStr: String, private val logger: Logger, private val clock: Clock = Clock.System) :
    UserPreferencesRepository {

    private val configPath = Path(configPathStr)
    private val configFile = Path(configPathStr, UserPreferencesConfig.CONFIG_FILE_NAME)
    private var cachedAggregate: UserPreferencesAggregate? = null
    private val currentUserAggregateId = AggregateId.Simple.generate()

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
                    hierarchyPreferences = HierarchyPreferencesConfig(
                        maxDepth = preferences.hierarchyPreferences.maxDepth,
                        maxChildrenPerScope = preferences.hierarchyPreferences.maxChildrenPerScope,
                    ),
                )

                val json = UserPreferencesConfig.json.encodeToString(
                    UserPreferencesConfig.serializer(),
                    config,
                )

                configFile.writeText(json)
                cachedAggregate = aggregate

                logger.info("Saved user preferences to $configFile")
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
                logger.debug("No preferences file found at $configFile")
                return@withContext null
            }

            try {
                val json = configFile.readText()
                val config = UserPreferencesConfig.json.decodeFromString(
                    UserPreferencesConfig.serializer(),
                    json,
                )

                val hierarchyPreferences = HierarchyPreferences.create(
                    maxDepth = config.hierarchyPreferences.maxDepth,
                    maxChildrenPerScope = config.hierarchyPreferences.maxChildrenPerScope,
                ).bind()

                val now = clock.now()
                val preferences = UserPreferences(
                    hierarchyPreferences = hierarchyPreferences,
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
                logger.info("Loaded user preferences from $configFile")

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
