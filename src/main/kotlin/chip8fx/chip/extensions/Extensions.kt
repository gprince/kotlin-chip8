package chip8fx.chip.extensions

import java.io.DataInputStream

fun Byte.toHex(): String = "%02X".format(this)

infix fun Byte.toIntAndMask(mask: Int) : Int = this.toInt() and mask

fun Int.toHex(): String = Integer.toHexString(this)


fun <T : DataInputStream> T.eachByte(consumer: (Byte) -> Unit) {
    while (this.available() > 0) {
        consumer(this.readByte())
    }
}