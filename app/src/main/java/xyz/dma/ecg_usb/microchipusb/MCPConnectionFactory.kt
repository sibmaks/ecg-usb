package xyz.dma.ecg_usb.microchipusb

import android.app.PendingIntent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import java.util.*

class MCPConnectionFactory(
    private val mUsbManager: UsbManager,
    private val permissionIntent: PendingIntent,
    private val logger: (String) -> Unit
) {

    fun openConnection(type: DeviceType) : MCPConnection? {
        val deviceList: HashMap<String, UsbDevice> = mUsbManager.deviceList
        for (device in deviceList.entries) {
            val mMcp2210Device = device.value
            if (mMcp2210Device.vendorId == type.vid && mMcp2210Device.productId == type.pid) {
                // we found the MCP2210
                // Now go through the interfaces until we find the HID one
                var hidInterface: UsbInterface? = null
                logger("Interfaced: " + mMcp2210Device.interfaceCount.toString())
                for (i in 0 until mMcp2210Device.interfaceCount) {
                    val mMcp2210Interface = mMcp2210Device.getInterface(i)
                    logger(mMcp2210Interface.toString())
                }
                for (i in 0 until mMcp2210Device.interfaceCount) {
                    val mMcp2210Interface = mMcp2210Device.getInterface(i)
                    if (mMcp2210Interface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                        hidInterface = mMcp2210Interface
                        break
                    }
                }
                if(hidInterface == null) {
                    continue
                }

                // if the user granted USB permission
                // try to open a connection
                val mMcp2210Connection = if (mUsbManager.hasPermission(mMcp2210Device)) {
                    mUsbManager.openDevice(mMcp2210Device)
                } else {
                    mUsbManager.requestPermission(mMcp2210Device, permissionIntent)
                    throw IllegalAccessException("No USB permission")
                }

                return MCPConnection(type.packetSize, mMcp2210Connection, hidInterface)
            }
        }
        return null
    }
}