package sexy.kostya.rpclib.util

import java.util.concurrent.atomic.AtomicInteger

open class RpcThread(name: String) : Thread(name) {

    companion object {
        val LocalCount = AtomicInteger()
    }

    private var locals = emptyArray<Any?>()

    fun <T> localCache(index: Int, generator: () -> T): T {
        var array = locals
        val requiredLength = index + 1
        if (array.size < requiredLength) {
            val temp = Array<Any?>(requiredLength) { null }
            System.arraycopy(array, 0, temp, 0, array.size)
            array = temp
            locals = array
        }
        val value = array[index]
        return if (value != null) {
            value as T
        } else {
            val newValue = generator()
            array[index] = newValue
            newValue
        }
    }

}