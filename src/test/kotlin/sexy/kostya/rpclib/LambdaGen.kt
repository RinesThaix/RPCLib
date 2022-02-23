package sexy.kostya.rpclib

import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

object LambdaGen {

    @JvmStatic
    fun main(args: Array<String>) {
        val argumentTypes = listOf("Boolean", "Int", "Long", "Float", "Double", "Any?")
        val returnTypes = listOf("Any?")
        val maxArgs = 4
        Path("SyntheticMethod.kt").bufferedWriter().use { writer ->
            writer.append("package sexy.kostya.rpclib.util.lambda\n\n")
            writer.append("// Autogenerated\n")
            writer.append("interface SyntheticMethod {\n\n")
            fun create(arguments: Array<String>, nameType: String, returnType: String) {
                writer.append("    fun get$nameType(${arguments.mapIndexed { i, type -> "a${i + 1}: $type" }.joinToString(", ")}): $returnType\n")
            }
            for (returnType in returnTypes) {
                val nameType = returnType.replace("?", "")
                for (count in 0..maxArgs) {
                    val arguments = Array(count + 2) { "" }
                    arguments[0] = "Any?"
                    arguments[arguments.lastIndex] = "Any?"
                    when (count) {
                        0 -> create(arguments, nameType, returnType)
                        1 -> {
                            for (a1 in argumentTypes) {
                                arguments[1] = a1
                                create(arguments, nameType, returnType)
                            }
                        }
                        2 -> {
                            for (a1 in argumentTypes) {
                                arguments[1] = a1
                                for (a2 in argumentTypes) {
                                    arguments[2] = a2
                                    create(arguments, nameType, returnType)
                                }
                            }
                        }
                        3 -> {
                            for (a1 in argumentTypes) {
                                arguments[1] = a1
                                for (a2 in argumentTypes) {
                                    arguments[2] = a2
                                    for (a3 in argumentTypes) {
                                        arguments[3] = a3
                                        create(arguments, nameType, returnType)
                                    }
                                }
                            }
                        }
                        4 -> {
                            for (a1 in argumentTypes) {
                                arguments[1] = a1
                                for (a2 in argumentTypes) {
                                    arguments[2] = a2
                                    for (a3 in argumentTypes) {
                                        arguments[3] = a3
                                        for (a4 in argumentTypes) {
                                            arguments[4] = a4
                                            create(arguments, nameType, returnType)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            writer.append("\n}")
        }
    }

}