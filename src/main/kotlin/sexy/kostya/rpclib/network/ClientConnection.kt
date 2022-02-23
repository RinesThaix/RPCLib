package sexy.kostya.rpclib.network

import sexy.kostya.rpclib.processor.ServerProcessor
import java.nio.channels.SocketChannel
import java.util.*

class ClientConnection(
    channel: SocketChannel,
    worker: AbstractWorker,
    private val downstreamProcessor: ServerProcessor,
    val id: UUID = UUID.randomUUID()
) : AbstractConnection(channel, worker, downstreamProcessor.upstreamProcessor) {

    override fun disconnect() {
        super.disconnect()
        downstreamProcessor.clientDisconnected(this)
    }

}