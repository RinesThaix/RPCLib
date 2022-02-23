package sexy.kostya.rpclib.util

import sexy.kostya.rpclib.network.ClientConnection
import sexy.kostya.rpclib.network.Connection
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

data class CoroutineCaller(val connection: Connection) : AbstractCoroutineContextElement(CoroutineCaller) {
    companion object Key : CoroutineContext.Key<CoroutineCaller>
    override fun toString() = "CoroutineCaller($connection)"
}

suspend fun getRpcCaller(): Connection? = coroutineContext[CoroutineCaller.Key]?.connection

suspend fun getRpcCallerID(): UUID? = (getRpcCaller() as? ClientConnection)?.id