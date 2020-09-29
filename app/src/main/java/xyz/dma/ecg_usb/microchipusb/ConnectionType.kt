package xyz.dma.ecg_usb.microchipusb

enum class ConnectionType {
    /** Connection to the device was successful. */
    SUCCESS,
    /** USB permission not granted for the device. */
    NO_USB_PERMISSION,
    /** Connection to the device could not be established. */
    CONNECTION_FAILED
}