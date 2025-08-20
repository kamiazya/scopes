package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.properties
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Architecture test to ensure proper time representation types are used.
 *
 * This test enforces:
 * - Time durations should use kotlin.time.Duration instead of Long
 * - Time instants/timestamps should use kotlinx.datetime.Instant instead of Long
 * - Property names should be descriptive (e.g., 'expiresAt' instead of 'expirationTime')
 */
class TimeRepresentationTest :
    DescribeSpec({

        describe("Time Representation Architecture Rules") {

            it("should not use Long for time-related properties") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .properties()
                    .assertTrue { property ->
                        // Skip if not a Long type
                        if (property.type?.name != "Long" && property.type?.name != "Long?") {
                            return@assertTrue true
                        }

                        val propertyName = property.name.lowercase()

                        // List of common time-related property name patterns
                        val timeRelatedPatterns = listOf(
                            "time", "millis", "seconds", "minutes", "hours", "days",
                            "duration", "timeout", "delay", "interval", "period",
                            "timestamp", "date", "epoch", "expir", "created", "updated",
                            "modified", "last", "next", "when", "until", "since",
                            "start", "end", "begin", "finish", "deadline", "recovery",
                        )

                        // Check if property name contains any time-related pattern
                        val isTimeRelated = timeRelatedPatterns.any { pattern ->
                            propertyName.contains(pattern)
                        }

                        // If it's time-related, it should NOT be Long
                        !isTimeRelated
                    }
            }

            it("should use Duration for time intervals") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .properties()
                    .assertTrue { property ->
                        val propertyName = property.name.lowercase()

                        // Skip test-specific internal variables
                        if (property.containingFile.path.contains("test", ignoreCase = true) &&
                            propertyName.endsWith("ms")
                        ) {
                            return@assertTrue true
                        }

                        // Duration-related property patterns
                        val durationPatterns = listOf(
                            "duration", "timeout", "delay", "interval", "period",
                            "elapsed", "remaining", "ttl", "lifetime", "wait",
                        )

                        val isDurationRelated = durationPatterns.any { pattern ->
                            propertyName.contains(pattern) && !propertyName.contains("at")
                        }

                        // If it's duration-related, it should use Duration type
                        if (isDurationRelated) {
                            val typeName = property.type?.name
                            typeName == "Duration" ||
                                typeName == "Duration?" ||
                                // Allow these for backwards compatibility or special cases
                                typeName == "Int" ||
                                typeName == "Int?" ||
                                // For simple counts
                                property.hasAnnotation { it.name == "Deprecated" }
                        } else {
                            true
                        }
                    }
            }

            it("should use Instant for timestamps") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .properties()
                    .assertTrue { property ->
                        val propertyName = property.name.lowercase()

                        // Timestamp-related property patterns
                        val timestampPatterns = listOf(
                            "timestamp", "createdat", "updatedat", "modifiedat",
                            "deletedat", "expiresat", "startedat", "endedat",
                            "lastat", "nextat", "detectedat", "occurredat",
                            "publishedat", "receivedat", "sentat", "scheduledat",
                        )

                        val isTimestampRelated = timestampPatterns.any { pattern ->
                            propertyName.replace("_", "").contains(pattern)
                        }

                        // If it's timestamp-related, it should use Instant type
                        if (isTimestampRelated) {
                            val typeName = property.type?.name
                            typeName == "Instant" ||
                                typeName == "Instant?" ||
                                property.hasAnnotation { it.name == "Deprecated" }
                        } else {
                            true
                        }
                    }
            }

            it("should use proper naming for time properties ending with 'Time'") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .properties()
                    .assertTrue { property ->
                        val propertyName = property.name

                        // If property ends with "Time" and represents a point in time
                        if (propertyName.endsWith("Time") && !propertyName.contains("Duration")) {
                            val typeName = property.type?.name

                            // These patterns typically represent points in time and should end with "At"
                            val pointInTimePatterns = listOf(
                                "createdTime", "updatedTime", "deletedTime",
                                "expiredTime", "expirationTime", "startTime",
                                "endTime", "resetTime",
                                "lastTime", "nextTime",
                            )
                            // Note: "recoveryTime" can be a duration (time until recovery)
                            // so it's excluded from point-in-time patterns

                            val isPointInTime = pointInTimePatterns.any { pattern ->
                                propertyName.contains(pattern, ignoreCase = true)
                            }

                            // If it's a point in time, it should use Instant and ideally end with "At"
                            if (isPointInTime) {
                                typeName == "Instant" ||
                                    typeName == "Instant?" ||
                                    property.hasAnnotation { it.name == "Deprecated" }
                            } else {
                                true
                            }
                        } else {
                            true
                        }
                    }
            }

            it("error classes should import time types instead of using fully qualified names") {
                Konsist
                    .scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("error", ignoreCase = true) ||
                            file.path.contains("exception", ignoreCase = true)
                    }
                    .assertTrue { file ->
                        val content = file.text

                        // Check if file uses Duration or Instant in property declarations
                        val usesDuration = content.contains(Regex("val\\s+\\w+:\\s*Duration")) ||
                            content.contains(Regex("val\\s+\\w+:\\s*Duration\\?"))
                        val usesInstant = content.contains(Regex("val\\s+\\w+:\\s*Instant")) ||
                            content.contains(Regex("val\\s+\\w+:\\s*Instant\\?"))

                        // If doesn't use these types, it's fine
                        if (!usesDuration && !usesInstant) {
                            return@assertTrue true
                        }

                        // If uses these types, should have imports
                        val hasProperImports =
                            (!usesDuration || content.contains("import kotlin.time.Duration")) &&
                                (!usesInstant || content.contains("import kotlinx.datetime.Instant"))

                        // Should not use fully qualified names in property declarations
                        val hasFullyQualifiedDuration =
                            content.contains(Regex("val\\s+\\w+:\\s*kotlin\\.time\\.Duration"))
                        val hasFullyQualifiedInstant =
                            content.contains(Regex("val\\s+\\w+:\\s*kotlinx\\.datetime\\.Instant"))

                        hasProperImports && !hasFullyQualifiedDuration && !hasFullyQualifiedInstant
                    }
            }
        }
    })
