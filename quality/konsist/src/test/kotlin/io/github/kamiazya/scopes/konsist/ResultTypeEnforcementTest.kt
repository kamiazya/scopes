package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

class ResultTypeEnforcementTest :
    StringSpec({
        "Handlers should consistently return Either types" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter {
                    it.name.endsWith("Handler") &&
                        (
                            it.packagee?.name?.contains("command.handler") == true ||
                                it.packagee?.name?.contains("query.handler") == true
                            )
                }
                .flatMap { it.functions() }
                .filter { it.name == "invoke" || it.name == "handle" }
                .assertTrue { function ->
                    val returnType = function.returnType?.name
                    // Handlers should return Either
                    returnType?.contains("Either") == true
                }
        }

        // Temporarily disabled tests pending migration
        "Contracts layer should not define custom Result types".config(enabled = false) {
            Konsist
                .scopeFromProduction()
                .interfaces()
                .filter { it.packagee?.name?.contains("contracts") == true }
                .filter { it.hasSealedModifier }
                .assertTrue { iface ->
                    // No custom Result sealed interfaces
                    iface.name.endsWith("Result") != true
                }
        }

        "Application layer should return Either instead of custom Results".config(enabled = false) {
            Konsist
                .scopeFromProduction()
                .functions()
                .filter {
                    it.resideInPackage("..application..") &&
                        !it.resideInPackage("..test..")
                }
                .filter { function ->
                    val returnType = function.returnType?.name
                    returnType != null && returnType.endsWith("Result") && !returnType.contains("Either")
                }
                .assertTrue {
                    // Should not return custom Result types
                    false
                }
        }

        "Infrastructure adapters should convert to Either types".config(enabled = false) {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.name.endsWith("Adapter") }
                .filter { it.packagee?.name?.contains("infrastructure") == true }
                .flatMap { it.functions() }
                .filter { !it.hasPrivateModifier }
                .assertTrue { function ->
                    val returnType = function.returnType?.name
                    // Public adapter methods should return Either
                    returnType == null ||
                        returnType.contains("Either") ||
                        returnType == "Unit" ||
                        !returnType.endsWith("Result")
                }
        }

        "Repository interfaces should specify Either return types".config(enabled = false) {
            Konsist
                .scopeFromProduction()
                .interfaces()
                .filter { it.name.endsWith("Repository") }
                .flatMap { it.functions() }
                .filter { !it.hasPrivateModifier }
                .assertTrue { function ->
                    val returnType = function.returnType?.name
                    // Repository methods should return Either for fallible operations
                    returnType == null ||
                        returnType == "Unit" ||
                        returnType.contains("Either") ||
                        returnType.contains("Flow") ||
                        returnType.contains("List") ||
                        !returnType.contains("Result")
                }
        }

        "Domain services should use Either for error handling".config(enabled = false) {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter {
                    it.name.endsWith("Service") &&
                        it.packagee?.name?.contains("domain") == true
                }
                .flatMap { it.functions() }
                .filter { !it.hasPrivateModifier }
                .assertTrue { function ->
                    val returnType = function.returnType?.name
                    // Domain service methods should use Either for fallible operations
                    returnType == null ||
                        returnType == "Unit" ||
                        returnType.contains("Either") ||
                        !returnType.endsWith("Result")
                }
        }

        "Query handlers should return Either types consistently".config(enabled = false) {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter {
                    it.name.endsWith("Handler") &&
                        it.packagee?.name?.contains("query.handler") == true
                }
                .flatMap { it.functions() }
                .filter { it.name == "handle" || it.name == "invoke" }
                .assertTrue { function ->
                    val returnType = function.returnType?.name
                    // Query handlers should return Either<Error, Result>
                    returnType?.contains("Either") == true
                }
        }
    })
