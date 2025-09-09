package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.core.spec.style.StringSpec

/**
 * Test to track ULID.generate() usage and encourage migration to ULIDGenerator.
 *
 * This test helps monitor the progress of migrating from static ULID generation
 * to dependency-injected ULIDGenerator for better testability.
 */
class ULIDGenerationTest :
    StringSpec({

        "warn about ULID.generate() usage outside platform layer" {
            val functions = Konsist
                .scopeFromProduction()
                .files
                // Exclude platform infrastructure where SystemULIDGenerator is defined
                .filterNot { it.packagee?.name?.contains("platform.infrastructure") ?: false }
                // Exclude the ULID class itself
                .filterNot { it.packagee?.name?.contains("platform.commons.id") ?: false }
                .flatMap { it.functions() }
                .filter { function ->
                    // Check if the function body contains ULID.generate() call
                    function.text.contains("ULID.generate()")
                }

            if (functions.isNotEmpty()) {
                println("⚠️  Found ${functions.size} functions using ULID.generate():")
                functions.forEach { function ->
                    println("   - ${function.containingFile.path}")
                }
                println("   Consider migrating to ULIDGenerator interface for better testability.")
            }

            // Pass the test - this is just for tracking
            // To enforce, change this to assertFalse { functions.isNotEmpty() }
        }
    })
