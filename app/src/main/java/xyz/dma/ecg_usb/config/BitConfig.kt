package xyz.dma.ecg_usb.config

import java.util.*

open class BitConfig {
    val bits = BitSet(32)

    fun toInt() : Int {
        var value = 0
        for (i in 0 until bits.length()) {
            value += if(bits.get(i)) (1 shl i) else 0
        }
        return value
    }

    fun setBits(fromIndex: Int, value: Int, bitCount: Int) {
        var offset = 0
        var cValue = value
        while(cValue != 0 && bitCount != offset) {
            bits[fromIndex - bitCount + offset + 1] = (cValue and 1) != 0
            offset++
            cValue = cValue ushr 1
        }
    }

    fun getBits(fromIndex: Int, bitCount: Int) : Int {
        var value = 0
        for(i in (fromIndex - bitCount + 1)..fromIndex) {
            if(bits[i]) {
                value = value or 0x1
            }
            if(i != fromIndex) {
                value = value shl 1
            }
        }
        return value
    }
}