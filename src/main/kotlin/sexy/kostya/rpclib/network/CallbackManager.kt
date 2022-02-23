package sexy.kostya.rpclib.network

import sexy.kostya.rpclib.processor.ProcessingContext
import sexy.kostya.rpclib.util.CoroutineCaller
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CallbackManager(private val processingContext: ProcessingContext) {

    companion object {
        private const val MaxCallbackID = 32000
        private const val Timeout = 1_000_000L
    }

    private val CallbackID = AtomicInteger(1)
    private val Callbacks = ConcurrentHashMap<Short, CallbackData>()

    init {
        Thread({
               while (true) {
                   val current = System.currentTimeMillis()
                   val iterator = Callbacks.entries.iterator()
                   while (iterator.hasNext()) {
                       val next = iterator.next()
                       if (current - next.value.timestamp >= Timeout) {
                           next.value.timeoutCallback()
                           iterator.remove()
                       }
                   }
                   Thread.sleep(50L)
               }
        }, "RPCLib-CallbackManagerThread").apply {
            isDaemon = true
            start()
        }
    }

    fun registerCallback(id: Short, callback: (ByteBuffer) -> Unit, timeoutCallback: () -> Unit) {
        Callbacks[id] = CallbackData(callback, timeoutCallback)
    }

    fun processCallback(connection: Connection, id: Short, buffer: ByteBuffer) {
        val data = Callbacks.remove(id) ?: return
        processingContext.execute(CoroutineCaller(connection)) { data.callback(buffer) }
    }

    fun getNextCallbackID(): Short = CallbackID.getAndUpdate {
        if (it == MaxCallbackID) {
            1
        } else {
            it + 1
        }
    }.toShort()

    private data class CallbackData(
        val callback: (ByteBuffer) -> Unit,
        val timeoutCallback: () -> Unit,
        val timestamp: Long = System.currentTimeMillis()
    )

}