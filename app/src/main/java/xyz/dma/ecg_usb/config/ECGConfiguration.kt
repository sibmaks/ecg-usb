package xyz.dma.ecg_usb.config

class ECGConfiguration : BitConfig() {
    // reserved 12

    var digitalLowPassFilter : Int
        get() = getBits(13, 2)
        set(value) { setBits(13, value, 2) }

    var digitalHighPassFilter : Boolean
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

    fun read(data: UInt) {
        var value = data shr 12
        digitalLowPassFilter = readBits(value, 2u).toInt()
        value = value shr 2
        digitalHighPassFilter = readBit(value)
        value = value shr 2
        gain = readBits(value, 2u).toInt()
        value = value shr 6
        rate = readBits(value, 2u).toInt()
    }

    override fun toString(): String {
        return "ECGConfiguration(digitalLowPassFilter: $digitalLowPassFilter, digitalHighPassFilter: $digitalHighPassFilter, gain: $gain, rate: $rate)"
    }

}