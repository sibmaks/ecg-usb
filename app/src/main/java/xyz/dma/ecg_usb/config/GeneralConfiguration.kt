package xyz.dma.ecg_usb.config

class GeneralConfiguration: BitConfig() {
    var enableResistiveBiasOnNegativeInput : Boolean
        get() = bits[0]
        set(value) { bits[0] = value }

    var enableResistiveBiasOnPositiveInput : Boolean
        get() = bits[1]
        set(value) { bits[1] = value }

    var resistiveBiasMode : Int
        get() = getBits(3, 2)
        set(value) { setBits(3, value, 2) }

    var enableResistiveLeadBiasMode : Int
        get() = getBits(5, 2)
        set(value) { setBits(5, value, 2) }

    var leadOffVoltageThreshold : Int
        get() = getBits(7, 2)
        set(value) { setBits(7, value, 2) }

    var leadOffCurrentMagnitude : Int
        get() = getBits(10, 3)
        set(value) { setBits(10, value, 3) }

    var leadOffCurrentPolarity : Boolean
        get() = bits[11]
        set(value) { bits[11] = value }

    var enableDCLeadOffDetection : Int
        get() = getBits(13, 2)
        set(value) { setBits(13, value, 2) }

    // reserved 5

    var enableECG : Boolean
        get() = bits[19]
        set(value) { bits[19] = value }

    var masterClockFrequency : Int
        get() = getBits(21, 2)
        set(value) { setBits(21, value, 2) }

    var enableUltraLowPowerLeadOn : Int
        get() = getBits(23, 2)
        set(value) { setBits(23, value, 2) }

    // reserved 8

    fun read(data: UInt) {
        var value = data
        enableResistiveBiasOnNegativeInput = readBit(value)
        value = value shr 1
        enableResistiveBiasOnPositiveInput = readBit(value)
        value = value shr 1
        resistiveBiasMode = readBits(value, 2u).toInt()
        value = value shr 2
        enableResistiveLeadBiasMode = readBits(value, 2u).toInt()
        value = value shr 2
        leadOffVoltageThreshold = readBits(value, 2u).toInt()
        value = value shr 2
        leadOffCurrentMagnitude = readBits(value, 3u).toInt()
        value = value shr 3
        leadOffCurrentPolarity = readBit(value)
        value = value shr 1
        enableDCLeadOffDetection = readBits(value, 2u).toInt()
        value = value shr 7
        enableECG = readBit(value)
        value = value shr 1
        masterClockFrequency = readBits(value, 2u).toInt()
        value = value shr 2
        enableUltraLowPowerLeadOn = readBits(value, 2u).toInt()
    }

    override fun toString(): String {
        return """GeneralConfiguration(
 enableResistiveBiasOnNegativeInput: $enableResistiveBiasOnNegativeInput,
 enableResistiveBiasOnPositiveInput: $enableResistiveBiasOnPositiveInput,
 resistiveBiasMode: $resistiveBiasMode,
 enableResistiveLeadBiasMode: $enableResistiveLeadBiasMode,
 leadOffVoltageThreshold: $leadOffVoltageThreshold,
 leadOffCurrentMagnitude: $leadOffCurrentMagnitude,
 leadOffCurrentPolarity: $leadOffCurrentPolarity,
 enableDCLeadOffDetection: $enableDCLeadOffDetection,
 enableECG: $enableECG,
 masterClockFrequency: $masterClockFrequency,
 enableUltraLowPowerLeadOn: $enableUltraLowPowerLeadOn)"""
    }


}