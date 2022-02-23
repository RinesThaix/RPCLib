package sexy.kostya.rpclib.internal

import sexy.kostya.rpclib.ConstSize
import sexy.kostya.rpclib.ListType
import sexy.kostya.rpclib.MapType
import sexy.kostya.rpclib.VarInt
import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

@Suppress("UNCHECKED_CAST")
object RpcSerializers {

    object Flag {
        const val VarInt = 0x1
        const val ConstSize = 0x2
    }

    private val Serializers = mutableMapOf<KClass<*>, SerializationData<*>>()

    init {
        register(
            Byte::class,
            {{ b, v -> b.put(v) }},
            {{ b -> b.get() }}
        )
        registerPrimitiveArray(
            ByteArray::class,
            { it.size },
            { ByteArray(it) },
            { ByteBuffer::put },
            { ByteBuffer::get }
        )
        register(
            Boolean::class,
            {{ b, v -> b.put(if (v) 1 else 0) }},
            {{ b -> b.get() == 1.toByte() }}
        )
        registerPrimitiveArray(
            BooleanArray::class,
            { it.size },
            { BooleanArray(it) },
            {
                { b, v ->
                    for (el in v) {
                        b.put(if (el) 1 else 0)
                    }
                }
            },
            {
                { b, v ->
                    for (i in v.indices) {
                        v[i] = b.get() == 1.toByte()
                    }
                }
            }
        )
        register(
            Short::class,
            {{ b, v -> b.putShort(v) }},
            {{ b -> b.short }}
        )
        registerPrimitiveArray(
            ShortArray::class,
            { it.size },
            { ShortArray(it) },
            {
                { b, v ->
                    for (el in v) {
                        b.putShort(el)
                    }
                }
            },
            {
                { b, v ->
                    for (i in v.indices) {
                        v[i] = b.short
                    }
                }
            }
        )
        register(
            Int::class,
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b, v -> b.putInt(v) }
                } else {
                    { b, v -> b.putVarInt(v) }
                }
            },
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b -> b.int }
                } else {
                    { b -> b.varInt }
                }
            }
        )
        registerPrimitiveArray(
            IntArray::class,
            { it.size },
            { IntArray(it) },
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b, v ->
                        for (el in v) {
                            b.putInt(el)
                        }
                    }
                } else {
                    { b, v ->
                        for (el in v) {
                            b.putVarInt(el)
                        }
                    }
                }
            },
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b, v ->
                        for (i in v.indices) {
                            v[i] = b.int
                        }
                    }
                } else {
                    { b, v ->
                        for (i in v.indices) {
                            v[i] = b.varInt
                        }
                    }
                }
            }
        )
        register(
            Long::class,
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b, v -> b.putLong(v) }
                } else {
                    { b, v -> b.putVarLong(v) }
                }
            },
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b -> b.long }
                } else {
                    { b -> b.varLong }
                }
            }
        )
        registerPrimitiveArray(
            LongArray::class,
            { it.size },
            { LongArray(it) },
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b, v ->
                        for (el in v) {
                            b.putLong(el)
                        }
                    }
                } else {
                    { b, v ->
                        for (el in v) {
                            b.putVarLong(el)
                        }
                    }
                }
            },
            { f ->
                if ((f and Flag.VarInt) == 0) {
                    { b, v ->
                        for (i in v.indices) {
                            v[i] = b.long
                        }
                    }
                } else {
                    { b, v ->
                        for (i in v.indices) {
                            v[i] = b.varLong
                        }
                    }
                }
            }
        )
        register(
            Float::class,
            {{ b, v -> b.putFloat(v) }},
            {{ b -> b.float }}
        )
        registerPrimitiveArray(
            FloatArray::class,
            { it.size },
            { FloatArray(it) },
            {
                { b, v ->
                    for (el in v) {
                        b.putFloat(el)
                    }
                }
            },
            {
                { b, v ->
                    for (i in v.indices) {
                        v[i] = b.float
                    }
                }
            }
        )
        register(
            Double::class,
            {{ b, v -> b.putDouble(v) }},
            {{ b -> b.double }}
        )
        registerPrimitiveArray(
            DoubleArray::class,
            { it.size },
            { DoubleArray(it) },
            {
                { b, v ->
                    for (el in v) {
                        b.putDouble(el)
                    }
                }
            },
            {
                { b, v ->
                    for (i in v.indices) {
                        v[i] = b.double
                    }
                }
            }
        )
        register(
            String::class,
            {{ b, v ->
                val array = v.encodeToByteArray()
                b.putVarInt(array.size)
                b.put(array)
            }},
            {{ b ->
                val array = ByteArray(b.varInt)
                b.get(array)
                array.decodeToString()
            }}
        )
        register(
            UUID::class,
            {{ b, v ->
                b.putLong(v.mostSignificantBits)
                b.putLong(v.leastSignificantBits)
            }},
            {{ b -> UUID(b.long, b.long) }}
        )
    }

    private fun <T : Any> registerRaw(clazz: KClass<T>, serializer: (Extra) -> ((ByteBuffer, T) -> Unit), deserializer: (Extra) -> ((ByteBuffer) -> T)) {
        Serializers[clazz] = SerializationData(serializer, deserializer)
    }

    private fun <T : Any> register(clazz: KClass<T>, serializer: (Int) -> ((ByteBuffer, T) -> Unit), deserializer: (Int) -> ((ByteBuffer) -> T)) {
        registerRaw(
            clazz,
            { e -> serializer(e.flags) },
            { e -> deserializer(e.flags) }
        )
    }

    private fun <T : Any> registerPrimitiveArray(clazz: KClass<T>, sizeGetter: (T) -> Int, initializer: (Int) -> T, serializer: (Int) -> ((ByteBuffer, T) -> Unit), deserializer: (Int) -> ((ByteBuffer, T) -> Unit)) {
        registerRaw(
            clazz,
            { e ->
                val s = serializer(e.flags)
                if ((e.flags and Flag.ConstSize) == 0) {
                    { b, v ->
                        b.putVarInt(sizeGetter(v))
                        s(b, v)
                    }
                } else {
                    s
                }
            },
            { e ->
                val d = deserializer(e.flags)
                if ((e.flags and Flag.ConstSize) == 0) {
                    { b ->
                        val size = b.varInt
                        val array = initializer(size)
                        d(b, array)
                        array
                    }
                } else {
                    { b ->
                        val array = initializer(e.constSize)
                        d(b, array)
                        array
                    }
                }
            }
        )
    }

    fun getSerializer(type: KType): (ByteBuffer, Any?) -> Unit {
        val extra = if (type.annotations.isEmpty()) {
            Extra.Empty
        } else {
            var flags = 0
            var constSize = 0
            for (a in type.annotations) {
                when (a) {
                    is VarInt -> flags = flags or Flag.VarInt
                    is ConstSize -> {
                        flags = flags or Flag.ConstSize
                        constSize = a.size
                    }
                    else -> continue
                }
            }
            Extra(flags, constSize)
        }
        return getSerializer0(type, extra, type.isMarkedNullable)
    }

    private fun getSerializer0(type: KType, extra: Extra, nullable: Boolean): (ByteBuffer, Any?) -> Unit {
        if (nullable) {
            val downstream = getSerializer0(type, extra, false)
            return { b, v ->
                if (v == null) {
                    b.put(0)
                } else {
                    b.put(1)
                    downstream(b, v)
                }
            }
        }
        val classifier = type.classifier
        if (classifier !is KClass<*>) {
            throw IllegalStateException("could not get serialization data for $type")
        }
        val data = Serializers[classifier]
        if (data != null) {
            return data.serializer(extra) as (ByteBuffer, Any?) -> Unit
        }
        val arguments = ArrayList(type.arguments)
        val result = when {
            classifier.java.isArray -> {
                val downstream = getSerializer(type.arguments.first().type!!)
                val res: (ByteBuffer, Array<*>) -> Unit = if ((extra.flags and Flag.ConstSize) == 0) {
                    { b, v ->
                        b.putVarInt(v.size)
                        for (el in v) {
                            downstream(b, el)
                        }
                    }
                } else {
                    { b, v ->
                        for (el in v) {
                            downstream(b, el)
                        }
                    }
                }
                res
            }
            classifier == MutableList::class || classifier == List::class || matches(type, setOf(MutableList::class, List::class), arguments) -> {
                val downstream = getSerializer(arguments.first().type!!)
                val res: (ByteBuffer, List<*>) -> Unit = if ((extra.flags and Flag.ConstSize) == 0) {
                    { b, v ->
                        b.putVarInt(v.size)
                        for (el in v) {
                            downstream(b, el)
                        }
                    }
                } else {
                    { b, v ->
                        for (el in v) {
                            downstream(b, el)
                        }
                    }
                }
                res
            }
            classifier == MutableMap::class || classifier == Map::class || matches(type, setOf(MutableMap::class, Map::class), arguments) -> {
                val keys = getSerializer(arguments.first().type!!)
                val values = getSerializer(arguments.last().type!!)
                val res: (ByteBuffer, Map<*, *>) -> Unit = if ((extra.flags and Flag.ConstSize) == 0) {
                    { b, v ->
                        b.putVarInt(v.size)
                        for ((key, value) in v) {
                            keys(b, key)
                            values(b, value)
                        }
                    }
                } else {
                    { b, v ->
                        for ((key, value) in v) {
                            keys(b, key)
                            values(b, value)
                        }
                    }
                }
                res
            }
            else -> throw IllegalStateException("could not get serialization data for $classifier")
        }
        return result as (ByteBuffer, Any?) -> Unit
    }

    fun getDeserializer(type: KType): (ByteBuffer) -> Any? {
        val extra = if (type.annotations.isEmpty()) {
            Extra.Empty
        } else {
            var flags = 0
            var constSize = 0
            var listType = Extra.Empty.listType
            var mapType = Extra.Empty.mapType
            for (a in type.annotations) {
                when (a) {
                    is VarInt -> flags = flags or Flag.VarInt
                    is ConstSize -> {
                        flags = flags or Flag.ConstSize
                        constSize = a.size
                    }
                    is ListType -> listType = a.type
                    is MapType -> mapType = a.type
                    else -> continue
                }
            }
            Extra(flags, constSize, listType, mapType)
        }
        return getDeserializer0(type, extra, type.isMarkedNullable)
    }

    private fun getDeserializer0(type: KType, extra: Extra, nullable: Boolean): (ByteBuffer) -> Any? {
        if (nullable) {
            val downstream = getDeserializer0(type, extra, false)
            return { b ->
                if (b.get() == 0.toByte()) {
                    null
                } else {
                    downstream(b)
                }
            }
        }
        val classifier = type.classifier
        if (classifier !is KClass<*>) {
            throw IllegalStateException("could not get serialization data for $type")
        }
        val data = Serializers[classifier]
        if (data != null) {
            return data.deserializer(extra)
        }
        val arguments = ArrayList(type.arguments)
        val result = when {
            classifier.java.isArray -> {
                val downstream = getDeserializer(type.arguments.first().type!!)
                val res: (ByteBuffer) -> Array<*> = if ((extra.flags and Flag.ConstSize) == 0) {
                    { b ->
                        val size = b.varInt
                        val array = Array<Any?>(size) { null }
                        for (i in 0 until size) {
                            array[i] = downstream(b)
                        }
                        array
                    }
                } else {
                    { b ->
                        val array = Array<Any?>(extra.constSize) { null }
                        for (i in array.indices) {
                            array[i] = downstream(b)
                        }
                        array
                    }
                }
                res
            }
            classifier == MutableList::class || classifier == List::class || matches(type, setOf(MutableList::class, List::class), arguments) -> {
                val downstream = getDeserializer(arguments.first().type!!)
                val listInst: (Int) -> MutableList<Any?> = when (extra.listType) {
                    ListType.Type.Array -> {{ ArrayList(it) }}
                    ListType.Type.Linked -> {{ LinkedList() }}
                }
                val res: (ByteBuffer) -> MutableList<*> = if ((extra.flags and Flag.ConstSize) == 0) {
                    { b ->
                        val size = b.varInt
                        val list = listInst(size)
                        for (i in 0 until size) {
                            list.add(downstream(b))
                        }
                        list
                    }
                } else {
                    { b ->
                        val list = listInst(extra.constSize)
                        for (i in 0 until extra.constSize) {
                            list.add(downstream(b))
                        }
                        list
                    }
                }
                res
            }
            classifier == MutableMap::class || classifier == Map::class || matches(type, setOf(MutableMap::class, Map::class), arguments) -> {
                val keys = getDeserializer(arguments.first().type!!)
                val values = getDeserializer(arguments.last().type!!)
                val mapInst: (Int) -> MutableMap<Any?, Any?> = when (extra.mapType) {
                    MapType.Type.Hash -> {{ HashMap(it) }}
                    MapType.Type.LinkedHash -> {{ LinkedHashMap(it) }}
                }
                val res: (ByteBuffer) -> MutableMap<*, *> = if ((extra.flags and Flag.ConstSize) == 0) {
                    { b ->
                        val size = b.varInt
                        val map = mapInst(size)
                        for (i in 0 until size) {
                            map[keys(b)] = values(b)
                        }
                        map
                    }
                } else {
                    { b ->
                        val map = mapInst(extra.constSize)
                        for (i in 0 until extra.constSize) {
                            map[keys(b)] = values(b)
                        }
                        map
                    }
                }
                res
            }
            else -> throw IllegalStateException("could not get serialization data for $classifier")
        }
        return result
    }

    private fun matches(current: KType, targets: Set<KClass<*>>, arguments: MutableList<KTypeProjection>, used: MutableSet<KClass<*>> = mutableSetOf()): Boolean {
        val clazz = current.classifier as? KClass<*> ?: return false
        fun replaceArguments() {
            for ((i, arg) in current.arguments.withIndex()) {
                if (i >= arguments.size) {
                    arguments.add(arg)
                } else if (arguments[i].type !is KClass<*>) {
                    arguments[i] = arg
                }
            }
        }
        if (targets.contains(clazz)) {
            replaceArguments()
            return true
        }
        if (used.contains(clazz)) {
            return false
        }
        used.add(clazz)
        return if (clazz.supertypes.any { matches(it, targets, arguments, used) }) {
            replaceArguments()
            true
        } else {
            false
        }
    }

    private class SerializationData<T>(
        val serializer: (Extra) -> ((ByteBuffer, T) -> Unit),
        val deserializer: (Extra) -> ((ByteBuffer) -> T)
    )

    data class Extra(
        val flags: Int = 0,
        val constSize: Int = 0,
        val listType: ListType.Type = ListType.Type.Array,
        val mapType: MapType.Type = MapType.Type.Hash
    ) {

        companion object {
            val Empty = Extra()
        }

    }

}