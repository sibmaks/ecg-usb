package xyz.dma.ecg_usb

import android.app.Activity
import android.graphics.Color
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import xyz.dma.ecg_usb.util.ResourceUtils
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by maksim.drobyshev on 27-Mar-21.
 */
class Channel(private val activity: Activity,
              private val lineChart: LineChart,
              name: String,
              logger: (String) -> Unit) {
    private val ecgPoints = LinkedBlockingQueue<Int>()
    private val pointRecorder = PointRecorder(activity.filesDir, "${name}-channel", logger)
    private val executionService = Executors.newSingleThreadExecutor()
    private var active = false
    var recordOn = false
    var pointPrinting = false

    init {
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(true)
        lineChart.setPinchZoom(true)
        lineChart.setBackgroundColor(Color.WHITE)
        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false

        lineChart.xAxis.setDrawAxisLine(true)
        lineChart.xAxis.setDrawGridLines(true)
        lineChart.xAxis.axisMinimum = 0f
        lineChart.xAxis.axisMaximum = 8000f

        lineChart.axisLeft.setDrawAxisLine(true)
        lineChart.axisLeft.setDrawGridLines(true)

        lineChart.visibility = View.GONE

        val points = ArrayList<Entry>()

        for(i in (lineChart.xAxis.axisMinimum.toInt())..(lineChart.xAxis.axisMaximum.toInt())) {
            points.add(Entry(i.toFloat(), 0f))
        }

        val lineDataSet = LineDataSet(points, "DataSet")
        lineDataSet.axisDependency = AxisDependency.LEFT
        lineDataSet.color = ColorTemplate.getHoloBlue()
        lineDataSet.valueTextColor = ColorTemplate.getHoloBlue()
        lineDataSet.lineWidth = 1.5f
        lineDataSet.setDrawCircles(false)
        lineDataSet.setDrawValues(false)
        lineDataSet.fillAlpha = 65
        lineDataSet.fillColor = ColorTemplate.getHoloBlue()
        lineDataSet.highLightColor = Color.rgb(244, 117, 117)
        lineDataSet.setDrawCircleHole(false)

        val lineData = LineData(lineDataSet)
        lineData.setValueTextColor(Color.WHITE)
        lineData.setValueTextSize(9f)

        lineChart.data = lineData

        executionService.submit {
            try {
                val min = lineChart.xAxis.axisMinimum.toInt()
                val max = lineChart.xAxis.axisMaximum.toInt()
                var count = min
                while (!Thread.interrupted()) {
                    val egcData = ecgPoints.take()
                    if (pointPrinting) {
                        if (recordOn) {
                            pointRecorder.onPoint(egcData)
                        }
                        printPoint(count++, egcData)
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

    fun refresh() {
        lineChart.postInvalidate()
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
        activity.runOnUiThread {
            lineChart.visibility = View.VISIBLE
        }
        active = true
    }

    fun stop() {
        pointPrinting = false
        recordOn = false
        activity.runOnUiThread {
            lineChart.visibility = View.GONE
        }
        active = false
    }

    fun isActive() : Boolean {
        return active
    }

    fun onBoardChange(boardName: String) {
        val boardConfigs = ResourceUtils.getHashMapResource(activity, R.xml.board_configs)
        lineChart.axisLeft.axisMinimum = boardConfigs["${boardName}_min"]?.toFloat() ?: throw IllegalStateException("${boardName}_min not defined")
        lineChart.axisLeft.axisMaximum = boardConfigs["${boardName}_max"]?.toFloat() ?: throw IllegalStateException("${boardName}_max not defined")
    }

    private fun printPoint(time: Int, value: Int) {
        lineChart.data.dataSets[0].getEntryForIndex(time).y = value.toFloat()
    }
}