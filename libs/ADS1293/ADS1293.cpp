#include "ADS1293.h"

#include <SPI.h>

namespace ADS1293 {
    ADS1293::ADS1293(float vRef, uint32_t adcMax, uint8_t csPin, uint8_t dataReadyPin):
        vRef(vRef), adcMax(adcMax), MobileECG(csPin, dataReadyPin) {
			// 1000000 * (vRef * ((2.0f * adc_out) / adcMax - 1.0f) / 3.5f);
			//this->ecgX = 1000000 * vRef / 3.5f;
			this->ecgX = 24.f/35.f;
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
        return this->readRegister((uint8_t) address);
    }

    int32_t * ADS1293::readECG() {
        uint8_t reg = DATA_CH1_ECG_1;
        for (uint8_t i = 0; i < 3; i++) {
            int32_t adc_out = 0;
            adc_out |= readRegister(reg++);
			delayMicroseconds(5);
            adc_out = (adc_out << 8) | (readRegister(reg++));
			delayMicroseconds(5);
            adc_out = (adc_out << 8) | (readRegister(reg++));
			delayMicroseconds(5);
            //ecgData[i] = 1000000 * (vRef * ((2.0f * adc_out) / adcMax - 1.0f) / 3.5f);
            //ecgData[i] = ecgX * ((2.0f * adc_out) / adcMax - 1.0f);
            ecgData[i] = 1000000 * (adc_out / 11612160.f - ecgX);
        }
        return ecgData;
    }

    bool ADS1293::readSensorID() const {
        uint8_t ID = readRegister(REVID);
        Serial.println(ID);
        if (ID != 0xff) {
            return true;
        } else {
            return false;
        }
    }

    void ADS1293::printConfigs() const {
		for(uint8_t i = 0; i <= 0x30; i++) {
			if(RegisterNames[i] != NULL) {
				Serial.print("[0x");
				Serial.print(i, HEX);
				Serial.print("]: ");
				Serial.print(RegisterNames[i]);
				Serial.print("=");
				Serial.println(readRegister(i), HEX);
			}
		}
    }

    const char * ADS1293::getName() const {
        return "ADS1293";
    }

    uint8_t ADS1293::getChannels() const {
        return 3;
    }

    int32_t ADS1293::getMinValue() const {
        return -600000;
    }

    int32_t ADS1293::getMaxValue() const {
        return 600000;
    }
}