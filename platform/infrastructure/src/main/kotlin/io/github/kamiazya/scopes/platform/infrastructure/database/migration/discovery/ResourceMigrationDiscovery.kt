package io.github.kamiazya.scopes.platform.infrastructure.database.migration.scanner

import io.github.kamiazya.scopes.platform.infrastructure.database.migration.Migration
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.SqlMigration
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.JarURLConnection
import java.net.URL
import java.util.jar.JarFile

/**
 * Discovers migration files from the classpath resources.
 *
 * This implementation looks for SQL migration files in the specified resource path
 * and creates Migration instances from them. Migration files should follow the
 * naming convention: V{version}__{description}.sql
 *
 * For example:
 * - V1__Initial_schema.sql
 * - V2__Add_user_preferences.sql
 * - V3__Create_indices.sql
 */
class ResourceMigrationDiscovery(
    private val resourcePath: String,
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    private val logger: Logger,
) {

    fun discoverMigrations(): List<Migration> {
        logger.debug("Discovering migrations from resources at: $resourcePath")

        val migrations = mutableListOf<Migration>()

        try {
            // Get all resources in the migration path
            val resources = findResources()

            resources.forEach { resourceUrl ->
                val resourceName = resourceUrl.toString().substringAfterLast('/')

                if (resourceName.endsWith(".sql") && isValidMigrationFile(resourceName)) {
                    logger.debug("Found migration file: $resourceName")

                    try {
                        val migration = createMigrationFromResource(resourceUrl, resourceName)
                        migrations.add(migration)
                        logger.debug("Loaded migration: ${migration.version} - ${migration.description}")
                    } catch (e: Exception) {
                        logger.error("Failed to load migration from $resourceName", throwable = e)
                    }
                }
            }

            // Sort migrations by version
            migrations.sortBy { it.version }

            logger.info("Discovered ${migrations.size} migrations from resources")
        } catch (e: Exception) {
            logger.error("Failed to discover migrations from resources", throwable = e)
        }

        return migrations
    }

    private fun findResources(): List<URL> {
        val resources = mutableListOf<URL>()

        // Try to get resources from the classloader
        val urls = classLoader.getResources(resourcePath)

        while (urls.hasMoreElements()) {
            val url = urls.nextElement()

            when (url.protocol) {
                "file" -> {
                    // Handle file system resources
                    val file = java.io.File(url.toURI())
                    if (file.isDirectory) {
                        file.listFiles()?.forEach { migrationFile ->
                            if (migrationFile.isFile && migrationFile.name.endsWith(".sql")) {
                                resources.add(migrationFile.toURI().toURL())
                            }
                        }
                    }
                }

                "jar" -> {
                    // Handle resources inside JAR files via JarURLConnection for robustness
                    val connection = url.openConnection()
                    val jarFile: JarFile
                    val prefix: String
                    if (connection is JarURLConnection) {
                        jarFile = connection.jarFile
                        prefix = (connection.entryName?.let { "$it/" } ?: "$resourcePath/")
                    } else {
                        // Fallback: parse path
                        val jarPath = url.path.substringBefore("!")
                        jarFile = JarFile(jarPath.removePrefix("file:"))
                        prefix = "$resourcePath/"
                    }

                    jarFile.use { jf ->
                        jf.entries().asSequence()
                            .filter { entry ->
                                entry.name.startsWith(prefix) &&
                                    entry.name.endsWith(".sql") &&
                                    !entry.isDirectory
                            }
                            .forEach { entry ->
                                classLoader.getResource(entry.name)?.let { resources.add(it) }
                            }
                    }
                }
            }
        }

        return resources
    }

    private fun isValidMigrationFile(filename: String): Boolean {
        // Check if filename matches the pattern V{version}__{description}.sql
        val regex = Regex("V(\\d+)__(.+)\\.sql")
        return regex.matches(filename)
    }

    private fun createMigrationFromResource(resourceUrl: URL, filename: String): Migration {
        // Parse version and description from filename
        val regex = Regex("V(\\d+)__(.+)\\.sql")
        val matchResult = regex.matchEntire(filename)
        require(matchResult != null) { "Invalid migration filename: $filename" }

        val version = matchResult!!.groupValues[1].toLong()
        val description = matchResult.groupValues[2].replace('_', ' ')

        // Read SQL content
        val sql = resourceUrl.openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        }

        // Create an anonymous SqlMigration with the loaded content
        return object : SqlMigration(
            version = version,
            description = description,
        ) {
            override val sql: List<String> = sql.split(";").filter { it.isNotBlank() }.map { it.trim() }
        }
    }
}
