package sexy.kostya.rpclib.internal

import sexy.kostya.rpclib.util.lambda.LambdaUtils
import sexy.kostya.rpclib.util.lambda.SyntheticInvocator
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.functions

internal object ImplementationMatcher {

    fun <T : Any, R : T> match(theInterface: KClass<T>, theImplementation: R): Map<Int, Pair<FunctionSerializationData, suspend (Array<Any?>) -> Any?>> {
        val data = InterfaceSerializationData(theInterface)
        val methods = HashMap<Int, Pair<FunctionSerializationData, suspend (Array<Any?>) -> Any?>>()
        val implementationClass = theImplementation::class
        val implementationFuncs = implementationClass.functions
        data.forEach { func, funcData ->
            check(func.parameters.size <= 4) { "you can't declare functions with more than 4 arguments within RPCLib" }
            var targetFunc = findFunction(func, implementationFuncs)
            if (targetFunc == null) {
                targetFunc = implementationClass.allSuperclasses.firstNotNullOfOrNull { findFunction(func, it.functions) }
            }
            if (targetFunc == null) {
                throw IllegalStateException("could not find implementation of func $func inside $theImplementation for $theInterface")
            }
            val invocation = SyntheticInvocator.createInvocation(
                LambdaUtils.create(targetFunc),
                func.parameters.map { it.type }.toTypedArray().let { it.sliceArray(1 until it.size) },
                func.returnType
            )
            methods[funcData.id] = funcData to { args ->
                args[0] = theImplementation
                suspendCoroutineUninterceptedOrReturn {
                    args[args.lastIndex] = it
                    invocation(args)
                }
            }
        }
        return methods
    }

    private fun findFunction(func: KFunction<*>, targets: Collection<KFunction<*>>) =
        targets.find { it.name == func.name && it.parameters == func.parameters }

}