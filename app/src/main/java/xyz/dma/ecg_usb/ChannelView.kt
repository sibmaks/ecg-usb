package xyz.dma.ecg_usb

import android.app.Activity
import android.graphics.Color
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import xyz.dma.ecg_usb.util.ResourceUtils

/**
 * Created by maksim.drobyshev on 12-May-21.
 */
class ChannelView(private val activity: Activity,
                  private val lineChart: LineChart) {
    private val lineDataSet: LineDataSet

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
        //lineChart.axisLeft.granularity = 100f

        lineChart.visibility = View.VISIBLE

        val points = ArrayList<Entry>()

        for(i in (lineChart.xAxis.axisMinimum.toInt())..(lineChart.xAxis.axisMaximum.toInt())) {
            points.add(Entry(i.toFloat(), 0f))
        }

        lineDataSet = LineDataSet(points, "DataSet")
        lineDataSet.axisDependency = YAxis.AxisDependency.LEFT
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
    }

    fun refresh() {
        lineChart.postInvalidate()
    }

    fun onBoardChange(boardName: String) {
        val boardConfigs = ResourceUtils.getHashMapResource(activity, R.xml.board_configs)
        lineChart.axisLeft.axisMinimum = boardConfigs["${boardName}_min"]?.toFloat() ?: throw IllegalStateException("${boardName}_min not defined")
        lineChart.axisLeft.axisMaximum = boardConfigs["${boardName}_max"]?.toFloat() ?: throw IllegalStateException("${boardName}_max not defined")
    }

    fun setViewPoints(points: List<Entry>) {
        lineDataSet.values = points
    }

    fun xMin(): Int {
        return lineChart.xAxis.axisMinimum.toInt()
    }

    fun xMax(): Int {
        return lineChart.xAxis.axisMaximum.toInt()
    }
}