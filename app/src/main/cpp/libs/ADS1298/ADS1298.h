#ifndef ADS1298_H_
#define ADS1298_H_

#include <Arduino.h>
#include "MobileECG.h"

#define WREG 0x40
#define RREG 0x20
#define N2 16777216
	
namespace ADS1298 {

	union DeviceId_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t model : 3;
			uint8_t reserved : 2;
			uint8_t family : 3;
		} bits;
	};
	
	union Config1_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t dr : 3;
			uint8_t reserved : 2;
			uint8_t clk_en : 1;
			uint8_t daisy_en : 1;
			uint8_t hr : 1;
		} bits;
	};
	
	union Config2_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t test_freq : 2;
			uint8_t test_amp : 1;
			uint8_t reserved2 : 1;
			uint8_t int_test : 1;
			uint8_t wct_chop : 1;
			uint8_t reserved : 2;
		} bits;
	};
	
	union Config3_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t rld_stat : 1;
			uint8_t rld_loff_sens : 1;
			uint8_t pd_rld : 1;
			uint8_t rldref_int : 1;
			uint8_t rld_meas : 1;
			uint8_t vref_4v : 1;
			uint8_t reserved : 1;
			uint8_t pd_refbuf : 1;			
		} bits;
	};
	
	union Loff_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t flead_off : 2;
			uint8_t ilead_off : 2;
			uint8_t vlead_off_en : 1;
			uint8_t comp_th : 3;
		} bits;
	};
	
	union ChNSet_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t mux : 3;
			uint8_t reserved : 1;
			uint8_t gain : 3;
			uint8_t pd : 1;
		} bits;
	};
	
	union ChannelSetting_u
	{
		uint8_t all;

		struct BitField_s
		{
			uint8_t c1 : 1;
			uint8_t c2 : 1;
			uint8_t c3 : 1;
			uint8_t c4 : 1;
			uint8_t c5 : 1;
			uint8_t c6 : 1;
			uint8_t c7 : 1;
			uint8_t c8 : 1;
		} bits;
	};
	
	enum SPICommands_e {
		// system commands
		WAKEUP = 0x02,  // Wake-up from standby mode
		STANDBY = 0x04, // Enter Standby mode
		RESET = 0x06,   // Reset the device registers to default
		START = 0x08,   // Start and restart (synchronize) conversions
		STOP = 0x0a,    // Stop conversion

		// read commands
		RDATAC = 0x10, // Enable Read Data Continuous mode (default mode at power-up)
		SDATAC = 0x11, // Stop Read Data Continuous mode
		RDATA = 0x12   // Read data by command; supports multiple read back
	};
	
	enum Registers_e
	{
		// device settings
		ID = 0x00,

		// global settings
		CONFIG1 = 0x01,
		CONFIG2 = 0x02,
		CONFIG3 = 0x03,

		// channel specific settings
		LOFF = 0x04,
		CH1SET = LOFF + 1,
		CH2SET = LOFF  + 2,
		CH3SET = LOFF + 3,
		CH4SET = LOFF + 4,
		CH5SET = LOFF + 5,
		CH6SET = LOFF + 6,
		CH7SET = LOFF + 7,
		CH8SET = LOFF + 8,
		RLD_SENSP = 0x0D,
		RLD_SENSN = 0x0E,
		LOFF_SENSP = 0x0F,
		LOFF_SENSN = 0x10,
		LOFF_FLIP = 0x11,

		// lead off status
		LOFF_STATP = 0x12,
		LOFF_STATN = 0x13,

		// other
		GPIO = 0x14,
		PACE = 0x15,
		RESP = 0x16,
		CONFIG4 = 0x17,
		WCT1 = 0x18,
		WCT2 = 0x19
	};

	class ADS1298 : public virtual MobileECG
	{
	private:
		int32_t ecgData[8];
		int32_t stat;
		//float LSB = 1000 * (2 * 2.5f) / (4 * N2 - 1); // 2^24 = 16777216 N2
		float LSB = 0.0000745058f; // 1000000 * (2 * 2.5f) / (4 * N2 - 1)

	public:
		ADS1298(uint8_t csPin, uint8_t dataReadyPin);

		void writeRegister(const Registers_e address, uint8_t data);
		
		void writeCommand(uint8_t data) const;

		uint8_t readRegister(const Registers_e address) const;
	
		virtual void begin() const override;
		
		virtual uint8_t readRegister(const uint8_t address) const;
		
		virtual int32_t* readECG();

		virtual bool readSensorID() const;

		virtual void printConfigs() const;
		
		virtual const char* getName() const;
		
		virtual uint8_t getChannels() const;
		
		virtual int32_t getMinValue() const;
		
		virtual int32_t getMaxValue() const;
	};
}

#endif