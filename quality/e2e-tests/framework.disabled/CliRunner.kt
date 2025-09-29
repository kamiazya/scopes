package io.github.kamiazya.scopes.e2e.framework

import org.zeroturnaround.exec.ProcessExecutor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runs CLI commands for E2E testing.
 */
class CliRunner(private val cliBinary: File, private val workDir: File = File(System.getProperty("java.io.tmpdir")), private val commandTimeout: Long = 60000) {

    data class CliResult(val exitCode: Int, val stdout: String, val stderr: String, val success: Boolean) {
        fun requireSuccess(): CliResult {
            require(success) {
                "CLI command failed with exit code $exitCode\nSTDOUT:\n$stdout\nSTDERR:\n$stderr"
            }
            return this
        }
    }

    fun run(vararg args: String, env: Map<String, String> = emptyMap()): CliResult {
        val command = listOf(cliBinary.absolutePath) + args.toList()

        val executor = ProcessExecutor()
            .command(command)
            .directory(workDir)
            .readOutput(true)
            .timeout(commandTimeout, TimeUnit.MILLISECONDS)
            .destroyOnExit()

        // Add environment variables
        env.forEach { (key, value) ->
            executor.environment(key, value)
        }

        val result = try {
            executor.execute()
        } catch (e: Exception) {
            return CliResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Process execution failed",
                success = false,
            )
        }

        return CliResult(
            exitCode = result.exitValue,
            stdout = result.outputUTF8(),
            stderr = result.errorUTF8(),
            success = result.exitValue == 0,
        )
    }

    fun runWithDaemon(vararg args: String, daemonEndpoint: String? = null): CliResult {
        val env = if (daemonEndpoint != null) {
            mapOf("SCOPESD_ENDPOINT" to daemonEndpoint)
        } else {
            emptyMap()
        }

        return run(*args, env = env)
    }

    fun runWithGrpcTransport(vararg args: String, daemonEndpoint: String? = null): CliResult {
        val env = mutableMapOf("SCOPES_TRANSPORT" to "grpc")
        if (daemonEndpoint != null) {
            env["SCOPESD_ENDPOINT"] = daemonEndpoint
        }

        return run(*args, env = env)
    }

    // Common command helpers

    fun version(): CliResult = run("--version")

    fun help(): CliResult = run("--help")

    fun info(daemonEndpoint: String? = null): CliResult = runWithDaemon("info", daemonEndpoint = daemonEndpoint)

    fun create(title: String, description: String? = null, useGrpc: Boolean = false): CliResult {
        val args = mutableListOf("create", title)
        description?.let { args.addAll(listOf("-d", it)) }

        return if (useGrpc) {
            runWithGrpcTransport(*args.toTypedArray())
        } else {
            run(*args.toTypedArray())
        }
    }

    fun list(): CliResult = run("list")

    fun get(alias: String): CliResult = run("get", alias)

    fun delete(alias: String): CliResult = run("delete", alias)

    fun update(alias: String, vararg args: String): CliResult = run("update", alias, *args)

    companion object {
        fun extractScopeId(output: String): String? {
            // Extract scope ID from create command output
            val regex = """Created scope with canonical alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
            return regex.find(output)?.groupValues?.get(1)
        }

        fun extractDaemonInfo(output: String): Map<String, String> {
            val info = mutableMapOf<String, String>()
            val lines = output.lines()

            for (line in lines) {
                when {
                    line.contains("Version:") -> {
                        info["version"] = line.substringAfter("Version:").trim()
                    }
                    line.contains("Status:") -> {
                        info["status"] = line.substringAfter("Status:").trim()
                    }
                    line.contains("Address:") -> {
                        info["address"] = line.substringAfter("Address:").trim()
                    }
                    line.contains("PID:") -> {
                        info["pid"] = line.substringAfter("PID:").trim()
                    }
                    line.contains("API Version:") -> {
                        info["apiVersion"] = line.substringAfter("API Version:").trim()
                    }
                }
            }

            return info
        }
    }
}
