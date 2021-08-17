#include "ADS1298.h"

#include <SPI.h>

namespace ADS1298 {
    ADS1298::ADS1298(uint8_t csPin, uint8_t dataReadyPin): MobileECG(csPin, dataReadyPin) {}

    void ADS1298::begin() const {
        MobileECG::begin();
        SPI.begin();
        SPI.setBitOrder(MSBFIRST);
        SPI.setDataMode(SPI_MODE1);
        //SPI.setClockDivider(SPI_CLOCK_DIV2);
        SPI.setClockDivider(SPI_CLOCK_DIV8);
        //SPI.setClockDivider(SPI_CLOCK_DIV32);
    }

    void ADS1298::writeRegister(const Registers_e address, uint8_t data) {
        //see pages 40,43 of datasheet -		
        uint8_t reg = address | WREG;
        digitalWrite(csPin, LOW);
        SPI.transfer(reg);
        SPI.transfer(0); // number of registers to be read/written – 1
        SPI.transfer(data);
        digitalWrite(csPin, HIGH);
    }

    void ADS1298::writeCommand(uint8_t data) const {
        digitalWrite(csPin, LOW);
        SPI.transfer(data);
        digitalWrite(csPin, HIGH);
    }

    uint8_t ADS1298::readRegister(const uint8_t address) const {
        uint8_t reg = address | RREG;
        digitalWrite(csPin, LOW);
        SPI.transfer(reg);
        SPI.transfer(0);
        uint8_t data = SPI.transfer(0);
        digitalWrite(csPin, HIGH);
        return data;
    }

    uint8_t ADS1298::readRegister(const Registers_e address) const {
        return this -> readRegister((uint8_t) address);
    }

    int32_t * ADS1298::readECG() {
        static uint8_t i;
        static int32_t data;

        stat = 0;

        digitalWrite(csPin, LOW);
        SPI.transfer(SPICommands_e::RDATAC);

        // READ CHANNEL DATA FROM FIRST ADS IN DAISY LINE
        for (i = 0; i < 3; i++) {
            stat = (stat << 8) | (SPI.transfer(0x00));
        }

        for (i = 0; i < 8; i++) {
            data = 0;
            data |= SPI.transfer(0x00);
            data = (data << 8) | (SPI.transfer(0x00));
            data = (data << 8) | (SPI.transfer(0x00));

            if ((data & 0x800000) == 0) {
                ecgData[i] = LSB * data;
            } else {
                ecgData[i] = LSB * (data - N2);
            }
        }
        digitalWrite(csPin, HIGH);
        return ecgData;
    }

    bool ADS1298::readSensorID() const {
        uint8_t id = readRegister(ID);
        Serial.println(id);
        if (id != 0xff) {
            return true;
        } else {
            return false;
        }
    }

    void ADS1298::printConfigs() const {
        this -> writeCommand(SPICommands_e::SDATAC);
        DeviceId_u du;
        du.all = this -> readRegister(0);
        Serial.print("Board: ");
        if (du.bits.family == 0b100) {
            if (du.bits.model == 0b000) {
                Serial.println("4-channel ADS1294");
            } else if (du.bits.model == 0b001) {
                Serial.println("6-channel ADS1296");
            } else if (du.bits.model == 0b010) {
                Serial.println("8-channel ADS1298");
            } else {
                Serial.print("unknown model (");
                Serial.print(du.bits.model, BIN);
                Serial.println(")");
            }
        } else if (du.bits.family == 0b110) {
            if (du.bits.model == 0b000) {
                Serial.println("4-channel ADS1294R");
            } else if (du.bits.model == 0b001) {
                Serial.println("6-channel ADS1296R");
            } else if (du.bits.model == 0b010) {
                Serial.println("8-channel ADS1298R");
            } else {
                Serial.print("unknown R model (");
                Serial.print(du.bits.model, BIN);
                Serial.println(")");
            }
        } else {
            Serial.print("unknown family (");
            Serial.print(du.bits.family, BIN);
            Serial.println(")");
        }

        Config1_u c1;
        c1.all = this -> readRegister(1);
        Serial.println("CONFIG1");
        Serial.print("- hr: ");
        Serial.println(c1.bits.hr);
        Serial.print("- daisy_en: ");
        Serial.println(c1.bits.daisy_en);
        Serial.print("- clk_en: ");
        Serial.println(c1.bits.clk_en);
        Serial.print("- dr: ");
        Serial.println(c1.bits.dr);

        Config2_u c2;
        c2.all = this -> readRegister(2);
        Serial.println("CONFIG2");
        Serial.print("- wct_chop: ");
        Serial.println(c2.bits.wct_chop);
        Serial.print("- int_test: ");
        Serial.println(c2.bits.int_test);
        Serial.print("- test_amp: ");
        Serial.println(c2.bits.test_amp);
        Serial.print("- test_freq: ");
        Serial.println(c2.bits.test_freq);

        Config3_u c3;
        c3.all = this -> readRegister(3);
        Serial.println("CONFIG3");
        Serial.print("- pd_refbuf: ");
        Serial.println(c3.bits.pd_refbuf);
        Serial.print("- vref_4v: ");
        Serial.println(c3.bits.vref_4v);
        Serial.print("- rld_meas: ");
        Serial.println(c3.bits.rld_meas);
        Serial.print("- rldref_int: ");
        Serial.println(c3.bits.rldref_int);
        Serial.print("- pd_rld: ");
        Serial.println(c3.bits.pd_rld);
        Serial.print("- rld_loff_sens: ");
        Serial.println(c3.bits.rld_loff_sens);
        Serial.print("- rld_stat: ");
        Serial.println(c3.bits.rld_stat);

        Loff_u loff;
        loff.all = this -> readRegister(4);
        Serial.println("LOFF");
        Serial.print("- comp_th: ");
        Serial.println(loff.bits.comp_th);
        Serial.print("- vlead_off_en: ");
        Serial.println(loff.bits.vlead_off_en);
        Serial.print("- ilead_off: ");
        Serial.println(loff.bits.ilead_off);
        Serial.print("- flead_off: ");
        Serial.println(loff.bits.flead_off);

        ChNSet_u ch_u;
        for (uint8_t i = 0x05; i <= 0x0C; i++) {
            ch_u.all = this -> readRegister(i);
            Serial.print("CH");
            Serial.print(i - 0x05 + 1);
            Serial.println("SET");
            Serial.print("- pd: ");
            Serial.println(ch_u.bits.pd);
            Serial.print("- gain: ");
            Serial.println(ch_u.bits.gain);
            Serial.print("- mux: ");
            Serial.println(ch_u.bits.mux);
        }
        for (uint8_t i = 0x0D; i <= 0x19; i++) {
            Serial.print(i, HEX);
            Serial.print(": ");
            Serial.println(this -> readRegister(i), HEX);
        }
        this -> writeCommand(SPICommands_e::START);
    }

    const char * ADS1298::getName() const {
        return "ADS1298";
    }

    uint8_t ADS1298::getChannels() const {
        return 8;
    }

    int32_t ADS1298::getMinValue() const {
        return -2000000;
    }

    int32_t ADS1298::getMaxValue() const {
        return 2000000;
    }
}