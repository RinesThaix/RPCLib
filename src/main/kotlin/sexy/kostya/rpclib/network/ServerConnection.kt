package sexy.kostya.rpclib.network

import java.nio.channels.SocketChannel

class ServerConnection(
    channel: SocketChannel,
    private val client: Client
) : AbstractConnection(channel, client.worker, client.upstreamProcessor) {

    override fun disconnect() {
        fullyDisconnect()
        client.reconnect()
    }

    fun fullyDisconnect() {
        super.disconnect()
        client.disconnect()
    }
}