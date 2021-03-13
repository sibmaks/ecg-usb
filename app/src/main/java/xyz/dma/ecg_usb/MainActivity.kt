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
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import xyz.dma.ecg_usb.serial.SerialDataListener
import xyz.dma.ecg_usb.serial.SerialSocket
import xyz.dma.ecg_usb.serial.SerialSocketListener
import xyz.dma.ecg_usb.util.isInt
import xyz.dma.ecg_usb.util.plus
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), SerialDataListener, SerialSocketListener {
    private lateinit var series: LineGraphSeries<DataPoint>
    private val ecgPoints = LinkedBlockingQueue<Int>()
    private var recordOn: Boolean = false
    private var pointPrinting: Boolean = false
    private val executionService: ExecutorService = Executors.newFixedThreadPool(8)
    private lateinit var serialSocket: SerialSocket
    private lateinit var pointRecorder: PointRecorder

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
        graph.gridLabelRenderer.numHorizontalLabels = 15
        graph.gridLabelRenderer.numVerticalLabels = 15
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false

        /** UsbManager used to scan for connected MCP2210 devices and grant USB permission.  */
        try {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_USB_PERMISSION), 0
            )

            serialSocket = SerialSocket(getSystemService(Context.USB_SERVICE) as UsbManager, permissionIntent) { log(it, false)}
            serialSocket.addDataListener(this)
            serialSocket.addSocketListener(this)

            pointRecorder = PointRecorder(this) {log(it)}

            val filter = IntentFilter(ACTION_USB_PERMISSION)

            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_USB_PERMISSION -> {
                            serialSocket.connect()
                        }
                        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                            try {
                                serialSocket.close()
                            } catch (e: Exception) {
                                log(e.message ?: "null message exception")
                                log(e.stackTraceToString())
                            }
                        }
                        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                            serialSocket.connect()
                        }
                    }
                }
            }, filter)
        } catch (e: Exception) {
            log(e.message ?: "null message exception")
            log(e.stackTraceToString())
        }

        executionService.submit {
            var count = 0L
            while (!Thread.interrupted()) {
                val egcData = ecgPoints.take()
                if(pointPrinting) {
                    if(recordOn) {
                        pointRecorder.onPoint(egcData)
                    }
                    addPoint(count++, egcData)
                }
            }
        }
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
        pointPrinting = !pointPrinting
        if(view is Button) {
            switchStartButton(pointPrinting, view)
        }
    }

    fun onRecordButtonClick(view: View) {
        if(view is ToggleButton) {
            recordOn = view.isChecked
        }
    }

    private fun switchStartButton(start: Boolean, view: Button = findViewById(R.id.startRecordButton)) {
        if(start) {
            view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
            pointPrinting = true
            serialSocket.send("3")
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0)
            if(serialSocket.isConnected()) {
                serialSocket.send("4")
            }
            executionService.submit {
                TimeUnit.SECONDS.sleep(5)
                pointPrinting = false
            }
            findViewById<ToggleButton>(R.id.recordToggleButton).isActivated = false
        }
        findViewById<ToggleButton>(R.id.recordToggleButton).isEnabled = pointPrinting
        serialSocket.reset()
    }

    fun onResetButtonClick(view: View) {
        executionService.submit {
            pointRecorder.reset()
        }
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
        executionService.submit {
            val recordFile = pointRecorder.getRecordFile()

            val intentShareFile = Intent(Intent.ACTION_SEND)
            intentShareFile.type = "text/csv"
            intentShareFile.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(this@MainActivity, "xyz.dma.ecg_usb.FILE_PROVIDER", recordFile)
            )
            intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_file_text))

            startActivity(Intent.createChooser(intentShareFile, getString(R.string.save_result_text)))
        }
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

    override fun onConnect(serialSocket: SerialSocket) {
        runOnUiThread {
            findViewById<Button>(R.id.startRecordButton).isEnabled = true
            findViewById<Button>(R.id.sendButton).isEnabled = true
        }
        serialSocket.send("0")
    }

    override fun onDisconnect(serialSocket: SerialSocket) {
        runOnUiThread {
            findViewById<Button>(R.id.startRecordButton).isEnabled = false
            findViewById<Button>(R.id.sendButton).isEnabled = false
            findViewById<ToggleButton>(R.id.recordToggleButton).isEnabled = false
        }
    }
}