package sexy.kostya.rpclib.internal

import sexy.kostya.rpclib.ForClientUsage
import sexy.kostya.rpclib.TargetAll
import sexy.kostya.rpclib.TargetArgument
import java.nio.ByteBuffer
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.functions

internal class InterfaceSerializationData<T : Any>(theInterface: KClass<T>) {

    companion object {
        private val IgnoredMethods = setOf("equals", "hashCode", "finalize", "toString")
    }

    private val functions = HashMap<KFunction<*>, FunctionSerializationData>()
    private val id2Func = HashMap<Int, KFunction<*>>()

    init {
        val clientSide = theInterface.annotations.any { it is ForClientUsage }
        theInterface.functions
            .filter { !IgnoredMethods.contains(it.name) }
            .sortedWith(
                Comparator.comparing<KFunction<*>?, String?> { it.name }
                    .thenBy { it.toString() }
                    .thenBy { it.parameters.joinToString { it.type.toString() } }
            )
            .forEachIndexed { i, func ->
                var arguments = func.parameters.subList(1, func.parameters.size)
                val targeting = if (clientSide) {
                    Targeting.Server
                } else {
                    var targeting: Targeting? = null
                    for (a in func.annotations) {
                        when (a) {
                            is TargetAll -> targeting = Targeting.All
                            else -> continue
                        }
                    }
                    if (arguments.isNotEmpty()) {
                        val first = arguments.first()
                        if (first.annotations.any { it is TargetArgument }) {
                            targeting = if (arguments.first().type.classifier == UUID::class) {
                                Targeting.InternalID
                            } else {
                                Targeting.InternalList
                            }
                            arguments = arguments.subList(1, arguments.size)
                        }
                    }

                    targeting ?: throw IllegalStateException("target for function ${func.name} is not selected (use @TargetAll on a function or @TargetArgument on a first argument)")
                }

                try {

                    functions[func] = FunctionSerializationData(
                        i,
                        targeting,
                        arguments.map { ValueSerializationData(it.type) },
                        if (func.returnType.classifier == Unit::class) {
                            null
                        } else {
                            ValueSerializationData(func.returnType)
                        }
                    )
                    id2Func[i] = func
                } catch (t: Throwable) {
                    throw IllegalStateException("could not parse serialization data for $func", t)
                }
            }
    }

    operator fun get(func: KFunction<*>) =
        functions[func] ?: throw IllegalArgumentException("could not get serialization data for function $func")

    operator fun get(id: Int) =
        id2Func[id] ?: throw IllegalArgumentException("could not get serialization data for function of id $id")

    fun forEach(block: (KFunction<*>, FunctionSerializationData) -> Unit) = functions.forEach(block)

}

internal class FunctionSerializationData(
    val id: Int,
    val targeting: Targeting,
    val arguments: List<ValueSerializationData>,
    val result: ValueSerializationData?
)

internal class ValueSerializationData(
    val serializer: (ByteBuffer, Any?) -> Unit,
    val deserializer: (ByteBuffer) -> Any?
) {

    constructor(type: KType) : this(
        RpcSerializers.getSerializer(type),
        RpcSerializers.getDeserializer(type)
    )
}