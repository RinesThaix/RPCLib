package sexy.kostya.rpclib.processor

import sexy.kostya.rpclib.internal.Targeting
import sexy.kostya.rpclib.network.Client

class ClientProcessor(
    private val client: Client
) : DownstreamProcessor(client.upstreamProcessor) {

    override fun selectTargets(targeting: Targeting) = client.connection?.let { listOf(it) } ?: emptyList()
}