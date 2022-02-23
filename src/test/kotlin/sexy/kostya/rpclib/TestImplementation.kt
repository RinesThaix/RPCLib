package sexy.kostya.rpclib

import java.util.*

class TestImplementation : TestInterface {

    override suspend fun sum(a: Int, b: Int) = a + b

    override suspend fun sum(a: Double, b: Double, c: Double) = a + b + c

    override suspend fun bytesToString(bytes: ByteArray): String {
        return bytes.decodeToString()
    }

    override suspend fun merge(first: List<List<UUID>>, second: List<List<UUID>>): List<List<UUID>> {
        val result = ArrayList<List<UUID>>(first.size + second.size)
        result.addAll(first)
        result.addAll(second)
        return result
    }

}