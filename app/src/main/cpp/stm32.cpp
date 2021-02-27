#include "MAX30003.h"

#define IO_DELAY 50
#define COMMAND_MAX_LENGTH 32
#define MAX30003_CS_PIN PB0
#define APP_VERSION "0.0.2"

unsigned long etagFifoOvf = 0;
unsigned long interruptsError = 0;
boolean output_on = false;
boolean print_time = false;
long print_time_start;
long print_time_duration;
char command[COMMAND_MAX_LENGTH];
int commandLength;

MAX30003* max30003;

void setup() {
  Serial.begin(9600);

  max30003 = new MAX30003(MAX30003_CS_PIN, IO_DELAY);
  max30003->begin();

  MAX30003_begin();
}

void clearCommand() {
  commandLength = 0;
  for (int i = 0; i < COMMAND_MAX_LENGTH; i++) {
    command[i] = '\0';
  }
}

char readChar(const char* caption = NULL) {
  if (caption != NULL) {
    Serial.println(caption);
  }
  while (Serial.available() == 0) {
    delayMicroseconds(10);
  }
  return Serial.read();
}

int hex2int(char c) {
  if (c >= '0' && c <= '9') {
    return c - '0';
  } else if (c >= 'A' && c <= 'F') {
    return c - 'A' + 10;
  } else if (c >= 'a' && c <= 'f') {
    return c - 'a' + 10;
  } else {
    return -1;
  }
}

void loop() {
  long c_time = millis();

  if (Serial.available()) {
    int c;
    do {
      c = Serial.read();
    } while (isSpace(c));
    if (c == -1) {
      return;
    }
    clearCommand();
    command[commandLength++] = c;

    while (!isSpace((c = Serial.read()))) {
      if (c == -1) {
        break;
      }
      command[commandLength++] = c;
      if (commandLength >= COMMAND_MAX_LENGTH - 1) {
        Serial.print("Command is too long: ");
        Serial.print(command);
        while (!isSpace(c = Serial.read()) && c != -1) {
          Serial.print((char) c);
        }
        Serial.println();
        return;
      }
    }

    if (strcmp(command, "1") == 0) {
      printConfigs();
    } else if (strcmp(command, "2") == 0) {
      printStat();
    } else if (strcmp(command, "3") == 0) {
      output_on = true;
    } else if (strcmp(command, "4") == 0) {
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
    } else if (strcmp(command, "7") == 0) {
      Serial.println("0. Back to menu");
      Serial.println("1. CNFG_GEN");
      Serial.println("2. CNFG_CAL");
      Serial.println("3. CNFG_EMUX");
      Serial.println("4. CNFG_ECG");
      Serial.println("5. CNFG_RTOR1");
      Serial.println("6. EN_INT");
      Serial.println("7. MNGR_DYN");
      Serial.println("8. MNGR_INT");

      char type = readChar("Input type:");
      Registers_e reg;

      if (type == '1') {
        reg = Registers_e::CNFG_GEN;
      } else if (type == '2') {
        reg = Registers_e::CNFG_ALL;
      } else if (type == '3') {
        reg = Registers_e::CNFG_EMUX;
      } else if (type == '4') {
        reg = Registers_e::CNFG_ECG;
      } else if (type == '5') {
        reg = Registers_e::CNFG_RTOR1;
      } else if (type == '6') {
        reg = Registers_e::EN_INT;
      } else if (type == '7') {
        reg = Registers_e::MNGR_DYN;
      } else if (type == '8') {
        reg = Registers_e::MNGR_INT;
      } else if(type == '0') {
        return;
      } else {
        Serial.print("Unknown type: '");
        Serial.print(type);
        Serial.println("'");
        return;
      }

      uint32_t code = 0;
      Serial.println("Input value: 0xXXYYZZ");
      for (int i = 0; i < 8; i++) {
        code |= (hex2int(readChar())) << ((7 - i) * 4);
      }

      max30003->swReset();
      max30003->fifoReset();
      max30003->writeRegister(reg , code);
      max30003->synch();
      Serial.print("Value ");
      Serial.print(code, HEX);
      Serial.print(" was writen in register ");
      Serial.println(reg);
    } else if (strcmp(command, "0") == 0) {
      Serial.println("0. Show menu");
      Serial.println("1. Print configs");
      Serial.println("2. Print status");
      Serial.println("3. On print output");
      Serial.println("4. Off print output");
      Serial.println("5. Print samples 1 second");
      Serial.println("6. Print samples N millisecond");
      Serial.println("7. Change Type to N");
    } else {
      Serial.print("Unknown command: '");
      Serial.print(command);
      Serial.println("'");
    }
  }

  Status_u s = max30003->readStatus();
  if (!s.bits.eint) {
    interruptsError++;
    delayMicroseconds(IO_DELAY);
    return;
  }
  if (s.bits.eovf) {
    max30003->fifoReset();
    etagFifoOvf++;
    return;
  }

  byte etag;

  int_least32_t ecgdata = readEcg(etag);

  if (etag == FIFO_VALID_SAMPLE || etag == FIFO_LAST_SAMPLE) {
    if (output_on || print_time && c_time - print_time_start <= print_time_duration) {
      Serial.println(ecgdata);
    } else if (print_time && c_time - print_time_start > print_time_duration) {
      print_time = false;
    }
  } else if (etag == FIFO_OVF) {
    max30003->fifoReset();
    etagFifoOvf++;
  }
}

