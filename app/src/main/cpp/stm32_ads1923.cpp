#include "ADS1293.h"
#include "CommandReader.h"

#define VERSION "1.0.0"

#define ADS1293_CS_PIN PB0
#define ADS1293_DR_PIN PB1

#define V_REF 2.4
#define ADC_MAX 0xF30000

boolean output_on = false;
boolean print_time = false;
long print_time_start;
long print_time_duration;

ADS1293::ADS1293* ads1293;
CommandReader* commandReader;

void setup() {
  Serial.begin(9600);

  ads1293 = new ADS1293::ADS1293(ADS1293_CS_PIN, ADS1293_DR_PIN);
  ads1293->begin();

  commandReader = new CommandReader();
  setup_ECG();
}

int counter = 0;

void loop() {
  if (Serial.available() > 0) {
    const char* command = commandReader->readCommand();
    if (command == NULL) {
      return;
    }
    if (strcmp(command, "MENU") == 0) {
      Serial.println("MENU. Show menu");
      Serial.println("GET_CONFIGS. Print configs");
      Serial.println("GET_ALL_CONFIGS. Print all board configs");
      Serial.println("GET_ALL_CONFIGS. Print status");
      Serial.println("START. On print output");
      Serial.println("STOP. Off print output");
      Serial.println("5. Print samples per second");
      Serial.println("6. Print samples N millisecond");
      Serial.println("x. Change Type to N");
      Serial.println("GET_PARAMETER. Get parameter by name");
    } else if (strcmp(command, "GET_CONFIGS") == 0) {
      ads1293->readSensorID();
      readConfigs();
    } else if (strcmp(command, "GET_ALL_CONFIGS") == 0) {
      ads1293->readSensorID();
      readAllConfigs();
    }  else if (strcmp(command, "START") == 0) {
      output_on = true;
      Serial.println("DATA_FLOW_STARTED");
    } else if (strcmp(command, "STOP") == 0) {
      output_on = false;
      Serial.println("DATA_FLOW_STOPPED");
    } else if (strcmp(command, "5") == 0) {
      output_on = false;
      print_time = true;
      counter  = 4267;
      print_time_start = millis();
    } else if (strcmp(command, "6") == 0) {
      output_on = false;
      print_time = true;
      while (!Serial.available()) {
        delayMicroseconds(50);
      }
      Serial.println("Input N: ");
      print_time_duration = Serial.parseInt();
      print_time_start = millis();
    } else if(strcmp(command, "GET_PARAMETER") == 0) {
      const char* parameter = NULL;
      do {
        parameter = commandReader->readCommand();
      } while(parameter == NULL);
      if(strcmp(parameter, "MODEL") == 0) {
        Serial.println("ADS1293");
      } else if(strcmp(parameter, "CHANNELS_COUNT") == 0) {
        Serial.println("3");
      } else if(strcmp(parameter, "VERSION") == 0) {
        Serial.println(VERSION);
      } else if(strcmp(parameter, "MIN_VALUE") == 0) {
        Serial.println(-2000000);
      } else if(strcmp(parameter, "MAX_VALUE") == 0) {
        Serial.println(2000000);
      } else {
        Serial.print("Unknown parameter: '");
        Serial.print(parameter);
        Serial.println('\'');
      }
    } else {
      Serial.print("Unknown command: '");
      Serial.print(command);
      Serial.println('\'');
    }
  }

  if (ads1293->isDataReady()) {
    int32_t ecgVal = ads1293->readECG(1, V_REF, ADC_MAX);
    int32_t ecgVal2 = ads1293->readECG(2, V_REF, ADC_MAX);
    int32_t ecgVal3 = ads1293->readECG(3, V_REF, ADC_MAX);

    if (output_on || print_time && counter-- > 0) {
      Serial.print(ecgVal);
      Serial.write(',');
      Serial.print(ecgVal2);
      Serial.write(',');
      Serial.println(ecgVal3);
    } else if (print_time && counter <= 0) {
      print_time = false;
      Serial.print("Send: ");
      Serial.println(millis() - print_time_start);
    }
  }
}

void readAllConfigs() {
  for(int reg = 0; reg <= 0x50; reg ++) {
    Serial.print(reg, HEX);
    Serial.print(": ");
    Serial.println(ads1293->readRegister(reg), HEX);
  }
}

