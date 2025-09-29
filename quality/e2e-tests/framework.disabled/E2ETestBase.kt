package io.github.kamiazya.scopes.e2e.framework

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldNotBe
import java.nio.file.Files
import java.nio.file.Path

/**
 * Base class for E2E tests with common setup and utilities.
 */
abstract class E2ETestBase : DescribeSpec() {

    protected lateinit var testDir: Path
    protected lateinit var cliRunner: CliRunner
    protected lateinit var daemonController: DaemonController

    init {
        beforeSpec {
            // Validate binaries
            BinaryManager.validateBinaries()
            BinaryManager.ensureExecutable()

            // Create test directory
            testDir = Files.createTempDirectory("scopes-e2e-test-")
            val testDirFile = testDir.toFile()

            // Get binary paths
            val cliBinary = BinaryManager.getCliBinary()
            val daemonBinary = BinaryManager.getDaemonBinary()

            // Log test environment
            println("=== E2E Test Environment ===")
            println("Platform: ${PlatformUtils.currentPlatform}")
            println("Test directory: $testDir")
            println("CLI binary: ${cliBinary.path} (${cliBinary.size} bytes)")
            println("Daemon binary: ${daemonBinary.path} (${daemonBinary.size} bytes)")
            println("===========================")

            // Create runners
            cliRunner = CliRunner(cliBinary.path.toFile(), testDirFile)
            daemonController = DaemonController()
        }

        afterSpec {
            // Cleanup
            try {
                daemonController.close()
            } catch (e: Exception) {
                println("Failed to stop daemon: $e")
            }

            try {
                testDir.toFile().deleteRecursively()
            } catch (e: Exception) {
                println("Failed to delete test directory: $e")
            }
        }
    }

    protected suspend fun withDaemon(block: suspend (DaemonController.DaemonInfo) -> Unit) {
        val info = daemonController.connect()
        if (info != null) {
            try {
                block(info)
            } finally {
                daemonController.disconnect()
            }
        }
    }

    protected fun createTestScope(title: String = "Test Scope", useGrpc: Boolean = false): String {
        val result = cliRunner.create(title, "E2E test scope", useGrpc = useGrpc)
        result.requireSuccess()

        val scopeId = CliRunner.extractScopeId(result.stdout)
        scopeId shouldNotBe null
        return scopeId!!
    }
}
