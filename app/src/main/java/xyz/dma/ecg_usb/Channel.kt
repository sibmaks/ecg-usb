package xyz.dma.ecg_usb

import android.app.Activity
import android.view.View
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.max

/**
 * Created by maksim.drobyshev on 27-Mar-21.
 */
class Channel(private val activity: Activity,
              private val graphView: GraphView,
              private val logger: (String) -> Unit) {

    private val series = LineGraphSeries<DataPoint>()
    private val ecgPoints = LinkedBlockingQueue<Double>()
    private val pointRecorder = PointRecorder(activity.filesDir, logger)
    private val executionService = Executors.newFixedThreadPool(2)
    private var active = false
    var recordOn = false
    var pointPrinting = false

    init {
        graphView.addSeries(series)
        graphView.minimumWidth = 100
        graphView.viewport.isScalable = true
        graphView.viewport.setScalableY(true)
        graphView.gridLabelRenderer.labelVerticalWidth = 180
        graphView.gridLabelRenderer.numHorizontalLabels = 15
        graphView.gridLabelRenderer.numVerticalLabels = 15
        graphView.gridLabelRenderer.isHorizontalLabelsVisible = false

        executionService.submit {
            var count = 0L
            while (!Thread.interrupted()) {
                val egcData = ecgPoints.take()
                if(pointPrinting) {
                    if(recordOn) {
                        pointRecorder.onPoint(egcData)
                    }
                    printPoint(count++, egcData)
                }
            }
        }
    }

    fun reset() {
        pointRecorder.reset()
    }

    fun addPoint(value: Double) {
        ecgPoints.add(value)
    }

    fun start() {
        pointPrinting = true
        graphView.visibility = View.VISIBLE
        active = true
    }

    fun switchRecording() {
        recordOn = !recordOn
    }

    fun stop() {
        pointPrinting = false
        recordOn = false
        graphView.visibility = View.INVISIBLE
        active = false
    }

    fun isActive() : Boolean {
        return active
    }

    private fun printPoint(time: Long, value: Double) {
        activity.runOnUiThread {
            series.appendData(DataPoint(time.toDouble(), value), true, 2000)
            graphView.viewport.setMinX(max(0, time - 128 * 5).toDouble())
            graphView.viewport.setMaxX(max(128 * 5, time).toDouble())
        }
    }
}