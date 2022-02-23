package sexy.kostya.rpclib.internal

import java.nio.ByteBuffer

internal fun ByteBuffer.putVarInt(value: Int) {
    var value = value
    while (true) {
        var part = value and 0x7F
        value = value ushr 7
        if (value != 0) {
            part = part or 0x80
        }
        put(part.toByte())
        if (value == 0) {
            break
        }
    }
}

internal val ByteBuffer.varInt: Int
    get() {
        var value = 0
        for (i in 0 until 5) {
            val k = get().toInt()
            value = value.or(k.and(0x7F).shl(i * 7))
            if ((k and 0x80) != 128) {
                return value
            }
        }
        throw IllegalStateException("VarInt is too big")
    }

internal fun ByteBuffer.putVarLong(value: Long) {
    var value = value
    while (true) {
        var part = value and 0x7F
        value = value ushr 7
        if (value != 0L) {
            part = part or 0x80
        }
        put(part.toByte())
        if (value == 0L) {
            break
        }
    }
}

internal val ByteBuffer.varLong: Long
    get() {
        var value = 0L
        for (i in 0 until 9) {
            val k = get().toLong()
            value = value.or(k.and(0x7F).shl(i * 7))
            if ((k and 0x80) != 128L) {
                return value
            }
        }
        throw IllegalStateException("VarLong is too big")
    }

internal fun ByteBuffer.putString(value: String) {
    val bytes = value.encodeToByteArray()
    putVarInt(bytes.size)
    put(bytes)
}

internal val ByteBuffer.string: String
    get() {
        val bytes = ByteArray(varInt)
        get(bytes)
        return bytes.decodeToString()
    }

internal fun ByteBuffer.prepareForReading() {
    limit(position())
    position(0)
}