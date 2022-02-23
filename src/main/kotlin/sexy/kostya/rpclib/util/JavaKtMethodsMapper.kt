package sexy.kostya.rpclib.util

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction

object JavaKtMethodsMapper {

    private val map = ConcurrentHashMap<Method, KFunction<*>>()

    operator fun get(method: Method) = map.computeIfAbsent(method) { method.kotlinFunction!! }

}