package xyz.dma.ecg_usb.serial

interface SerialListener {
    fun onLine(line: String)
}