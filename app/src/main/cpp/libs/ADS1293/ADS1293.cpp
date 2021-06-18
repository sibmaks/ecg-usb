#include "ADS1293.h"
#include <SPI.h>

namespace ADS1293 {
	ADS1293::ADS1293(float vRef, uint32_t adcMax, uint8_t csPin, uint8_t dataReadyPin) : 
		vRef(vRef), adcMax(adcMax), MobileECG(csPin, dataReadyPin) {
	}

	void ADS1293::begin() const {
		MobileECG::begin();

		SPI.begin();
		SPI.setBitOrder(MSBFIRST);
		SPI.setDataMode(SPI_MODE1);
		//SPI.setClockDivider(SPI_CLOCK_DIV2);
		SPI.setClockDivider(SPI_CLOCK_DIV8);
	}

	void ADS1293::writeRegister(const Registers_e address, uint8_t data) const {
	  uint8_t reg = address & WREG;
	  digitalWrite(csPin, LOW);
	  SPI.transfer(reg);
	  SPI.transfer(data);
	  digitalWrite(csPin, HIGH);
	}

	uint8_t ADS1293::readRegister(const uint8_t address) const {
	  uint8_t reg = address | RREG;
	  digitalWrite(csPin, LOW);
	  SPI.transfer(reg);
	  uint8_t data = SPI.transfer(0);
	  digitalWrite(csPin, HIGH);
	  return data;
	}

	uint8_t ADS1293::readRegister(const Registers_e address) const {
	  uint8_t reg = address | RREG;
	  digitalWrite(csPin, LOW);
	  SPI.transfer(reg);
	  uint8_t data = SPI.transfer(0);
	  digitalWrite(csPin, HIGH);
	  return data;
	}

	int32_t* ADS1293::readECG() {
		digitalWrite(csPin, LOW);
		for(uint8_t i = 0; i < 3; i++) {
		  uint8_t reg = (DATA_CH1_ECG_1 + 3 * i) | RREG;
		  SPI.transfer(reg);
		  int32_t adc_out = SPI.transfer(0) << 16;
		  SPI.transfer(reg + 1);
		  adc_out |= SPI.transfer(0) << 8;
		  SPI.transfer(reg + 2);
		  adc_out |= SPI.transfer(0);
		  ecgData[i] = 1000000 * (vRef * ((2.0f * adc_out) / adcMax - 1.0f) / 3.5f);
		}
		digitalWrite(csPin, HIGH);
		return ecgData;
	}

	bool ADS1293::readSensorID() const {
	  uint8_t ID = readRegister(REVID);
	  Serial.println(ID);
	  if(ID != 0xff) {
		return true;
	  } else {
		  return false;
	  }
	}
	
	void ADS1293::printConfigs() const {
	  Serial.print("FLEX_CH1_CN: ");
	  Serial.println(readRegister(FLEX_CH1_CN), HEX);
	  Serial.print("FLEX_CH2_CN: ");
	  Serial.println(readRegister(FLEX_CH2_CN), HEX);
	  Serial.print("FLEX_CH3_CN: ");
	  Serial.println(readRegister(FLEX_CH3_CN), HEX);

	  Serial.print("CMDET_EN: ");
	  Serial.println(readRegister(CMDET_EN), HEX);
	  Serial.print("RLD_CN: ");
	  Serial.println(readRegister(RLD_CN), HEX);

	  Serial.print("WILSON_EN1: ");
	  Serial.println(readRegister(WILSON_EN1), HEX);
	  Serial.print("WILSON_EN2: ");
	  Serial.println(readRegister(WILSON_EN2), HEX);
	  Serial.print("WILSON_EN3: ");
	  Serial.println(readRegister(WILSON_EN3), HEX);
	  Serial.print("WILSON_CN: ");
	  Serial.println(readRegister(WILSON_CN), HEX);

	  Serial.print("OSC_CN: ");
	  Serial.println(readRegister(OSC_CN), HEX);

	  Serial.print("AFE_RES: ");
	  Serial.println(readRegister(AFE_RES), HEX);

	  Serial.print("R1_RATE: ");
	  Serial.println(readRegister(R1_RATE), HEX);

	  Serial.print("AFE_SHDN_CN: ");
	  Serial.println(readRegister(AFE_SHDN_CN), HEX);

	  Serial.print("R2_RATE: ");
	  Serial.println(readRegister(R2_RATE), HEX);

	  Serial.print("R3_RATE_CH1: ");
	  Serial.println(readRegister(R3_RATE_CH1), HEX);
	  Serial.print("R3_RATE_CH2: ");
	  Serial.println(readRegister(R3_RATE_CH2), HEX);
	  Serial.print("R3_RATE_CH3: ");
	  Serial.println(readRegister(R3_RATE_CH3), HEX);

	  Serial.print("DRDYB_SRC: ");
	  Serial.println(readRegister(DRDYB_SRC), HEX);
	  Serial.print("CH_CNFG: ");
	  Serial.println(readRegister(CH_CNFG), HEX);
	  Serial.print("CONFIG: ");
	  Serial.println(readRegister(CONFIG), HEX);
	}
	
	const char* ADS1293::getName() const {
		return "ADS1293";
	}
		
	uint8_t ADS1293::getChannels() const {
		return 3;
	}
		
	int32_t ADS1293::getMinValue() const {
		return -2000000;
	}
		
	int32_t ADS1293::getMaxValue() const {
		return 2000000;
	}
}