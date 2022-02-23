package sexy.kostya.rpclib.base

import sexy.kostya.rpclib.util.getRpcCallerID

class ClientRegistryImpl : ClientRegistry {
    override suspend fun getMyID() = getRpcCallerID()!!
}