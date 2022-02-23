package sexy.kostya.rpclib.network

import org.jctools.queues.MessagePassingQueue
import org.jctools.queues.MpscUnboundedXaddArrayQueue
import sexy.kostya.rpclib.util.RpcThread
import sexy.kostya.rpclib.util.recycler.ByteBufferPool
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractWorker(
    private val id: Int = Id.incrementAndGet()
) : RpcThread("RPCLib-Worker-$id") {

    companion object {
        private val Id = AtomicInteger()
        private const val SocketSendBufferSize = 1 shl 20
        private const val SocketRecvBufferSize = 1 shl 16
        private const val NoDelay = true
        private const val SoTimeout = 30_000
    }

    private val connectionMap = ConcurrentHashMap<SocketChannel, AbstractConnection>()
    internal val queue: MessagePassingQueue<() -> Unit> = MpscUnboundedXaddArrayQueue(1024)
    internal val selector = Selector.open()

    protected abstract val open: Boolean

    override fun run() {
        while (open) {
            try {
                try {
                    queue.drain { it() }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                try {
                    connectionMap.values.forEach { it.flush() }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                selector.select({ key ->
                    val channel = key.channel() as SocketChannel
                    if (!channel.isOpen || !key.isReadable) {
                        return@select
                    }
                    val connection = connectionMap[channel]!!
                    try {
                        val buffer = ByteBufferPool.threadLocalDirectBuffer()
                        connection.consumeCache(buffer)
                        if (channel.read(buffer) == -1) {
                            throw IOException("Disconnected")
                        }
                        connection.processInput(buffer)
                    } catch (e: IOException) {
                        connection.disconnect()
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                        connection.disconnect()
                    }
                }, 50L)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    internal fun disconnect(connection: AbstractConnection) {
        try {
            val channel = connection.channel
            channel.close()
            connectionMap.remove(channel)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    internal fun receiveConnection(connection: AbstractConnection) {
        val channel = connection.channel
        connectionMap[channel] = connection
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ)
        val socket = channel.socket()
        socket.sendBufferSize = SocketSendBufferSize
        socket.receiveBufferSize = SocketRecvBufferSize
        socket.tcpNoDelay = NoDelay
        socket.soTimeout = SoTimeout
        selector.wakeup()
    }
}