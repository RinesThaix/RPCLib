package sexy.kostya.rpclib.node

import sexy.kostya.rpclib.processor.DownstreamProcessor
import sexy.kostya.rpclib.processor.ProcessingContext
import sexy.kostya.rpclib.processor.UpstreamProcessor
import kotlin.reflect.KClass

sealed class RpcNode(
    processingContext: ProcessingContext
) {

    protected val upstreamProcessor = UpstreamProcessor(processingContext)
    protected abstract val downstreamProcessor: DownstreamProcessor

    fun <T : Any> registerInterface(interfaceClass: KClass<T>) =
        upstreamProcessor.registerInterface(interfaceClass)

    fun <T : Any, R : T> registerImplementation(interfaceClass: KClass<T>, implementation: R) =
        upstreamProcessor.registerImplementation(interfaceClass, implementation)

    open fun <T : Any> proxy(interfaceClass: KClass<T>) = downstreamProcessor.proxy(interfaceClass)

    abstract fun stop()

}