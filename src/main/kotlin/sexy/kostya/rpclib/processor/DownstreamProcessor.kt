package sexy.kostya.rpclib.processor

import sexy.kostya.rpclib.internal.*
import sexy.kostya.rpclib.internal.FunctionSerializationData
import sexy.kostya.rpclib.internal.InterfaceSerializationData
import sexy.kostya.rpclib.internal.putVarInt
import sexy.kostya.rpclib.internal.string
import sexy.kostya.rpclib.network.Connection
import sexy.kostya.rpclib.util.JavaKtMethodsMapper
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
sealed class DownstreamProcessor(
    internal val upstreamProcessor: UpstreamProcessor
) {

    fun <T : Any> proxy(theInterface: KClass<T>): T {
        val data = InterfaceSerializationData(theInterface)
        return Proxy.newProxyInstance(
            DownstreamProcessor::class.java.classLoader,
            arrayOf(theInterface.java)
        ) { _, method, args ->
            val continuation = args.last() as Continuation<Any?>
            val func = JavaKtMethodsMapper[method]
            val funcData = data[func]
            val argsRange: IntRange
            val targeting: Targeting
            when (funcData.targeting) {
                Targeting.InternalList -> {
                    val first = args.first() as? List<UUID>
                    require(first != null) { "first argument must be a List<UUID>" }
                    targeting = Targeting.Many(first)
                    argsRange = 1 until args.size - 1
                }

                Targeting.InternalID -> {
                    val first = args.first() as? UUID
                    require(first != null) { "first argument must be a UUID" }
                    targeting = Targeting.Single(first)
                    argsRange = 1 until args.size - 1
                }

                else -> {
                    targeting = funcData.targeting
                    argsRange = 0 until args.size - 1
                }
            }
            val buffer = upstreamProcessor.processingContext.buffers.acquire()
            try {
                buffer.putVarInt(upstreamProcessor.interfaces[theInterface]!!)
                buffer.putVarInt(funcData.id)
                for ((i, j) in argsRange.withIndex()) {
                    funcData.arguments[i].serializer(buffer, args[j])
                }
            } catch (t: Throwable) {
                upstreamProcessor.processingContext.buffers.release(buffer)
                continuation.resumeWithException(RuntimeException("could not serialize method arguments", t))
                return@newProxyInstance 0
            }
            try {
                process(targeting, buffer, funcData, continuation)
                if (funcData.result == null) {
                    0
                } else {
                    kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
                }
            } catch (t: Throwable) {
                upstreamProcessor.processingContext.buffers.release(buffer)
                continuation.resumeWithException(RuntimeException("could not process networking", t))
                0
            }
        } as T
    }

    protected abstract fun selectTargets(targeting: Targeting): List<Connection>

    private fun process(
        targeting: Targeting,
        buffer: ByteBuffer,
        funcData: FunctionSerializationData,
        continuation: Continuation<Any?>
    ) {
        val targets = selectTargets(targeting)
        if (funcData.result == null) {
            buffer.putShort(0)
            targets.forEach { it.send(buffer) }
        } else {
            require(targeting is Targeting.Single || targeting is Targeting.Server) { "could not execute non-unit returning function with more targets than 1; targets are $targeting" }
            val callbackID = upstreamProcessor.processingContext.callbackManager.getNextCallbackID()
            buffer.putShort(callbackID)
            upstreamProcessor.processingContext.callbackManager.registerCallback(
                callbackID,
                { outputBuffer ->
                    if (outputBuffer.get() == 0.toByte()) {
                        val reason = outputBuffer.string
                        continuation.resumeWithException(RuntimeException("remote side responded with an error: $reason"))
                    } else {
                        val result = funcData.result.deserializer(outputBuffer)
                        continuation.resume(result)
                    }
                },
                {
                    continuation.resumeWithException(TimeoutException())
                }
            )
            targets.first().send(buffer)
        }
    }

}