package sexy.kostya.rpclib.node

import sexy.kostya.rpclib.ForClientUsage
import sexy.kostya.rpclib.ForServerUsage
import sexy.kostya.rpclib.network.Client
import sexy.kostya.rpclib.network.Server
import sexy.kostya.rpclib.processor.ClientProcessor
import sexy.kostya.rpclib.processor.ProcessingContext
import sexy.kostya.rpclib.processor.ServerProcessor
import java.net.InetSocketAddress
import java.net.SocketAddress
import kotlin.reflect.KClass

class RpcClient(
    processingContext: ProcessingContext = ProcessingContext()
) : RpcNode(processingContext) {

    private val client = Client(upstreamProcessor)
    override val downstreamProcessor = ClientProcessor(client)

    override fun <T : Any> proxy(interfaceClass: KClass<T>): T {
        require(interfaceClass.annotations.any { it is ForClientUsage } && interfaceClass.annotations.all { it !is ForServerUsage }) {
            "Interface that's being proxied by RpcClient must be marked with @ForClientUsage and mustn't be marked with @ForServerUsage"
        }
        return super.proxy(interfaceClass)
    }

    fun connect(address: SocketAddress) = client.connect(address)

    fun connect(address: String, port: Int) = connect(InetSocketAddress(address, port))

    override fun stop() = client.disconnect()

    fun onConnected(action: () -> Unit) = client.onConnected(action)
}