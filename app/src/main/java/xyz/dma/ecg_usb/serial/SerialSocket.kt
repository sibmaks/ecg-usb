package xyz.dma.ecg_usb.serial

import android.app.PendingIntent
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class SerialSocket(private val usbManager: UsbManager,
                   private val permissionIntent: PendingIntent,
            private val logger: ((String) -> (Unit))) : SerialInputOutputManager.Listener {
    private val DRIVER_WRITE_TIMEOUT = 2000
    private val byteQueue = LinkedBlockingQueue<Byte>()
    private val dataListeners: MutableList<SerialDataListener>
    private val socketListeners: MutableList<SerialSocketListener>
    private val usbSerialProber: UsbSerialProber
    private var connected: Boolean
    private lateinit var usbSerialPort: UsbSerialPort
    private val executionService: ExecutorService = Executors.newFixedThreadPool(4)

    init {
        val probeTable = UsbSerialProber.getDefaultProbeTable()
        probeTable.addProduct(1155, 22336, CdcAcmSerialDriver::class.java)
        usbSerialProber = UsbSerialProber(probeTable)
        dataListeners = CopyOnWriteArrayList()
        socketListeners = CopyOnWriteArrayList()
        connected = false
        executionService.submit {
            receive()
        }
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
            executionService.submit {
                socketListeners.forEach{
                    it.onConnect(this)
                }
            }
        } catch (e: Exception) {
            log("Error happened: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    private fun receive() {
        val lineBuilder = StringBuilder()
        var nl = false
        while(!Thread.currentThread().isInterrupted) {
            try {
                val byte = byteQueue.take()
                if(byte == '\r'.toByte()) {
                    nl = true
                    continue
                } else if(byte == '\n'.toByte()) {
                    dataListeners.forEach { it.onLine(lineBuilder.toString()) }
                    lineBuilder.clear()
                    nl = false
                } else {
                    if(nl) {
                        dataListeners.forEach { it.onLine(lineBuilder.toString()) }
                        lineBuilder.clear()
                        nl = false
                    }
                    lineBuilder.append(byte.toChar())
                }
            } catch (e: Exception) {
                log("On new data exception.\n${e.stackTraceToString()}")
            }
        }
    }

    fun reset() {
        byteQueue.clear()
    }

    fun isConnected() : Boolean {
        return connected
    }

    override fun onNewData(data: ByteArray?) {
        if(data != null) {
            for(a in data) {
                byteQueue.add(a)
            }
        }
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
        if(connected) {
            log("On run error exception.\n${e?.stackTraceToString()}")
        }
    }

    fun close() {
        try {
            if (connected) {
                usbSerialPort.close()
            }
        } finally {
            connected = false
            executionService.submit {
                socketListeners.forEach{
                    it.onDisconnect(this)
                }
            }
        }
    }
}