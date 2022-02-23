package sexy.kostya.rpclib.network

import java.nio.ByteBuffer

interface Connection {

    fun send(buffer: ByteBuffer)

    fun flush()

    fun sendAndFlush(buffer: ByteBuffer) {
        send(buffer)
        flush()
    }

    fun disconnect()

}