package sexy.kostya.rpclib.network

import sexy.kostya.rpclib.processor.UpstreamProcessor
import sexy.kostya.rpclib.util.CoroutineCaller
import java.net.*
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

class Client(internal val upstreamProcessor: UpstreamProcessor) {

    @Volatile
    private var open = false
    private val selector = Selector.open()
    internal val worker = object : AbstractWorker() {
        override val open: Boolean
            get() = this@Client.open
    }
    private lateinit var channel: SocketChannel
    private lateinit var lastAddress: SocketAddress
    internal var connection: ServerConnection? = null

    private val onConnected = mutableListOf<() -> Unit>()

    init {
        start()
    }

    fun connect(address: SocketAddress) {
        channel = SocketChannel.open()
        val connected = channel.connect(address)
        channel.configureBlocking(false)
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
        channel.register(selector, SelectionKey.OP_CONNECT or SelectionKey.OP_READ or SelectionKey.OP_WRITE)
        open = true
        lastAddress = address
        worker.start()

        if (connected) {
            onConnected()
        }
    }

    private fun start() {
        Thread({
               while (true) {
                   try {
                       selector.select { key ->
                           if (!key.isConnectable) {
                               return@select
                           }
                           onConnected()
                       }
                   } catch (t: Throwable) {
                       t.printStackTrace()
                   }
               }
        }, "RPCLib-ClientThread").apply {
            isDaemon = true
            start()
        }
    }

    private fun onConnected() {
        connection = ServerConnection(channel, this)
        worker.receiveConnection(connection!!)
        upstreamProcessor.processingContext.execute(CoroutineCaller(connection!!)) {
            synchronized(onConnected) {
                onConnected.forEach { it() }
            }
        }
    }

    fun disconnect() {
        selector.wakeup()
        open = false
        connection = null
    }

    fun reconnect() = connect(lastAddress)

    fun onConnected(block: () -> Unit) {
        synchronized(onConnected) {
            onConnected.add(block)
        }
    }

}