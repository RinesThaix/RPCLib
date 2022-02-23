package sexy.kostya.rpclib.util.recycler

import org.jctools.queues.MpmcUnboundedXaddArrayQueue
import sexy.kostya.rpclib.util.LocalCache
import java.lang.ref.SoftReference
import java.nio.ByteBuffer

class ByteBufferPool(
    private val size: Int = DefaultSize,
    private val direct: Boolean = false
) : Recycler<ByteBuffer> {

    private val pool = MpmcUnboundedXaddArrayQueue<SoftReference<ByteBuffer>>(1024)

    companion object {
        private const val DefaultSize = 1 shl 10
        private val ThreadLocalHeapBuffers = LocalCache { ByteBuffer.allocate(DefaultSize) }
        private val ThreadLocalDirectBuffers = LocalCache { ByteBuffer.allocateDirect(DefaultSize) }

        fun threadLocalHeapBuffer(): ByteBuffer = ThreadLocalHeapBuffers.get().clear()

        fun threadLocalDirectBuffer(): ByteBuffer = ThreadLocalDirectBuffers.get().clear()
    }

    override fun acquire(): ByteBuffer {
        while (true) {
            val ref = pool.relaxedPoll() ?: break
            return ref.get() ?: continue
        }
        return if (direct) ByteBuffer.allocateDirect(size) else ByteBuffer.allocate(size)
    }

    override fun release(value: ByteBuffer) {
        pool.relaxedOffer(SoftReference(value.clear()))
    }
}