package sexy.kostya.rpclib.util.lambda

import java.lang.invoke.CallSite
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

object LambdaUtils {

    fun create(function: KFunction<*>) = create(
        function.javaMethod!!,
        SyntheticMethod::class.java,
        "getAny"
    )

    fun <T> create(method: Method, interfaceClass: Class<T>, methodName: String): T {
        val lookup =
            MethodHandles.privateLookupIn(method.declaringClass, MethodHandles.lookup()).`in`(method.declaringClass)
        val methodHandle = lookup.unreflect(method)
        val methodHandleType = methodHandle.type()
        val signature = syntheticMethodType(method, methodHandleType)
        val callSite = callsite(methodName, lookup, methodHandle, methodHandleType, signature, interfaceClass)
        val factory = callSite.target

        @Suppress("UNCHECKED_CAST")
        return factory.invoke() as T
    }

    private fun callsite(
        methodName: String,
        lookup: MethodHandles.Lookup,
        methodHandle: MethodHandle,
        methodHandleType: MethodType,
        signature: MethodType,
        interfaceClass: Class<*>
    ): CallSite = LambdaMetafactory.metafactory(
        lookup,
        methodName,
        MethodType.methodType(interfaceClass),
        signature,
        methodHandle,
        methodHandleType
    )

    private fun syntheticMethodType(method: Method, methodHandleType: MethodType): MethodType {
        val static = Modifier.isStatic(method.modifiers)
        var signature = if (static) {
            methodHandleType
        } else {
            methodHandleType.changeParameterType(0, Object::class.java)
        }
        val params = method.parameterTypes
        for (i in params.indices) {
            if (Object::class.java.isAssignableFrom(params[i])) {
                signature = signature.changeParameterType(
                    if (static) i else i + 1,
                    Object::class.java
                )
            }
        }
        if (Object::class.java.isAssignableFrom(signature.returnType())) {
            signature = signature.changeReturnType(Object::class.java)
        }
        return signature
    }

}