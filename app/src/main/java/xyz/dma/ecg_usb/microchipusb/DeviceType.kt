package xyz.dma.ecg_usb.microchipusb

enum class DeviceType(val vid: Int, val pid: Int, val packetSize: Int) {
    MCP2210(0x4D8, 0xDE, 64),
    MCP2221(0x4D8, 0xDD, 64)
}