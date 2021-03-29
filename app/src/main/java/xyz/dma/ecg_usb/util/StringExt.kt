package xyz.dma.ecg_usb.util


operator fun CharSequence.plus(string: String): CharSequence {
    return this.toString() + string
}

fun String.isInt(): Boolean {
    return try {
        this.toInt()
        true
    } catch (e: Exception) {
        false
    }
}

fun String.isDouble(): Boolean {
    return try {
        this.toDouble()
        true
    } catch (e: Exception) {
        false
    }
}
