package sexy.kostya.rpclib

import sexy.kostya.rpclib.internal.RpcSerializers
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

object Test {

    private const val primitive = 1
    private const val instance = "test"
    private val primitiveArray = IntArray(2)
    @ConstSize(5) private val instanceArray = Array<UUID>(5) { UUID.randomUUID() }
    private val list = mutableListOf<String>()
    private val nullableList = mutableListOf<String?>()
    private val map = mutableMapOf<UUID, Double>()

    private val hashMap = HashMap<Int, ByteArray>()
    private val testMap = TestMap()

    private val multiLayered = HashMap<Int, Map<DoubleArray, List<@ConstSize(4) @VarInt IntArray>>>()

    @JvmStatic
    fun main(args: Array<String>) {
        Test::class.members.forEach { member ->
            if (member !is KProperty<*>) {
                return@forEach
            }
            println("${member.name}:")
            show(member.returnType, 1)
            RpcSerializers.getSerializer(member.returnType)
            RpcSerializers.getDeserializer(member.returnType)
        }
    }

    private fun show(type: KType, offset: Int = 0) {
        val prefix = " ".repeat(offset shl 1)
        fun println(value: String) = kotlin.io.println("$prefix$value")

        println("$type")
        val classifier = type.classifier ?: return
        if (classifier == Any::class || classifier == Number::class || classifier == Comparable::class || classifier !is KClass<*>) {
            return
        }
        if (type.arguments.isNotEmpty()) {
            println("Arguments:")
            for (arg in type.arguments) {
                show(arg.type!!, offset + 1)
            }
        }
        for (supertype in classifier.supertypes) {
            show(supertype, offset + 1)
        }
    }

    private class TestMap : HashMap<String, UUID>()

}