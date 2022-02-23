package sexy.kostya.rpclib.processor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import sexy.kostya.rpclib.network.CallbackManager
import sexy.kostya.rpclib.util.recycler.ByteBufferPool
import sexy.kostya.rpclib.util.recycler.Recycler
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ProcessingContext(threads: Int = Runtime.getRuntime().availableProcessors()) {

    companion object {
        private val ContextID = AtomicInteger()
    }

    val callbackManager = CallbackManager(this)
    val buffers: Recycler<ByteBuffer> = ByteBufferPool(direct = true)

    private val dispatcher = Executors.newFixedThreadPool(threads, object : ThreadFactory {

        private val id = ContextID.incrementAndGet()
        private val threadID = AtomicInteger()

        override fun newThread(r: Runnable) = Thread(r, "RPCLib-ProcessingContext-$id-Thread-${threadID.incrementAndGet()}").apply { isDaemon = false }
    }).asCoroutineDispatcher()

    private val scope = CoroutineScope(dispatcher)

    fun execute(context: CoroutineContext = EmptyCoroutineContext, task: suspend () -> Unit) {
        scope.launch(context) {
            try {
                task()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

}