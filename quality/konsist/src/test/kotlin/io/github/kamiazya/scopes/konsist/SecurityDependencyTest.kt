package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

/**
 * Security dependency tests to prevent known vulnerabilities.
 */
class SecurityDependencyTest :
    StringSpec({

        "should not have vulnerable Netty versions" {
            Konsist
                .scopeFromProject()
                .files
                .filter { it.path.endsWith("build.gradle.kts") }
                .assertFalse { file ->
                    // Check for vulnerable Netty versions (< 4.1.124.Final)
                    val content = file.text
                    val nettyPattern = Regex("""io\.netty:netty-codec-http2:(\d+)\.(\d+)\.(\d+)\.Final""")
                    val matches = nettyPattern.findAll(content)

                    matches.any { match ->
                        val major = match.groupValues[1].toIntOrNull() ?: 0
                        val minor = match.groupValues[2].toIntOrNull() ?: 0
                        val patch = match.groupValues[3].toIntOrNull() ?: 0

                        // Vulnerable if version < 4.1.124
                        when {
                            major < 4 -> true
                            major == 4 && minor < 1 -> true
                            major == 4 && minor == 1 && patch < 124 -> true
                            else -> false
                        }
                    }
                }
        }

        "should have security patches in resolution strategy" {
            Konsist
                .scopeFromProject()
                .files
                .filter { it.path.endsWith("build.gradle.kts") && it.path.contains("root") }
                .assertFalse { file ->
                    // Root build.gradle.kts should have resolution strategy for security patches
                    val content = file.text
                    val hasResolutionStrategy = content.contains("resolutionStrategy")
                    val hasNettyForce = content.contains("force(\"io.netty:netty-codec-http2:")

                    // If we have a resolution strategy, it should include security patches
                    hasResolutionStrategy && !hasNettyForce
                }
        }
    })
