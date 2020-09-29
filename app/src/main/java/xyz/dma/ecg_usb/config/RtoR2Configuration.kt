package xyz.dma.ecg_usb.config

class RtoR2Configuration : BitConfig() {
    // reserved 8

    var rhsf : Int
        get() = getBits(10, 3)
        set(value) { setBits(10, value, 3) }

    // reserved 1

    var ravg : Int
        get() = getBits(13, 2)
        set(value) { setBits(13, value, 2) }

    // reserved 2

    var hoff : Int
        get() = getBits(21, 6)
        set(value) { setBits(21, value, 6) }

    // reserved 10
}