package xyz.dma.ecg_usb.serial

import android.app.PendingIntent
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class SerialSocket(private val usbManager: UsbManager,
                   private val permissionIntent: PendingIntent,
            private val logger: ((String) -> (Unit))) : SerialInputOutputManager.Listener {
    private val DRIVER_WRITE_TIMEOUT = 2000
    private val buffer: MutableList<Byte>
    private val listeners: MutableList<SerialListener>
    private val usbSerialProber: UsbSerialProber
    private var connected: Boolean
    private lateinit var usbSerialPort: UsbSerialPort

    init {
        val probeTable = UsbSerialProber.getDefaultProbeTable()
        probeTable.addProduct(1155, 22336, CdcAcmSerialDriver::class.java)
        usbSerialProber = UsbSerialProber(probeTable)
        buffer = CopyOnWriteArrayList()
        listeners = CopyOnWriteArrayList()
        connected = false
    }

    fun addListener(listener: SerialListener) {
        listeners.add(listener)
    }

    fun connect() {
        try {
            val availableDrivers = usbSerialProber.findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                return
            }

            val driver = availableDrivers[0]
            val connection = if (usbManager.hasPermission(driver.device)) {
                usbManager.openDevice(driver.device)
            } else {
                usbManager.requestPermission(driver.device, permissionIntent)
                return
            }

            usbSerialPort = driver.ports[0] // Most devices have just one port (port 0)

            usbSerialPort.open(connection)
            usbSerialPort.setParameters(9600, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort.dtr = true
            usbSerialPort.rts = true

            val usbIoManager = SerialInputOutputManager(usbSerialPort, this)
            Executors.newSingleThreadExecutor().submit(usbIoManager)
            log("Device connected")
            send("0")
            connected = true
        } catch (e: Exception) {
            log("Error happened: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    fun reset() {
        buffer.clear()
    }

    fun isConnected() : Boolean {
        return connected
    }

    override fun onNewData(data: ByteArray?) {
        try {
            if (data != null) {
                for(a in data) {
                    buffer.add(a)
                }
                while (buffer.size > 0 && isNewLine(buffer[0])) {
                    buffer.removeFirst()
                    listeners.forEach{ it.onLine("")}
                }
                var nl = indexOf(buffer, '\n'.toByte())
                while (nl != -1) {
                    val line = buffer.subList(0, nl - 1).toByteArray().decodeToString()
                    listeners.forEach{ it.onLine(line)}
                    for(i in 0..nl) {
                        buffer.removeAt(0)
                    }
                    nl = indexOf(buffer, '\n'.toByte())
                }
            }
        } catch (e: Exception) {
            log("On new data exception.\n${e.stackTraceToString()}")
        }
    }

    private fun isNewLine(byte: Byte) = byte == '\n'.toByte() || byte == '\r'.toByte()

    private fun indexOf(list: List<Byte>, value: Byte) : Int {
        for((i, item) in list.withIndex()) {
            if(item == value) {
                return i
            }
        }
        return -1
    }

    fun send(text: String) {
        usbSerialPort.write(text.toByteArray(), DRIVER_WRITE_TIMEOUT)
    }

    private fun log(text: String, newLine: Boolean = true) {
        logger(text)
        if(newLine) {
            logger("\n")
        }
    }

    override fun onRunError(e: Exception?) {
        log("On run error exception.\n${e?.stackTraceToString()}")
    }

    fun close() {
        try {
            if (connected) {
                usbSerialPort.close()
            }
        } finally {
            connected = false
        }
    }
}