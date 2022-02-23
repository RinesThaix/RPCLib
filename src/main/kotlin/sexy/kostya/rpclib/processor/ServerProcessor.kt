package sexy.kostya.rpclib.processor

import sexy.kostya.rpclib.base.ClientRegistry
import sexy.kostya.rpclib.base.ClientRegistryImpl
import sexy.kostya.rpclib.internal.Targeting
import sexy.kostya.rpclib.network.ClientConnection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ServerProcessor(upstreamProcessor: UpstreamProcessor) : DownstreamProcessor(upstreamProcessor) {

    private val targets = ConcurrentHashMap<UUID, ClientConnection>()

    init {
        upstreamProcessor.registerImplementation(ClientRegistry::class, ClientRegistryImpl())
    }

    fun clientConnected(connection: ClientConnection) {
        targets[connection.id] = connection
    }

    fun clientDisconnected(connection: ClientConnection) {
        targets.remove(connection.id)
    }

    override fun selectTargets(targeting: Targeting) = when (targeting) {
        Targeting.All -> targets.values.toList()
        is Targeting.Many -> targeting.ids.mapNotNull { targets[it] }
        is Targeting.Single -> targets[targeting.id].let {
            if (it == null) {
                emptyList()
            } else {
                listOf(it)
            }
        }
        else -> throw IllegalStateException("unexpected targeting $targeting")
    }

}