package xyz.dma.ecg_usb.microchipusb;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public class MCP2210Driver {

    /**
     * Used to check the error.
     */
    public static final boolean DEBUG = false;
    /**
     * MCP2210 object used to send or receive data.
     */
    private final MCPConnection mcpConnection;
    private final Consumer<String> logger;

    /* SPI TRANSFER SETTINGS */
    /**
     * SPI bit rate which can be between 1500bps and 12Mbps.
     */
    private int mBaudRate;
    /**
     * IDLE chip select field.
     */
    private int mIdleCsVal;
    /**
     * ACTIVE chip select field.
     */
    private int mActiveCsVal;
    /**
     * Chip select to data delay.
     */
    private int mCsToDataDly;
    /**
     * Delay between subsequent data bytes.
     */
    private int mDataToDataDly;
    /**
     * Last data byte to chip select.
     */
    private int mDataToCsDly;
    /**
     * Bytes per SPI transaction.
     */
    private int mTxferSize;
    /**
     * SPI mode. Supports all four SPI modes (Mode 0, 1, 2, 3).
     */
    private byte mSpiMd;

    /* CHIP SETTINGS */
    /**
     * Pin designations for the GP pins.
     */
    private final byte[] mGpPinDes = new byte[9];
    /**
     * Default GPIO output.
     */
    private int mGpioDefaultOutput;
    /**
     * Default GPIO direction.
     */
    private int mGpioDefaultDir;
    /**
     * Access control settings for the chip.
     */
    private byte mCurNvramAccessSetting;
    /**
     * SPI Bus release enable.
     */
    private byte mSpiBusRelEnable;
    /**
     * Remote wake-up enable.
     */
    private byte mRmtWkupEn;
    /**
     * Interrupt mode for pins.
     */
    private byte mInterruptPinMd;

    /**
     * SPI bus release external request status.
     */
    private byte mSpiBusExtReq;
    /**
     * SPI bus current owner.
     */
    private byte mSpiBusOwner;
    /**
     * Number of tries at password.
     */
    private byte mAccessAttemptCnt;
    /**
     * Was the password correctly given?
     */
    private byte mAccessGranted;
    /**
     * USB-SPI transaction ongoing (at cancellation moment).
     */
    private byte mUsbSpiTxOnGoing;

    /**
     * Create a new MCP2210.
     *
     * @param mcpConnection (MCP2210)    A reference to an MCP2210 object
     */
    public MCP2210Driver(final MCPConnection mcpConnection, Consumer<String> logger) {
        this.mcpConnection = mcpConnection;
        this.logger = logger;
    }

    /**
     * Allow the user to configure default settings.
     *
     * @param whichToGet       (int) [IN] Use constants defined in Mcp2210Constants class.
     *                         Current setting = 0,
     *                         Power-up default = 1,
     *                         Both = 2
     * @param baudRateBuf      (IntBuffer) [OUT] SPI bit rate speed
     * @param idleCsValBuf     (IntBuffer) [OUT] IDLE chip-select
     * @param activeCsValBuf   (IntBuffer) [OUT] ACTIVE chip-select
     * @param csToDataDlyBuf   (IntBuffer) [OUT] Delay between the assertion of Chip Select
     *                         and the first data byte
     * @param dataToDataDlyBuf (IntBuffer) [OUT] Delay between subsequent data bytes
     * @param dataToCsDlyBuf   (IntBuffer) [OUT] Delay between the end of the last byte of the SPI transfer
     *                         and the de-assertion of the Chip Select
     * @param txferSizeBuf     (IntBuffer) [OUT] Bytes per SPI transaction
     * @param spiMdBuf         (ByteBuffer) [OUT] SPI mode
     * @return (int) Error code. Indicates if the operation was successful or not.
     */

    public final int getAllSpiSettings(final ExchangeType whichToGet, final IntBuffer baudRateBuf,
                                       final IntBuffer idleCsValBuf, final IntBuffer activeCsValBuf,
                                       final IntBuffer csToDataDlyBuf, final IntBuffer dataToDataDlyBuf,
                                       final IntBuffer dataToCsDlyBuf, final IntBuffer txferSizeBuf, final IntBuffer spiMdBuf) {

        int result;

        /* Get the settings */
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            result =
                    getCurrentSpiTxferSettings(
                    );

        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            result =
                    getPwrUpSpiTxferSettings();

        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }

        /* Verify if errors have been occurred */
        if (result != Mcp2210Constants.SUCCESSFUL) {
            interpretErrorCode((byte) result);
        }

        /* Put the new settings in the output variable buffers */
        baudRateBuf.put(mBaudRate);
        idleCsValBuf.put(mIdleCsVal);
        activeCsValBuf.put(mActiveCsVal);
        csToDataDlyBuf.put(mCsToDataDly);
        dataToDataDlyBuf.put(mDataToDataDly);
        dataToCsDlyBuf.put(mDataToCsDly);
        txferSizeBuf.put(mTxferSize);
        spiMdBuf.put(mSpiMd);

        /* Return SUCCESS message if no errors have been occurred */
        return Mcp2210Constants.SUCCESSFUL;

    }

    /**
     * Set all the SPI settings with one function.
     *
     * @param whichToSet       (int) [IN] Use static constants defined in Mcp2210Constants class.
     *                         Use one of the following:
     *                         CURRENT_SETTINGS_ONLY = 0,
     *                         PWR_DEFAULT_ONLY = 1,
     *                         BOTH = 2
     * @param baudRateSet      (int) [IN] SPI bit rate speed
     * @param idleCsValSet     (int) [IN] IDLE chip select
     * @param activeCsValSet   (int) [IN] ACTIVE chip select
     * @param csToDataDlySet   (int) [IN] CS to data delay
     * @param dataToDataDlySet (int) [IN] Delay between subsequent data bytes
     * @param dataToCsDlySet   (int) [IN] Last data byte to chip select
     * @param txferSizeSet     (int) [IN] Bytes per SPI transaction
     * @param spiMdSet         (byte) [IN] SPI mode(Possible values:0,1,2, or 3)
     * @return (int) Error code. Indicates if the operation was successful or not.
     */

    public final int setAllSpiSettings(final ExchangeType whichToSet, final int baudRateSet,
                                       final int idleCsValSet, final int activeCsValSet,
                                       final int csToDataDlySet, final int dataToDataDlySet,
                                       final int dataToCsDlySet, final int txferSizeSet, final byte spiMdSet) {

        /* Ensure parameters are valid values */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        if (spiMdSet < 0 || spiMdSet > 3) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_9;
        }

        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result =
                    getCurrentSpiTxferSettings(
                    );
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mBaudRate = baudRateSet;
            mIdleCsVal = idleCsValSet;
            mActiveCsVal = activeCsValSet;
            mCsToDataDly = csToDataDlySet;
            mDataToDataDly = dataToDataDlySet;
            mDataToCsDly = dataToCsDlySet;
            mTxferSize = txferSizeSet;
            mSpiMd = spiMdSet;
            /* Set the current settings */
            result =
                    setCurrentSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }

            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result =
                    getPwrUpSpiTxferSettings();
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mBaudRate = baudRateSet;
            mIdleCsVal = idleCsValSet;
            mActiveCsVal = activeCsValSet;
            mCsToDataDly = csToDataDlySet;
            mDataToDataDly = dataToDataDlySet;
            mDataToCsDly = dataToCsDlySet;
            mTxferSize = txferSizeSet;
            mSpiMd = spiMdSet;
            /* Set the power-up defaults */
            result =
                    setPwrUpSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;

    }

    /**
     * Get SPI bit rate value.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) Returns the value of the SPI bit rate.
     */

    public final int getSpiBitRate(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentBitRate;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get bit rate (32 bit value) */
                currentBitRate =
                        ((rxData[7] << 24) & 0xFF000000) + ((rxData[6] << 16) & 0xFF0000)
                                + ((rxData[5] << 8) & 0xFF00) + (rxData[4] & 0xFF);
                return currentBitRate;

            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrBitRate;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/

                /* Get bit rate value (32 bit value). */
                pwrBitRate =
                        (((rxData[7] << 24) & 0xFF000000) + ((rxData[6] << 16) & 0xFF00)
                                + ((rxData[5] << 8) & 0xFF00) + (rxData[4] & 0xFF));
                return pwrBitRate;
            }
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Set SPI bit rate value.
     *
     * @param whichToSet (int) [IN] Use static constants defined in this class.
     *                   Use one of the following:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1,
     *                   BOTH = 2
     * @param bitRate    (int) [IN] Value of desired bit rate
     * @return (int) Contains error code. 0 = successful. Other = failed.
     */

    public final int setSpiBitRate(final ExchangeType whichToSet, final int bitRate) {

        /* Ensure parameters are valid values */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result =
                    getCurrentSpiTxferSettings(
                    );
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mBaudRate = bitRate;
            /* Set the current settings */
            result =
                    setCurrentSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result =
                    getPwrUpSpiTxferSettings();
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mBaudRate = bitRate;
            /* Set the power-up defaults */
            result =
                    setPwrUpSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;

    }


    /**
     * Get the SPI chip select idle value.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) Returns the value of the SPI chip select idle value.
     * If the return value is negative, the operation failed.
     */

    public final int getSpiCsIdleValue(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentIdleCsVal;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get IDLE CS value. */
                currentIdleCsVal = (rxData[8] & 0xFF) + ((rxData[9] << 8) & 0xFF00);
                return currentIdleCsVal;

            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrIdleCsVal;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get IDLE CS value (16 bit value) */
                pwrIdleCsVal = ((rxData[8] & 0xFF) + ((rxData[9] << 8) & 0xFF00));
                return pwrIdleCsVal;
            }
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Get the SPI chip select active value.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) Return the value of the SPI chip select active value.
     * If the return value is negative, the operation failed.
     */

    public final int getSpiCsActiveValue(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentActiveCsVal;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get ACTIVE CS value */
                currentActiveCsVal = ((rxData[10] & 0xFF) + ((rxData[11] << 8) & 0xFF00));
                return currentActiveCsVal;

            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrActiveCsVal;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get ACTIVE CS value */
                pwrActiveCsVal = ((rxData[10] & 0xFF) + ((rxData[11] << 8) & 0xFF00));
                return pwrActiveCsVal;
            }

        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }

    }


    /**
     * Set the SPI chip select values.
     *
     * @param whichToSet     (int) [IN] Use static constants defined in class Mcp2210Constants.
     *                       Use one of the following:
     *                       CURRENT_SETTINGS_ONLY = 0;
     *                       PWRUP_DEFAULTS_ONLY = 1;
     *                       BOTH = 2.
     * @param idleCsValSet   (int) [IN] IDLE chip select value
     * @param activeCsValSet (Integer) [IN] ACTIVE chip select value
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful, other = failed
     */
    public final int setSpiCsValues(final ExchangeType whichToSet, final int idleCsValSet,
                                    final int activeCsValSet) {
        /* Ensure parameters are valid values. */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result =
                    getCurrentSpiTxferSettings(
                    );
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mIdleCsVal = idleCsValSet;
            mActiveCsVal = activeCsValSet;
            /* Set the current settings */
            result =
                    setCurrentSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result =
                    getPwrUpSpiTxferSettings();
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mIdleCsVal = idleCsValSet;
            mActiveCsVal = activeCsValSet;
            /* Set the power-up defaults */
            result =
                    setPwrUpSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;
    }

    /**
     * Get the SPI mode.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (Integer) Returns the value of the SPI Mode (0,1,2 or 3).
     * If return code is less than zero, an error occurred
     */

    public final int getSpiMode(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            byte currentSpiMd;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get SPI mode */
                currentSpiMd = (byte) (rxData[20] & 0xFF);
                /* Return 0 */
                return currentSpiMd;

            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            byte pwrSpiMd;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get SPI mode */
                pwrSpiMd = (byte) (rxData[20] & 0xFF00);
                /* Return 0 */
                return pwrSpiMd;
            }
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Set the SPI mode.
     *
     * @param whichToSet (int) [IN] Use constants defined in this class.
     *                   Current setting = 0,
     *                   Power-up default = 1,
     *                   Both = 2
     * @param spiMdSet   (byte) [IN] Specify SPI mode 0, 1, 2, or 3
     * @return (int) Containes error code. 0 = successful. Other = failed
     */

    public final int setSpiMode(final ExchangeType whichToSet, final byte spiMdSet) {

        /* Ensure parameters are valid values */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        if (mSpiMd < 0 || mSpiMd > 3) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_2;
        }
        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result =
                    getCurrentSpiTxferSettings(
                    );
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable. */
            mSpiMd = spiMdSet;
            /* Set the current settings. */
            result =
                    setCurrentSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result =
                    getPwrUpSpiTxferSettings();
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mSpiMd = spiMdSet;
            /* Set the power-up defaults */
            result =
                    setPwrUpSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;

    }

    /**
     * Get CS to data SPI delay.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (Integer) Returns CS to data SPI delay value.
     * If return code is less than zero, error occurred.
     */

    public final int getSpiDelayCsToData(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentCsToDataDly;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get CS to data delay */
                currentCsToDataDly = ((rxData[12] & 0xFF) + ((rxData[13] << 8) & 0xFF00));
                return currentCsToDataDly;
            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrCsToDataDly;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get CS to data delay */
                pwrCsToDataDly = ((rxData[12] & 0xFF) + ((rxData[13] << 8) & 0xFF00));
                return pwrCsToDataDly;
            }
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Get data to data SPI delay.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) Returns data to data SPI delay value.
     * If return code is less than zero, an error occurred.
     */

    public final int getSpiDelayDataToData(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentDataToDataDly;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get data to data delay */
                currentDataToDataDly = ((rxData[16] & 0xFF) + ((rxData[17] << 8) & 0xFF00));
                return currentDataToDataDly;

            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrDataToDataDly;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get data to data delay */
                pwrDataToDataDly = ((rxData[16] & 0xFF) + ((rxData[17] << 8) & 0xFF00));
                return pwrDataToDataDly;
            }
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Get data to CS SPI delay.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) Returns data to CS SPI delay value.
     * If return code is less than zero, an error occurred.
     */

    public final int getSpiDelayDataToCs(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentDataToCsDly;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get data to CS delay */
                currentDataToCsDly = ((rxData[14] & 0xFF) + ((rxData[15] << 8) & 0xFF00));
                return currentDataToCsDly;
            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrDataToCsDly;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get data to CS delay */
                pwrDataToCsDly = ((rxData[14] & 0xFF) + ((rxData[15] << 8) & 0xFF00));
                return pwrDataToCsDly;
            }

        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Set the SPI delays.
     *
     * @param whichToSet       (int) [IN] Use constants defined in Mcp2210Constants class.
     *                         Current setting = 0,
     *                         Power-up default = 1,
     *                         Both = 2
     * @param csToDataDlySet   (int) [IN] CS to data delay
     * @param dataToDataDlySet (int) [IN] Delay between subsequent data bytes
     * @param dataToCsDlySet   (int) [IN] Last data byte to CS
     * @return (int) Indicates if the operation was successful or not.
     */

    public final int setSpiDelays(final ExchangeType whichToSet, final int csToDataDlySet,
                                  final int dataToDataDlySet, final int dataToCsDlySet) {

        /* Ensure parameters are valid values */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result =
                    getCurrentSpiTxferSettings(
                    );
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mCsToDataDly = csToDataDlySet;
            mDataToDataDly = dataToDataDlySet;
            mDataToCsDly = dataToCsDlySet;
            /* Set the current settings */
            result =
                    setCurrentSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result =
                    getPwrUpSpiTxferSettings();
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            mCsToDataDly = csToDataDlySet;
            mDataToDataDly = dataToDataDlySet;
            mDataToCsDly = dataToCsDlySet;
            /* Set the power-up defaults */
            result =
                    setPwrUpSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;

    }

    /**
     * Get the SPI transfer size.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) The SPI transfer size is returned.
     * If return code is less than zero, an error occurred.
     */

    public final int getSpiTxferSize(final ExchangeType whichToGet) {
        //Verify input parameter is correct and get appropriate settings
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentTxferSize;
            /*Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current SPI transfer settings. */
            cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[64];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get SPI transfer size */
                currentTxferSize = ((rxData[18] & 0xFF) + ((rxData[19] << 8) & 0xFF00));
                return currentTxferSize;

            }
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrTxferSize;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get SPI power-up settings. */
            cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
            /* Write the command to the device. */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error. */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host. */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error. */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {
                /* Unknown error, return the exact value. */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0.*/
                /* Get SPI transfer size */
                pwrTxferSize = ((rxData[18] & 0xFF) + ((rxData[19] << 8) & 0xFF00));
                return pwrTxferSize;
            }
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }

    }

    /**
     * Set the SPI transfer size.
     *
     * @param whichToSet   (int) [IN] Use constants defined in Mcp2210Constants class.
     *                     Current setting = 0,
     *                     Power-up default = 1,
     *                     Both = 2
     * @param txferSizeSet (int) [IN] Bytes per SPI transaction(can range from 0 to 65535 inclusive)
     * @return (int) Contains error code. 0 = successful. Other = failed
     */

    public final int setSpiTxferSize(final ExchangeType whichToSet, final int txferSizeSet) {
        /* Ensure parameters are valid values */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result =
                    getCurrentSpiTxferSettings(
                    );
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable. */
            mTxferSize = txferSizeSet;
            /* Set the current settings. */
            result =
                    setCurrentSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result =
                    getPwrUpSpiTxferSettings();
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable. */
            mTxferSize = txferSizeSet;
            /* Set the power-up defaults. */
            result =
                    setPwrUpSpiTxferSettings(mBaudRate, mIdleCsVal, mActiveCsVal, mCsToDataDly,
                            mDataToDataDly, mDataToCsDly, mTxferSize, mSpiMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;

    }

    /**
     * Retrieve the power-up SPI parameter information.
     *
     * @return (int) Indicates if the operation was successful or not.
     */

    public final int getPwrUpSpiTxferSettings() {

        /* Setup a buffer with command - Retrieve manufacture string */
        byte[] cmdData = new byte[64];
        /* Get NVRAM */
        cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
        /* Get SPI power-up settings. */
        cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
        /* Write the command to the device. */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error. */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host. */
        byte[] rxData = new byte[65];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error. */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {
            /* Unknown error, return the exact value. */
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        } else {
            /* Successful operation. Get needed data and return 0.*/

            /* Get bit rate value (32 bit value). */
            mBaudRate =
                    (((rxData[7] << 24) & 0xFF000000) + ((rxData[6] << 16) & 0xFF00)
                            + ((rxData[5] << 8) & 0xFF00) + (rxData[4] & 0xFF));
            /* Get IDLE CS value (16 bit value) */
            mIdleCsVal = ((rxData[8] & 0xFF) + ((rxData[9] << 8) & 0xFF00));
            /* Get ACTIVE CS value */
            mActiveCsVal = ((rxData[10] & 0xFF) + ((rxData[11] << 8) & 0xFF00));
            /* Get CS to data delay */
            mCsToDataDly = ((rxData[12] & 0xFF) + ((rxData[13] << 8) & 0xFF00));
            /* Get data to CS delay */
            mDataToCsDly = ((rxData[14] & 0xFF) + ((rxData[15] << 8) & 0xFF00));
            /* Get data to data delay */
            mDataToDataDly = ((rxData[16] & 0xFF) + ((rxData[17] << 8) & 0xFF00));
            /* Get SPI transfer size */
            mTxferSize = ((rxData[18] & 0xFF) + ((rxData[19] << 8) & 0xFF00));
            /* Get SPI mode */
            mSpiMd = (byte) (rxData[20] & 0xFF00);
            /* Return 0 */
            return Mcp2210Constants.SUCCESSFUL;
        }

    }

    /**
     * Set the power-up SPI parameter information.
     *
     * @param pwrBaudRate      (int) [IN] Power-up SPI bit rate speed
     * @param pwrIdleCsVal     (int) [IN] Power-up IDLE chip select value
     * @param pwrActiveCsVal   (int) [IN] Power-up ACTIVE chip select value
     * @param pwrCsToDataDly   (int) [IN] Power-up chip select to data delay
     * @param pwrDataToDataDly (int) [IN] Power-up delay between subsequent data bytes
     * @param pwrDataToCsDly   (int) [IN] Power-up last data byte to chip select
     * @param pwrTxferSize     (int) [IN] Power-up bytes per SPI transaction
     * @param pwrSpiMd         (byte) [IN] Power-up SPI mode (Possible values:0, 1, 2, or 3)
     * @return (int) Error code. Indicates if the operation was successful or not.
     * o = successful, other = failed
     */

    public final int setPwrUpSpiTxferSettings(final int pwrBaudRate, final int pwrIdleCsVal,
                                              final int pwrActiveCsVal, final int pwrCsToDataDly,
                                              final int pwrDataToDataDly, final int pwrDataToCsDly,
                                              final int pwrTxferSize, final byte pwrSpiMd) {

        /* Setup a buffer with command */
        byte[] cmdData = new byte[64];
        cmdData[Mcp2210Constants.PKT_INDX_CMD] = Mcp2210Constants.CMD_NVRAM_PARAM_SET;
        cmdData[1] = Mcp2210Constants.NVRAM_SPI_TX_SETTINGS;
        /* Reserved */
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Set bit rate (32 bit value) */
        cmdData[4] = (byte) ((pwrBaudRate << 24) >> 24);
        cmdData[5] = (byte) ((pwrBaudRate << 16) >> 24);
        cmdData[6] = (byte) ((pwrBaudRate << 8) >> 24);
        cmdData[7] = (byte) (pwrBaudRate >> 24);
        /* Set IDLE CS value (16 bit value) */
        // low byte
        cmdData[8] = (byte) ((pwrIdleCsVal << 8) >> 8);
        // high byte
        cmdData[9] = (byte) (pwrIdleCsVal >> 8);
        /* Set ACTIVE CSvalue */
        // low byte
        cmdData[10] = (byte) ((pwrActiveCsVal << 8) >> 8);
        // high byte
        cmdData[11] = (byte) (pwrActiveCsVal >> 8);
        /* Set CS to data delay */
        // low byte
        cmdData[12] = (byte) ((pwrCsToDataDly << 8) >> 8);
        //high byte
        cmdData[13] = (byte) (pwrCsToDataDly >> 8);
        /* Set data to CS delay */
        // low byte
        cmdData[14] = (byte) ((pwrDataToCsDly << 8) >> 8);
        // high byte
        cmdData[15] = (byte) (pwrDataToCsDly >> 8);
        /* Set data to data delay */
        // low byte
        cmdData[16] = (byte) ((pwrDataToDataDly << 8) >> 8);
        // high byte
        cmdData[17] = (byte) (pwrDataToDataDly >> 8);
        /* Set SPI transfer size */
        // low byte
        cmdData[18] = (byte) ((pwrTxferSize << 8) >> 8);
        // high byte
        cmdData[19] = (byte) (pwrTxferSize >> 8);
        /* Set SPI mode */
        cmdData[20] = pwrSpiMd;

        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;

        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /*Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else {
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        }

    }

    /**
     * Retrieve the current SPI parameter information.
     *
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful, other = failed
     */
    public final int getCurrentSpiTxferSettings() {

        /*Setup a buffer with command - Retrieve manufacture string */
        byte[] cmdData = new byte[64];
        /* Get current SPI transfer settings. */
        cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_GET;
        /* Reserved */
        cmdData[1] = 0x00;
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }

        /* Retrieve the data sent from the device to the host */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {
            /* Unknown error, return the exact value */
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        } else {
            /* Successful operation. Get needed data and return 0 */
            /* Get bit rate (32 bit value) */
            mBaudRate =
                    ((rxData[7] << 24) & 0xFF000000) + ((rxData[6] << 16) & 0xFF0000)
                            + ((rxData[5] << 8) & 0xFF00) + (rxData[4] & 0xFF);
            /* Get IDLE CS value */
            mIdleCsVal = (rxData[8] & 0xFF) + ((rxData[9] << 8) & 0xFF00);
            /* Get ACTIVE CS value */
            mActiveCsVal = ((rxData[10] & 0xFF) + ((rxData[11] << 8) & 0xFF00));
            /* Get CS to data delay */
            mCsToDataDly = ((rxData[12] & 0xFF) + ((rxData[13] << 8) & 0xFF00));
            /* Get data to CS delay */
            mDataToCsDly = ((rxData[14] & 0xFF) + ((rxData[15] << 8) & 0xFF00));
            /* Get data to data delay */
            mDataToDataDly = ((rxData[16] & 0xFF) + ((rxData[17] << 8) & 0xFF00));
            /* Get SPI transfer size */
            mTxferSize = ((rxData[18] & 0xFF) + ((rxData[19] << 8) & 0xFF00));
            /* Get SPI mode */
            mSpiMd = (byte) (rxData[20] & 0xFF);
            /* Return 0 */
            return Mcp2210Constants.SUCCESSFUL;

        }

    }

    /**
     * Set the current SPI parameters.
     *
     * @param currentBaudRate      (int) [IN] Current SPI bit rate speed
     * @param currentIdleCsVal     (int) [IN] Current IDLE chip select value
     * @param currentActiveCsVal   (int) [IN] Current ACTIVE chip select value
     * @param currentCsToDataDly   (int) [IN] Current chip select to data delay
     * @param currentDataToDataDly (int) [IN] Current delay between subsequent data bytes
     * @param currentDataToCsDly   (int) [IN] Current last data byte to chip select
     * @param currentTxferSize     (int) [IN] Current bytes per SPI transaction
     * @param currentSpiMd         (byte) [IN] Current SPI mode (Possible values:0, 1, 2, or 3)
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful, other = failed
     */
    public final int setCurrentSpiTxferSettings(final int currentBaudRate, final int currentIdleCsVal,
                                                final int currentActiveCsVal, final int currentCsToDataDly,
                                                final int currentDataToDataDly, final int currentDataToCsDly,
                                                final int currentTxferSize, final byte currentSpiMd) {

        /* Setup a buffer with command */
        byte[] cmdData = new byte[64];
        cmdData[Mcp2210Constants.PKT_INDX_CMD] = Mcp2210Constants.CMD_SPI_TXFER_SETTINGS_SET;
        /* Reserved */
        cmdData[1] = 0x00;
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Set bit rate (32 bit value) */
        cmdData[4] = (byte) ((currentBaudRate << 24) >> 24);
        cmdData[5] = (byte) ((currentBaudRate << 16) >> 24);
        cmdData[6] = (byte) ((currentBaudRate << 8) >> 24);
        cmdData[7] = (byte) (currentBaudRate >> 24);
        /* Set IDLE CS value (16 bit value) */
        // low byte
        cmdData[8] = (byte) ((currentIdleCsVal << 8) >> 8);
        // high byte
        cmdData[9] = (byte) (currentIdleCsVal >> 8);
        /* Set ACTIVE CSvalue */
        // low byte
        cmdData[10] = (byte) ((currentActiveCsVal << 8) >> 8);
        // high byte
        cmdData[11] = (byte) (currentActiveCsVal >> 8);
        /* Set CS to data delay */
        // low byte
        cmdData[12] = (byte) ((currentCsToDataDly << 8) >> 8);
        // high byte
        cmdData[13] = (byte) (currentCsToDataDly >> 8);
        /* Set data to CS delay */
        // low byte
        cmdData[14] = (byte) ((currentDataToCsDly << 8) >> 8);
        // high byte
        cmdData[15] = (byte) (currentDataToCsDly >> 8);
        /* Set data to data delay */
        // low byte
        cmdData[16] = (byte) ((currentDataToDataDly << 8) >> 8);
        // high byte
        cmdData[17] = (byte) (currentDataToDataDly >> 8);
        /* Set SPI transfer size */
        // low byte
        cmdData[18] = (byte) ((currentTxferSize << 8) >> 8);
        // high byte
        cmdData[19] = (byte) (currentTxferSize >> 8);
        /* Set SPI mode */
        cmdData[20] = currentSpiMd;

        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else {
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        }
    }

    /**
     * Retrieve the key power-up USB parameter information (VID, PID, power option, requested
     * current).
     *
     * @param gpioPinDesInit            (byte[]) Initial PIN designations for the GPIO pins
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful, other = failed
     */
    public final int getPwrUpChipSettings(byte[] gpioPinDesInit) {
        /* Setup a buffer with command - Retrieve manufacture string */
        byte[] cmdData = new byte[64];
        /* Get NVRAM */
        cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
        /* Get chip power-up settings */
        cmdData[1] = Mcp2210Constants.NVRAM_CHIP_SETTINGS;
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host */
        byte[] rxData = new byte[65];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.RXPKT_INDX_SUB_CMD]
                != cmdData[Mcp2210Constants.CMDPKT_INDX_SUB_CMD]) {

            /*Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_SUBCMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {

            /* Unknown error, return the exact value */
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        } else {
            /* Successful operation. Get needed data and return 0 */

            /* Get the GP pin designations */
            System.arraycopy(rxData, 4, gpioPinDesInit, 0, 9);
            /* Get GPIO default output */
            mGpioDefaultOutput = (rxData[13] & 0xff) + (rxData[14] << 8);
            /* Get GPIO default direction */
            mGpioDefaultDir = (rxData[15] & 0xff) + (rxData[16] << 8);
            /* Get other chip settings */
            mRmtWkupEn = (byte) (rxData[17] & Mcp2210Constants.REMOTE_WAKEUP_MASK);
            /* Isolate bit 4 */
            mInterruptPinMd = (byte) (rxData[17] & Mcp2210Constants.INTERRUPT_PIN_MD_MASK);
            /* Isolate bits 1,2, and 3 */
            mSpiBusRelEnable = (byte) (rxData[17] & Mcp2210Constants.SPI_BUS_RELEASE_MASK);
            /* Isolate bit 0 */
            mCurNvramAccessSetting = rxData[18];
            /* Return 0 */
            return Mcp2210Constants.SUCCESSFUL;
        }
    }

    /**
     * @param pwrGpioPinDes            (byte[]) Power-up PIN designations for the GPIO pins
     * @param pwrDefaultGpioOutput     (int) Power-up default GPIO output
     * @param pwrDefaultGpioDir        (int) Power-up default GPIO direction
     * @param pwrCurNvramAccessSetting (byte) Power-up access control settings for the chip
     * @param pwrSpiBusRelEnable       (byte) Power-up SPI Bus release enable
     * @param pwrRmtWkupEn             (byte) Power-up remote wake-up enable
     * @param pwrInterruptPinMd        (byte) Power-up interrupt mode
     * @param password                 (CharBuffer) Password to set for the device. If no password wanted, use 'null' as
     *                                 input.
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful. other = failed
     */
    public final int setPwrUpChipSettings(final byte[] pwrGpioPinDes, final int pwrDefaultGpioOutput,
                                          final int pwrDefaultGpioDir, final byte pwrCurNvramAccessSetting,
                                          final byte pwrSpiBusRelEnable, final byte pwrRmtWkupEn,
                                          final byte pwrInterruptPinMd, final CharBuffer password) {

        /* Setup a buffer with command */
        byte[] cmdData = new byte[64];
        cmdData[Mcp2210Constants.PKT_INDX_CMD] = Mcp2210Constants.CMD_NVRAM_PARAM_SET;
        cmdData[Mcp2210Constants.CMDPKT_INDX_SUB_CMD] = Mcp2210Constants.NVRAM_CHIP_SETTINGS;
        /* Reserved */
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Set the GP pin designations */
        System.arraycopy(pwrGpioPinDes, 0, cmdData, 4, 9);
        /* Set GPIO default output */
        // low byte
        cmdData[13] = (byte) ((pwrDefaultGpioOutput << 8) >> 8);
        // high byte
        cmdData[14] = (byte) (pwrDefaultGpioOutput >> 8);
        /* Set GPIO default direction */
        // low byte
        cmdData[15] = (byte) ((pwrDefaultGpioDir << 8) >> 8);
        // high byte
        cmdData[16] = (byte) (pwrDefaultGpioDir >> 8);
        /* Set other chip settings */
        cmdData[17] = (byte) (pwrRmtWkupEn | pwrInterruptPinMd | pwrSpiBusRelEnable);
        cmdData[18] = pwrCurNvramAccessSetting;
        // Set password (if given) -- this will change the password
        if (password == null) {
            /* If NULL, use the password that was used last time (if it was used) */
            for (int i = 0; i < 8; i++) {
                cmdData[19 + i] = 0;
            }
        } else {
            /* If no current password for the chip, send zeros, otherwise send current password value */
            for (int i = 0; i < 8; i++) {
                cmdData[19 + i] = (byte) password.get(i);
            }
        }
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else {
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        }

    }

    /**
     * Retrieve the current chip settings.
     *
     * @param initialGpioPinDes            (byte[]) [OUT] Current PIN designations for the GPIO pins
     * @return (int) Error code.Indicates if the operation was successful or not.
     * 0 = successful. other = failed
     */
    public final int getCurrentChipSettings(byte[] initialGpioPinDes) {

        /* Setup a buffer with command - Retrieve manufacture string */
        byte[] cmdData = new byte[64];
        /* Get current settings command */
        cmdData[0] = Mcp2210Constants.CMD_SETTINGS_READ;
        /* Reserved */
        cmdData[1] = 0x00;
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }

        /* Retrieve the data sent from the device to the host */
        byte[] rxData = new byte[65];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {

            /* Unknown error, return the exact value . */
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        } else {
            /* Successful operation. Get needed data and return 0 */
            /* Get the GP pin designations */
            System.arraycopy(rxData, 4, initialGpioPinDes, 0, 9);
            /* Get GPIO default output */
            mGpioDefaultOutput = ((rxData[13] & 0xFF) + ((rxData[14] << 8) & 0xFF00));
            /* Get GPIO default direction */
            mGpioDefaultDir = ((rxData[15] & 0xFF) + ((rxData[16] << 8) & 0xFF00));
            /* Get other chip settings */
            mRmtWkupEn = (byte) (rxData[17] & Mcp2210Constants.REMOTE_WAKEUP_MASK);
            /* Isolate bit 4 */
            mInterruptPinMd = (byte) (rxData[17] & Mcp2210Constants.INTERRUPT_PIN_MD_MASK);
            /* Isolate bits 1, 2, and 3 */
            mSpiBusRelEnable = (byte) (rxData[17] & Mcp2210Constants.SPI_BUS_RELEASE_MASK);
            /* Isolate bit 0 */
            mCurNvramAccessSetting = rxData[18];
            return Mcp2210Constants.SUCCESSFUL;
        }

    }

    /**
     * Set the current chip settings.
     *
     * @param initialGpioPinDes            (byte[]) [IN] PIN designations for the GPIO pins
     * @param initialDefaultGpioOutput     (int) [IN] Default GPIO output
     * @param initialDefaultGpioDir        (int) [IN] Default GPIO direction
     * @param initialSpiBusRelEnable       (byte) [IN] SPI Bus release enable
     * @param initialRmtWkupEn             (byte) [IN] Remote wake-up enable
     * @param initialInterruptPinMd        (byte) [IN] Interrupt mode
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful. other = failed
     */
    public final int setCurrentChipSettings(final byte[] initialGpioPinDes,
                                            final int initialDefaultGpioOutput, final int initialDefaultGpioDir,
                                            final byte initialSpiBusRelEnable,
                                            final byte initialRmtWkupEn, final byte initialInterruptPinMd) {

        /* Setup a buffer with command */
        byte[] cmdData = new byte[64];
        cmdData[Mcp2210Constants.PKT_INDX_CMD] = Mcp2210Constants.CMD_SETTINGS_WRITE;
        /* Write current settings command */
        /* Reserved */
        cmdData[1] = 0x00;
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Set the GPIO pin designations */
        System.arraycopy(initialGpioPinDes, 0, cmdData, 4, 9);
        /* Set GPIO default output */
        // low byte
        cmdData[13] = (byte) ((initialDefaultGpioOutput << 8) >> 8);
        // high byte
        cmdData[14] = (byte) (initialDefaultGpioOutput >> 8);
        /* Set GPIO default direction */
        // low byte
        cmdData[15] = (byte) ((initialDefaultGpioDir << 8) >> 8);
        // high byte
        cmdData[16] = (byte) (initialDefaultGpioDir >> 8);
        /* Set other chip settings */
        cmdData[17] = (byte) (initialRmtWkupEn | initialInterruptPinMd | initialSpiBusRelEnable);
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }

        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

            /* Ensure command byte was echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else {
            return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
        }

    }

    /**
     * Get GPIO configuration default output.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) If less than zero, there was an error. Otherwise this value is the default GPIO
     * output value. More info: Mapping(MSB to LSB - only lower 9 bits used): GP8VAL GP7VAL
     * GP6VAL GP5VAL GP4VAL GP3VAL GP2VAl GP1VAl GP0VAL
     */

    public final int getDefaultGpioOutput(final ExchangeType whichToGet) {
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int initialDefaultGpioOutput;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current settings command */
            cmdData[0] = Mcp2210Constants.CMD_SETTINGS_READ;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {

                /* Unknown error, return the exact value . */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get GPIO default output */
                initialDefaultGpioOutput = ((rxData[13] & 0xFF) + ((rxData[14] << 8) & 0xFF00));
                return initialDefaultGpioOutput;
            }

        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrDefaultGpioOutput;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get chip power-up settings */
            cmdData[1] = Mcp2210Constants.NVRAM_CHIP_SETTINGS;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.RXPKT_INDX_SUB_CMD]
                    != cmdData[Mcp2210Constants.CMDPKT_INDX_SUB_CMD]) {

                /*Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_SUBCMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {

                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get GPIO default output */
                pwrDefaultGpioOutput = (rxData[13] & 0xff) + (rxData[14] << 8);
                return pwrDefaultGpioOutput;
            }

        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    }

    /**
     * Get GPIO configuration default direction.
     *
     * @param whichToGet (int) [IN] Use constants defined in Mcp2210Constants class:
     *                   CURRENT_SETTINGS_ONLY = 0;
     *                   PWRUP_DEFAULTS_ONLY = 1.
     * @return (int) If less than zero, there was an error. Otherwise this value is the dfltGpioDir
     * value. More info: Default GPIO direction, where 0 = output and 1 = input Mapping(MSB
     * to LSB - only lower 9 bits used): GP8VAL GP7VAL GP6VAL GP5VAL GP4VAL GP3VAL GP2VAL
     * GP1VAL GP0VAL
     */

    public final int getDefaultGpioDirection(final ExchangeType whichToGet) {

        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            int currentDefaultGpioDir;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get current settings command */
            cmdData[0] = Mcp2210Constants.CMD_SETTINGS_READ;
            /* Reserved */
            cmdData[1] = 0x00;
            cmdData[2] = 0x00;
            cmdData[3] = 0x00;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }

            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {

                /* Unknown error, return the exact value . */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get GPIO default direction */
                currentDefaultGpioDir = ((rxData[15] & 0xFF) + ((rxData[16] << 8) & 0xFF00));
                return currentDefaultGpioDir;
            }

        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            int pwrDefaultGpioDir;
            /* Setup a buffer with command - Retrieve manufacture string */
            byte[] cmdData = new byte[64];
            /* Get NVRAM */
            cmdData[0] = Mcp2210Constants.CMD_NVRAM_PARAM_GET;
            /* Get chip power-up settings */
            cmdData[1] = Mcp2210Constants.NVRAM_CHIP_SETTINGS;
            /* Write the command to the device */
            boolean writeResult = false;
            boolean readResult = false;
            ByteBuffer command = ByteBuffer.wrap(cmdData);
            ByteBuffer response = mcpConnection.sendData(command);
            if (response != null) {
                writeResult = true;
                readResult = true;
            }
            /* Check for error */
            if (!writeResult) {
                return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
            }
            /* Retrieve the data sent from the device to the host */
            byte[] rxData = new byte[65];
            for (int i = 0; i < response.capacity(); i++) {
                rxData[i] = response.get(i);
            }
            /* Check for error */
            if (!readResult) {
                return Mcp2210Constants.ERROR_DEV_READ_FAILED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {

                /* Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.RXPKT_INDX_SUB_CMD]
                    != cmdData[Mcp2210Constants.CMDPKT_INDX_SUB_CMD]) {

                /*Ensure command byte was echoed back. */
                return Mcp2210Constants.ERROR_SUBCMD_NOT_ECHOED;
            } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                    != Mcp2210Constants.SUCCESSFUL) {

                /* Unknown error, return the exact value */
                return interpretErrorCode(rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]);
            } else {
                /* Successful operation. Get needed data and return 0 */
                /* Get GPIO default direction */
                pwrDefaultGpioDir = (rxData[15] & 0xff) + (rxData[16] << 8);
                return pwrDefaultGpioDir;
            }

        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }

    }

    /**
     * Get GP Pin designations.
     *
     * @param whichToGet (int)  - Use static constants defined in Mcp2210Constants class.
     *                   BOTH cannot be used in any get... functions.
     *                   Use one of the following:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1
     * @param gpPinDes   (ByteBuffer)   - Array of 9 elements specifying each GP pin designation
     * @return (int) - If the output ByteBuffer is NULL, then an error occurred .
     * Otherwise it was successful
     * <p>
     * Notes:
     * Since the output of this function is through the ByteBuffer given as a parameter,
     * contents of this array does not need to be assigned to anything
     */
    public final int getGpPinDesignations(final ExchangeType whichToGet, final ByteBuffer gpPinDes) {

        int result;
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            result = getCurrentChipSettings(mGpPinDes);
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            result = getPwrUpChipSettings(mGpPinDes);
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        if (result != Mcp2210Constants.SUCCESSFUL) {
            interpretErrorCode((byte) result);
        }
        /* Assign the current value of the GPIO pin designation to the output array */
        for (int i = 0; i < 9; i++) {
            /* GP pin designation */
            gpPinDes.put(i, mGpPinDes[i]);
        }
        return Mcp2210Constants.SUCCESSFUL;
    }


    /**
     * Get interrupt pin mode settings.
     *
     * @param whichToGet (int)  -  Use static constants defined in Mcp2210Constants class.
     *                   Cannot use BOTH in any get... functions.
     *                   Use one of the following:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1
     * @return (int)  -  Dedicated Pin function (interrupt pin mode).
     * If the return value is negative, the operation failed.
     * See below for different modes:
     * 4 - count high pulses
     * 3 - count low pulses
     * 2 - count rising edges
     * 1 - count falling edges
     * 0 - no interrupt counting
     */
    public final int getInterruptPinMode(final ExchangeType whichToGet) {
        int result;
        /* Verify input parameter is correct and get appropriate settings */
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            result = getCurrentChipSettings(mGpPinDes);
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            result = getPwrUpChipSettings(mGpPinDes);
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Check error code */
        if (result != Mcp2210Constants.SUCCESSFUL) {
            interpretErrorCode((byte) result);
        }
        return (mInterruptPinMd >> 1);
    }


    /**
     * Get the enable state of the remote wake-up setting.
     *
     * @param whichToGet (int) - Use static constants defined in Mcp2210Constants class.
     *                   Cannot use BOTH in any get... functions.
     *                   Use one of the following:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1
     * @return (int) - The status of the remote wake-up enable(1)/disable(0).
     * If negative, this is an error code.
     */
    public final int getRmtWkupEnStatus(final ExchangeType whichToGet) {
        int result;
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            result = getCurrentChipSettings(mGpPinDes);
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            result = getPwrUpChipSettings(mGpPinDes);
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Check return code */
        if (result != Mcp2210Constants.SUCCESSFUL) {
            interpretErrorCode((byte) result);
        }
        /* Return appropriate value */
        if (mRmtWkupEn == Mcp2210Constants.REMOTE_WAKEUP_ENABLED) {
            /* Remote wake-up */
            return Mcp2210Constants.TRUE;
        } else if (mRmtWkupEn == Mcp2210Constants.REMOTE_WAKEUP_DISABLED) {
            return Mcp2210Constants.FALSE;
        } else {
            return Mcp2210Constants.ERROR_USB_INVALID_PWR_VALUE;
        }
    }


    /**
     * Get the enable state of the SPI bus release setting.
     *
     * @param whichToGet (int) - Use static constants defined in Mcp2210Constants class.
     *                   Cannot use BOTH in any get... functions.
     *                   Use one of the following:
     *                   CURRENT_SETTINGS_ONLY = 0,
     *                   PWRUP_DEFAULTS_ONLY = 1
     * @return (int) - Returns the status of the remote wake-up enable(1)/disable(0).
     * If not these values, it is an error code.
     */
    public final int getSpiBusReleaseEnStatus(final ExchangeType whichToGet) {
        int result;
        /* Verify input parameter is correct and get appropriate settings */
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            result = getCurrentChipSettings(mGpPinDes);
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            result = getPwrUpChipSettings(mGpPinDes);
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Check error code */
        if (result != Mcp2210Constants.SUCCESSFUL) {
            return interpretErrorCode((byte) result);
        } else {
            return mSpiBusRelEnable;
        }
    }

    /**
     * Get GPIO configuration settings.
     *
     * @param whichToGet     (int) Use constants defined in Mcp2210Constants class.
     *                       Cannot use BOTH in any get... functions.
     *                       Use one of the following:
     *                       CURRENT_SETTINGS_ONLY = 0,
     *                       PWRUP_DEFAULTS_ONLY = 1
     * @param gpPinDes       (ByteBuffer) Array of 9 elements specifying each GPIO pin designation (0 = GPIO, 1
     *                       = Chip-selects, 2 = Dedicated pin function)
     * @param dfltGpioOutput (IntBuffer) Default GPIO output. Mapping(MSB to LSB - only lower 9 bits used):
     *                       GP8VAL GP7VAL GP6VAL GP5VAL GP4VAL GP3VAL GP2VAL GP1VAL GP0VA
     * @param dfltGpioDir    (IntBuffer) Default GPIO direction, where 0 = output and 1 = input Mapping(MSB to
     *                       LSB - only lower 9 bits used): GP8VAL GP7VAL GP6VAL GP5VAL GP4VAL GP3VAL GP2VAL
     *                       GP1VAL GP0VAL
     * @return (int) Error code.Indicates if the operation was successful or not.
     * 0 = successful. Other = failed.
     */

    public final int getGpConfig(final ExchangeType whichToGet, final ByteBuffer gpPinDes,
                                 final IntBuffer dfltGpioOutput, final IntBuffer dfltGpioDir) {
        int result;
        if (whichToGet == ExchangeType.CURRENT_SETTINGS_ONLY) {
            result = getCurrentChipSettings(mGpPinDes);
        } else if (whichToGet == ExchangeType.POWER_UP_DEFAULTS_ONLY) {
            result = getPwrUpChipSettings(mGpPinDes);
        } else {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        if (result != Mcp2210Constants.SUCCESSFUL) {
            interpretErrorCode((byte) result);
        }
        /* No current password protection or the protection has been disabled. */
        dfltGpioOutput.put(mGpioDefaultOutput);
        dfltGpioDir.put(mGpioDefaultDir);
        for (int i = 0; i < 9; i++) {
            /* GPIO pin designation */
            gpPinDes.put(i, mGpPinDes[i]);
        }
        return Mcp2210Constants.SUCCESSFUL;
    }

    /**
     * Allow the user to adjust GPIO configuration settings.
     *
     * @param whichToSet     (int) [IN] Use constants defined in Mcp2210Constants class.
     *                       Use one of the following:
     *                       CURRENT_SETTINGS_ONLY = 0,
     *                       PWRUP_DEFAULTS_ONLY = 1,
     *                       BOTH = 2
     * @param gpPinDes       (ByteBuffer) [IN] Array of 9 elements specifying each GPIO pin designation ( 0 =
     *                       GPIO, 1 = Chip-selects, 2 = Dedicated pin function
     * @param dfltGpioOutput (int) [IN] Default GPIO output. Mapping(MSB to LSB - only lower 9 bits used):
     *                       GP8VAL GP7VAL GP6VAL GP5VAL GP4VAL GP3VAL GP2VAL GP1VAL GP0VAL
     * @param dfltGpioDir    (int) [IN] Default GPIO direction, where 0 = output and 1 = input Mapping(MSB to
     *                       LSB - only lower 9 bits used): GP8VAL GP7VAL GP6VAL GP5VAL GP4VAL GP3VAL GP2VAL
     *                       GP1VAL GP0VAL
     * @return (int) Error code.Indicates if the operation was successful or not.
     * 0 = successful. Other = failed.
     */
    public final int setGpConfig(final ExchangeType whichToSet, final ByteBuffer gpPinDes,
                                 final int dfltGpioOutput, final int dfltGpioDir) {

        /* Ensure parameters are valid values */
        if (whichToSet != ExchangeType.CURRENT_SETTINGS_ONLY
                && whichToSet != ExchangeType.POWER_UP_DEFAULTS_ONLY
                && whichToSet != ExchangeType.BOTH) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
        /* Declare local variables */
        int result;
        /* Based off of whichToSet, determine which settings to grab as the 'base' */
        if (whichToSet == ExchangeType.CURRENT_SETTINGS_ONLY || whichToSet == ExchangeType.BOTH) {
            /* Get the current settings */
            result = getCurrentChipSettings(mGpPinDes);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */

            /* GPIO output defaults */
            mGpioDefaultOutput = dfltGpioOutput;
            /* GPIO direction defaults*/
            mGpioDefaultDir = dfltGpioDir;
            /* GPIO pin designation */
            for (int i = 0; i < 9; i++) {
                mGpPinDes[i] = gpPinDes.get(i);
            }
            /* Set the current settings */
            result =
                    setCurrentChipSettings(mGpPinDes, mGpioDefaultOutput, mGpioDefaultDir,
                            mSpiBusRelEnable, mRmtWkupEn, mInterruptPinMd);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART1 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        if (whichToSet == ExchangeType.POWER_UP_DEFAULTS_ONLY
                || whichToSet == ExchangeType.BOTH) {
            /* Get the power-up defaults */
            result = getPwrUpChipSettings(mGpPinDes);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART1
                            + result;
                } else {
                    return result;
                }
            }
            /* Set the new settings to the local variable */
            /* GPIO output defaults */
            mGpioDefaultOutput = dfltGpioOutput;
            /* GPIO direction defaults */
            mGpioDefaultDir = dfltGpioDir;
            /* GP pin designation */
            for (int i = 0; i < 9; i++) {
                mGpPinDes[i] = gpPinDes.get(i);
            }
            /* Set the power-up defaults */
            result = setPwrUpChipSettings(mGpPinDes, mGpioDefaultOutput, mGpioDefaultDir,
                            mCurNvramAccessSetting, mSpiBusRelEnable, mRmtWkupEn, mInterruptPinMd, null);
            if (result != Mcp2210Constants.SUCCESSFUL) {
                if (DEBUG) {
                    return Mcp2210Constants.ERROR_LOC_PART2 + Mcp2210Constants.ERROR_LOC_SUBPART2
                            + result;
                } else {
                    return result;
                }
            }
        }
        return Mcp2210Constants.SUCCESSFUL;

    }

    /**
     * Further expound on the error code returned from the device.
     *
     * @param errorCode (byte)
     * @return (int) Return code hold a more descriptive error code as determined by the DLL Notes:
     * Some error codes need no further expounding, hence the same value as input to this
     * function will be returned. Other times, there will be several steps taken to ensure
     * the user is given the most descriptive return code possible.
     */
    public final int interpretErrorCode(final byte errorCode) {
        /* This error code could mean device is permanently locked or
         * just password protected. */
        if (errorCode == Mcp2210Constants.SPI_ERR_BLOCKEDACCESS) {// Check to see if device is permanently locked first
            getPwrUpChipSettings(mGpPinDes);

        /* Signed bytes can only have a value in the range -128 to 127. Comparing a signed
           byte with a value outside that range is vacuous and likely to be incorrect.
           To convert a signed byte b to an unsigned value in the range 0..255, use 0xff & b */
            if ((0xff & mCurNvramAccessSetting) == Mcp2210Constants.CHIP_SETTINGS_PERM_LOCKED) {
                return Mcp2210Constants.ERROR_ACCESS_PERM_LOCKED;
            } else if (mCurNvramAccessSetting == Mcp2210Constants.CHIP_SETTINGS_PASS_PROTECTED) {
                /* Since we know a password is required, ensure there are attempts left */
                getMCP2210Status();

                if (mAccessAttemptCnt > 5) {
                    return Mcp2210Constants.ERROR_ACCESS_PASSWD_MECH_BLOCKED;
                } else {
                    /* Password access attempts remain */
                    return Mcp2210Constants.ERROR_ACCESS_PSSWD_ATTEMPT_FAILED;
                }
            } else {
                /* Got into this function by mistake, return 0 */
                return Mcp2210Constants.SUCCESSFUL;
            }
        }
        return errorCode;

    }

    /**
     * Request chip to release SPI bus.
     *
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful. Other = failed.
     */
    public final int getMCP2210Status() {
        /* Setup a buffer to send with command data */
        byte[] cmdData = new byte[64];
        /* Get MCP2210 status */
        cmdData[0] = Mcp2210Constants.CMD_STATUS_GET;
        /* Reserved */
        cmdData[1] = 0x00;
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Ensure command byte eas echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {
            /* Unknown error, return the exact value. */
            return rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE];
        } else {
            /* Successful */
            mSpiBusExtReq = rxData[2];
            mSpiBusOwner = rxData[3];
            mAccessAttemptCnt = rxData[4];
            mAccessGranted = rxData[5];
            mUsbSpiTxOnGoing = rxData[9];
            return Mcp2210Constants.SUCCESSFUL;
        }
    }

    /**
     * Cancel the current SPI transfer.
     *
     * @return (int) Error code. Indicates if the operation was successful or not.
     * 0 = successful. Other = failed.
     */
    public final int cancelSpiTxfer() {
        /* Setup a buffer to send with command data */
        byte[] cmdData = new byte[64];
        /* Get GPIO value command */
        cmdData[0] = Mcp2210Constants.CMD_SPI_TXFER_CANCEL;
        /* Reserved */
        cmdData[1] = 0x00;
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.allocate(64);
        for (byte cmdDatum : cmdData) {
            command.put(cmdDatum);
        }
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Error: Read failed */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Error: command byte wasn't echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {
            /* Unknown error, return the exact value. */
            return rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE];
        } else {
            /* Successful -- return value of GPIO direction */
            return Mcp2210Constants.SUCCESSFUL;
        }
    }

    /**
     * Transfer the specified SPI data.
     *
     * @param spiDataTx (ByteBuffer) [IN] Array of data to send to device
     * @param spiDataRx (ByteBuffer) [OUT] Array that will contain the SPI data received from device.
     * @return (int) Returns value: negative = error ; positive = success (in one of 3 SPI states)
     * Notes: The three possible success states are the follows: -SPI Transfer Done: 0x10
     * -SPI Transfer Started: 0x20 -Spi Transfer Pending: 0x30
     */

    public final int txferSpiDataBuf(final ByteBuffer spiDataTx, final ByteBuffer spiDataRx) {
        /* Ensure that array is valid */
        if (spiDataTx == null) {
            return Mcp2210Constants.ERROR_INVALID_PARAM_GIVEN_1;
        }
    /* If a partial transfer is needed (a transfer with size of <
                                    SPI_BRIDGE_MAX_DATA_PACKET), this will tell its size */
        int partialTxferSize;
        /* Holds operation result */
        int result;

        byte[] tx = new byte[Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET];
        byte[] rx = new byte[Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET];
        /* Get the transfer size for the SPI transfer */
        /* Get current setting for transfer size */
    /* Temporary variable to hold transfer size of the next transfer
                             operation */
        int txferSize = getSpiTxferSize(ExchangeType.CURRENT_SETTINGS_ONLY);
        if (txferSize < 0) {
      /* An error occured, return the error code (contained within txferSize
                               value) */
            return txferSize;
        }
    /* Determine number of individual SPI transfers to be completed
       Number of full transfers to be completed (max size given by
                                 cSPI_BRIDGE_MAX_DATA_PACKET constant) */
        int numFullTxfers = txferSize / Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET;
        if (txferSize % Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET == 0) {
            partialTxferSize = 0;
        } else {
            partialTxferSize = txferSize % Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET;
            /* Get the remaining number of bytes in last transfer. */
        }
        /* keep track of location in data array passed to this function */
        int idx = 0;
        for (; idx < numFullTxfers; ++idx) {
            /* Copy in array data to parameter */
            for (int i = 0; i < Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET; i++) {
                spiDataTx.put((Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET * idx) + i, tx[i]);
            }
            result = makeSpiTxfer(tx, Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET, rx);
            do {
        /* Wait a moment and then get the status of the chip to check if device is ready to
                 continue. If not, repeat */
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /* Send "dummy" packet to ping device for status */
                result = makeSpiTxfer(tx, 0, rx);
            } while ((result != Mcp2210Constants.SPI_STATE_TXFER_DONE)
                    && (result != Mcp2210Constants.SPI_STATE_TXFER_PENDING));
            /*Copy received data */
            for (int i = 0; i < Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET; i++) {
                spiDataRx.put((Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET * idx) + i, rx[i]);
            }
        }
    /* If there is a partial transfer remaining (<SPI_BRIDGE_MAX_DATA_PACKET bytes remaining),
       do it now. */
        if (partialTxferSize > 0) {
            /* Copy in array data to parameter */
            for (int i = 0; i < Math.min(spiDataTx.limit(), partialTxferSize); i++) {
                tx[i] = spiDataTx.get((Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET * idx) + i);
            }
            result = makeSpiTxfer(tx, partialTxferSize, rx);
            do {
        /* Wait a moment and then get the status of the chip to check if device is ready to
           continue. If not, repeat */
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /* Send "dummy" packet to ping device for status */
                result = makeSpiTxfer(tx, 0, rx);
            } while ((result != Mcp2210Constants.SPI_STATE_TXFER_DONE)
                    && (result != Mcp2210Constants.SPI_STATE_TXFER_PENDING));
            /* Copy received data */
            for (int i = 0; i < Math.min(spiDataRx.limit(), partialTxferSize); i++) {
                spiDataRx.put((Mcp2210Constants.SPI_BRIDGE_MAX_DATA_PACKET * idx) + i, rx[i]);
            }
        }
        /* If this point is reached, was successful */
        return Mcp2210Constants.SUCCESSFUL;
    }

    /**
     * Transfer the specified SPI data.
     *
     * @param dataTx (byte[]) [IN] Array of data to send to device.
     * @param dataRx (byte[]) [OUT] Array that will contain the SPI data received from device.
     * @return (int) Returns value: negative = error ; positive = success (in one of 3 SPI states)
     * Notes: The three possible success states are the follows: -SPI Transfer Done: 0x10
     * -SPI Transfer Started: 0x20 -Spi Transfer Pending: 0x30
     */
    public final int txferSpiData(final byte[] dataTx, byte[] dataRx) {
        int numBytesRx = dataRx.length;
        ByteBuffer umByteArrayTx = ByteBuffer.wrap(dataTx);
        ByteBuffer umByteArrayRx = ByteBuffer.allocate(numBytesRx);
        //Call the function
        int result = txferSpiDataBuf(umByteArrayTx, umByteArrayRx);
        //Copy output data into managed array
        for (int i = 0; i < numBytesRx; ++i) {
            dataRx[i] = umByteArrayRx.get(i);
        }
        return result;
    }


    // /////////// FUNCTIONS FOR VALIDATION OF THE PART ONLY -- OTHERWISE THESE ARE TO BE PRIVATE
    // /////////////////////

    /**
     * Set the current chip settings.
     *
     * @param spiDataTx (byte[]) [IN] Array of data to send to device
     * @param numBytes  (int) [IN] Number of bytes to write (can be up to 60)
     * @param spiDataRx (byte[]) [OUT] Array of data to be received from the device
     * @return (int) Returns value: negative = error; positive = success (in one of 3 SPI states)
     * Notes: The three possible success states are as follows: -SPI Transfer Done 0x10 -SPI
     * Transfer Started 0x20 -SPI Transfer Pending 0x30
     */
    private int makeSpiTxfer(final byte[] spiDataTx, final int numBytes,
                             final byte[] spiDataRx) {

        /* Setup a buffer with command */
        byte[] cmdData = new byte[64];
        /* Write SPI data */
        cmdData[Mcp2210Constants.PKT_INDX_CMD] = Mcp2210Constants.CMD_SPI_TXFER_DATA;
        /* Number of bytes to send in SPI transaction */
        cmdData[1] = (byte) numBytes;
        /* Reserved */
        cmdData[2] = 0x00;
        cmdData[3] = 0x00;
        /* Set the SPI data in the cmd packet */
        if (numBytes >= 0) {
            System.arraycopy(spiDataTx, 0, cmdData, 4, numBytes);
        }
        /* Write the command to the device */
        boolean writeResult = false;
        boolean readResult = false;
        ByteBuffer command = ByteBuffer.wrap(cmdData);
        ByteBuffer response = mcpConnection.sendData(command);
        if (response != null) {
            writeResult = true;
            readResult = true;
        }
        /* Check for error */
        if (!writeResult) {
            return Mcp2210Constants.ERROR_DEV_WRITE_FAILED;
        }
        /* Retrieve the data sent from the device to the host -- Error checking */
        byte[] rxData = new byte[64];
        for (int i = 0; i < response.capacity(); i++) {
            rxData[i] = response.get(i);
        }
        /* Check for error */
        if (!readResult) {
            return Mcp2210Constants.ERROR_DEV_READ_FAILED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD] != cmdData[Mcp2210Constants.PKT_INDX_CMD]) {
            /* Error: command byte wasn't echoed back. */
            return Mcp2210Constants.ERROR_CMD_NOT_ECHOED;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                == Mcp2210Constants.SPI_ERR_TXFER_IN_PROG) {
            /* Error: SPI transfer in progress */
            return Mcp2210Constants.ERROR_SPI_TXFER_IN_PROGRESS;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                == Mcp2210Constants.SPI_ERR_SPI_BUS_NOT_AVAIL) {
            /* Error: SPI bus not available */
            return Mcp2210Constants.ERROR_SPI_BUS_NOT_AVAILABLE;
        } else if (rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE]
                != Mcp2210Constants.SUCCESSFUL) {
            /* Unknown error, return the exact value. */
            return rxData[Mcp2210Constants.PKT_INDX_CMD_RET_CODE];
        } else {
            int spiState;
            /* There are 3 possible states */
            if (rxData[Mcp2210Constants.PKT_INDX_SPI_BUS_STATUS] == Mcp2210Constants.SPI_OPS_XFER_DONE) {
                spiState = Mcp2210Constants.SPI_STATE_TXFER_DONE;
            } else if (rxData[Mcp2210Constants.PKT_INDX_SPI_BUS_STATUS]
                    == Mcp2210Constants.SPI_OPS_XFER_START) {
                spiState = Mcp2210Constants.SPI_STATE_TXFER_START;
            } else if (rxData[Mcp2210Constants.PKT_INDX_SPI_BUS_STATUS]
                    == Mcp2210Constants.SPI_OPS_XFER_PENDING) {
                spiState = Mcp2210Constants.SPI_STATE_TXFER_PENDING;
            } else {
                /* Error if this is reached */
                return Mcp2210Constants.SPI_STATE_INVALID;
            }
            /* Save the SPI received data in spiDataRx array */
            /* Offset of -4 must be added for the difference in array lengths */
            System.arraycopy(rxData, 4, spiDataRx, 0, 60);
            /* Return the spi state in the return code */
            return spiState;
        }
    }
}
