package sexy.kostya.rpclib.util

class LocalCache<T>(
    private val generator: () -> T
) {

    private val tickIndex = RpcThread.LocalCount.getAndIncrement()
    private val fallback = ThreadLocal.withInitial(generator)

    fun get(): T = when (val current = Thread.currentThread()) {
        is RpcThread -> current.localCache(tickIndex, generator)
        else -> fallback.get()
    }

}