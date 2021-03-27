#include "ADS1293.h"
#include "CommandReader.h"
#define IO_DELAY 50
#define ADS1293_CS_PIN PB0
#define ADS1293_DR_PIN PB1

#define V_REF 2.4
#define ADC_MAX 0xB964F0

boolean output_on = false;
boolean print_time = false;
long print_time_start;
long print_time_duration;

ADS1293::ADS1293* ads1293;
CommandReader* commandReader;

void setup() {
  Serial.begin(9600);

  ads1293 = new ADS1293::ADS1293(ADS1293_CS_PIN, ADS1293_DR_PIN, IO_DELAY);
  ads1293->begin();

  commandReader = new CommandReader();
  setup_ECG();
}

void loop() {
  long c_time = millis();
  if (Serial.available() > 0) {
    const char* command = commandReader->readCommand();
    if(command == NULL) {
      return;
    }
    if (strcmp(command, "0") == 0) {
      Serial.println("0. Show menu");
      Serial.println("1. Print configs");
      Serial.println("x. Print status");
      Serial.println("3. On print output");
      Serial.println("4. Off print output");
      Serial.println("5. Print samples 1 second");
      Serial.println("6. Print samples N millisecond");
      Serial.println("x. Change Type to N");
    } else if(strcmp(command, "1") == 0) {
      ads1293->readSensorID();
      readConfigs();
    }  else if(strcmp(command, "3") == 0) {
      output_on = true;
    } else if(strcmp(command, "4") == 0) {
      output_on = false;
    } else if (strcmp(command, "5") == 0) {
      output_on = false;
      print_time = true;
      print_time_duration  = 1000;
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
    } else {
      Serial.print("Unknown command: '");
      Serial.print(command);
      Serial.println("'");
    }
  }

  uint8_t dataStatus = ads1293->readRegister(ADS1293::DATA_STATUS);
  if (((dataStatus >> 5) & 0b11) == 0b11) {
    double ecgVal = readEcg(1);
    double ecgVal2 = readEcg(2);

    if (output_on || print_time && c_time - print_time_start <= print_time_duration) {
      Serial.print(ecgVal * 1000);
      Serial.print(",");
      Serial.println(ecgVal2 * 1000);
    } else if (print_time && c_time - print_time_start > print_time_duration) {
      print_time = false;
    }
  }
}

double readEcg(int channel) {
    uint8_t x1 = ads1293->readRegister((ADS1293::Registers_e)(ADS1293::DATA_CH1_ECG_1 + 3 * (channel - 1)));
    uint8_t x2 = ads1293->readRegister((ADS1293::Registers_e)(ADS1293::DATA_CH1_ECG_2 + 3 * (channel - 1)));
    uint8_t x3 = ads1293->readRegister((ADS1293::Registers_e)(ADS1293::DATA_CH1_ECG_3 + 3 * (channel - 1)));
    int32_t adc_out = (((x1 << 8) | x2) << 8) | x3;
    //double ecgVal = 2.0 * V_REF * (adc_out / ADC_MAX - 0.5) / 3.5;
    return V_REF * (2.0 * adc_out / ADC_MAX - 1.0) / 3.5;
}

void readConfigs() {
  Serial.print("FLEX_CH1_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::FLEX_CH1_CN), HEX);
  Serial.print("FLEX_CH2_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::FLEX_CH2_CN), HEX);

  Serial.print("CMDET_EN: ");
  Serial.println(ads1293->readRegister(ADS1293::CMDET_EN), HEX);
  Serial.print("RLD_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::RLD_CN), HEX);
  Serial.print("OSC_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::OSC_CN), HEX);
  Serial.print("AFE_SHDN_CN: ");
  Serial.println(ads1293->readRegister(ADS1293::AFE_SHDN_CN), HEX);

  Serial.print("R2_RATE: ");
  Serial.println(ads1293->readRegister(ADS1293::R2_RATE), HEX);
  Serial.print("R3_RATE_CH1: ");
  Serial.println(ads1293->readRegister(ADS1293::R3_RATE_CH1), HEX);
  Serial.print("R3_RATE_CH2: ");
  Serial.println(ads1293->readRegister(ADS1293::R3_RATE_CH2), HEX);

  Serial.print("DRDYB_SRC: ");
  Serial.println(ads1293->readRegister(ADS1293::DRDYB_SRC), HEX);
  Serial.print("CH_CNFG: ");
  Serial.println(ads1293->readRegister(ADS1293::CH_CNFG), HEX);
  Serial.print("CONFIG: ");
  Serial.println(ads1293->readRegister(ADS1293::CONFIG), HEX);
}

void setup_ECG() {
  //Follow the next steps to configure the device for this example, starting from default registers values.
  //1. Set address 0x01 = 0x11: Connect channel 1’s INP to IN2 and INN to IN1.
  ads1293->writeRegister(ADS1293::FLEX_CH1_CN, 0x11);
  //ads1293->writeRegister(ADS1293::FLEX_CH1_CN, 0x40);
  //2. Set address 0x02 = 0x19: Connect channel 2’s INP to IN3 and INN to IN1.
  ads1293->writeRegister(ADS1293::FLEX_CH2_CN, 0x19);
  //ads1293->writeRegister(ADS1293::FLEX_CH2_CN, 0x80);
  //3. Set address 0x0A = 0x07: Enable the common-mode detector on input pins IN1, IN2 and IN3.
  ads1293->writeRegister(ADS1293::CMDET_EN, 0x07);
  //4. Set address 0x0C = 0x04: Connect the output of the RLD amplifier internally to pin IN4.
  ads1293->writeRegister(ADS1293::RLD_CN, 0x04);
  //5. Set address 0x12 = 0x04: Use external crystal and feed the internal oscillator's output to the digital.
  ads1293->writeRegister(ADS1293::OSC_CN, 0x04);
  //6. Set address 0x14 = 0x24: Shuts down unused channel 3’s signal path.
  ads1293->writeRegister(ADS1293::AFE_SHDN_CN, 0x24);
  //7. Set address 0x21 = 0x02: Configures the R2 decimation rate as 5 for all channels.
  ads1293->writeRegister(ADS1293::R2_RATE, 0x02);
  //8. Set address 0x22 = 0x02: Configures the R3 decimation rate as 6 for channel 1.
  ads1293->writeRegister(ADS1293::R3_RATE_CH1, 0x02);
  //9. Set address 0x23 = 0x02: Configures the R3 decimation rate as 6 for channel 2.
  ads1293->writeRegister(ADS1293::R3_RATE_CH2, 0x02);
  //10. Set address 0x27 = 0x08: Configures the DRDYB source to channel 1 ECG (or fastest channel).
  ads1293->writeRegister(ADS1293::DRDYB_SRC, 0b001000);
  //11. Set address 0x2F = 0x30: Enables channel 1 ECG and channel 2 ECG for loop read-back mode.
  ads1293->writeRegister(ADS1293::CH_CNFG, 0x31);
  //12. Set address 0x00 = 0x01: Starts data conversion.
  ads1293->writeRegister(ADS1293::CONFIG, 0x01);
}