package xyz.dma.ecg_usb.serial

import android.app.PendingIntent
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
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
    private val dataListeners: MutableList<SerialDataListener>
    private val socketListeners: MutableList<SerialSocketListener>
    private val usbSerialProber: UsbSerialProber
    private var connected: Boolean
    private lateinit var usbSerialPort: UsbSerialPort
    private val mainLooper: Handler

    init {
        val probeTable = UsbSerialProber.getDefaultProbeTable()
        probeTable.addProduct(1155, 22336, CdcAcmSerialDriver::class.java)
        usbSerialProber = UsbSerialProber(probeTable)
        buffer = CopyOnWriteArrayList()
        dataListeners = CopyOnWriteArrayList()
        socketListeners = CopyOnWriteArrayList()
        connected = false
        mainLooper = Handler(Looper.getMainLooper())
    }

    fun addDataListener(dataListener: SerialDataListener) {
        dataListeners.add(dataListener)
    }

    fun addSocketListener(socketListener: SerialSocketListener) {
        socketListeners.add(socketListener)
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
            connected = true
            mainLooper.post {
                socketListeners.forEach{
                    it.onConnect(this)
                }
            }
        } catch (e: Exception) {
            log("Error happened: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    private fun receive(data: ByteArray) {
        if(!connected) {
            throw IllegalStateException("Serial socket is not connected")
        }
        try {
            for(a in data) {
                buffer.add(a)
            }
            while (buffer.size > 0 && isNewLine(buffer[0])) {
                buffer.removeFirst()
                dataListeners.forEach{ it.onLine("")}
            }
            var nl = indexOf(buffer, '\n'.toByte())
            while (nl != -1) {
                val line = buffer.subList(0, nl - 1).toByteArray().decodeToString()
                dataListeners.forEach{ it.onLine(line)}
                for(i in 0..nl) {
                    buffer.removeAt(0)
                }
                nl = indexOf(buffer, '\n'.toByte())
            }
        } catch (e: Exception) {
            log("On new data exception.\n${e.stackTraceToString()}")
        }
    }

    fun reset() {
        buffer.clear()
    }

    fun isConnected() : Boolean {
        return connected
    }

    override fun onNewData(data: ByteArray?) {
        if(data != null) {
            mainLooper.post {
                receive(data)
            }
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
            mainLooper.post {
                socketListeners.forEach{
                    it.onDisconnect(this)
                }
            }
        }
    }
}