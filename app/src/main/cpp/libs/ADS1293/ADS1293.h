#ifndef ADS1293_H_
#define ADS1293_H_

#include <Arduino.h>
#define WREG 0x7f
#define RREG 0x80

namespace ADS1293 {
	enum Registers_e
	{
		FLEX_CH1_CN = 0x01,
		FLEX_CH2_CN = 0x02,
		FLEX_CH3_CN = 0x03,
		CMDET_EN = 0x0A,
		RLD_CN = 0x0C,
		WILSON_EN1 = 0x0D,
		WILSON_EN2 = 0x0E,
		WILSON_EN3 = 0x0F,
		WILSON_CN = 0x10,
		OSC_CN = 0x12,
		AFE_RES = 0x13,
		AFE_SHDN_CN = 0x14,
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

	class ADS1293
	{
	private:
		int csPin;
		int dataReadyPin;

	public:
		ADS1293(int csPin, int dataReadyPin);

		void begin();

		void writeRegister(const Registers_e address, uint8_t data);

		uint8_t readRegister(const Registers_e address);
		
		uint8_t readRegister(const uint8_t address);

		int32_t readECG(uint8_t channel, uint32_t vRef, uint32_t adcMax);

		bool readSensorID();

		bool isDataReady();
	};
}

#endif