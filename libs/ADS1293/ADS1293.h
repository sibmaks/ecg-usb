#ifndef ADS1293_H_
#define ADS1293_H_

#include <Arduino.h>

#include "MobileECG.h"

#define WREG 0x7f
#define RREG 0x80

namespace ADS1293 {
	
    enum Registers_e {
        FLEX_CH1_CN = 0x01,
            FLEX_CH2_CN = 0x02,
            FLEX_CH3_CN = 0x03,
			FLEX_PACE_CN = 0x04,
            CMDET_EN = 0x0A,
            RLD_CN = 0x0C,
            WILSON_EN1 = 0x0D,
            WILSON_EN2 = 0x0E,
            WILSON_EN3 = 0x0F,
            WILSON_CN = 0x10,
            OSC_CN = 0x12,
            AFE_RES = 0x13,
            AFE_SHDN_CN = 0x14,
			AFE_PACE_CN = 0x17,
            R2_RATE = 0x21,
            R3_RATE_CH1 = 0x22,
            R3_RATE_CH2 = 0x23,
            R3_RATE_CH3 = 0x24,
            R1_RATE = 0x25,
            DRDYB_SRC = 0x27,
            CH_CNFG = 0x2F,
            CONFIG = 0x00,

            DATA_STATUS = 0x30,

            DATA_CH1_ECG_1 = 0x37,
            DATA_CH1_ECG_2 = 0x38,
            DATA_CH1_ECG_3 = 0x39,

            DATA_CH2_ECG_1 = 0x3A,
            DATA_CH2_ECG_2 = 0x3B,
            DATA_CH2_ECG_3 = 0x3C,

            REVID = 0x40
    };

    class ADS1293: public virtual MobileECG {
        private: 
			float vRef;
			uint32_t adcMax;
			float ecgX;
			
			int32_t ecgData[3];
		
			const char* RegisterNames[0x31] = {
				"CONFIG", "FLEX_CH1_CN", "FLEX_CH2_CN", "FLEX_CH3_CN",
				"FLEX_PACE_CN", "FLEX_VBAT_CN", "LOD_CN", "LOD_EN",
				"LOD_CURRENT", "LOD_AC_CN", "CMDET_EN", "CMDET_CN",
				"RLD_CN", "WILSON_EN1", "WILSON_EN2", "WILSON_EN3", 
				"WILSON_CN", "REF_CN", "OSC_CN", "AFE_RES",
				"AFE_SHDN_CN", "AFE_FAULT_CN", NULL, "AFE_PACE_CN",
				"ERROR_LOD", "ERROR_STATUS", "ERROR_RANGE1", 
				"ERROR_RANGE2", "ERROR_RANGE3", "ERROR_SYNC",
				"ERROR_MISC", "DIGO_STRENGTH", NULL, "R2_RATE",
				"R3_RATE_CH1", "R3_RATE_CH2", "R3_RATE_CH3",
				"R1_RATE", "DIS_EFILTER", "DRDYB_SRC",
				"SYNCB_CN", "MASK_DRDYB", "MASK_ERR",
				NULL, NULL, NULL, "ALARM_FILTER",
				"CH_CNFG", "DATA_STATUS"
			};
        public: 
			ADS1293(float vRef, uint32_t adcMax, uint8_t csPin, uint8_t dataReadyPin);

			void writeRegister(const Registers_e address, uint8_t data) const;

			uint8_t readRegister(const uint8_t address) const;

			uint8_t readRegister(const Registers_e address) const;

			virtual void begin() const override;

			virtual int32_t * readECG();

			virtual bool readSensorID() const;

			virtual void printConfigs() const;

			virtual const char * getName() const;

			virtual uint8_t getChannels() const;

			virtual int32_t getMinValue() const;

			virtual int32_t getMaxValue() const;
    };
}

#endif