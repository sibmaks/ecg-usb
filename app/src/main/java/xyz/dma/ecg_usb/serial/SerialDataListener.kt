package xyz.dma.ecg_usb.serial

interface SerialDataListener {
    fun onLine(line: String)
}