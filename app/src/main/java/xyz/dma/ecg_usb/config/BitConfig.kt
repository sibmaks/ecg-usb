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
        for(i in fromIndex downTo (fromIndex - bitCount + 1)) {
            if(bits[i]) {
                value = value or 1
            }
            if(i != (fromIndex - bitCount + 1)) {
                value = value shl 1
            }
        }
        return value
    }

    fun readBit(data: UInt) : Boolean {
        return (data and 1u) == 1u
    }

    fun readBits(data: UInt, bits: UInt) : UInt {
        var value = 0u
        for(i in 0u until bits) {
            value = (value shl 1) or 1u
        }
        return data and value
    }
}