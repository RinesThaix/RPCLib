package sexy.kostya.rpclib.network

import sexy.kostya.rpclib.processor.ServerProcessor
import java.net.SocketAddress
import java.net.UnixDomainSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.file.Files

class Server(
    internal val downstreamProcessor: ServerProcessor,
    workersCount: Int = Runtime.getRuntime().availableProcessors()
) {

    @Volatile
    private var stop = false

    private val selector = Selector.open()
    private val workers = (1..workersCount).map { object : AbstractWorker() {
        override val open: Boolean
            get() = !this@Server.stop
    } }
    private var index = 0

    private lateinit var serverSocket: ServerSocketChannel
    private lateinit var socketAddress: SocketAddress

    fun initialize(address: SocketAddress) {
        val server = ServerSocketChannel.open()
        server.bind(address)
        server.configureBlocking(false)
        server.register(selector, SelectionKey.OP_ACCEPT)

        serverSocket = server
        socketAddress = address
    }

    fun start() {
        workers.forEach { it.start() }
        Thread({
               while (!stop) {
                   try {
                       selector.select { key ->
                           if (!key.isAcceptable) {
                               return@select
                           }
                           try {
                               val worker = findWorker()
                               val client = serverSocket.accept()
                               val connection = ClientConnection(client, worker, downstreamProcessor)
                               worker.receiveConnection(connection)
                               downstreamProcessor.clientConnected(connection)
                           } catch (t: Throwable) {
                               t.printStackTrace()
                           }
                       }
                   } catch (t: Throwable) {
                       t.printStackTrace()
                   }
               }
        }, "RPCLib-ServerThread").start()
    }

    fun stop() {
        stop = true
        try {
            serverSocket.close()
            val address = socketAddress
            if (address is UnixDomainSocketAddress) {
                Files.deleteIfExists(address.path)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        selector.wakeup()
        workers.forEach { it.selector.wakeup() }
    }

    private fun findWorker(): AbstractWorker {
        index = ++index % workers.size
        return workers[index]
    }

}