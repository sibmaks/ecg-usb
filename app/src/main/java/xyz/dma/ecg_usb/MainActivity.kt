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
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import xyz.dma.ecg_usb.serial.SerialListener
import xyz.dma.ecg_usb.serial.SerialSocket
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.max


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), SerialListener {
    private val ACTION_USB_PERMISSION = "xyz.dma.ecg_usb.USB_PERMISSION"
    private lateinit var series: LineGraphSeries<DataPoint>
    private val ecgPoints = LinkedBlockingQueue<Int>()
    private val recordedPoints = CopyOnWriteArrayList<Int>()
    private var recordOn: Boolean = false
    private var pointPrinting: Boolean = false
    private lateinit var serialSocket: SerialSocket

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

            serialSocket = SerialSocket(getSystemService(Context.USB_SERVICE) as UsbManager, permissionIntent) { log(it, false)}
            serialSocket.addListener(this)

            val filter = IntentFilter(ACTION_USB_PERMISSION)

            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_USB_PERMISSION -> {
                            serialSocket.connect()
                            if(serialSocket.isConnected()) {
                                findViewById<Button>(R.id.startRecordButton).isEnabled = true
                                findViewById<Button>(R.id.sendButton).isEnabled = true
                            }
                        }
                        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                            try {
                                serialSocket.close()
                                switchStartButton(false)
                            } catch (e: Exception) {
                                log(e.message ?: "null message exception")
                                log(e.stackTraceToString())
                            } finally {
                                findViewById<Button>(R.id.startRecordButton).isEnabled = false
                                findViewById<Button>(R.id.sendButton).isEnabled = false
                            }
                        }
                        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                            serialSocket.connect()
                            if(serialSocket.isConnected()) {
                                findViewById<Button>(R.id.startRecordButton).isEnabled = true
                                findViewById<Button>(R.id.sendButton).isEnabled = true
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
                val egcData = ecgPoints.take()
                if(recordOn) {
                    recordedPoints.add(egcData)
                    addPoint(count++, egcData)
                }
            }
        }.start()
    }

    private fun log(text: String, newLine: Boolean = true) {
        val textView = findViewById<TextView>(R.id.textView) ?: return
        runOnUiThread {
            textView.text = textView.text + text
            if(newLine) {
                textView.text = textView.text + "\n"
            }
        }
    }

    private fun addPoint(time: Long, value: Int) {
        val graph = findViewById<View>(R.id.ecgGraph) as GraphView
        runOnUiThread {
            series.appendData(DataPoint(time.toDouble(), value.toDouble()), true, 2000)
            graph.viewport.setMinX(max(0, time - 128 * 5).toDouble())
            graph.viewport.setMaxX(max(128 * 5, time).toDouble())
        }
    }

    fun onStartButtonClick(view: View) {
        recordOn = !recordOn
        if(view is Button) {
            switchStartButton(recordOn, view)
        }
    }

    private fun switchStartButton(start: Boolean, view: Button = findViewById(R.id.startRecordButton)) {
        if(start) {
            view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
            pointPrinting = true
            serialSocket.send("3")
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0)
            pointPrinting = false
            if(serialSocket.isConnected()) {
                serialSocket.send("4")
            }
        }
        serialSocket.reset()
    }

    fun onResetButtonClick(view: View) {
        recordedPoints.clear()
    }

    fun onSendButtonClick(view: View) {
        try {
            val commandView = findViewById<EditText>(R.id.commandLinePlainText)
            val text = commandView.text.trim().toString()
            if (text.isNotBlank()) {
                serialSocket.send(text)
                commandView.setText("")
            }
        } catch (e: Exception) {
            log(e.message ?: "null message exception")
            log(e.stackTraceToString())
        }
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

    override fun onLine(line: String) {
        if (line.isNotEmpty()) {
            if(pointPrinting && line.isInt()) {
                ecgPoints.add(line.toInt())
            } else {
                log(line)
            }
        }
    }
}