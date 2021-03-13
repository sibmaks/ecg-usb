package xyz.dma.ecg_usb

import android.content.Context
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by maksim.drobyshev on 07-Mar-21.
 */
class PointRecorder(context: Context) {
    private val recordsPath = File(context.filesDir, "records")
    private var recordFile: File? = null
    private val recordedPoints = CopyOnWriteArrayList<Int>()
    private var firstWrite = true
    private val executionService: ExecutorService

    init {
        if(!recordsPath.exists()) {
            recordsPath.mkdirs()
        }
        executionService = Executors.newFixedThreadPool(2)
        executionService.submit { writePoints() }
        createFile()
    }

    private fun createFile() {
        recordFile = File(recordsPath, "ecg-records-${System.currentTimeMillis()}.csv")
        firstWrite = true
    }

    fun onPoint(point: Int) {
        recordedPoints.add(point)
    }

    private fun writePoints() {
        while (!Thread.currentThread().isInterrupted) {
            val recordFile = this.recordFile ?: throw IllegalStateException("Record file not exists")
            val lineBuilder = StringBuilder()
            if (!firstWrite) {
                lineBuilder.append(",")
            }
            var first = true
            while (!recordedPoints.isEmpty()) {
                if (first) {
                    first = false
                } else {
                    lineBuilder.append(",")
                }
                lineBuilder.append(recordedPoints.removeAt(0))
            }
            recordFile.writeText(lineBuilder.toString())
        }
    }

    fun getRecordFile() : File {
        return this.recordFile ?: throw IllegalStateException("Record file not exists")
    }

    fun reset() {
        val recordFile = this.recordFile ?: throw IllegalStateException("Record file not exists")
        recordFile.delete()
        createFile()
    }
}