void printStat() {
  Serial.print("FIFO overflow: ");
  Serial.println(etagFifoOvf);
  Serial.print("Interrupts error: ");
  Serial.println(interruptsError);
  Serial.print("App version: ");
  Serial.println(APP_VERSION);
}

int_least32_t readEcg(byte& etag) {
  uint32_t data = max30003->readUint32(Registers_e::ECG_FIFO);
  etag = (data >> 3) & 0x7;
  return (((int_least32_t)((int_least16_t)(data >> 8))) << 2) | ((int_least32_t)(data & 0b11));
}

void MAX30003_begin() {
  max30003->swReset();
  max30003->fifoReset();

  GeneralConfiguration_u CNFG_GEN_r;
  setupGeneralCfg(CNFG_GEN_r);
  max30003->writeRegister(Registers_e::CNFG_GEN, CNFG_GEN_r.all);

  CalConfiguration_u CNFG_CAL_r;
  setupCalCfg(CNFG_CAL_r);
  max30003->writeRegister(Registers_e::CNFG_ALL, CNFG_CAL_r.all);

  MuxConfiguration_u CNFG_MUX_r;
  setupManageCfg(CNFG_MUX_r);
  max30003->writeRegister(Registers_e::CNFG_EMUX, CNFG_MUX_r.all);

  ECGConfiguration_u CNFG_ECG_r;
  setupECGCfg(CNFG_ECG_r);
  max30003->writeRegister(Registers_e::CNFG_ECG, CNFG_ECG_r.all);

  RtoR1Configuration_u CNFG_RTOR_r;
  setupRtoRCfg(CNFG_RTOR_r);
  max30003->writeRegister( Registers_e::CNFG_RTOR1 , CNFG_RTOR_r.all);

  EnableInterrupts_u EN_INT_r;
  setupInterruptsCfg(EN_INT_r);
  max30003->writeRegister( Registers_e::EN_INT , EN_INT_r.all);

  ManageDynamicModes_u MNG_DYN_r;
  setupDynamicCfg(MNG_DYN_r);
  max30003->writeRegister( Registers_e::MNGR_DYN , MNG_DYN_r.all);

  ManageInterrupts_u MNG_INT_r;
  setupManageCfg(MNG_INT_r);
  max30003->writeRegister( Registers_e::MNGR_INT , MNG_INT_r.all);

  max30003->synch();
}

void printConfigs() {
  GeneralConfiguration_u CNFG_GEN_r;
  setupGeneralCfg(CNFG_GEN_r);
  Serial.print("CNFG_GEN: ");
  Serial.print(CNFG_GEN_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::CNFG_GEN, CNFG_GEN_r.all);
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
  setupCalCfg(CNFG_CAL_r);
  Serial.print("CNFG_CAL: ");
  Serial.print(CNFG_CAL_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::CNFG_ALL, CNFG_CAL_r.all);

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
  setupManageCfg(CNFG_MUX_r);
  Serial.print("CNFG_EMUX: ");
  Serial.print(CNFG_MUX_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::CNFG_EMUX, CNFG_MUX_r.all);
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
  setupECGCfg(CNFG_ECG_r);
  Serial.print("CNFG_ECG: ");
  Serial.print(CNFG_ECG_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::CNFG_ECG, CNFG_ECG_r.all);
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
  setupRtoRCfg(CNFG_RTOR_r);
  Serial.print("CNFG_RTOR: ");
  Serial.print(CNFG_RTOR_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::CNFG_RTOR1, CNFG_RTOR_r.all);
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
  setupManageCfg(MNG_INT_r);
  Serial.print("MNGR_INT: ");
  Serial.print(MNG_INT_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::MNGR_INT, MNG_INT_r.all);
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
  setupInterruptsCfg(EN_INT_r);
  Serial.print("EN_INT: ");
  Serial.print(EN_INT_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::EN_INT, EN_INT_r.all);
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
  setupDynamicCfg(MNG_DYN_r);
  Serial.print("MNG_DYN: ");
  Serial.print(MNG_DYN_r.all, HEX);
  Serial.print(" ");
  max30003->readConfig(Registers_e::MNGR_DYN, MNG_DYN_r.all);
  Serial.println(MNG_DYN_r.all, HEX);
  Serial.print("* fast_th: ");
  Serial.println(MNG_DYN_r.bits.fast_th);
  Serial.print("* fast: ");
  Serial.println(MNG_DYN_r.bits.fast);
}

