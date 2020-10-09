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

    var ecgFastRecoveryMode : Boolean
        get() = bits[21]
        set(value) { bits[21] = value }

    var ecgFIFOOverflow : Boolean
        get() = bits[22]
        set(value) { bits[22] = value }

    var ecgFIFOInterrupt : Boolean
        get() = bits[23]
        set(value) { bits[23] = value }

    // reserved 8 bits

    fun read(data: UInt) {
        var value = data
        loff_nl = readBit(value)
        value = value shr 1
        loff_nh = readBit(value)
        value = value shr 1
        loff_pl = readBit(value)
        value = value shr 1
        loff_ph = readBit(value)
        value = value shr 5
        pllint = readBit(value)
        value = value shr 1
        samp = readBit(value)
        value = value shr 1
        rrint = readBit(value)
        value = value shr 1
        lonint = readBit(value)
        value = value shr 9
        dcloffint = readBit(value)
        value = value shr 1
        ecgFastRecoveryMode = readBit(value)
        value = value shr 1
        ecgFIFOOverflow = readBit(value)
        value = value shr 1
        ecgFIFOInterrupt = readBit(value)
    }

    override fun toString(): String {
        return "Status(" +
                "loff_nl: $loff_nl," +
                "loff_nh: $loff_nl," +
                "loff_pl: $loff_pl," +
                "loff_ph: $loff_ph," +
                "pllint: $pllint," +
                "samp: $samp," +
                "rrint: $rrint," +
                "lonint: $lonint," +
                "dcloffint: $dcloffint," +
                "ecgFastRecoveryMode: $ecgFastRecoveryMode," +
                "ecgFIFOOverflow: $ecgFIFOOverflow," +
                "ecgFIFOInterrupt: $ecgFIFOInterrupt)"
    }


}