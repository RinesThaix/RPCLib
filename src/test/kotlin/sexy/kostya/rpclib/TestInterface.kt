package sexy.kostya.rpclib

import java.util.*

@ForClientUsage
interface TestInterface {

    suspend fun sum(a: Int, b: @VarInt Int): @VarInt Int

    suspend fun sum(a: Double, b: Double, c: Double): Double

    suspend fun bytesToString(bytes: ByteArray): String

    suspend fun merge(first: List<List<UUID>>, second: List<List<UUID>>): List<List<UUID>>

}