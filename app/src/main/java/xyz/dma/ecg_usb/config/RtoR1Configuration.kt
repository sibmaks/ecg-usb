package xyz.dma.ecg_usb.config

class RtoR1Configuration : BitConfig() {
    // reserved 8

    var peakThresholdScalingFactor : Int
        get() = getBits(11, 4)
        set(value) { setBits(11, value, 4) }

    var peakAverageWeightFactor : Int
        get() = getBits(13, 2)
        set(value) { setBits(13, value, 2) }

    // reserved 1

    var enableRTOR : Boolean
        get() = bits[15]
        set(value) { bits[15] = value }

    var gain : Int
        get() = getBits(19, 4)
        set(value) { setBits(19, value, 4) }

    var averageWindowWidth : Int
        get() = getBits(23, 4)
        set(value) { setBits(23, value, 4) }

    // reserved 8
}