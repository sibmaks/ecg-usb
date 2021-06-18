#ifndef MOBILE_ECG_H_
#define MOBILE_ECG_H_

#include <Arduino.h>

class MobileECG
{
	protected:
		uint8_t csPin;
		uint8_t dataReadyPin;

	public:
		MobileECG(uint8_t csPin, uint8_t dataReadyPin) : csPin(csPin), dataReadyPin(dataReadyPin) {
			
		}

		virtual void begin() const {
			pinMode(csPin, OUTPUT);
			pinMode(dataReadyPin, INPUT_PULLUP);
			digitalWrite(csPin, HIGH);
		}
		
		virtual const char* getName() const = 0;
		
		virtual uint8_t getChannels() const = 0;
		
		virtual int32_t getMinValue() const = 0;
		
		virtual int32_t getMaxValue() const = 0;
				
		virtual uint8_t readRegister(const uint8_t address) const = 0;

		virtual int32_t* readECG() = 0;

		virtual bool readSensorID() const = 0;

		virtual bool isDataReady() const {
			return !digitalRead(dataReadyPin);
		}
		
		virtual void printConfigs() const;
		
		virtual ~MobileECG() {
			
		}
};

#endif