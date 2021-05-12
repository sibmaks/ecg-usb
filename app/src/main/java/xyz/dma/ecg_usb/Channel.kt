package xyz.dma.ecg_usb

import android.app.Activity
import android.widget.TextView
import com.github.mikephil.charting.data.Entry
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by maksim.drobyshev on 27-Mar-21.
 */
class Channel(
    private val activity: Activity,
    private val name: String,
    private val channelView: ChannelView,
    logger: (String) -> Unit) {
    private val ecgPoints = LinkedBlockingQueue<Int>()
    private val pointRecorder = PointRecorder(activity.filesDir, "${name}-channel", logger)
    private val executionService = Executors.newSingleThreadExecutor()
    private val viewPoints = ArrayList<Entry>()
    private var active = false
    var pointPrinting = false
    var recordOn = false

    init {
        for(i in channelView.xMin()..channelView.xMax()) {
            viewPoints.add(Entry(i.toFloat(), 0f))
        }

        executionService.submit {
            try {
                val min = channelView.xMin()
                val max = channelView.xMax()
                var count = min
                while (!Thread.interrupted()) {
                    val egcData = ecgPoints.take()
                    if (pointPrinting) {
                        if (recordOn) {
                            pointRecorder.onPoint(egcData)
                        }
                        viewPoints[count++].y = egcData.toFloat()
                        if(count >= max) {
                            count = min
                        }
                    }
                }
            } catch (e: Exception) {
                logger(e.message ?: "null message exception")
                logger(e.stackTraceToString())
            }
        }
    }

    fun reset() {
        pointRecorder.reset()
    }

    fun getRecordFile() : File {
        return pointRecorder.getRecordFile()
    }

    fun addPoint(value: Int) {
        ecgPoints.add(value)
    }

    fun start() {
        active = true
    }

    fun stop() {
        recordOn = false
        active = false
    }

    fun isActive() : Boolean {
        return active
    }

    fun onSelect() {
        activity.runOnUiThread {
            activity.findViewById<TextView>(R.id.channel_name_view).text = name
        }
        channelView.setViewPoints(viewPoints)
    }
}