void setupGeneralCfg(GeneralConfiguration_u& CNFG_GEN_r) {
  CNFG_GEN_r.all = 0;

  CNFG_GEN_r.bits.rbiasn = 1;
  CNFG_GEN_r.bits.rbiasp = 1;
  CNFG_GEN_r.bits.rbiasv = 0b00;
  CNFG_GEN_r.bits.en_rbias = 1;

  CNFG_GEN_r.bits.vth = 0b00;
  CNFG_GEN_r.bits.imag = 0b10;
  CNFG_GEN_r.bits.ipol = 0;
  CNFG_GEN_r.bits.en_dcloff = 1;

  CNFG_GEN_r.bits.en_ecg = 1;

  CNFG_GEN_r.bits.fmstr = 0;
  CNFG_GEN_r.bits.en_ulp_lon = 0;
}

void setupCalCfg(CalConfiguration_u& CNFG_CAL_r) {
  CNFG_CAL_r.all = 0;

  CNFG_CAL_r.bits.thigh = 0;
  CNFG_CAL_r.bits.fifty = 0;
  CNFG_CAL_r.bits.fcal = 0b000;

  CNFG_CAL_r.bits.vmag = 0;
  CNFG_CAL_r.bits.vmode = 0;
  CNFG_CAL_r.bits.en_vcal = 0;
}

void setupManageCfg(MuxConfiguration_u& CNFG_MUX_r) {
  CNFG_MUX_r.all = 0;
  CNFG_MUX_r.bits.calp_sel = 0;
  CNFG_MUX_r.bits.caln_sel = 0;

  CNFG_MUX_r.bits.openn = 0;
  CNFG_MUX_r.bits.openp = 0;

  CNFG_MUX_r.bits.pol = 0;
}

void setupECGCfg(ECGConfiguration_u& CNFG_ECG_r) {
  CNFG_ECG_r.all = 0;
  CNFG_ECG_r.bits.dlpf = 0b10;
  CNFG_ECG_r.bits.dhpf = 1;
  CNFG_ECG_r.bits.gain = 0b11;
  CNFG_ECG_r.bits.rate = 0b00;
}

void setupRtoRCfg(RtoR1Configuration_u& CNFG_RTOR_r) {
  CNFG_RTOR_r.all = 0;
  CNFG_RTOR_r.bits.wndw = 0b0011;
  CNFG_RTOR_r.bits.rgain = 0b1111;
  CNFG_RTOR_r.bits.pavg = 0b10;
  CNFG_RTOR_r.bits.ptsf = 0b0011;
  CNFG_RTOR_r.bits.en_rtor = 1;
}

void setupManageCfg(ManageInterrupts_u& MNG_INT_r) {
  MNG_INT_r.all = 0;
  MNG_INT_r.bits.samp_it = 0;
  MNG_INT_r.bits.clr_samp = 1;
  MNG_INT_r.bits.clr_rrint = 1;
  MNG_INT_r.bits.clr_fast = 0;
  MNG_INT_r.bits.efit = 0;
}

void setupInterruptsCfg(EnableInterrupts_u& EN_INT_r) {
  EN_INT_r.all = 0;
  EN_INT_r.bits.intb_type = 0b11;
  EN_INT_r.bits.en_pllint = 0;
  EN_INT_r.bits.en_samp = 0;
  EN_INT_r.bits.en_rrint = 1;
  EN_INT_r.bits.en_loint = 0;
  EN_INT_r.bits.en_dcloffint = 0;
  EN_INT_r.bits.en_fstint = 0;
  EN_INT_r.bits.en_eovf = 1;
  EN_INT_r.bits.en_eint = 1;
}

void setupDynamicCfg(ManageDynamicModes_u& MNG_DYN_r) {
  MNG_DYN_r.all = 0;
  MNG_DYN_r.bits.fast_th = 0;
  MNG_DYN_r.bits.fast = 0;
}