#include "ADS1293.h"
#include "ECGAdapter.h"

#define ADS1293_CS_PIN PB0
#define ADS1293_DR_PIN PB1

#define V_REF 2.4f
#define ADC_MAX 0xF30000

ADS1293::ADS1293 ads1293(V_REF, ADC_MAX, ADS1293_CS_PIN, ADS1293_DR_PIN);
ECGAdapter ecgAdapter(ads1293, "2.0.0");

void setup() {
  ads1293.begin();

  setup_ECG();
}

void loop() {
  ecgAdapter.loop();

  if (ads1293.isDataReady()) {
      int32_t* ecgs = ads1293.readECG();
      ecgAdapter.send(ecgs);
  }
}

void setup_ECG() {
  ads1293.writeRegister(ADS1293::CONFIG, 0x00);

  //ads1293.writeRegister(ADS1293::FLEX_CH1_CN, 0b01000000);
  //ads1293.writeRegister(ADS1293::FLEX_CH2_CN, 0b10000000);
  //ads1293.writeRegister(ADS1293::FLEX_CH3_CN, 0b11000000);

  ads1293.writeRegister(ADS1293::FLEX_CH1_CN, 0x11);
  ads1293.writeRegister(ADS1293::FLEX_CH2_CN, 0x19);
  ads1293.writeRegister(ADS1293::FLEX_CH3_CN, 0x2E);

  ads1293.writeRegister(ADS1293::CMDET_EN, 0x07);
  ads1293.writeRegister(ADS1293::RLD_CN, 0x04);

  ads1293.writeRegister(ADS1293::WILSON_EN1, 0x01);
  ads1293.writeRegister(ADS1293::WILSON_EN2, 0x02);
  ads1293.writeRegister(ADS1293::WILSON_EN3, 0x03);
  ads1293.writeRegister(ADS1293::WILSON_CN, 0x01);

  ads1293.writeRegister(ADS1293::OSC_CN, 0x04);

  ads1293.writeRegister(ADS1293::AFE_RES, 0b00111111);

  ads1293.writeRegister(ADS1293::AFE_SHDN_CN, 0);

  ads1293.writeRegister(ADS1293::R2_RATE, 0x04);

  ads1293.writeRegister(ADS1293::R3_RATE_CH1, 0x01);
  ads1293.writeRegister(ADS1293::R3_RATE_CH2, 0x01);
  ads1293.writeRegister(ADS1293::R3_RATE_CH3, 0x01);

  ads1293.writeRegister(ADS1293::R1_RATE, 0b00000111);

  ads1293.writeRegister(ADS1293::DRDYB_SRC, 0b111000);
  ads1293.writeRegister(ADS1293::CH_CNFG, 0b01110000);

  ads1293.writeRegister(ADS1293::CONFIG, 0x01);
}