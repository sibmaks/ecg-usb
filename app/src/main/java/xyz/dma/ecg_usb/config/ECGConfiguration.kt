package xyz.dma.ecg_usb.config

class ECGConfiguration : BitConfig() {
    // reserved 12

    var dlpf : Int
        get() = getBits(13, 2)
        set(value) { setBits(13, value, 2) }

    var dhpf : Boolean
        get() = bits[14]
        set(value) { bits[14] = value }

    // reserved 1

    var gain : Int
        get() = getBits(17, 2)
        set(value) { setBits(17, value, 2) }

    // reserved 4

    var rate : Int
        get() = getBits(23, 2)
        set(value) { setBits(23, value, 2) }

    // reserved 8
}