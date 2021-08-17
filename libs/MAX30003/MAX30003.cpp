#include "MAX30003.h"

#include <SPI.h>

MAX30003::MAX30003(uint8_t ioDelay, uint8_t csPin, uint8_t dataReadyPin): ioDelay(ioDelay), MobileECG(csPin, dataReadyPin) {}

void MAX30003::switchClock(bool on) const {
    digitalWrite(csPin, on ? LOW : HIGH);
    delayMicroseconds(ioDelay);
}

void MAX30003::begin() const {
    MobileECG::begin();

    SPI.begin();
    SPI.setBitOrder(MSBFIRST);
    SPI.setDataMode(SPI_MODE0);
}

void MAX30003::swReset() const {
    this->writeRegister(Registers_e::SW_RST, 0x000000);
}

void MAX30003::synch() const {
    this->writeRegister(Registers_e::SYNCH, 0x000000);
}

void MAX30003::fifoReset() const {
    this->writeRegister(Registers_e::FIFO_RST, 0x000000);
}

void MAX30003::writeRegister(const Registers_e address, unsigned long data) const {
    byte dataToSend = (address << 1) | WREG;

    this->switchClock(true);

    SPI.transfer(dataToSend);
    SPI.transfer(data >> 16);
    SPI.transfer(data >> 8);
    SPI.transfer(data);

    this->switchClock(false);
}

void MAX30003::readConfig(const Registers_e address, uint32_t & all) const {
    all = this->readRegister(address);
}

uint32_t MAX30003::readRegister(uint8_t address) const {
    this->switchClock(true);

    uint8_t reg = (address << 1) | RREG;
    SPI.transfer(reg);

    uint32_t data = 0;

    for (uint8_t i = 0; i < 3; i++) {
        data = (data << 8) | (SPI.transfer(0xff));
    }
    this->switchClock(false);

    return data;
}

uint32_t MAX30003::readRegister(const Registers_e address) const {
    return this->readRegister((uint8_t) address);
}

Status_u MAX30003::readStatus() const {
    return Status_u {
        this->readRegister(Registers_e::STATUS)
    };
}

uint8_t MAX30003::getEtag() const {
    return etag;
}

int32_t * MAX30003::readECG() {
    uint32_t data = this->readRegister(Registers_e::ECG_FIFO);
    etag = (data >> 3) & 0x7;
    ecgData[0] = (((int32_t)((int16_t)(data >> 8))) << 2) | ((int32_t)(data & 0b11));
    return ecgData;
}

bool MAX30003::readSensorID() const {
    return false;
}

