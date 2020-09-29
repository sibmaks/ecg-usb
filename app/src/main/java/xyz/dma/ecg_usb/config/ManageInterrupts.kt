package xyz.dma.ecg_usb.config

class ManageInterrupts: BitConfig() {
    var samp_it : Int
        get() = getBits(3, 4)
        set(value) { setBits(3, value, 4) }

    var clr_samp : Boolean
        get() = bits[4]
        set(value) { bits[4] = value }

    // reserved 1

    var clr_rrint : Int
        get() = getBits(7, 2)
        set(value) { setBits(7, value, 2) }

    var clr_fast : Boolean
        get() = bits[8]
        set(value) { bits[8] = value }

    // reserved 12

    var efit : Int
        get() = getBits(25, 5)
        set(value) { setBits(25, value, 5) }

    // reserved 6
}