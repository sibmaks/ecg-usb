package xyz.dma.ecg_usb.microchipusb

import android.hardware.usb.*
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MCPConnection(
    private val packetSize: Int,
    private val usbDeviceConnection: UsbDeviceConnection,
    private val usbInterface: UsbInterface,
    private val averager: (Long) -> Unit) {

    /** USB OUT endpoint. Used for sending commands to the MCP2210 via the HID interface.  */
    private lateinit var mMcp2210EpOut: UsbEndpoint
    /** USB IN endpoint. Used for getting data from the MCP2210 via the HID interface.  */
    private lateinit var mMcp2210EpIn: UsbEndpoint
    private var closed = false
    private val usbResponse = ByteBuffer.allocate(packetSize)
    private var accessLock = ReentrantLock()
    /** USB request used for queuing data to the OUT USB endpoint.  */
    private val usbOutRequest = UsbRequest()
    /** USB request used for getting data from the IN USB endpoint queue.  */
    private val usbInRequest = UsbRequest()

    init {
        // Now go through the interfaces until we find the HID one
        if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID) {
            for (j in 0 until usbInterface.endpointCount) {
                if (usbInterface.getEndpoint(j)?.direction == UsbConstants.USB_DIR_OUT) {
                    // found the OUT USB endpoint
                    mMcp2210EpOut = usbInterface.getEndpoint(j)
                } else {
                    // found the IN USB endpoint
                    mMcp2210EpIn = usbInterface.getEndpoint(j)
                }
            }
        }

        usbOutRequest.initialize(usbDeviceConnection, mMcp2210EpOut)
        usbInRequest.initialize(usbDeviceConnection, mMcp2210EpIn)
        usbDeviceConnection.claimInterface(usbInterface, true)
    }

    /**
     * Sends a command to the MCP_XXXX and retrieves the reply.
     *
     * @param data
     * (ByteBuffer) 64 bytes of data to be sent
     * @return (ByteBuffer) 64 bytes of data received as a response from the MCP2210 <br></br>
     * null - if the transaction wasn't successful
     */
    fun sendData(data: ByteBuffer): ByteBuffer? {
        if (closed) {
            throw IllegalStateException("Connection closed")
        }
        if (data.capacity() > packetSize) {
            // USB packet size is 64 bytes
            return null
        }

        val time = System.currentTimeMillis()

        try {
            //accessLock.withLock {
            // queue the USB command
            usbOutRequest.queue(data, packetSize)
            if (usbDeviceConnection.requestWait() == null) {
                // an error has occurred
                return null
            }
            usbInRequest.queue(usbResponse, packetSize)
            return if (usbDeviceConnection.requestWait() == null) {
                // an error has occurred
                null
            } else usbResponse
        } finally {
            averager(System.currentTimeMillis() - time)
        }
       // }
    }

    /**
     * Close the communication with the MCP2210, release the USB interface <br></br>
     * and all resources related to the object.
     */
    fun close() {
        accessLock.withLock {
            usbDeviceConnection.releaseInterface(usbInterface)
            usbDeviceConnection.close()
            closed = true
        }
    }
}