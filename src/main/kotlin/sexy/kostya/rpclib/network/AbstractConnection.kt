package sexy.kostya.rpclib.network

import sexy.kostya.rpclib.internal.prepareForReading
import sexy.kostya.rpclib.internal.putVarInt
import sexy.kostya.rpclib.internal.varInt
import sexy.kostya.rpclib.processor.UpstreamProcessor
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

sealed class AbstractConnection(
    val channel: SocketChannel,
    val worker: AbstractWorker,
    private val upstreamProcessor: UpstreamProcessor
) : Connection {

    private val waitingBuffers = mutableListOf<ByteBuffer>()
    private val tickBuffer: AtomicReference<ByteBuffer>
    private var cacheBuffer: ByteBuffer? = null

    init {
        val pool = upstreamProcessor.processingContext.buffers
        tickBuffer = AtomicReference(pool.acquire())
        pool.watch(this, tickBuffer::get)
        pool.watchMany(this) { waitingBuffers }
    }

    override fun send(buffer: ByteBuffer) {
        worker.queue.relaxedOffer { write(buffer) }
    }

    override fun flush() {
        try {
            if (!channel.isConnected) {
                throw ClosedChannelException()
            }
            try {
                val localBuffer = tickBuffer.plain
                if (waitingBuffers.isEmpty()) {
                    if (localBuffer.position() == 0) {
                        return
                    }
                    localBuffer.prepareForReading()
                    if (channel.write(localBuffer) == -1) {
                        throw IOException("Disconnected")
                    }
                    if (localBuffer.limit() == localBuffer.position()) {
                        localBuffer.clear()
                        return
                    }
                }
                try {
                    ensureLocalBufferCapacity(localBuffer)
                } catch (oom: OutOfMemoryError) {
                    waitingBuffers.clear()
                    System.gc()
                    throw ClosedChannelException()
                }
                val iterator = waitingBuffers.iterator()
                while (iterator.hasNext()) {
                    val buffer = iterator.next()
                    if (channel.write(buffer) == -1) {
                        throw IOException("Disconnected")
                    }
                    if (buffer.limit() != buffer.position()) {
                        break
                    }
                    iterator.remove()
                    upstreamProcessor.processingContext.buffers.release(buffer)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                throw ClosedChannelException()
            }
        } catch (t: Throwable) {
            disconnect()
        }
    }

    override fun disconnect() {
        worker.disconnect(this)
    }

    internal fun consumeCache(buffer: ByteBuffer) {
        val cache = cacheBuffer
        if (cache != null) {
            cacheBuffer = null
            buffer.put(cache)
        }
    }

    internal fun processInput(buffer: ByteBuffer) {
        buffer.prepareForReading()
        while (buffer.hasRemaining()) {
            val startingPos = buffer.position()
            try {
                val packetLength = buffer.varInt
                if (buffer.remaining() < packetLength) {
                    throw BufferUnderflowException()
                }
                val packet = buffer.slice(buffer.position(), packetLength)
                buffer.position(buffer.position() + packetLength)
                upstreamProcessor.handleInput(this, packet)
            } catch (ex: BufferUnderflowException) {
                buffer.position(startingPos)
                break
            }
        }
    }

    private fun write(buffer: ByteBuffer) {
        if (!channel.isConnected) {
            return
        }
        var localBuffer = tickBuffer.plain
        buffer.prepareForReading()
        val size = buffer.limit()
        val capacity = localBuffer.capacity()
        localBuffer = ensureLocalBufferCapacity(localBuffer, 5, capacity)
        localBuffer.putVarInt(size)
        if (size <= capacity) {
            localBuffer = ensureLocalBufferCapacity(localBuffer, size, capacity)
            localBuffer.put(buffer)
        } else {
            val count = size / capacity + 1
            for (i in 0 until count) {
                val sliceStart = i * capacity
                val sliceSize = min(size, sliceStart + capacity) - sliceStart
                localBuffer = ensureLocalBufferCapacity(localBuffer, size, capacity)
                localBuffer.put(localBuffer.position(), buffer, sliceStart, sliceSize)
            }
        }
        upstreamProcessor.processingContext.buffers.release(buffer)
    }

    private fun ensureLocalBufferCapacity(localBuffer: ByteBuffer, size: Int = 1, capacity: Int = 0): ByteBuffer {
        if (capacity - localBuffer.position() >= size) {
            return localBuffer
        }
        val newBuffer = upstreamProcessor.processingContext.buffers.acquire()
        localBuffer.prepareForReading()
        waitingBuffers.add(localBuffer)
        tickBuffer.plain = newBuffer
        return newBuffer
    }

}