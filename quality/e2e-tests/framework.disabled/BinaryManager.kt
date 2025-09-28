package io.github.kamiazya.scopes.e2e.framework

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Manages binary artifacts for E2E testing.
 */
class BinaryManager {
    
    data class BinaryInfo(
        val path: Path,
        val exists: Boolean,
        val isExecutable: Boolean,
        val size: Long,
        val hash: String? = null
    )
    
    companion object {
        private const val CLI_BINARY_PROPERTY = "scopes.e2e.cli.binary"
        private const val DAEMON_BINARY_PROPERTY = "scopes.e2e.daemon.binary"
        private const val BINARY_DIR_PROPERTY = "scopes.e2e.binary.dir"
        
        private val platform = PlatformUtils.currentPlatform
        
        fun getCliBinary(): BinaryInfo {
            val path = findBinary("scopes", CLI_BINARY_PROPERTY)
            return getBinaryInfo(path)
        }
        
        fun getDaemonBinary(): BinaryInfo {
            val path = findBinary("scopesd", DAEMON_BINARY_PROPERTY)
            return getBinaryInfo(path)
        }
        
        private fun findBinary(baseName: String, propertyName: String): Path {
            // First, check if specific path is provided
            val specificPath = System.getProperty(propertyName)
            if (!specificPath.isNullOrBlank()) {
                return Paths.get(specificPath)
            }
            
            // Check binary directory
            val binaryDir = System.getProperty(BINARY_DIR_PROPERTY)
            if (!binaryDir.isNullOrBlank()) {
                val execName = PlatformUtils.getExecutableName(baseName)
                return Paths.get(binaryDir, execName)
            }
            
            // Look for platform-specific binary in common locations
            val execName = PlatformUtils.getExecutableName(baseName)
            val platformId = platform.toBinaryIdentifier()
            
            val searchPaths = listOf(
                // CI artifact locations
                Paths.get("binaries", "scopes-$platformId", execName),
                Paths.get("binaries", execName),
                
                // Local build locations
                Paths.get("apps", baseName, "build", "native", "nativeCompile", execName),
                Paths.get("../../apps", baseName, "build", "native", "nativeCompile", execName),
                
                // Downloaded binary locations
                Paths.get("build", "binaries", "scopes-$platformId", execName),
                
                // Current directory
                Paths.get(execName)
            )
            
            for (path in searchPaths) {
                if (Files.exists(path)) {
                    return path.toAbsolutePath()
                }
            }
            
            throw IllegalStateException(
                "Binary '$baseName' not found. Searched paths: ${searchPaths.joinToString(", ")}\n" +
                "Set $propertyName system property or $BINARY_DIR_PROPERTY to specify location."
            )
        }
        
        private fun getBinaryInfo(path: Path): BinaryInfo {
            val file = path.toFile()
            val exists = file.exists()
            val isExecutable = exists && file.canExecute()
            val size = if (exists) file.length() else 0
            
            // Calculate hash if file exists
            val hash = if (exists) {
                calculateSha256(file)
            } else null
            
            return BinaryInfo(path, exists, isExecutable, size, hash)
        }
        
        private fun calculateSha256(file: File): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        
        fun validateBinaries() {
            val cliBinary = getCliBinary()
            val daemonBinary = getDaemonBinary()
            
            val errors = mutableListOf<String>()
            
            if (!cliBinary.exists) {
                errors.add("CLI binary not found: ${cliBinary.path}")
            } else if (!cliBinary.isExecutable) {
                errors.add("CLI binary is not executable: ${cliBinary.path}")
            }
            
            if (!daemonBinary.exists) {
                errors.add("Daemon binary not found: ${daemonBinary.path}")
            } else if (!daemonBinary.isExecutable) {
                errors.add("Daemon binary is not executable: ${daemonBinary.path}")
            }
            
            if (errors.isNotEmpty()) {
                throw IllegalStateException(
                    "Binary validation failed:\n${errors.joinToString("\n")}"
                )
            }
        }
        
        fun ensureExecutable() {
            val cliBinary = getCliBinary()
            val daemonBinary = getDaemonBinary()
            
            if (cliBinary.exists) {
                PlatformUtils.makeExecutable(cliBinary.path.toFile())
            }
            
            if (daemonBinary.exists) {
                PlatformUtils.makeExecutable(daemonBinary.path.toFile())
            }
        }
    }
}