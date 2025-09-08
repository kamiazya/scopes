package io.github.kamiazya.scopes.interfaces.cli.exitcode

/**
 * Standard exit codes for the Scopes CLI application.
 *
 * These codes follow UNIX conventions:
 * - 0: Success
 * - 1: General errors
 * - 2: Misuse of shell command
 * - 64-78: Specific error categories (following sysexits.h)
 * - 126: Command found but not executable
 * - 127: Command not found
 * - 128+n: Terminated by signal n
 */
enum class ExitCode(val code: Int, val description: String) {
    // Success
    SUCCESS(0, "Successful execution"),

    // General errors (1-63)
    GENERAL_ERROR(1, "General or unspecified error"),
    MISUSE(2, "Misuse of shell command (incorrect arguments)"),

    // Following sysexits.h conventions (64-78)
    USAGE_ERROR(64, "Command line usage error"),
    DATA_ERROR(65, "Data format error"),
    NO_INPUT(66, "Cannot open input"),
    NO_USER(67, "Addressee unknown"),
    NO_HOST(68, "Host name unknown"),
    UNAVAILABLE(69, "Service unavailable"),
    SOFTWARE_ERROR(70, "Internal software error"),
    OS_ERROR(71, "System error (e.g., can't fork)"),
    OS_FILE_ERROR(72, "Critical OS file missing"),
    CANT_CREATE(73, "Can't create (user) output file"),
    IO_ERROR(74, "Input/output error"),
    TEMP_FAIL(75, "Temporary failure; user invited to retry"),
    PROTOCOL(76, "Remote error in protocol"),
    NO_PERM(77, "Permission denied"),
    CONFIG_ERROR(78, "Configuration error"),

    // Application-specific codes (100-125)
    SCOPE_NOT_FOUND(100, "Scope not found"),
    CONTEXT_NOT_FOUND(101, "Context not found"),
    ASPECT_NOT_FOUND(102, "Aspect not found"),
    ALIAS_NOT_FOUND(103, "Alias not found"),
    INVALID_FILTER(104, "Invalid filter expression"),
    DUPLICATE_RESOURCE(105, "Resource already exists"),
    VALIDATION_ERROR(106, "Validation error"),
    DATABASE_ERROR(107, "Database operation failed"),
    NETWORK_ERROR(108, "Network operation failed"),
    SYNC_ERROR(109, "Synchronization failed"),
    AUTH_ERROR(110, "Authentication failed"),
    DEPENDENCY_ERROR(111, "Required dependency not available"),
    STATE_ERROR(112, "Invalid state for operation"),
    TRANSACTION_ERROR(113, "Transaction failed"),

    // Reserved for future use
    RESERVED_126(126, "Reserved - typically means command found but not executable"),
    RESERVED_127(127, "Reserved - typically means command not found"),
    ;

    companion object {
        /**
         * Maps common error types to appropriate exit codes
         */
        fun fromThrowable(throwable: Throwable): ExitCode = when (throwable) {
            is IllegalArgumentException -> USAGE_ERROR
            is IllegalStateException -> STATE_ERROR
            is SecurityException -> NO_PERM
            is NoSuchElementException -> SCOPE_NOT_FOUND
            is UnsupportedOperationException -> UNAVAILABLE
            else -> GENERAL_ERROR
        }
    }
}
