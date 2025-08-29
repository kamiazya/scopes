package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionManagementTest {

    @Nested
    inner class `Transaction management should be handled at the application layer` {

        @Test
        fun `Repository classes should not use TransactionManager directly`() {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("Repository")
                .assertTrue {
                    val hasTransactionManager = it.properties()
                        .any { prop ->
                            prop.type?.name?.contains("TransactionManager") == true
                        }

                    val usesTransactionManager = it.functions()
                        .any { func ->
                            func.text.contains("transactionManager") ||
                                func.text.contains("transaction {") ||
                                func.text.contains(".transaction(")
                        }

                    // Repository should not have TransactionManager property or use it
                    !hasTransactionManager && !usesTransactionManager
                }
        }

        @Test
        fun `Handler classes should manage transactions`() {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("Handler")
                .filter { it.resideInPackage("..application..") }
                .assertTrue {
                    // Check if handler has TransactionManager as a dependency
                    val hasTransactionManager = it.primaryConstructor?.parameters
                        ?.any { param ->
                            param.type.name.contains("TransactionManager")
                        } ?: false

                    // Check if handler is using transaction blocks
                    val usesTransaction = it.functions()
                        .withName("invoke", "handle", "execute")
                        .any { func ->
                            func.text.contains("transactionManager") ||
                                func.text.contains("withTransaction") ||
                                func.text.contains("inTransaction")
                        }

                    // Handler should either have TransactionManager or not need it
                    // (some handlers might be read-only)
                    !hasTransactionManager || usesTransaction
                }
        }

        @Test
        fun `Infrastructure repositories should not create their own transactions`() {
            Konsist
                .scopeFromProject()
                .files
                .filter { it.path.contains("/infrastructure/") }
                .filter { it.path.contains("repository", ignoreCase = true) }
                .flatMap { it.functions() }
                .assertTrue { function ->
                    // Check for direct transaction usage patterns
                    val hasDirectTransaction = function.text.let { text ->
                        text.contains("transaction(database)") ||
                            text.contains("database.transaction") ||
                            text.contains("newSuspendedTransaction") ||
                            text.contains("beginTransaction()") ||
                            (text.contains("transaction {") && !text.contains("transactionManager"))
                    }

                    !hasDirectTransaction
                }
        }

        @Test
        fun `Domain services should not depend on transaction management`() {
            Konsist
                .scopeFromProject()
                .classes()
                .filter { it.resideInPackage("..domain..") }
                .assertTrue {
                    val importsTransaction = it.containingFile
                        .imports
                        .any { import ->
                            import.text.contains("transaction", ignoreCase = true) ||
                                import.text.contains("TransactionManager")
                        }

                    val usesTransaction = it.text.contains("transaction", ignoreCase = true) ||
                        it.text.contains("TransactionManager")

                    // Domain layer should be pure - no transaction dependencies
                    !importsTransaction && !usesTransaction
                }
        }

        @Test
        fun `Port adapters should delegate transaction management to handlers`() {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("PortAdapter", "Adapter")
                .filter { it.resideInPackage("..application..adapter..") }
                .assertTrue { adapter ->
                    // Port adapters should not manage transactions themselves
                    val hasTransactionManager = adapter.properties()
                        .any { prop ->
                            prop.type?.name?.contains("TransactionManager") == true
                        }

                    val usesTransaction = adapter.functions()
                        .any { func ->
                            func.text.contains("transaction {") ||
                                func.text.contains("transactionManager")
                        }

                    // Adapters should delegate to handlers, not manage transactions
                    !hasTransactionManager && !usesTransaction
                }
        }
    }

    @Nested
    inner class `Transaction patterns should be consistent` {

        @Test
        fun `All transaction usage should use the platform TransactionManager abstraction`() {
            Konsist
                .scopeFromProject()
                .files
                .filter { !it.path.contains("/test/") }
                .filter { !it.path.contains("TransactionManager.kt") }
                .flatMap { it.functions() }
                .filter { function ->
                    function.text.contains("transaction") ||
                        function.text.contains("Transaction")
                }
                .assertTrue { function ->
                    val usesRawExposedTransaction = function.text.let { text ->
                        text.contains("org.jetbrains.exposed.sql.transactions.transaction") ||
                            text.contains("exposed.sql.transactions") ||
                            text.contains("Transaction.current()")
                    }

                    // Should not use Exposed transactions directly
                    !usesRawExposedTransaction
                }
        }

        @Test
        fun `Repository interfaces should not expose transaction-specific methods`() {
            Konsist
                .scopeFromProject()
                .interfaces()
                .filter { it.resideInPackage("..domain..repository..") }
                .assertTrue { interfaceDeclaration ->
                    interfaceDeclaration.functions().none { function ->
                        function.name.contains("transaction", ignoreCase = true) ||
                            function.returnType?.name?.contains("Transaction") == true ||
                            function.parameters.any { param ->
                                param.type.name.contains("Transaction")
                            }
                    }
                }
        }
    }
}