void MAX30003::printConfigs() const {
    GeneralConfiguration_u CNFG_GEN_r;
    Serial.print("CNFG_GEN: ");
    this->readConfig(Registers_e::CNFG_GEN, CNFG_GEN_r.all);
    Serial.println(CNFG_GEN_r.all, HEX);

    Serial.print("* rbiasn: ");
    Serial.println(CNFG_GEN_r.bits.rbiasn);
    Serial.print("* rbiasp: ");
    Serial.println(CNFG_GEN_r.bits.rbiasp);
    Serial.print("* rbiasv: ");
    Serial.println(CNFG_GEN_r.bits.rbiasv);
    Serial.print("* en_rbias: ");
    Serial.println(CNFG_GEN_r.bits.en_rbias);

    Serial.print("* vth: ");
    Serial.println(CNFG_GEN_r.bits.vth);
    Serial.print("* imag: ");
    Serial.println(CNFG_GEN_r.bits.imag);
    Serial.print("* ipol: ");
    Serial.println(CNFG_GEN_r.bits.ipol);
    Serial.print("* en_dcloff: ");
    Serial.println(CNFG_GEN_r.bits.en_dcloff);

    Serial.print("* en_ecg: ");
    Serial.println(CNFG_GEN_r.bits.en_ecg);

    Serial.print("* fmstr: ");
    Serial.println(CNFG_GEN_r.bits.fmstr);
    Serial.print("* en_ulp_lon: ");
    Serial.println(CNFG_GEN_r.bits.en_ulp_lon);

    CalConfiguration_u CNFG_CAL_r;
    Serial.print("CNFG_CAL: ");
    this->readConfig(Registers_e::CNFG_ALL, CNFG_CAL_r.all);
    Serial.println(CNFG_CAL_r.all, HEX);
	
    Serial.print("* thigh: ");
    Serial.println(CNFG_CAL_r.bits.thigh);
    Serial.print("* fifty: ");
    Serial.println(CNFG_CAL_r.bits.fifty);
    Serial.print("* fcal: ");
    Serial.println(CNFG_CAL_r.bits.fcal);

    Serial.print("* vmag: ");
    Serial.println(CNFG_CAL_r.bits.vmag);
    Serial.print("* vmode: ");
    Serial.println(CNFG_CAL_r.bits.vmode);
    Serial.print("* en_vcal: ");
    Serial.println(CNFG_CAL_r.bits.en_vcal);

    MuxConfiguration_u CNFG_MUX_r;
    Serial.print("CNFG_EMUX: ");
    this->readConfig(Registers_e::CNFG_EMUX, CNFG_MUX_r.all);
    Serial.println(CNFG_MUX_r.all, HEX);
	
    Serial.print("* calp_sel: ");
    Serial.println(CNFG_MUX_r.bits.calp_sel);
    Serial.print("* caln_sel: ");
    Serial.println(CNFG_MUX_r.bits.caln_sel);

    Serial.print("* openn: ");
    Serial.println(CNFG_MUX_r.bits.openn);
    Serial.print("* openp: ");
    Serial.println(CNFG_MUX_r.bits.openp);

    Serial.print("* pol: ");
    Serial.println(CNFG_MUX_r.bits.pol);

    ECGConfiguration_u CNFG_ECG_r;
    Serial.print("CNFG_ECG: ");
    this->readConfig(Registers_e::CNFG_ECG, CNFG_ECG_r.all);
    Serial.println(CNFG_ECG_r.all, HEX);
	
    Serial.print("* dlpf: ");
    Serial.println(CNFG_ECG_r.bits.dlpf);
    Serial.print("* dhpf: ");
    Serial.println(CNFG_ECG_r.bits.dhpf);
    Serial.print("* gain: ");
    Serial.println(CNFG_ECG_r.bits.gain);
    Serial.print("* rate: ");
    Serial.println(CNFG_ECG_r.bits.rate);

    RtoR1Configuration_u CNFG_RTOR_r;
    Serial.print("CNFG_RTOR: ");
    this->readConfig(Registers_e::CNFG_RTOR1, CNFG_RTOR_r.all);
    Serial.println(CNFG_RTOR_r.all, HEX);
	
    Serial.print("* wndw: ");
    Serial.println(CNFG_RTOR_r.bits.wndw);
    Serial.print("* rgain: ");
    Serial.println(CNFG_RTOR_r.bits.rgain);
    Serial.print("* pavg: ");
    Serial.println(CNFG_RTOR_r.bits.pavg);
    Serial.print("* ptsf: ");
    Serial.println(CNFG_RTOR_r.bits.ptsf);
    Serial.print("* en_rtor: ");
    Serial.println(CNFG_RTOR_r.bits.en_rtor);

    ManageInterrupts_u MNG_INT_r;
    Serial.print("MNGR_INT: ");
    this->readConfig(Registers_e::MNGR_INT, MNG_INT_r.all);
    Serial.println(MNG_INT_r.all, HEX);
	
    Serial.print("* samp_it: ");
    Serial.println(MNG_INT_r.bits.samp_it);
    Serial.print("* clr_samp: ");
    Serial.println(MNG_INT_r.bits.clr_samp);
    Serial.print("* clr_rrint: ");
    Serial.println(MNG_INT_r.bits.clr_rrint);
    Serial.print("* clr_fast: ");
    Serial.println(MNG_INT_r.bits.clr_fast);
    Serial.print("* efit: ");
    Serial.println(MNG_INT_r.bits.efit);

    EnableInterrupts_u EN_INT_r;
    Serial.print("EN_INT: ");
    this->readConfig(Registers_e::EN_INT, EN_INT_r.all);
    Serial.println(EN_INT_r.all, HEX);
	
    Serial.print("* intb_type: ");
    Serial.println(EN_INT_r.bits.intb_type);
    Serial.print("* en_pllint: ");
    Serial.println(EN_INT_r.bits.en_pllint);
    Serial.print("* en_samp: ");
    Serial.println(EN_INT_r.bits.en_samp);
    Serial.print("* en_rrint: ");
    Serial.println(EN_INT_r.bits.en_rrint);
    Serial.print("* en_loint: ");
    Serial.println(EN_INT_r.bits.en_loint);
    Serial.print("* en_dcloffint: ");
    Serial.println(EN_INT_r.bits.en_dcloffint);
    Serial.print("* en_fstint: ");
    Serial.println(EN_INT_r.bits.en_fstint);
    Serial.print("* en_eovf: ");
    Serial.println(EN_INT_r.bits.en_eovf);
    Serial.print("* en_eint: ");
    Serial.println(EN_INT_r.bits.en_eint);

    ManageDynamicModes_u MNG_DYN_r;
    Serial.print("MNG_DYN: ");
    this->readConfig(Registers_e::MNGR_DYN, MNG_DYN_r.all);
    Serial.println(MNG_DYN_r.all, HEX);
	
    Serial.print("* fast_th: ");
    Serial.println(MNG_DYN_r.bits.fast_th);
    Serial.print("* fast: ");
    Serial.println(MNG_DYN_r.bits.fast);
}

const char * MAX30003::getName() const {
    return "MAX30003";
}

uint8_t MAX30003::getChannels() const {
    return 1;
}

int32_t MAX30003::getMinValue() const {
    return -30000;
}

int32_t MAX30003::getMaxValue() const {
    return 45000;
}