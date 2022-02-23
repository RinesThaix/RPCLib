package sexy.kostya.rpclib.internal

import java.util.UUID

sealed interface Targeting {

    object All : Targeting
    object Server : Targeting
    object InternalList : Targeting
    object InternalID : Targeting
    data class Many(val ids: List<UUID>) : Targeting
    data class Single(val id: UUID) : Targeting

}