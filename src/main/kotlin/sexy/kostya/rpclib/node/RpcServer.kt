package sexy.kostya.rpclib.node

import sexy.kostya.rpclib.ForClientUsage
import sexy.kostya.rpclib.ForServerUsage
import sexy.kostya.rpclib.network.Server
import sexy.kostya.rpclib.processor.ProcessingContext
import sexy.kostya.rpclib.processor.ServerProcessor
import java.net.InetSocketAddress
import java.net.SocketAddress
import kotlin.reflect.KClass

class RpcServer(
    processingContext: ProcessingContext = ProcessingContext(),
    workerThreads: Int = Runtime.getRuntime().availableProcessors()
) : RpcNode(processingContext) {

    override val downstreamProcessor = ServerProcessor(upstreamProcessor)
    private val server = Server(downstreamProcessor, workerThreads)

    override fun <T : Any> proxy(interfaceClass: KClass<T>): T {
        require(interfaceClass.annotations.any { it is ForServerUsage } && interfaceClass.annotations.all { it !is ForClientUsage }) {
            "Interface that's being proxied by RpcServer must be marked with @ForServerUsage and mustn't be marked with @ForClientUsage"
        }
        return super.proxy(interfaceClass)
    }

    fun start(address: SocketAddress) {
        server.initialize(address)
        server.start()
    }

    fun start(address: String, port: Int) = start(InetSocketAddress(address, port))

    override fun stop() = server.stop()
}