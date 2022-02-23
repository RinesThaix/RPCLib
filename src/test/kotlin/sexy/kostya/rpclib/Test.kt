package sexy.kostya.rpclib

import kotlinx.coroutines.runBlocking
import sexy.kostya.rpclib.base.ClientRegistry
import sexy.kostya.rpclib.node.RpcClient
import sexy.kostya.rpclib.node.RpcServer
import java.util.*
import kotlin.system.exitProcess

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        val client = RpcClient()
        val server = RpcServer()
        client.onConnected {
            println("connected!")
            val impl = client.proxy(TestInterface::class)
            val registry = client.proxy(ClientRegistry::class)

            runBlocking {
                println("my id = ${registry.getMyID()}")
                println("2 + 3 = ${impl.sum(2, 3)}")
                println("2 + 3 + 3.5 = ${impl.sum(2.0, 3.0, 3.5)}")
                println("string conversion: Konstantin =?= ${impl.bytesToString("Konstantin".encodeToByteArray())}")
                val first = listOf(
                    listOf(UUID.randomUUID(), UUID.randomUUID()),
                    listOf(UUID.randomUUID())
                )
                val second = listOf(
                    listOf(UUID.randomUUID()),
                    listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                )
                println("$first + $second = ${impl.merge(first, second)}")
            }
            server.stop()
            exitProcess(0)
        }
        server.registerInterface(TestInterface::class)
        server.registerImplementation(TestInterface::class, TestImplementation())

        client.registerInterface(TestInterface::class)

        server.start("127.0.0.1", 6556)
        client.connect("127.0.0.1", 6556)
    }

}