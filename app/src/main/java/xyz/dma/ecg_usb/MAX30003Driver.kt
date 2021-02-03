package xyz.dma.ecg_usb

import xyz.dma.ecg_usb.config.*
import xyz.dma.ecg_usb.microchipusb.ExchangeType
import xyz.dma.ecg_usb.microchipusb.MCP2210Driver
import xyz.dma.ecg_usb.microchipusb.Mcp2210Constants
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


@ExperimentalUnsignedTypes
class MAX30003Driver(
    private val driver: MCP2210Driver,
    private val logger: (String) -> Unit
) {
    private val accessLock = ReentrantLock()
    private val exchangeArray = ByteArray(64)
    private var exchangeBuffer = ByteBuffer.wrap(exchangeArray)
    private val dataArray = ByteArray(64)
    private val dataBuffer = ByteBuffer.wrap(dataArray)

    companion object {
        const val WRITE_REGISTER = 0x00
        const val READ_REGISTER = 0x01

        const val NO_OP = 0x00
        const val STATUS = 0x01
        const val EN_INT = 0x02
        const val EN_INT2 = 0x03
        const val MNGR_INT = 0x04
        const val MNGR_DYN = 0x05
        const val SW_RST = 0x08
        const val SYNCH = 0x09
        const val FIFO_RST = 0x0A
        const val INFO = 0x0F
        const val CNFG_GEN = 0x10
        const val CNFG_CAL = 0x12
        const val CNFG_EMUX = 0x14
        const val CNFG_ECG = 0x15
        const val CNFG_RTOR1 = 0x1D
        const val CNFG_RTOR2 = 0x1E
        const val ECG_FIFO_BURST = 0x20
        const val ECG_FIFO = 0x21
        const val RTOR = 0x25
        const val NO_OP2 = 0x7F
    }

    private val gpPinDes: ByteBuffer = ByteBuffer.allocate(9)
    private val defaultGpioOutput: IntBuffer = IntBuffer.allocate(1)
    private val defaultGpioDirection: IntBuffer = IntBuffer.allocate(1)

    fun open(spsMode: Int) {
        driver.setSpiTxferSize(ExchangeType.CURRENT_SETTINGS_ONLY,  16 * 3 + 1, 1000 * 1000 * 12)

        turnOffBoard()
        swReset()
        fifoReset()
        if(driver.getCurrentSpiTxferSettings() == Mcp2210Constants.SUCCESSFUL) {
            logger("Delay CS: ${driver.getmCsToDataDly()}")
            logger("Delay data to CS: ${driver.getmDataToCsDly()}")
            logger("Delay data to data: ${driver.getmDataToDataDly()}")
            logger("Baud rate: ${driver.getmBaudRate()}")
        } else {
            logger("Error get delays")
        }
        updateConfigs()
        gpPinDes.array()[0] = 0x01
        driver.setGpConfig(ExchangeType.CURRENT_SETTINGS_ONLY, gpPinDes, defaultGpioOutput[0], defaultGpioDirection[0])

        logger("Start configure")
        // General config register setting
        val CNFG_GEN_r = GeneralConfiguration()
        CNFG_GEN_r.enableECG = true   // Enable ECG channel
        CNFG_GEN_r.enableResistiveBiasOnNegativeInput = true   // Enable resistive bias on negative input
        CNFG_GEN_r.enableResistiveBiasOnPositiveInput = true   // Enable resistive bias on positive input
        CNFG_GEN_r.resistiveBiasMode = 1    // (was 1)
        CNFG_GEN_r.enableResistiveLeadBiasMode = 1    // Enable resistive bias (was 0)
        CNFG_GEN_r.leadOffCurrentMagnitude = 0    // Current magnitude = 10nA (was 0)
        CNFG_GEN_r.enableDCLeadOffDetection = 1   // Enable DC lead-off detection
        writeRegister(CNFG_GEN, CNFG_GEN_r.toInt())
        logger("End configure CNFG_GEN: ${CNFG_GEN_r.toInt().toString(16)}")
        var inMemory = readRegisterTransactional(CNFG_GEN)
        CNFG_GEN_r.read(inMemory)
        logger("In memory CNFG_GEN: ${inMemory.toString(16)}")

        val CNFG_CAL_r = CalConfiguration()
        CNFG_CAL_r.en_vcal = true
        writeRegister(CNFG_CAL, CNFG_CAL_r.toInt())
        logger("End configure CNFG_CAL: ${CNFG_CAL_r.toInt().toString(16)}")
        inMemory = readRegisterTransactional(CNFG_CAL)
        CNFG_CAL_r.read(inMemory)
        logger("In memory CNFG_CAL: ${inMemory.toString(16)}")

        // ECG Config register setting
        val CNFG_ECG_r = ECGConfiguration()
        CNFG_ECG_r.digitalLowPassFilter = 0 // Digital LPF cutoff = 40Hz
        CNFG_ECG_r.digitalHighPassFilter = false // Digital HPF cutoff = 0.5Hz
        CNFG_ECG_r.gain = 1 // ECG gain = 40V/V
        CNFG_ECG_r.rate = spsToRate(spsMode) // Sample rate = 128 sps
        writeRegister(CNFG_ECG, CNFG_ECG_r.toInt())
        logger("End configure CNFG_ECG: ${CNFG_ECG_r.toInt().toString(16)}")
        inMemory = readRegisterTransactional(CNFG_ECG)
        CNFG_ECG_r.read(inMemory)
        logger("In memory CNFG_ECG: ${inMemory.toString(16)}")

        //R-to-R configuration
        val CNFG_RTOR_r = RtoR1Configuration()
        CNFG_RTOR_r.averageWindowWidth = 3 // WNDW = 96ms
        CNFG_RTOR_r.gain = 15 // Auto-scale gain
        CNFG_RTOR_r.peakAverageWeightFactor = 0b10 // 16-average (was 0)
        CNFG_RTOR_r.peakThresholdScalingFactor = 3 // PTSF = 4/16 (was 6)
        CNFG_RTOR_r.enableRTOR = true // Enable R-to-R detection
        writeRegister(CNFG_RTOR1, CNFG_RTOR_r.toInt())
        logger("End configure CNFG_RTOR1: ${CNFG_RTOR_r.toInt().toString(16)}")

        //Manage interrupts register setting
        val MNG_INT_r = ManageInterrupts()
        MNG_INT_r.efit = 0 // Assert EINT w/ 4 unread samples
        MNG_INT_r.clr_rrint = 1 // Clear R-to-R on RTOR reg. read back
        writeRegister(MNGR_INT, MNG_INT_r.toInt())
        logger("End configure MNGR_INT: ${MNG_INT_r.toInt().toString(16)}")

        // MUX Config
        val CNFG_MUX_r = MuxConfiguration()
        CNFG_MUX_r.calibrationECGN = 3 // was (3)
        CNFG_MUX_r.calibrationECGP = 2 // was (2)
        writeRegister(CNFG_EMUX, CNFG_MUX_r.toInt())
        logger("End configure CNFG_EMUX: ${CNFG_MUX_r.toInt().toString(16)}")

        logger("End configure")

        sendSynch()
    }

    fun readRegisterTransactional(address: Int): UInt {
        accessLock.withLock {
            val spiTXBuffer = (address shl 1) or READ_REGISTER
            exchangeArray[0] = spiTXBuffer.toByte()

            //turnOnBoard()
            //logger("3 ${System.currentTimeMillis() - now}")
            val responseCode = driver.txferSpiDataBuf(exchangeBuffer, dataBuffer)
            //logger("4 ${System.currentTimeMillis() - now}")
            //turnOffBoard()

            if (responseCode != Mcp2210Constants.SUCCESSFUL) {
                throw RuntimeException("Read data exception: $responseCode")
            }
            //logger("Incoming data: ${dataBuffer[0]} ${dataBuffer[1]} ${dataBuffer[2]} ${dataBuffer[3]}")

            var data = dataArray[1] + 0u
            data = data shl 8
            data = data or (dataArray[2] + 0u)
            data = data shl 8
            data = data or (dataArray[3] + 0u)

            //logger("6 ${System.currentTimeMillis() - now}")
            return data
        }
    }

    fun readsRegisterTransactional(address: Int): List<UInt> {
        accessLock.withLock {
            val spiTXBuffer = (address shl 1) or READ_REGISTER
            val command = spiTXBuffer.toByte()
            for(i in 0..(16 * 3)) {
                exchangeArray[i] = command
            }

            //turnOnBoard()
            //logger("3 ${System.currentTimeMillis() - now}")
            val responseCode = driver.txferSpiDataBuf(exchangeBuffer, dataBuffer)
            //logger("4 ${System.currentTimeMillis() - now}")
            //turnOffBoard()

            if (responseCode != Mcp2210Constants.SUCCESSFUL) {
                throw RuntimeException("Read data exception: $responseCode")
            }
            //logger("Incoming data: ${dataBuffer[0]} ${dataBuffer[1]} ${dataBuffer[2]} ${dataBuffer[3]}")

            val datas = ArrayList<UInt>()

            for(i in 0..16) {
                var data = dataArray[i * 3 + 1] + 0u
                data = data shl 8
                data = data or (dataArray[i * 3 + 2] + 0u)
                data = data shl 8
                data = data or (dataArray[i * 3 + 3] + 0u)

                datas.add(data)
            }

            //logger("6 ${System.currentTimeMillis() - now}")
            return datas
        }
    }

    private fun swReset() {
        writeRegister(SW_RST, 0x000000)
    }

    fun fifoReset() {
        writeRegister(FIFO_RST, 0x000000)
    }

    private fun writeRegister(address: Int, data: Int) {
        // now combine the register address and the command into one byte:
        val dataToSend = (address shl 1) or WRITE_REGISTER

        accessLock.withLock {
            exchangeArray[0] = dataToSend.toByte()
            exchangeArray[1] = (data ushr 16).toByte()
            exchangeArray[2] = (data ushr 8).toByte()
            exchangeArray[3] = data.toByte()

            driver.txferSpiDataBuf(exchangeBuffer, dataBuffer)
        }
    }

    private fun turnOffBoard() {
        // Made output mode
        val defaultGpioDirectionVal = defaultGpioDirection[0] and 0xFE
        // Turn on chip
        val defaultGpioOutputVal = defaultGpioOutput[0] or 1

        driver.setGpConfig(ExchangeType.CURRENT_SETTINGS_ONLY, gpPinDes, defaultGpioOutputVal, defaultGpioDirectionVal)
    }

    private fun updateConfigs() {
        gpPinDes.clear()
        defaultGpioOutput.clear()
        defaultGpioDirection.clear()
        val config = driver.getGpConfig(
            ExchangeType.CURRENT_SETTINGS_ONLY, gpPinDes, defaultGpioOutput,
            defaultGpioDirection
        )
        if (config != Mcp2210Constants.SUCCESSFUL) {
            throw IllegalStateException("Get config error")
        }
    }

    fun sendSynch() {
        writeRegister(SYNCH, 0x000000)
    }

    fun changeSps(spsMode: Int) {
        // ECG Config register setting
        val CNFG_ECG_r = ECGConfiguration()
        CNFG_ECG_r.digitalLowPassFilter = 0 // Digital LPF cutoff = 40Hz
        CNFG_ECG_r.digitalHighPassFilter = true // Digital HPF cutoff = 0.5Hz
        CNFG_ECG_r.gain = 1 // ECG gain = 40V/V
        CNFG_ECG_r.rate = spsToRate(spsMode) // Sample rate = 128 sps

        accessLock.withLock {

            writeRegister(CNFG_ECG, CNFG_ECG_r.toInt())
            logger("Configure CNFG_ECG changed: ${CNFG_ECG_r.toInt().toString(16)}")
            val inMemory = readRegisterTransactional(CNFG_ECG)
            CNFG_ECG_r.read(inMemory)
            logger("In memory CNFG_ECG: ${inMemory.toString(16)}")

            sendSynch()
        }
    }

    private fun spsToRate(spsMode: Int): Int {
        return when(spsMode) {
            512 -> {
                0
            }
            256 -> {
                1
            }
            else -> 2
        }
    }
}

private operator fun Byte.plus(value: UInt): UInt {
    return this.toUByte() + value
}
