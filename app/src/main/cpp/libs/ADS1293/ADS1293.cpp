#include "ADS1293.h"
#include <SPI.h>

namespace ADS1293 {
	ADS1293::ADS1293(int csPin, int dataReadyPin) : csPin(csPin), dataReadyPin(dataReadyPin) {
	}

	void ADS1293::begin() {
		pinMode(csPin, OUTPUT);
		pinMode(dataReadyPin, INPUT_PULLUP);
		digitalWrite(csPin, HIGH);

		SPI.begin();
		SPI.setBitOrder(MSBFIRST);
		SPI.setDataMode(SPI_MODE1);
		SPI.setClockDivider(SPI_CLOCK_DIV2);
	}

	void ADS1293::writeRegister(const Registers_e address, uint8_t data) {
	  uint8_t reg = address & WREG;
	  digitalWrite(csPin, LOW);
	  SPI.transfer(reg);
	  SPI.transfer(data);
	  digitalWrite(csPin, HIGH);
	}

	uint8_t ADS1293::readRegister(const Registers_e address) {
	  uint8_t reg = address | RREG;
	  digitalWrite(csPin, LOW);
	  SPI.transfer(reg);
	  uint8_t data = SPI.transfer(0);
	  digitalWrite(csPin, HIGH);
	  return data;
	}

	int32_t ADS1293::readECG(uint8_t channel, uint32_t vRef, uint32_t adcMax) {
	  uint8_t reg = (DATA_CH1_ECG_1 + 3 * (channel - 1)) | RREG;
	  digitalWrite(csPin, LOW);
	  SPI.transfer(reg);
	  int32_t adc_out = SPI.transfer(0) << 16;
	  SPI.transfer(reg + 1);
	  adc_out |= SPI.transfer(0) << 8;
	  SPI.transfer(reg + 2);
	  adc_out |= SPI.transfer(0);
	  digitalWrite(csPin, HIGH);
	  //return 1000000 * (vRef * (2.0f * adc_out / adcMax - 1.0f) / 3.5f);
	  return 285714 * (vRef * (2.0f * adc_out / adcMax - 1.0f));
	}

	bool ADS1293::readSensorID() {
	  uint8_t ID = readRegister(REVID);
	  Serial.println(ID);
	  if(ID != 0xff) {
		return true;
	  } else {
		  return false;
	  }
	}

	bool ADS1293::isDataReady() {
		return !digitalRead(dataReadyPin);
	}
}