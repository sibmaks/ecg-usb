package xyz.dma.ecg_usb.config

class GeneralConfiguration: BitConfig() {
    var rbiasn : Boolean
        get() = bits[0]
        set(value) { bits[0] = value }

    var rbiasp : Boolean
        get() = bits[1]
        set(value) { bits[1] = value }

    var rbiasv : Int
        get() = getBits(3, 2)
        set(value) { setBits(3, value, 2) }

    var en_rbias : Int
        get() = getBits(5, 2)
        set(value) { setBits(5, value, 2) }

    var vth : Int
        get() = getBits(7, 2)
        set(value) { setBits(7, value, 2) }

    var imag : Int
        get() = getBits(10, 3)
        set(value) { setBits(10, value, 3) }

    var ipol : Boolean
        get() = bits[11]
        set(value) { bits[11] = value }

    var en_dcloff : Int
        get() = getBits(13, 2)
        set(value) { setBits(13, value, 2) }

    // reserved 5

    var en_ecg : Boolean
        get() = bits[19]
        set(value) { bits[19] = value }

    var fmstr : Int
        get() = getBits(21, 2)
        set(value) { setBits(21, value, 2) }

    var en_ulp_lon : Int
        get() = getBits(23, 2)
        set(value) { setBits(23, value, 2) }

    // reserved 8
}