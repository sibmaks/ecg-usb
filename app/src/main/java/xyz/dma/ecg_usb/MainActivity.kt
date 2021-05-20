package xyz.dma.ecg_usb

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
    private lateinit var channelView: ChannelView
    private val incomingMessages = LinkedBlockingQueue<String>()
    private val executionService = Executors.newScheduledThreadPool(4)
    private lateinit var serialSocket: SerialSocket
    private var nextParameter: BoardParameter? = null
    private var activeChannels = 0
    private var activeChannel = 1
    private var connectedBoard: String? = null
    private var pointPrinting = false
    private var logInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initChannels()

        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)

        startIncomingProcessing()
        serialSocket =
            SerialSocket(getSystemService(Context.USB_SERVICE) as UsbManager, permissionIntent) { log(it, false) }
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

        serialSocket.connect()
    }

    private fun initChannels() {
        findViewById<ConstraintLayout>(R.id.channels_control_layout).visibility = View.GONE
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density

        val graphView = findViewById<LineChart>(R.id.graph_line_view)
        graphView.layoutParams.height = (dpWidth - 64).toInt()
        channelView = ChannelView(this, graphView)
        for(channel in 1..12) {
            channels[channel] = Channel(this, "$channel", channelView) {log(it)}
        }
    }

    private fun startIncomingProcessing() {
        executionService.submit {
            while (!Thread.currentThread().isInterrupted) {
                val line = incomingMessages.take()

                if (line.isEmpty()) {
                    if(nextParameter != null) {
                        serialSocket.send("GET_PARAMETER\n${nextParameter}")
                    }
                    continue
                }
                if(logInput) {
                    log(line)
                }
                if(nextParameter != null) {
                    try {
                        when (nextParameter) {
                            BoardParameter.MODEL -> {
                                connectedBoard = line
                                log("Connected board %s".format(connectedBoard))
                                nextParameter = BoardParameter.VERSION
                            }
                            BoardParameter.VERSION -> {
                                log("Board version %s".format(line))
                                nextParameter = BoardParameter.MIN_VALUE
                            }
                            BoardParameter.MIN_VALUE -> {
                                val value = line.toFloat()
                                channelView.changeMinValue(value)
                                log("Board min value %s".format(value))
                                nextParameter = BoardParameter.MAX_VALUE
                            }
                            BoardParameter.MAX_VALUE -> {
                                val value = line.toFloat()
                                channelView.changeMaxValue(value)
                                log("Board max value %s".format(value))
                                nextParameter = BoardParameter.CHANNELS_COUNT
                            }
                            BoardParameter.CHANNELS_COUNT -> {
                                activeChannels = line.toInt()
                                log("Active channels %d".format(activeChannels))
                                channels[1]?.onSelect()
                                for (i in channels.entries) {
                                    if (i.key > activeChannels) {
                                        i.value.stop()
                                    } else {
                                        i.value.start()
                                    }
                                }
                                runOnUiThread {
                                    findViewById<ConstraintLayout>(R.id.channels_control_layout).visibility =
                                        if (activeChannels < 2) View.GONE else View.VISIBLE
                                    findViewById<Button>(R.id.startRecordButton).isEnabled = true
                                    findViewById<Button>(R.id.sendButton).isEnabled = true
                                }
                                log("Initialization finished")
                                nextParameter = null
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        if(nextParameter != null) {
                            serialSocket.send("GET_PARAMETER\n${nextParameter}")
                        }
                    } catch (e: Exception) {
                        if(nextParameter != null) {
                            serialSocket.send("GET_PARAMETER\n${nextParameter}")
                        }
                    }
                    if(nextParameter != null) {
                        serialSocket.send("GET_PARAMETER\n${nextParameter}")
                    }
                }
                if (connectedBoard != null) {
                    if(line.contains("DATA_FLOW_STARTED")) {
                        runOnUiThread {
                            val button = findViewById<Button>(R.id.startRecordButton)
                            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
                            button.isEnabled = true

                            channels.forEach {
                                if (it.value.isActive()) {
                                    it.value.pointPrinting = true
                                }
                            }
                            val recordToggleButton = findViewById<ToggleButton>(R.id.recordToggleButton)
                            recordToggleButton.isEnabled = pointPrinting
                        }
                    } else if(line.contains("DATA_FLOW_STOPPED")) {
                        runOnUiThread {
                            val button = findViewById<Button>(R.id.startRecordButton)
                            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow, 0, 0, 0)
                            button.isEnabled = true

                            channels.forEach {
                                if (it.value.isActive()) {
                                    it.value.pointPrinting = false
                                }
                            }
                            val recordToggleButton = findViewById<ToggleButton>(R.id.recordToggleButton)
                            recordToggleButton.isActivated = false
                            recordToggleButton.isEnabled = pointPrinting
                        }
                    } else if (activeChannels == 1 && line.isInt()) {
                        channels[1]?.addPoint(line.toInt())
                    } else if (activeChannels > 1) {
                        val parts = line.split(",")
                        if (parts.size == activeChannels || !parts[0].isInt()) {
                            for (i in parts.indices) {
                                if (parts[i].isInt()) {
                                    channels[i + 1]?.addPoint(parts[i].toInt())
                                }
                            }
                        }
                    }
                } else {
                    log("Device is unknown, request info")
                    serialSocket.send("GET_PARAMETER\nMODEL")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
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
                it.value.recordOn = view.isChecked
            }
        }
    }

    fun onLogOutputButtonClick(view: View) {
        if(view is ToggleButton) {
            logInput = view.isChecked
        }
    }

    fun onBackChannelButton(view: View) {
        changeActiveChannel(activeChannel - 1)
    }

    fun onForwardChannelButton(view: View) {
        changeActiveChannel(activeChannel + 1)
    }

    private fun changeActiveChannel(activeChannel: Int) {
        this.activeChannel = when {
            activeChannel > activeChannels -> 1
            activeChannel == 0 -> activeChannels
            else -> activeChannel
        }
        channels[this.activeChannel]?.onSelect()
    }

    private fun switchStartButton(view: Button = findViewById(R.id.startRecordButton)) {
        view.isEnabled = false
        if (pointPrinting) {
            serialSocket.send("START")
        } else {
            serialSocket.send("STOP")
        }
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
        serialSocket.reset()
        nextParameter = BoardParameter.MODEL
        serialSocket.send("GET_PARAMETER\nMODEL")
    }

    override fun onDisconnect(serialSocket: SerialSocket) {
        nextParameter = null
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