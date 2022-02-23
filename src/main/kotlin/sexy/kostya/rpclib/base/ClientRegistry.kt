package sexy.kostya.rpclib.base

import sexy.kostya.rpclib.ForClientUsage
import java.util.UUID

@ForClientUsage
interface ClientRegistry {

    suspend fun getMyID(): UUID

}