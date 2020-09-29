package xyz.dma.ecg_usb.config

class CalConfiguration : BitConfig() {
    var thigh : Int
        get() = getBits(10, 11)
        set(value) { setBits(10, value, 11) }

    var fifty : Boolean
        get() = bits[11]
        set(value) { bits[11] = value }

    var fcal : Int
        get() = getBits(14, 3)
        set(value) { setBits(14, value, 3) }

    // reserved 5

    var vmag : Boolean
        get() = bits[20]
        set(value) { bits[20] = value }

    var vmode : Boolean
        get() = bits[21]
        set(value) { bits[21] = value }

    var en_vcal : Boolean
        get() = bits[22]
        set(value) { bits[22] = value }

    // reserved 9
}