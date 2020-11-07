package xyz.dma.ecg_usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import xyz.dma.ecg_usb.MAX30003Driver.Companion.ECG_FIFO
import xyz.dma.ecg_usb.microchipusb.DeviceType
import xyz.dma.ecg_usb.microchipusb.MCP2210Driver
import xyz.dma.ecg_usb.microchipusb.MCPConnection
import xyz.dma.ecg_usb.microchipusb.MCPConnectionFactory
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.max


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {
    private val ACTION_USB_PERMISSION = "xyz.dma.ecg_usb.USB_PERMISSION"
    private lateinit var series: LineGraphSeries<DataPoint>
    private var workingThread: Thread? = null
    private var mcpConnection: MCPConnection? = null
    private val ecgPoints = LinkedBlockingQueue<UInt>()
    private val recordedPoints = CopyOnWriteArrayList<Double>()
    private var recordOn: Boolean = false
    @Volatile
    private var etagResponse7 = 0
    @Volatile
    private var otherError = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val graph = findViewById<View>(R.id.ecgGraph) as GraphView
        series = LineGraphSeries()
        graph.addSeries(series)
        graph.minimumWidth = 100
        graph.viewport.isScalable = true
        graph.viewport.setScalableY(true)
        graph.gridLabelRenderer.labelVerticalWidth = 180

        /** UsbManager used to scan for connected MCP2210 devices and grant USB permission.  */
        try {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_USB_PERMISSION), 0
            )

            val usbManager = this.getSystemService(Context.USB_SERVICE) as UsbManager
            val filter = IntentFilter(ACTION_USB_PERMISSION)

            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_USB_PERMISSION -> {
                            connectEcg(usbManager, permissionIntent)
                        }
                        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                            mcpConnection?.close()
                            mcpConnection = null
                            val wThread = workingThread
                            if (wThread != null && !wThread.isInterrupted) {
                                workingThread?.interrupt()
                            }
                        }
                        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                            connectEcg(usbManager, permissionIntent)
                        }
                    }
                }
            }, filter)

            connectEcg(usbManager, permissionIntent)
        } catch (e: Exception) {
            log(e.message ?: "null message exception")
            log(e.stackTraceToString())
        }

        Thread {
            var count = 0L
            var wasPoint: Double? = null
            while (!Thread.interrupted()) {
                val egcData = ecgPoints.take()
                val point = ((((egcData shr 8).toShort()).toInt() shl 2) or (egcData and 0b11u).toInt()).toDouble()
                if(wasPoint == null || wasPoint != point) {
                    wasPoint = point
                } else if(wasPoint == point) {
                    wasPoint = null
                    continue
                }
                if(recordOn) {
                    recordedPoints.add(point)
                }
                addPoint(count++, point)
            }
        }.start()

    }

    private fun connectEcg(usbManager: UsbManager, permissionIntent: PendingIntent) {
        val factory = MCPConnectionFactory(usbManager, permissionIntent) { log(it) }

        try {
            try {
                mcpConnection = factory.openConnection(DeviceType.MCP2210)
                if (mcpConnection == null) {
                    log("Connection exception")
                    return
                }
            } catch (e: java.lang.Exception) {
                log(e.message.toString())
                log(e.stackTraceToString())
                mcpConnection = factory.openConnection(DeviceType.MCP2210)
                if (mcpConnection == null) {
                    log("Connection exception")
                    return
                }
            }
            val driver = MCP2210Driver(mcpConnection) { log(it) }
            val maxDriver = MAX30003Driver(driver) { a -> log(a) }
            maxDriver.open()

            workingThread = Thread {
                max30003Read(maxDriver)
            }
            log("Start working thread")
            workingThread?.start()
        } catch (e: Exception) {
            log(e.message ?: "null")
            log(e.stackTraceToString())
        }
    }

    private fun max30003Read(maxDriver: MAX30003Driver) {
        try {
            while (!Thread.interrupted()) {
                val egcData = maxDriver.readRegister(ECG_FIFO)
                val etag = (egcData shr 3) and 0x7u
                if (etag != 0u && etag != 1u && etag != 2u) {
                    if (etag == 0x7u) {//FIFO_OVF
                        etagResponse7++
                        maxDriver.fifoReset()
                    } else {
                        otherError++
                    }
                } else {
                    ecgPoints.add(egcData)
                }
            }
        } catch (e: Exception) {
            log(e.message ?: "null message")
            log(e.stackTraceToString())
        }
    }

    private fun log(text: String) {
        val textView = findViewById<TextView>(R.id.textView) ?: return
        runOnUiThread {
            textView.text = textView.text + "\n" + text
        }
    }

    private fun addPoint(time: Long, value: Double) {
        val graph = findViewById<View>(R.id.ecgGraph) as GraphView
        runOnUiThread {
            series.appendData(DataPoint(time.toDouble(), value), true, 2000)
            graph.viewport.setMinX(max(0, time - 128 * 5).toDouble())
            graph.viewport.setMaxX(max(128 * 5, time).toDouble())
        }
    }

    fun onStartButtonClick(view: View) {
        recordOn = !recordOn
        if(view is Button) {
            if (recordOn) {
                view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
            } else {
                view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0)
            }
        }
    }

    fun onResetButtonClick(view: View) {
        recordedPoints.clear()
    }

    fun onShareButtonClick(view: View) {
        Thread {
            val recordsPath = File(this@MainActivity.filesDir, "records")
            if(!recordsPath.exists()) {
                recordsPath.mkdirs()
            }
            val recordFile = File(recordsPath, "ecg-records-${System.currentTimeMillis()}.csv")
            recordFile.writeText(recordedPoints.joinToString(separator = ",") { "$it" })

            val intentShareFile = Intent(Intent.ACTION_SEND)
            intentShareFile.type = "text/csv";
            intentShareFile.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(this@MainActivity, "xyz.dma.ecg_usb.FILE_PROVIDER", recordFile)
            )
            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Share file...")

            startActivity(Intent.createChooser(intentShareFile, "Сохранить результаты"))
        }.start()
    }
}

private operator fun CharSequence.plus(string: String): CharSequence {
    return this.toString() + string
}
