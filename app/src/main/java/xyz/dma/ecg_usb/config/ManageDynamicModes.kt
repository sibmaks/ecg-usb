package xyz.dma.ecg_usb.config

class ManageDynamicModes: BitConfig() {

    // reserved 16 bits

    var fast_th : Int
        get() = getBits(21, 6)
        set(value) { setBits(21, value, 6) }

    var fast : Int
        get() = getBits(23, 2)
        set(value) { setBits(23, value, 2) }

    // reserved 8 bits
}