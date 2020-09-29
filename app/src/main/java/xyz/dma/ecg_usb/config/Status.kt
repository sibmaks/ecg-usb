package xyz.dma.ecg_usb.config

class Status: BitConfig() {
    var loff_nl : Boolean
        get() = bits[0]
        set(value) { bits[0] = value }

    var loff_nh : Boolean
        get() = bits[1]
        set(value) { bits[1] = value }

    var loff_pl : Boolean
        get() = bits[2]
        set(value) { bits[2] = value }

    var loff_ph : Boolean
        get() = bits[3]
        set(value) { bits[3] = value }

    // reserved 4 bits

    var pllint : Boolean
        get() = bits[8]
        set(value) { bits[8] = value }

    var samp : Boolean
        get() = bits[9]
        set(value) { bits[9] = value }

    var rrint : Boolean
        get() = bits[10]
        set(value) { bits[10] = value }

    var lonint : Boolean
        get() = bits[11]
        set(value) { bits[11] = value }

    // reserved 8 bits

    var dcloffint : Boolean
        get() = bits[20]
        set(value) { bits[20] = value }

    var fstint : Boolean
        get() = bits[21]
        set(value) { bits[21] = value }

    var eovf : Boolean
        get() = bits[22]
        set(value) { bits[22] = value }

    var eint : Boolean
        get() = bits[23]
        set(value) { bits[23] = value }

    // reserved 8 bits
}