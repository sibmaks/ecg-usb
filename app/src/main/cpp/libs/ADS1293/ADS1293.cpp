#include "ADS1293.h"
#include <SPI.h>

namespace ADS1293 {
	ADS1293::ADS1293(int csPin, int dataReadyPin, int ioDelay) : csPin(csPin), dataReadyPin(dataReadyPin), ioDelay(ioDelay) {
	}

	void ADS1293::switchClock(bool on) {
		digitalWrite(csPin, on ? LOW : HIGH);
		delayMicroseconds(ioDelay);
	}
	
	void ADS1293::begin() {
		pinMode(csPin, OUTPUT);
		pinMode(dataReadyPin, INPUT_PULLUP);
		switchClock(false);

		SPI.begin();
		SPI.setBitOrder(MSBFIRST);
		SPI.setDataMode(SPI_MODE1);
		SPI.setClockDivider(SPI_CLOCK_DIV2);
	}
	
	void ADS1293::writeRegister(const Registers_e address, uint8_t data) {
	  uint8_t reg = address & WREG;
	  switchClock(true);
	  SPI.transfer(reg);
	  SPI.transfer(data);
	  switchClock(false);
	}
	
	uint8_t ADS1293::readRegister(const Registers_e address) {
	  uint8_t reg = address | RREG;
	  switchClock(true);
	  SPI.transfer(reg);
	  uint8_t data = SPI.transfer(0);
	  switchClock(false);
	  return data;
	}
	
	bool ADS1293::isDataReady() {
		return !digitalRead(dataReadyPin);
	}
	
	bool ADS1293::readSensorID() {

	  uint8_t ID = 0xff;
	  ID = readRegister(REVID);
	  Serial.println(ID);
	  if(ID != 0xff) {
		return true;
	  } else {
		  return false;
	  }
	}
}