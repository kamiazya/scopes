package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.withNameContaining
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Resource management tests to ensure proper cleanup and shutdown handling.
 */
class ResourceManagementTest : StringSpec({

    "runBlocking with server operations should have proper error handling" {
        Konsist
            .scopeFromProject()
            .functions()
            .filter { it.text.contains("runBlocking") && it.text.contains("server") }
            .assertTrue { function ->
                // Should have try-catch-finally or similar error handling
                val hasProperErrorHandling = 
                    function.text.contains("try") && 
                    (function.text.contains("catch") || function.text.contains("finally"))
                
                hasProperErrorHandling
            }
    }

    "server lifecycle methods should handle shutdown gracefully" {
        Konsist
            .scopeFromProject()
            .functions()
            .withNameContaining("run", "start", "connect")
            .filter { it.text.contains("server") || it.text.contains("Server") }
            .assertTrue { function ->
                val functionText = function.text
                
                // Should have shutdown hook or finally block
                val hasShutdownHook = functionText.contains("addShutdownHook")
                val hasFinallyBlock = functionText.contains("finally")
                val hasCancellationHandling = functionText.contains("CancellationException")
                
                // At least one of these should be present
                hasShutdownHook || hasFinallyBlock || hasCancellationHandling
            }
    }

    "resources should be properly closed in finally blocks" {
        Konsist
            .scopeFromProject()
            .functions()
            .filter { 
                it.text.contains("InputStream") || 
                it.text.contains("OutputStream") || 
                it.text.contains("PrintStream") 
            }
            .assertTrue { function ->
                val functionText = function.text
                
                // If function creates streams, should handle cleanup
                if (functionText.contains("= PrintStream") || 
                    functionText.contains("= FileInputStream") ||
                    functionText.contains("= FileOutputStream")) {
                    
                    // Should have finally block or use-expression
                    functionText.contains("finally") || 
                    functionText.contains(".use ") ||
                    functionText.contains("addShutdownHook")
                } else {
                    true // Not creating streams, so no cleanup needed
                }
            }
    }
})