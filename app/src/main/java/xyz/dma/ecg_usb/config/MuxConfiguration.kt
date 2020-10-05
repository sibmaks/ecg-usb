package xyz.dma.ecg_usb.config

class MuxConfiguration : BitConfig() {
    // reserved 16

    var calibrationECGN : Int
        get() = getBits(17, 2)
        set(value) { setBits(17, value, 2) }

    var calibrationECGP : Int
        get() = getBits(19, 2)
        set(value) { setBits(19, value, 2) }

    var openECGNSwitch : Boolean
        get() = bits[20]
        set(value) { bits[20] = value }

    var openECGPSwitch : Boolean
        get() = bits[21]
        set(value) { bits[21] = value }

    // reserved 1

    var polarity : Boolean
        get() = bits[23]
        set(value) { bits[23] = value }

    // reserved 8
}