void readConfigs() {
  Serial.print("FLEX_CH1_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::FLEX_CH1_CN), HEX);
  Serial.print("FLEX_CH2_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::FLEX_CH2_CN), HEX);
  Serial.print("FLEX_CH3_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::FLEX_CH3_CN), HEX);

  Serial.print("CMDET_EN: ");
  Serial.println(ads1293->readRegister(ADS1293::CMDET_EN), HEX);
  Serial.print("RLD_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::RLD_CN), HEX);

  Serial.print("WILSON_EN1: ");
  Serial.println(ads1293->readRegister(ADS1293::WILSON_EN1), HEX);
  Serial.print("WILSON_EN2: ");
  Serial.println(ads1293->readRegister(ADS1293::WILSON_EN2), HEX);
  Serial.print("WILSON_EN3: ");
  Serial.println(ads1293->readRegister(ADS1293::WILSON_EN3), HEX);
  Serial.print("WILSON_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::WILSON_CN), HEX);

  Serial.print("OSC_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::OSC_CN), HEX);

  Serial.print("AFE_RES: ");
  Serial.println(ads1293->readRegister(ADS1293::AFE_RES), HEX);

  Serial.print("R1_RATE: ");
  Serial.println(ads1293->readRegister(ADS1293::R1_RATE), HEX);

  Serial.print("AFE_SHDN_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::AFE_SHDN_CN), HEX);

  Serial.print("R2_RATE: ");
  Serial.println(ads1293->readRegister(ADS1293::R2_RATE), HEX);

  Serial.print("R3_RATE_CH1: ");
  Serial.println(ads1293->readRegister(ADS1293::R3_RATE_CH1), HEX);
  Serial.print("R3_RATE_CH2: ");
  Serial.println(ads1293->readRegister(ADS1293::R3_RATE_CH2), HEX);
  Serial.print("R3_RATE_CH3: ");
  Serial.println(ads1293->readRegister(ADS1293::R3_RATE_CH3), HEX);

  Serial.print("DRDYB_SRC: ");
  Serial.println(ads1293->readRegister(ADS1293::DRDYB_SRC), HEX);
  Serial.print("CH_CNFG: ");
  Serial.println(ads1293->readRegister(ADS1293::CH_CNFG), HEX);
  Serial.print("CONFIG: ");
  Serial.println(ads1293->readRegister(ADS1293::CONFIG), HEX);
}

void setup_ECG() {
  ads1293->writeRegister(ADS1293::CONFIG, 0x00);

  //ads1293->writeRegister(ADS1293::FLEX_CH1_CN, 0b01000000);
  //ads1293->writeRegister(ADS1293::FLEX_CH2_CN, 0b10000000);
  //ads1293->writeRegister(ADS1293::FLEX_CH3_CN, 0b11000000);

  ads1293->writeRegister(ADS1293::FLEX_CH1_CN, 0x11);
  ads1293->writeRegister(ADS1293::FLEX_CH2_CN, 0x19);
  ads1293->writeRegister(ADS1293::FLEX_CH3_CN, 0x2E);

  ads1293->writeRegister(ADS1293::CMDET_EN, 0x07);
  ads1293->writeRegister(ADS1293::RLD_CN, 0x04);

  ads1293->writeRegister(ADS1293::WILSON_EN1, 0x01);
  ads1293->writeRegister(ADS1293::WILSON_EN2, 0x02);
  ads1293->writeRegister(ADS1293::WILSON_EN3, 0x03);
  ads1293->writeRegister(ADS1293::WILSON_CN, 0x01);

  ads1293->writeRegister(ADS1293::OSC_CN, 0x04);

  ads1293->writeRegister(ADS1293::AFE_RES, 0b00111111);

  ads1293->writeRegister(ADS1293::AFE_SHDN_CN, 0);

  ads1293->writeRegister(ADS1293::R2_RATE, 0x04);

  ads1293->writeRegister(ADS1293::R3_RATE_CH1, 0x01);
  ads1293->writeRegister(ADS1293::R3_RATE_CH2, 0x01);
  ads1293->writeRegister(ADS1293::R3_RATE_CH3, 0x01);

  ads1293->writeRegister(ADS1293::R1_RATE, 0b00000111);

  ads1293->writeRegister(ADS1293::DRDYB_SRC, 0b111000);
  ads1293->writeRegister(ADS1293::CH_CNFG, 0b01110000);

  ads1293->writeRegister(ADS1293::CONFIG, 0x01);
}