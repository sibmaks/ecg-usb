#define DATA_LINE_SIZE 2048

#define IO_DELAY 50
#define MAX30003_CS_PIN PB0

#include "MAX30003.h"
#include "ECGAdapter.h"

MAX30003 max30003(MAX30003_CS_PIN, IO_DELAY);
ECGAdapter ecgAdapter("MAX30003", 1, "2.0.0", -30000, 45000, 64);

void setup() {
  max30003.begin();

  MAX30003_begin();
}

void loop() {
  ecgAdapter.loop();

  Status_u s = max30003.readStatus();
  if (!s.bits.eint) {
    //interruptsError++;
    delayMicroseconds(IO_DELAY);
    return;
  }
  if (s.bits.eovf) {
    max30003.fifoReset();
    //etagFifoOvf++;
    return;
  }

  byte etag;

  int_least32_t ecgdata = readEcg(etag);

  if (etag == FIFO_VALID_SAMPLE || etag == FIFO_LAST_SAMPLE) {
    if(ecgAdapter.isOutputOn()) {
      ecgAdapter.add(ecgdata);
    }
    //Serial.println(ecgdata);
  } else if (etag == FIFO_OVF) {
    max30003.fifoReset();
    //etagFifoOvf++;
  }
}

int_least32_t readEcg(byte& etag) {
  uint32_t data = max30003.readUint32(Registers_e::ECG_FIFO);
  etag = (data >> 3) & 0x7;
  return (((int_least32_t)((int_least16_t)(data >> 8))) << 2) | ((int_least32_t)(data & 0b11));
}

void MAX30003_begin() {
  max30003.swReset();
  max30003.fifoReset();

  GeneralConfiguration_u CNFG_GEN_r;
  setupGeneralCfg(CNFG_GEN_r);
  max30003.writeRegister(Registers_e::CNFG_GEN, CNFG_GEN_r.all);

  CalConfiguration_u CNFG_CAL_r;
  setupCalCfg(CNFG_CAL_r);
  max30003.writeRegister(Registers_e::CNFG_ALL, CNFG_CAL_r.all);

  MuxConfiguration_u CNFG_MUX_r;
  setupManageCfg(CNFG_MUX_r);
  max30003.writeRegister(Registers_e::CNFG_EMUX, CNFG_MUX_r.all);

  ECGConfiguration_u CNFG_ECG_r;
  setupECGCfg(CNFG_ECG_r);
  max30003.writeRegister(Registers_e::CNFG_ECG, CNFG_ECG_r.all);

  RtoR1Configuration_u CNFG_RTOR_r;
  setupRtoRCfg(CNFG_RTOR_r);
  max30003.writeRegister( Registers_e::CNFG_RTOR1 , CNFG_RTOR_r.all);

  EnableInterrupts_u EN_INT_r;
  setupInterruptsCfg(EN_INT_r);
  max30003.writeRegister( Registers_e::EN_INT , EN_INT_r.all);

  ManageDynamicModes_u MNG_DYN_r;
  setupDynamicCfg(MNG_DYN_r);
  max30003.writeRegister( Registers_e::MNGR_DYN , MNG_DYN_r.all);

  ManageInterrupts_u MNG_INT_r;
  setupManageCfg(MNG_INT_r);
  max30003.writeRegister( Registers_e::MNGR_INT , MNG_INT_r.all);

  max30003.synch();
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