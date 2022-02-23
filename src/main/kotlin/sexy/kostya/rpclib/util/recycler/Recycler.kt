package sexy.kostya.rpclib.util.recycler

import java.lang.ref.Cleaner

interface Recycler<T> {

    companion object {
        private val TheCleaner = Cleaner.create()
    }

    fun acquire(): T

    fun release(value: T)

    fun process(block: (T) -> Unit) {
        val value = acquire()
        try {
            block(value)
        } finally {
            release(value)
        }
    }

    fun watch(ref: Any?, getter: () -> T) {
        TheCleaner.register(ref) {
            release(getter())
        }
    }

    fun watchMany(ref: Any?, getter: () -> Collection<T>) {
        TheCleaner.register(ref) {
            getter().forEach(this::release)
        }
    }

}