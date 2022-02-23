package sexy.kostya.rpclib.processor

import sexy.kostya.rpclib.base.ClientRegistry
import sexy.kostya.rpclib.internal.*
import sexy.kostya.rpclib.internal.FunctionSerializationData
import sexy.kostya.rpclib.internal.putString
import sexy.kostya.rpclib.internal.varInt
import sexy.kostya.rpclib.network.Connection
import sexy.kostya.rpclib.util.CoroutineCaller
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class UpstreamProcessor(internal val processingContext: ProcessingContext) {

    private val cachedUUID = UUID.randomUUID()
    internal val interfaces = HashMap<KClass<*>, Int>()
    private val reverseInterfaces = HashMap<Int, String>()
    private val implementations = HashMap<String, Map<Int, Pair<FunctionSerializationData, suspend (Array<Any?>) -> Any?>>>()

    init {
        registerInterface(ClientRegistry::class)
    }

    fun registerInterface(theInterface: KClass<*>) {
        val id = interfaces.size
        interfaces[theInterface] = id
        reverseInterfaces[id] = theInterface.qualifiedName!!
    }

    fun <T : Any, R : T> registerImplementation(theInterface: KClass<T>, theImplementation: R) {
        implementations[theInterface.qualifiedName!!] = ImplementationMatcher.match(theInterface, theImplementation)
    }

    fun handleInput(connection: Connection, buffer: ByteBuffer) {
        val callbackID = buffer.getShort(buffer.capacity() - Short.SIZE_BYTES)
        if (callbackID >= 0.toShort()) {
            val interfaceID = buffer.varInt
            val implementationName = reverseInterfaces[interfaceID] ?: throw IllegalStateException("received unknown interface id $interfaceID")
            val implementation = implementations[implementationName] ?: throw IllegalStateException("no implementation was registered for $implementationName")
            val methodID = buffer.varInt
            val (funcData, func) = implementation[methodID] ?: throw IllegalStateException("received unknown method id $methodID for $implementationName")
            val args = if (funcData.targeting != Targeting.Server) {
                val array = Array<Any?>(funcData.arguments.size + 3) { null }
                for ((i, argData) in funcData.arguments.withIndex()) {
                    array[i + 2] = argData.deserializer(buffer)
                }
                if (funcData.targeting == Targeting.InternalList) {
                    array[1] = emptyList<UUID>()
                } else {
                    array[1] = cachedUUID
                }
                array
            } else {
                val array = Array<Any?>(funcData.arguments.size + 2) { null }
                for ((i, argData) in funcData.arguments.withIndex()) {
                    array[i + 1] = argData.deserializer(buffer)
                }
                array
            }
            processingContext.execute(CoroutineCaller(connection)) {
                try {
                    val result = func(args)
                    if (result != Unit) {
                        if (callbackID != 0.toShort()) {
                            val outputBuffer = processingContext.buffers.acquire()
                            outputBuffer.put(1)
                            funcData.result!!.serializer(outputBuffer, result)
                            outputBuffer.putShort((-callbackID).toShort())
                            connection.send(outputBuffer)
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    if (callbackID != 0.toShort()) {
                        val outputBuffer = processingContext.buffers.acquire()
                        outputBuffer.put(0)
                        outputBuffer.putString(t.toString())
                        outputBuffer.putShort((-callbackID).toShort())
                        connection.send(outputBuffer)
                    }
                }
            }
        } else {
            val trueCallbackID = (-callbackID).toShort()
            processingContext.callbackManager.processCallback(connection, trueCallbackID, buffer)
        }
    }

}