#include "ADS1298.h"
#include "ECGAdapter.h"

#define ADS1298_CS_PIN PB0
#define ADS1298_DR_PIN PB1
#define ADS1298_START_PIN PB10

ADS1298::ADS1298 ads1298(ADS1298_CS_PIN, ADS1298_DR_PIN);
ECGAdapter ecgAdapter(ads1298, "2.0.0", 64);

void setup() {
  ads1298.begin();
  pinMode(ADS1298_START_PIN, OUTPUT);

  setup_ECG();
}

void loop() {
  ecgAdapter.loop();

  if (ads1298.isDataReady()) {
    int32_t* ecgs = ads1298.readECG();
    ecgAdapter.send(ecgs);
    ads1298.writeCommand(ADS1298::SPICommands_e::START);
  }
}

void setup_ECG() {
  delay(1000); //pause to provide ads129n enough time to boot up...
  ads1298.writeCommand(ADS1298::SPICommands_e::SDATAC);

  ADS1298::Config1_u c1;
  c1.all = 0;
  c1.bits.hr = 1;
  //c1.bits.dr = 0b100; // 2000
  c1.bits.dr = 0b011; // 4000
  //c1.bits.dr = 0b010; // 8000
  ads1298.writeRegister(ADS1298::CONFIG1, c1.all);

  ADS1298::Config2_u c2;
  c2.all = 0;
  c2.bits.int_test = 0;
  c2.bits.test_freq = 0b00;
  c2.bits.wct_chop = 0;
  ads1298.writeRegister(ADS1298::CONFIG2, c2.all);

  ADS1298::Config3_u c3;
  c3.all = 0;
  c3.bits.pd_refbuf = 1;
  c3.bits.reserved = 1;
  c3.bits.rld_meas = 1;
  c3.bits.rldref_int = 1;
  c3.bits.pd_rld = 1;
  ads1298.writeRegister(ADS1298::CONFIG3, c3.all);

  ADS1298::Loff_u loff;
  loff.all = 0;
  loff.bits.flead_off = 0b10;
  ads1298.writeRegister(ADS1298::LOFF, loff.all);

  ADS1298::ChNSet_u chSet;
  chSet.all = 0;
  //chSet.bits.gain = 0b101;
  chSet.bits.gain = 0b100; // gain = 4

  for (int i = 0; i < 8; ++i) {
    ads1298.writeRegister((ADS1298::Registers_e)(ADS1298::Registers_e::CH1SET + i), chSet.all);
  }

  ADS1298::ChannelSetting_u defaultSettings;
  defaultSettings.all = 0;
  ads1298.writeRegister(ADS1298::RLD_SENSP, defaultSettings.all );
  ads1298.writeRegister(ADS1298::RLD_SENSN, defaultSettings.all );

  ADS1298::ChannelSetting_u loffSensP;
  loffSensP.all = 0xFF;
  //ads1298.writeRegister(ADS1298::LOFF_SENSP, loffSensP.all );
  ads1298.writeRegister(ADS1298::LOFF_SENSP, defaultSettings.all );

  ADS1298::ChannelSetting_u loffSensN;
  loffSensN.all = 0;
  loffSensN.bits.c2 = 1;
  //ads1298.writeRegister(ADS1298::LOFF_SENSN, loffSensN.all );
  ads1298.writeRegister(ADS1298::LOFF_SENSN, defaultSettings.all );

  ads1298.writeRegister(ADS1298::LOFF_FLIP, defaultSettings.all );

  // All GPIO set to output 0x0000: (floating CMOS inputs can flicker on and off, creating noise)
  ads1298.writeRegister(ADS1298::GPIO, 0x00);

  ads1298.writeRegister(ADS1298::PACE, 0x00);
  ads1298.writeRegister(ADS1298::RESP, 0x00);

  ads1298.writeRegister(ADS1298::WCT1, 0x0A);
  ads1298.writeRegister(ADS1298::WCT2, 0xE3);

  digitalWrite(ADS1298_START_PIN, HIGH);
  ads1298.writeCommand(ADS1298::SPICommands_e::START);
}