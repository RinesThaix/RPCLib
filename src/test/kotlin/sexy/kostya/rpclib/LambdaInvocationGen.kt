package sexy.kostya.rpclib

import java.io.BufferedWriter
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

object LambdaInvocationGen {

    private val ArgumentTypes = listOf("Boolean" to "B", "Int" to "I", "Long" to "L", "Float" to "F", "Double" to "D", "Any?" to "A")
    private val ReturnTypes = listOf("Any?" to "A")
    private const val MaxArgs = 4 + 2
    private val ArgumentNames = (0 until MaxArgs).map { "it[$it]" }

    @JvmStatic
    fun main(args: Array<String>) {
        Path("SyntheticInvocator.kt").bufferedWriter().use { writer ->
            writer.append("package sexy.kostya.rpclib.util.lambda\n\n")
            writer.append("import kotlin.reflect.KClass\n")
            writer.append("import kotlin.reflect.KType\n")
            writer.append("import kotlin.reflect.KClassifier\n\n")
            writer.append("// Autogenerated\n")
            writer.append("object SyntheticInvocator {\n\n")
            writer.append("    private val KType.c: KClassifier?\n")
            writer.append("        get() = this.classifier\n\n")
            writer.append("    internal fun isAny(type: KType) = type.isMarkedNullable || type.c == Any::class || (type.c as KClass<*>).javaPrimitiveType == null\n\n")
            writer.append("    fun createInvocation(sm: SyntheticMethod, at: Array<KType>, resultType: KType): (Array<Any?>) -> Any? {\n")
            create(writer, 2, "Any", false)
            writer.append("    }\n")
            writer.append("\n}\n\n")
            writer.append("private typealias B = Boolean\n")
            writer.append("private typealias I = Int\n")
            writer.append("private typealias L = Long\n")
            writer.append("private typealias F = Float\n")
            writer.append("private typealias D = Double\n")
            writer.append("private typealias A = Any?\n\n")
            writer.append("private val BC = B::class\n")
            writer.append("private val IC = I::class\n")
            writer.append("private val LC = L::class\n")
            writer.append("private val FC = F::class\n")
            writer.append("private val DC = D::class\n")
        }
    }

    private fun create(writer: BufferedWriter, offsetInTabs: Int, nameType: String, forMap: Boolean) {
        val offset = "    ".repeat(offsetInTabs)
        writer.append("${offset}when (at.size) {\n")
        for (size in 0..MaxArgs - 2) {
            val args = ArgumentNames.subList(0, size + 2)
            writer.append("$offset    $size -> {\n")
            val types = Array(size) { "" }
            fun create0(extraOffsetInTabs: Int = -1) {
                val offset = if (extraOffsetInTabs == -1) "" else (offset + "    ".repeat(extraOffsetInTabs))
                val mappedArgs = args.mapIndexed { i, name ->
                    if (i == 0 || i == args.lastIndex || types[i - 1] == "A") {
                        name
                    } else {
                        "$name as ${types[i - 1]}"
                    }
                }
                writer.append("${offset}return${if (forMap) "@func" else ""} { sm.get$nameType(${mappedArgs.joinToString(", ")}) }\n")
            }
            fun condition(extraOffsetInTabs: Int, type: String, withBracers: Boolean, block: () -> Unit) {
                val offset = offset + "    ".repeat(extraOffsetInTabs)
                val index = extraOffsetInTabs - 2
                val condition = if (type == "A") {
                    "isAny(at[$index])"
                } else {
                    "at[$index].c == ${type}C"
                }
                if (withBracers) {
                    writer.append("${offset}if ($condition) {\n")
                    block()
                    writer.append("$offset}\n")
                } else {
                    writer.append("${offset}if ($condition) ")
                    block()
                }
            }
            when (size) {
                0 -> create0(2)
                1 -> {
                    for (a1 in ArgumentTypes) {
                        types[0] = a1.second
                        condition(2, a1.second, false) {
                            create0()
                        }
                    }
                }
                2 -> {
                    for (a1 in ArgumentTypes) {
                        types[0] = a1.second
                        condition(2, a1.second, true) {
                            for (a2 in ArgumentTypes) {
                                types[1] = a2.second
                                condition(3, a2.second, false) {
                                    create0()
                                }
                            }
                        }
                    }
                }
                3 -> {
                    for (a1 in ArgumentTypes) {
                        types[0] = a1.second
                        condition(2, a1.second, true) {
                            for (a2 in ArgumentTypes) {
                                types[1] = a2.second
                                condition(3, a2.second, true) {
                                    for (a3 in ArgumentTypes) {
                                        types[2] = a3.second
                                        condition(4, a3.second, false) {
                                            create0()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    for (a1 in ArgumentTypes) {
                        types[0] = a1.second
                        condition(2, a1.second, true) {
                            for (a2 in ArgumentTypes) {
                                types[1] = a2.second
                                condition(3, a2.second, true) {
                                    for (a3 in ArgumentTypes) {
                                        types[2] = a3.second
                                        condition(4, a3.second, true) {
                                            for (a4 in ArgumentTypes) {
                                                types[3] = a4.second
                                                condition(5, a4.second, false) {
                                                    create0()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (size != 0) {
                writer.append("$offset        throw RuntimeException()\n")
            }
            writer.append("$offset    }\n")
        }
        writer.append("$offset    else -> throw RuntimeException()\n")
        writer.append("$offset}\n")
    }

}