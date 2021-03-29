package xyz.dma.ecg_usb

import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by maksim.drobyshev on 07-Mar-21.
 */
class PointRecorder(fileParent: File,
                    private val logger: (String) -> Unit) {
    private val recordsPath = File(fileParent, "records")
    private var recordFile: File? = null
    private var writer: BufferedWriter? = null
    private val recordedPoints = CopyOnWriteArrayList<Double>()
    private var firstWrite = true
    private val executionService: ExecutorService

    init {
        if(!recordsPath.exists()) {
            recordsPath.mkdirs()
        }
        createFile()
        executionService = Executors.newFixedThreadPool(2)
        executionService.submit { writePoints() }
    }

    private fun createFile() {
        val recordFile = File(recordsPath, "ecg-records-${System.currentTimeMillis()}.csv")
        val fileOutputString = recordFile.outputStream()
        writer = BufferedWriter(OutputStreamWriter(fileOutputString))
        this.recordFile = recordFile
        firstWrite = true
    }

    fun onPoint(point: Double) {
        recordedPoints.add(point)
    }

    private fun writePoints() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                if (recordedPoints.isEmpty()) {
                    continue
                }
                var lineBuilder = StringBuilder()
                if (!firstWrite) {
                    lineBuilder = lineBuilder.append(",")
                } else {
                    firstWrite = false
                }
                while (!recordedPoints.isEmpty()) {
                    lineBuilder = lineBuilder.append(recordedPoints.removeAt(0))
                    if(!recordedPoints.isEmpty()) {
                        lineBuilder = lineBuilder.append(",")
                    }
                }
                writer?.write(lineBuilder.toString())
                writer?.flush()
            }
        } catch (e: Exception) {
            logger(e.message ?: "null message exception")
            logger(e.stackTraceToString())
        } catch (e: java.lang.Exception) {
            logger(e.message ?: "null message exception")
            logger(e.stackTraceToString())
        }
    }

    fun getRecordFile() : File {
        return this.recordFile ?: throw IllegalStateException("Record file not exists")
    }

    fun close() {
        this.writer?.close()
    }

    fun reset() {
        close()
        recordFile?.delete()
        createFile()
    }
}