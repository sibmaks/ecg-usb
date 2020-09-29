package xyz.dma.ecg_usb.config

class EnableInterrupts : BitConfig() {
    var intb_type : Int
        get() = getBits(1, 2)
        set(value) { setBits(1, value, 2) }

    // reserved 6

    var en_pllint : Boolean
        get() = bits[8]
        set(value) { bits[8] = value }

    var en_samp : Boolean
        get() = bits[9]
        set(value) { bits[9] = value }

    var en_rrint : Boolean
        get() = bits[10]
        set(value) { bits[10] = value }

    var en_loint : Boolean
        get() = bits[11]
        set(value) { bits[11] = value }

    // reserved 8

    var en_dcloffint : Boolean
        get() = bits[20]
        set(value) { bits[20] = value }

    var en_fstint : Boolean
        get() = bits[21]
        set(value) { bits[21] = value }

    var en_eovf : Boolean
        get() = bits[22]
        set(value) { bits[22] = value }

    var en_eint : Boolean
        get() = bits[23]
        set(value) { bits[23] = value }

    // reserved 8
}