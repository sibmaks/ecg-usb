package xyz.dma.ecg_usb

import xyz.dma.ecg_usb.config.*
import xyz.dma.ecg_usb.microchipusb.ExchangeType
import xyz.dma.ecg_usb.microchipusb.MCP2210Driver
import xyz.dma.ecg_usb.microchipusb.Mcp2210Constants
import java.nio.ByteBuffer
import java.nio.IntBuffer


class MAX30003Driver(
    private val driver: MCP2210Driver,
    private val logger: (String) -> Unit
) {
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

    fun open() {
        driver.setSpiTxferSize(ExchangeType.CURRENT_SETTINGS_ONLY, 4, 1000 * 1000 * 12)

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
        var inMemory = readRegister(CNFG_GEN)
        CNFG_GEN_r.read(inMemory)
        logger("In memory CNFG_GEN: ${inMemory.toString(16)}")

        //writeRegister(CNFG_CAL, 0x720000)  // 0x700000

        // MUX Config
        val CNFG_MUX_r = MuxConfiguration()
        CNFG_MUX_r.calibrationECGN = 3 // was (3)
        CNFG_MUX_r.calibrationECGP = 2 // was (2)
        writeRegister(CNFG_EMUX, CNFG_MUX_r.toInt())
        logger("End configure CNFG_EMUX: ${CNFG_MUX_r.toInt().toString(16)}")

        // ECG Config register setting
        val CNFG_ECG_r = ECGConfiguration()
        CNFG_ECG_r.digitalLowPassFilter = 0 // Digital LPF cutoff = 40Hz
        CNFG_ECG_r.digitalHighPassFilter = true // Digital HPF cutoff = 0.5Hz
        CNFG_ECG_r.gain = 1 // ECG gain = 40V/V
        CNFG_ECG_r.rate = 2 // Sample rate = 128 sps
        writeRegister(CNFG_ECG, CNFG_ECG_r.toInt())
        logger("End configure CNFG_ECG: ${CNFG_ECG_r.toInt().toString(16)}")
        inMemory = readRegister(CNFG_ECG)
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

        //Enable interrupts register setting
        /*val EN_INT_r = EnableInterrupts()
        EN_INT_r.en_eint = true // Enable EINT interrupt
        EN_INT_r.en_rrint = true // Enable R-to-R interrupt
        EN_INT_r.intb_type = 3 // Open-drain NMOS with internal pullup
        writeRegister(EN_INT, EN_INT_r.toInt())
        logger("End configure EN_INT: ${EN_INT_r.toInt().toString(16)}")*/

        //Dyanmic modes config
        /*val MNG_DYN_r = ManageDynamicModes()
        MNG_DYN_r.fast = 0 // Fast recovery mode disabled
        writeRegister(MNGR_DYN, MNG_DYN_r.toInt())
        logger("End configure MNGR_DYN: ${MNG_DYN_r.toInt().toString(16)}")*/


        //sendSynch()

        logger("End configure")
        updateConfigs()
        //turnOnBoard()
        /*
        writeRegister(CNFG_GEN, 0x081007)
        MILLISECONDS.sleep(100)
        writeRegister(CNFG_CAL, 0x720000)  // 0x700000
        MILLISECONDS.sleep(100)
        writeRegister(CNFG_EMUX, 0x0B0000)
        MILLISECONDS.sleep(100)
        writeRegister(CNFG_ECG, 0x805000)  // d23 - d22 : 10 for 250sps , 00:500 sps
        MILLISECONDS.sleep(100)

        writeRegister(CNFG_RTOR1, 0x3fc600)
        sendSynch()
        MILLISECONDS.sleep(100)*/
    }

    fun readRegister(address: Int): UInt {
        val spiTXBuffer: Int = (address shl 1) or READ_REGISTER
        exchangeArray[0] = spiTXBuffer.toByte()

        turnOnBoard()
        //logger("3 ${System.currentTimeMillis() - now}")
        val responseCode = driver.txferSpiDataBuf(exchangeBuffer, dataBuffer)
        //logger("4 ${System.currentTimeMillis() - now}")
        turnOffBoard()

        if(responseCode != Mcp2210Constants.SUCCESSFUL) {
            throw RuntimeException("Read data exception: $responseCode")
        }
        //logger("Incoming data: ${dataBuffer[0]} ${dataBuffer[1]} ${dataBuffer[2]} ${dataBuffer[3]}")

        var data = dataBuffer[1] + 0u
        data = data shl 8
        data = data or (dataBuffer[2] + 0u)
        data = data shl 8
        data = data or (dataBuffer[3] + 0u)

        //logger("6 ${System.currentTimeMillis() - now}")
        return data
    }

    private fun swReset() {
        writeRegister(SW_RST, 0x000000)
    }

    fun fifoReset() {
        writeRegister(FIFO_RST, 0x000000)
    }

    fun writeRegister(address: Int, data: Int) {
        // now combine the register address and the command into one byte:
        val dataToSend = (address shl 1) or WRITE_REGISTER

        exchangeArray[0] = dataToSend.toByte()
        exchangeArray[1] = (data ushr 16).toByte()
        exchangeArray[2] = (data ushr 8).toByte()
        exchangeArray[3] = data.toByte()

        turnOnBoard()
        driver.txferSpiDataBuf(exchangeBuffer, dataBuffer)
        turnOffBoard()
    }

    fun turnOnBoard() {
        // Made output mode
        val defaultGpioDirectionVal = defaultGpioDirection[0] and 0xFE
        // Turn on chip
        val defaultGpioOutputVal = defaultGpioOutput[0] and 0xFE

        driver.setGpConfig(ExchangeType.CURRENT_SETTINGS_ONLY, gpPinDes, defaultGpioOutputVal, defaultGpioDirectionVal)
    }

    fun turnOffBoard() {
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
}

private operator fun Byte.plus(value: UInt): UInt {
    return this.toUByte() + value
}
