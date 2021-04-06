package xyz.dma.ecg_usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.LineChart
import xyz.dma.ecg_usb.serial.SerialDataListener
import xyz.dma.ecg_usb.serial.SerialSocket
import xyz.dma.ecg_usb.serial.SerialSocketListener
import xyz.dma.ecg_usb.util.FileUtils
import xyz.dma.ecg_usb.util.isInt
import xyz.dma.ecg_usb.util.plus
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), SerialDataListener, SerialSocketListener {
    private val channels = HashMap<Int, Channel>()
    private val incomingMessages = LinkedBlockingQueue<String>()
    private val executionService = Executors.newFixedThreadPool(4)
    private lateinit var serialSocket: SerialSocket
    private var activeChannels = 0
    private var connectedBoard: String? = null
    private var pointPrinting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initChannels()

        try {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_USB_PERMISSION), 0
            )

            startIncomingProcessing()
            startChannelRefresher()
            serialSocket = SerialSocket(getSystemService(Context.USB_SERVICE) as UsbManager, permissionIntent) { log(it, false)}
            serialSocket.addDataListener(this)
            serialSocket.addSocketListener(this)

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

            // Try connect
            serialSocket.connect()
        } catch (e: Exception) {
            log(e.message ?: "null message exception")
            log(e.stackTraceToString())
        }
    }

    private fun initChannels() {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density

        val graphLayout = findViewById<LinearLayout>(R.id.graph_layout)
        for(channel in 1..5) {
            val graphView = LineChart(this)
            graphView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (dpWidth - 64).toInt()
            )
            graphView.visibility = View.GONE
            graphLayout.addView(graphView)
            channels[channel] = Channel(this, graphView, "$channel") {log(it)}
        }
    }

    private fun startChannelRefresher() {
        executionService.submit {
            try {
                var time = System.currentTimeMillis()
                while (!Thread.interrupted()) {
                    val now = System.currentTimeMillis()
                    if (now - time >= (1000f / 60f)) {
                        for(channel in channels.values) {
                            if(channel.isActive()) {
                                channel.refresh()
                            }
                        }
                        time = now
                    }
                }
            } catch (e: Exception) {
                log(e.message ?: "null message exception")
                log(e.stackTraceToString())
            }
        }
    }

    private fun startIncomingProcessing() {
        executionService.submit {
            while (!Thread.currentThread().isInterrupted) {
                val line = incomingMessages.take()

                if (line.isEmpty()) {
                    continue
                }
                if (line.startsWith("ECG_STM32")) {
                    val appInfo = line.split(":")
                    if (appInfo.size != 4) {
                        log("ECG_STM32 header is wrong, repeat request")
                        serialSocket.send("M")
                    } else {
                        connectedBoard = appInfo[1]
                        try {
                            for (channel in channels) {
                                channel.value.onBoardChange(appInfo[1])
                            }
                        } catch (e: Exception) {
                            log(e.message ?: "null message exception")
                            log(e.stackTraceToString())
                        }
                        activeChannels = appInfo[2].toInt()
                        log("Connected board %s, %d active channels".format(connectedBoard, activeChannels))
                        serialSocket.send("0")
                        for (i in channels.entries) {
                            if (i.key > activeChannels) {
                                i.value.stop()
                            } else {
                                i.value.start()
                            }
                        }
                        runOnUiThread {
                            findViewById<Button>(R.id.startRecordButton).isEnabled = true
                            findViewById<Button>(R.id.sendButton).isEnabled = true
                        }
                    }
                } else if (activeChannels == 1 && line.isInt()) {
                    channels[1]?.addPoint(line.toInt())
                } else if (activeChannels > 1) {
                    val parts = line.split(",")
                    if (parts.size == activeChannels || !parts[0].isInt()) {
                        if (connectedBoard == "ADS1293") {
                            for (i in parts.indices) {
                                if (parts[i].isInt()) {
                                    channels[i + 1]?.addPoint(parts[i].toInt())
                                }
                            }
                        }
                    }
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

    fun onStartButtonClick(view: View) {
        pointPrinting = !pointPrinting
        if(view is Button) {
            switchStartButton(view)
        }
    }

    fun onRecordButtonClick(view: View) {
        if(view is ToggleButton) {
            channels.forEach {
                if (it.value.isActive()) {
                    it.value.recordOn = view.isChecked
                }
            }
        }
    }

    private fun switchStartButton(view: Button = findViewById(R.id.startRecordButton)) {
        if (pointPrinting) {
            view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
            channels.forEach {
                if (it.value.isActive()) {
                    it.value.pointPrinting = true
                }
            }
            serialSocket.send("3")
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0)
            if (serialSocket.isConnected()) {
                serialSocket.send("4")
            }
            channels.forEach {
                if (it.value.isActive()) {
                    it.value.pointPrinting = false
                }
            }
            findViewById<ToggleButton>(R.id.recordToggleButton).isActivated = false
        }
        findViewById<ToggleButton>(R.id.recordToggleButton).isEnabled = pointPrinting
        serialSocket.reset()
    }

    fun onResetButtonClick(view: View) {
        channels.forEach { channel ->
            executionService.submit { channel.value.reset() }
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
            val recordFiles = ArrayList<File>()
            channels.forEach{
                if(it.value.isActive()) {
                    recordFiles.add(it.value.getRecordFile())
                }
            }

            when (recordFiles.size) {
                0 -> {
                    return@submit
                }
                1 -> {
                    val recordFile = recordFiles[0]
                    shareFile( "text/csv", recordFile)
                }
                else -> {
                    val recordFile = File(this.filesDir, "channels-${System.currentTimeMillis()}.zip")
                    FileUtils.zip(recordFiles, recordFile)
                    shareFile("application/zip", recordFile)
                }
            }
        }
    }

    private fun shareFile(type: String, file: File) {
        val intentShareFile = Intent(Intent.ACTION_SEND)
        intentShareFile.type = type
        intentShareFile.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(this@MainActivity, "xyz.dma.ecg_usb.FILE_PROVIDER", file)
        )
        intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_file_text))

        startActivity(Intent.createChooser(intentShareFile, getString(R.string.save_result_text)))
    }

    override fun onLine(line: String) {
        incomingMessages.add(line)
    }

    override fun onConnect(serialSocket: SerialSocket) {
        serialSocket.send("M")
    }

    override fun onDisconnect(serialSocket: SerialSocket) {
        connectedBoard = null
        pointPrinting = false
        channels.forEach {
            it.value.stop()
        }
        runOnUiThread {
            findViewById<Button>(R.id.startRecordButton).setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0)
            findViewById<Button>(R.id.startRecordButton).isEnabled = false
            findViewById<Button>(R.id.sendButton).isEnabled = false
            findViewById<ToggleButton>(R.id.recordToggleButton).isEnabled = false
        }
    }
}