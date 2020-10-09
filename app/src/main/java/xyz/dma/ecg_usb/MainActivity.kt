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
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import xyz.dma.ecg_usb.MAX30003Driver.Companion.ECG_FIFO
import xyz.dma.ecg_usb.config.Status
import xyz.dma.ecg_usb.microchipusb.DeviceType
import xyz.dma.ecg_usb.microchipusb.MCP2210Driver
import xyz.dma.ecg_usb.microchipusb.MCPConnection
import xyz.dma.ecg_usb.microchipusb.MCPConnectionFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max


class MainActivity : AppCompatActivity() {
    private val ACTION_USB_PERMISSION = "xyz.dma.ecg_usb.USB_PERMISSION"
    private lateinit var series: LineGraphSeries<DataPoint>
    private var ecgType = 0
    private val ecgPoints = LinkedBlockingQueue<Double>()
    private var avgWriteTime = 0L

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
            val factory = MCPConnectionFactory(usbManager, permissionIntent) {
                avgWriteTime = it
            }

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
                                val maxDriver = MAX30003Driver(driver,{ a -> log(a) })
                                maxDriver.open()
                                Thread {
                                    max30003Read(maxDriver)
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
                                val driver = MCP2210Driver(connection, { log(it) })
                                val maxDriver = MAX30003Driver(driver,{ a -> log(a) })
                                maxDriver.open()

                                Thread {
                                    max30003Read(maxDriver)
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

        Thread {
            var count = 0L
            while (!Thread.interrupted()) {
                val point = ecgPoints.take()
                addPoint(count++, point)
            }
        }.start()
    }

    fun onTypeChane(view: View) {
        ecgType = if(ecgType + 1 >= 7) 0 else ecgType + 1
        (view as Button).text = "Type: $ecgType"
    }

    private fun max30003Read(maxDriver: MAX30003Driver) {
        // val time = System.currentTimeMillis()
        //var lastRTORTime = time
        var etagResponse7 = 0
        var skippedInterrupts = 0
        var skippedOverflow = 0
        var otherError = 0
        val ecgSamples = ArrayList<Number>()
        //val rtorSamples = ArrayList<Float>()
        //var hr = 0f
        val status = Status()

        var egcData: UInt
        var ecgdata: Number

        var startTime = System.currentTimeMillis()
        var points = 0
        var sps = 0L

        try {
            while (!Thread.interrupted()) {
                status.read(maxDriver.readRegister(MAX30003Driver.STATUS))
                if (!status.ecgFIFOInterrupt) {
                    skippedInterrupts++
                    continue
                }
                if (status.ecgFIFOOverflow) {
                    maxDriver.fifoReset()
                    skippedOverflow++
                    continue
                }

                 do {
                     val now = System.currentTimeMillis()
                     egcData = maxDriver.readRegister(ECG_FIFO)
                     ecgdata = when (ecgType) {
                         0 -> {
                             (egcData shr 8).toShort()
                         }
                         1 -> {
                             (egcData shr 6).toShort()
                         }
                         2 -> {
                             (((egcData shr 8).toShort()).toInt() shl 2) or (egcData and 0b11u).toInt()
                         }
                         3 -> {
                             (egcData shr 8).toInt()
                         }
                         4 -> {
                             (egcData shr 8).toInt() - (0xFFFF / 2)
                         }
                         5 -> {
                             (egcData shr 6).toInt()
                         }
                         else -> {
                             (egcData shr 6).toInt() - (0x3FFFF / 2)
                         }
                     }
                     val etag = (egcData shr 3) and 0x7u
                     if (etag != 0u && etag != 1u) {
                         if (etag == 0x7u) {//FIFO_OVF
                             etagResponse7++
                             maxDriver.fifoReset()
                             //maxDriver.sendSynch() // Reset FIFO
                         } else {
                             otherError++
                         }
                     } else {
                         points++
                         ecgPoints.add(ecgdata.toDouble())
                     }

                     if(now - startTime >= 1000) {
                         sps = 1000 * points / (now - startTime)
                         startTime = now
                         points = 0
                     }

                     changeData("$etagResponse7 $skippedInterrupts $skippedOverflow $otherError " +
                             "${System.currentTimeMillis() - now} $sps $avgWriteTime")

                     //ecgSamples.add(ecgdata)
                     //ecgData(ecgdata.toString())
                 } while (etag == 0u || etag == 1u)
                TimeUnit.MILLISECONDS.sleep(8)
                /*if(now != lastECGTime) {
                    var avg = 0.0
                    for(v in ecgSamples) {
                        avg += v.toDouble()
                    }
                    avg /= ecgSamples.size
                    addPoint(count++, avg)
                    ecgSamples.clear()
                    lastECGTime = now
                }*/


/*
                val responseRtor = maxDriver.readRegister(RTOR) shr 10
                rtorSamples.add(responseRtor.toFloat())

                if(now - lastRTORTime > 100) {
                    hr = 0f
                    for(rtor in rtorSamples) {
                        hr += rtor
                    }
                    hr /= rtorSamples.size
                    hr *= (now - lastRTORTime) / 1000.0f
                    hr = 60.0f / hr
                    lastRTORTime = now
                }*/

                //changeData("$hr:$etagResponse7")
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
            graph.viewport.setMinX(max(0, time - 128 * 5).toDouble())
            graph.viewport.setMaxX(max(128 * 5, time).toDouble())
/*
            when(ecgType) {
                0 -> {
                    graph.viewport.setMinY((Short.MIN_VALUE - 100).toDouble())
                    graph.viewport.setMaxY((Short.MAX_VALUE + 100).toDouble())
                }
                1 -> {
                    graph.viewport.setMinY((Short.MIN_VALUE - 100).toDouble())
                    graph.viewport.setMaxY((Short.MAX_VALUE + 100).toDouble())
                }
                2 -> {
                    graph.viewport.setMinY((Short.MIN_VALUE * 4 - 100).toDouble())
                    graph.viewport.setMaxY((Short.MAX_VALUE * 4 + 100).toDouble())
                }
                3 -> {
                    graph.viewport.setMinY(-100.0)
                    graph.viewport.setMaxY((Short.MAX_VALUE + 100).toDouble())
                }
                4 -> {
                    graph.viewport.setMinY((Short.MIN_VALUE - 100).toDouble())
                    graph.viewport.setMaxY((Short.MAX_VALUE + 100).toDouble())
                }
                5 -> {
                    graph.viewport.setMinY(-100.0)
                    graph.viewport.setMaxY((Short.MAX_VALUE * 4 + 100).toDouble())
                }
                6 -> {
                    graph.viewport.setMinY((Short.MIN_VALUE * 2 - 100).toDouble())
                    graph.viewport.setMaxY((Short.MAX_VALUE * 2 + 100).toDouble())
                }
            }*/
        }
    }
}

private operator fun CharSequence.plus(string: String): CharSequence {
    return this.toString() + string
}
