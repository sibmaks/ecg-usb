package xyz.dma.ecg_usb.microchipusb

interface Mcp2210Constants {
    companion object {
        const val DEV_FLAGS_SPI_EEP = 0x80
        const val DEV_FLAGS_SPI_EEP_MODE_MASK = 0x60
        const val DEV_FLAGS_SPI_EEP_USE_PASS = 0x10

        // SPI miscellaneous codes
        const val SPI_OPS_XFER_DONE = 0x10
        const val SPI_OPS_XFER_START = 0x20
        const val SPI_OPS_XFER_PENDING = 0x30

        // SPI dispatcher specific command  -- directly copied from firmware
        const val SPI_D_IDLE = 0x00
        const val SPI_D_GETSTATUS = 0x10
        const val SPI_D_CANCELCMD = 0x11
        const val SPI_D_READSETTINGS = 0x20
        const val SPI_D_WRITESETTINGS = 0x21
        const val SPI_D_SETGPIO = 0x30
        const val SPI_D_GETGPIO = 0x31
        const val SPI_D_SETGPIODIR = 0x32
        const val SPI_D_GETGPIODIR = 0x33
        const val SPI_D_SETSPITRANSACTION = 0x40
        const val SPI_D_GETSPITRANSACTION = 0x41
        const val SPI_D_XFERSPIDATA = 0x42
        const val SPI_D_READEEPROM = 0x50
        const val SPI_D_WRITEEEPROM = 0x51

        //used for modifying the Admin user settings, USB strings,...
        const val SPI_D_SETNVRAMPARAM = 0x60
        const val SPI_D_GETNVRAMPARAM = 0x61

        // SPI Bridge class error messages
        const val SPI_BRIDGE_NO_ERR = 0x00
        const val SPI_BRIDGE_DEV_MAJOR_ERROR = -1
        const val SPI_BRIDGE_CMD_ERROR = -2
        const val SPI_BRIDGE_WRONG_PARAMS = -3

        //XFER in progress on the bridge; cannot set new parameters
        const val SPI_BRIDGE_BRIDGE_XFER_IP = -4

        // SPI Bridge related constants
        const val SPI_BRIDGE_MAX_DATA_PACKET = 60

        // SPI dispatcher error codes
        const val SPI_ERR_OK = 0x00
        const val SPI_ERR_WRONGPARAMS = -1 //0xFF
        const val SPI_ERR_HWBUSY = -2 //0xFE
        const val SPI_ERR_WRONGPASSWORD = -3 //0xFD
        const val SPI_ERR_NVRAMLOCKED = -4 //0xFC

        /*This constant represents a "general" access blocked message
     * as well as a more specific message (over 5 password tries)*/
        const val SPI_ERR_BLOCKEDACCESS = -5 //0xFB
        const val SPI_ERR_SPISLAVEACCESS = -6 //0xFA
        const val SPI_ERR_CMDNOTSUPPORTED = -7 //0xF9
        const val SPI_ERR_TXFER_IN_PROG = -8 //0xF8
        const val SPI_ERR_SPI_BUS_NOT_AVAIL = -9 //0xF7

        /* Device Error Messages -- start at -1 up to -100*/ // General Error Messages -- start at -100
        const val SUCCESSFUL = 0
        const val ERROR_BOARD_NOT_PRESENT = -101
        const val ERROR_WRONG_DEVICE_ID = -102
        const val ERROR_INACTIVE_DEVICE = -103
        const val ERROR_DEV_WRITE_FAILED = -106
        const val ERROR_DEV_READ_FAILED = -107
        const val ERROR_CMD_NOT_ECHOED = -108
        const val ERROR_SUBCMD_NOT_ECHOED = -109
        const val ERROR_CMD_FAILED = -110

        // DLL Parameter errors - Start at -200
        //Use to indicate first out of x parameters was invalid
        const val ERROR_INVALID_PARAM_GIVEN_1 = -201
        const val ERROR_INVALID_PARAM_GIVEN_2 = -202 //and so on.
        const val ERROR_INVALID_PARAM_GIVEN_3 = -203
        const val ERROR_INVALID_PARAM_GIVEN_4 = -204
        const val ERROR_INVALID_PARAM_GIVEN_5 = -205
        const val ERROR_INVALID_PARAM_GIVEN_6 = -206
        const val ERROR_INVALID_PARAM_GIVEN_7 = -207
        const val ERROR_INVALID_PARAM_GIVEN_8 = -208
        const val ERROR_INVALID_PARAM_GIVEN_9 = -209
        const val ERROR_INVALID_PARAM_GIVEN_10 = -210
        const val ERROR_RESET_PARAM_WRONG = -212

        // USB Errors -- start at -300
        const val ERROR_USB_TXFER_IN_PROGRESS = -300
        const val ERROR_USB_INVALID_PWR_VALUE = -301

        // SPI Errors -- start at -400
        const val ERROR_SPI_TXFER_FINISHED = 0
        const val ERROR_SPI_BUS_NOT_AVAILABLE = -400
        const val ERROR_SPI_DATA_NOT_ACCEPTED = -401
        const val ERROR_SPI_TXFER_WAITING = -402
        const val ERROR_SPI_TXFER_IN_PROGRESS = -403
        const val ERROR_SPI_TXFER_FAILED = -404

        // Password Access -- start at -500
        const val ERROR_ACCESS_GRANTED = 0
        const val ERROR_ACCESS_PASSWD_PROTECTED = -501
        const val ERROR_ACCESS_PERM_LOCKED = -502
        const val ERROR_ACCESS_PSSWD_ATTEMPT_FAILED = -503
        const val ERROR_ACCESS_PASSWD_MECH_BLOCKED = -504

        // EEPROM Errors -- start at -600
        const val ERROR_EEPROM_WRITE_FAILED = -601
        const val ERROR_EEPROM_READ_FAILED = -602
        const val ERROR_EEPROM_ADDR_MISMATCH = -603

        // Supplements to error codes in order to classify which part of code error occurs in
        const val ERROR_LOC_PART1 = -100000
        const val ERROR_LOC_PART2 = -200000
        const val ERROR_LOC_PART3 = -300000
        const val ERROR_LOC_PART4 = -400000
        const val ERROR_LOC_SUBPART1 = -10000
        const val ERROR_LOC_SUBPART2 = -20000
        const val ERROR_LOC_SUBPART3 = -30000
        const val ERROR_LOC_SUBPART4 = -40000

        // Commands for the MCP2210 device
        const val CMD_IDLE = SPI_D_IDLE
        const val CMD_STATUS_GET = SPI_D_GETSTATUS
        const val CMD_SETTINGS_READ = SPI_D_READSETTINGS
        const val CMD_SETTINGS_WRITE = SPI_D_WRITESETTINGS
        const val CMD_GPIO_VAL_SET = SPI_D_SETGPIO
        const val CMD_GPIO_VAL_GET = SPI_D_GETGPIO
        const val CMD_GPIO_DIR_SET = SPI_D_SETGPIODIR
        const val CMD_GPIO_DIR_GET = SPI_D_GETGPIODIR
        const val CMD_SPI_TXFER_SETTINGS_SET = SPI_D_SETSPITRANSACTION
        const val CMD_SPI_TXFER_SETTINGS_GET = SPI_D_GETSPITRANSACTION
        const val CMD_SPI_TXFER_CANCEL = SPI_D_CANCELCMD
        const val CMD_SPI_TXFER_DATA = SPI_D_XFERSPIDATA
        const val CMD_SPI_BUS_REL_REQ = 0x80
        const val CMD_EEPROM_READ = SPI_D_READEEPROM
        const val CMD_EEPROM_WRITE = SPI_D_WRITEEEPROM
        const val CMD_NVRAM_PARAM_SET = SPI_D_SETNVRAMPARAM
        const val CMD_NVRAM_PARAM_GET = SPI_D_GETNVRAMPARAM
        const val CMD_USB_SET_KEY_PARAM = 0x30
        const val CMD_RESET = 0xF0
        const val CMD_INT_EVNT_CNT_GET = 0x12
        const val CMD_ENTER_ACCESS_PASS = 0x70

        // Sub-commands
        const val SUBCMD_RESET_B1 = 0xAB
        const val SUBCMD_RESET_B2 = 0xCD

        // Return packet structure indices
        const val PKT_INDX_CMD = 0
        const val PKT_INDX_CMD_RET_CODE = 1
        const val PKT_INDX_EEPROM_READ_ADDRESS = 2
        const val PKT_INDX_EEPROM_READ_VALUE = 3
        const val PKT_INDX_SPI_BUS_STATUS = 3
        const val RXPKT_INDX_SUB_CMD = 2
        const val CMDPKT_INDX_SUB_CMD = 1

        // NVRAM Data Locations
        const val NVRAM_SPI_TX_SETTINGS = 0x10
        const val NVRAM_CHIP_SETTINGS = 0x20
        const val NVRAM_USB_PARAMETERS = 0x30
        const val NVRAM_PRODUCT_NAME = 0x40
        const val NVRAM_MANUFACTURER = 0x50
        const val NVRAM_SERIAL_NUMBER = 0x60
        const val NVRAM_FW_VERSION = NVRAM_SERIAL_NUMBER

        // Multiple devices support
        const val MAX_DEVICES = 64
        const val MAX_LEN_DEV_PATH_NAME = 256

        // USB Spec constants -- As per USB spec in Ch. 9
        const val USBSPEC_STRING_DESC_ID = 0x03
        const val USBSPEC_PWR_MODE_HOST = 0x80
        const val USBSPEC_PWR_MODE_SELF = 0x40
        const val USBSPEC_PWR_MODE_RMT_WKUP_CPBLE = 0x20

        // Chip Setting Constants
        const val REMOTE_WAKEUP_ENABLED = 0x10
        const val REMOTE_WAKEUP_DISABLED = 0x00
        const val REMOTE_WAKEUP_MASK = 0x10
        const val INTERRUPT_PIN_MD_CNT_HIGH_PULSES = 0x08
        const val INTERRUPT_PIN_MD_CNT_LOW_PULSES = 0x06
        const val INTERRUPT_PIN_MD_CNT_RISING_EDGES = 0x04
        const val INTERRUPT_PIN_MD_CNT_FALLING_EDGES = 0x02
        const val INTERRUPT_PIN_MD_CNT_NONE = 0x00
        const val INTERRUPT_PIN_MD_MASK = 0x0E
        const val SPI_BUS_RELEASE_ENABLED = 0x01
        const val SPI_BUS_RELEASE_DISABLED = 0x00
        const val SPI_BUS_RELEASE_MASK = 0x01
        const val CHIP_SETTINGS_NOT_PROTECTED = 0x00
        const val CHIP_SETTINGS_PASS_PROTECTED = 0x40
        const val CHIP_SETTINGS_PERM_LOCKED = 0x80
        const val CHIP_ACCESS_GRANTED = 0x01
        const val CHIP_ACCESS_LOCKED = 0x00
        const val CHIP_ACCESS_MAX_ATTEMPT_CNT = 0x05

        // SPI bus states for SPI transfers
        const val SPI_STATE_TXFER_DONE = 0x10
        const val SPI_STATE_TXFER_START = 0x20
        const val SPI_STATE_TXFER_PENDING = 0x30
        const val SPI_STATE_INVALID = 0x40

        // Values used for selecting only current settings, power up defaults, or both
        const val SEL_CURRENT_SETTINGS = 0x0
        const val SEL_PWRUP_DEFAULTS = 0x1
        const val SEL_BOTH = 0x2

        // The two constants below are the same - user chooses which is more intuitive
        const val OFF = 0
        const val DISABLED = 0

        // The two constants below are the same - user chooses which is more intuitive
        const val ON = 1
        const val ENABLED = 1

        // Constants to be used for all whichTo(Get/Set) variables in function calls
        const val CURRENT_SETTINGS_ONLY = SEL_CURRENT_SETTINGS
        const val PWRUP_DEFAULTS_ONLY = SEL_PWRUP_DEFAULTS
        const val BOTH = SEL_BOTH
        const val TRUE = 1
        const val FALSE = 0
        const val NO_ERROR = 0
    }
}