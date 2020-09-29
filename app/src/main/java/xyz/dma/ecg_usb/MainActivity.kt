package xyz.dma.ecg_usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import xyz.dma.ecg_usb.MAX30003Driver.Companion.ECG_FIFO
import xyz.dma.ecg_usb.MAX30003Driver.Companion.RTOR
import xyz.dma.ecg_usb.microchipusb.DeviceType
import xyz.dma.ecg_usb.microchipusb.MCP2210Driver
import xyz.dma.ecg_usb.microchipusb.MCPConnection
import xyz.dma.ecg_usb.microchipusb.MCPConnectionFactory
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.max


class MainActivity : AppCompatActivity() {
    private val ACTION_USB_PERMISSION = "xyz.dma.ecg_usb.USB_PERMISSION"
    private lateinit var series: LineGraphSeries<DataPoint>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val graph = findViewById<View>(R.id.ecgGraph) as GraphView
        series = LineGraphSeries()
        graph.addSeries(series)
        graph.minimumWidth = 100
        graph.viewport.isScalable = true
        graph.viewport.setScalableY(true)

        /** UsbManager used to scan for connected MCP2210 devices and grant USB permission.  */
        try {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_USB_PERMISSION), 0
            )

            val usbManager = this.getSystemService(Context.USB_SERVICE) as UsbManager
            val factory = MCPConnectionFactory(usbManager, permissionIntent)

            val filter = IntentFilter(ACTION_USB_PERMISSION)

            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_USB_PERMISSION -> {
                            log(ACTION_USB_PERMISSION)
                            try {
                                var connection: MCPConnection?
                                try {
                                    connection = factory.openConnection(DeviceType.MCP2210)
                                    if (connection == null) {
                                        log("Connection exception")
                                        return
                                    }
                                } catch (e: java.lang.Exception) {
                                    log(e.message.toString())
                                    log(e.stackTraceToString())
                                    connection = factory.openConnection(DeviceType.MCP2210)
                                    if (connection == null) {
                                        log("Connection exception")
                                        return
                                    }
                                }
                                val driver = MCP2210Driver(connection) { t: String -> log(t) }
                                val maxDriver = MAX30003Driver(driver)
                                maxDriver.open {log(it)}
                                Thread {
                                    var time : Long = 0
                                    val RTOR_LSB_RES = 0.0078125f
                                    while (true) {
                                        try {
                                            val egcData = maxDriver.readRegister(ECG_FIFO)
                                            //log("Response: ${response[1].toString(2)} ${response[2].toString(2)} ${response[3].toString(2)}")

                                            val ecgdata = (egcData shr 8).toShort()
                                            val etag = (egcData shr 3) and 0x7u
                                            if(etag != 0u && etag != 1u) {
                                                log("ETAG repones $etag")
                                                if (etag == 0x7u) {//FIFO_OVF
                                                    maxDriver.sendSynch() // Reset FIFO
                                                }
                                                continue
                                            }

                                            ecgData(ecgdata.toString())
                                            addPoint(time, ecgdata.toDouble())
                                            time += 8

                                            val responseRtor = maxDriver.readRegister(RTOR)

                                            val hr = 1.0f / ( responseRtor.toFloat() * RTOR_LSB_RES / 60.0f )

                                            changeData(etag.toString(16) + ":" + hr)
                                        } catch (e: Exception) {
                                            log(e.message ?: "null message")
                                            log(e.stackTraceToString())
                                            break
                                        }
                                        TimeUnit.MILLISECONDS.sleep(8)
                                    }
                                }.start()
                            } catch (e: Exception) {
                                log(e.message ?: "null 1")
                                log(e.stackTraceToString())
                            }
                        }
                        UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                            log("Usb detached")
                        }
                        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                            try {

                                var connection: MCPConnection?
                                try {
                                    connection = factory.openConnection(DeviceType.MCP2210)
                                    if (connection == null) {
                                        log("Connection exception")
                                        return
                                    }
                                } catch (e: java.lang.Exception) {
                                    log(e.message.toString())
                                    log(e.stackTraceToString())
                                    connection = factory.openConnection(DeviceType.MCP2210)
                                    if (connection == null) {
                                        log("Connection exception")
                                        return
                                    }
                                }
                                val driver = MCP2210Driver(connection, Consumer { log(it) })
                                val maxDriver = MAX30003Driver(driver)
                                maxDriver.open { log(it) }
                                Thread {
                                    var time : Long = 0
                                    val RTOR_LSB_RES = 0.0078125f
                                    while (true) {
                                        try {
                                            val status = maxDriver.readRegister(MAX30003Driver.STATUS)
                                            val egcData = maxDriver.readRegister(ECG_FIFO)
                                            //log("Response: ${response[1].toString(2)} ${response[2].toString(2)} ${response[3].toString(2)}")

                                            val ecgdata = (egcData shr 8).toShort()
                                            val etag = (egcData shr 3) and 0x7u
                                            if(etag != 0u && etag != 1u) {
                                                if (etag == 0x7u) {//FIFO_OVF
                                                    log("ETAG repones $etag")
                                                    log("Send sync")
                                                    maxDriver.sendSynch() // Reset FIFO
                                                }
                                                continue
                                            }

                                            ecgData(ecgdata.toString())
                                            addPoint(time, ecgdata.toDouble())
                                            time += 8

                                            val responseRtor = maxDriver.readRegister(RTOR)

                                            val hr = 1.0f / ( responseRtor.toFloat() * RTOR_LSB_RES / 60.0f )

                                            changeData(status.toString(16) + ":" + etag.toString(16) + ":" + hr)
                                        } catch (e: Exception) {
                                            log(e.message ?: "null message")
                                            log(e.stackTraceToString())
                                            break
                                        }
                                        TimeUnit.MILLISECONDS.sleep(8)
                                    }
                                }.start()
                            } catch (e: Exception) {
                                log(e.message ?: "null 2")
                                log(e.stackTraceToString())
                            }
                        }
                    }
                }
            }, filter)

        } catch (e: Exception) {
            log(e.message ?: "null message exception")
            log(e.stackTraceToString())
        }
    }

    private fun log(text: String) {
        val textView = findViewById<TextView>(R.id.textView) ?: return
        runOnUiThread {
            textView.text = textView.text + "\n" + text
        }
    }

    private fun ecgData(text: String) {
        val textView = findViewById<TextView>(R.id.egcTextView) ?: return
        runOnUiThread {
            textView.text = text
        }
    }

    private fun changeData(text: String) {
        val textView = findViewById<TextView>(R.id.dataTextView) ?: return
        runOnUiThread {
            textView.text = text
        }
    }

    private fun addPoint(time:  Long, value: Double) {
        val graph = findViewById<View>(R.id.ecgGraph) as GraphView
        runOnUiThread {
            series.appendData(DataPoint(time.toDouble(), value), true, 11200)
            graph.viewport.setMinX(max(0, time - 8 * 125).toDouble())
            graph.viewport.setMaxX(time.toDouble())
        }
    }
}

private operator fun CharSequence.plus(string: String): CharSequence {
    return this.toString() + string
}
