#include "ECGAdapter.h"
#define DATA_APPROVE_AWAITING 50
#ifndef ECG_DEBUG_MODE_H_
#define ECG_DEBUG_MODE_H_ false
#endif

ECGAdapter::ECGAdapter(MobileECG& mobileEcg, const char* version) : 
	mobileEcg(mobileEcg), version(version) {
	Serial.begin(1228800);
	this->output_on = false;
	this->data_sent = false;
	this->data_sent_id = 0;
}

const char* ECGAdapter::loop() {
	if (Serial.available() >= 3) {
	const char* command = commandReader.readCommand();
	if (command == NULL) {
	  return NULL;
	}
	if (strcmp(command, "GET_PARAMETER") == 0) {
	  const char* parameter = NULL;
	  do {
		parameter = commandReader.readCommand();
	  } while (parameter == NULL);
	  if (strcmp(parameter, "MODEL") == 0) {
		Serial.print("\nMDL");
		Serial.print(this->mobileEcg.getName());
	  } else if (strcmp(parameter, "CHANNELS_COUNT") == 0) {
		Serial.print("\nCHC");
		Serial.print(this->mobileEcg.getChannels());
	  } else if (strcmp(parameter, "VERSION") == 0) {
		Serial.print("\nVRS");
		Serial.print(this->version);
	  } else if (strcmp(parameter, "MIN_VALUE") == 0) {
		Serial.print("\nMNV");
		Serial.print(this->mobileEcg.getMinValue());
	  } else if (strcmp(parameter, "MAX_VALUE") == 0) {
		Serial.print("\nMXV");
		Serial.print(this->mobileEcg.getMaxValue());
	  }
	  Serial.print("\nEND");
	} else if (strcmp(command, "ON_DF") == 0) {
	  output_on = true;
	  Serial.print("\nDF1\nEND");
	} else if (strcmp(command, "OFF_DF") == 0) {
	  output_on = false;
	  Serial.print("\nDF0\nEND");
	} else if (strcmp(command, "CONFIGS") == 0) {
	  this->mobileEcg.printConfigs();
	} else {
	  return command;
	}
  }
  return NULL;
}

boolean ECGAdapter::isOutputOn() const {
	return output_on;
}

/* for debug
void ECGAdapter::send(int32_t* values) const {
    for(uint8_t c = 0; c < this->channels; c++) {
		Serial.print(values[c]);
	    Serial.print(",");
    }
	Serial.println();
}*/

void ECGAdapter::send(int32_t* values) const {
    if(!output_on) {
		return;
	}
	  Serial.print("\nDAT");
	  uint8_t channels = this->mobileEcg.getChannels();
	  uint32_t hash = channels;
	  uint8_t size = channels * 4 + 4;
	  Serial.write(size);
	  Serial.write((byte*)values, channels * 4);
      for(uint8_t c = 0; c < channels; c++) {
		hash ^= values[c];
      }
	  Serial.write((byte*)&hash, 4);
}

/* for PC
void ECGAdapter::send(int32_t* values) const {
    if(!output_on) {
		return;
	}
	  Serial.print("\nDAT");
	  uint8_t channels = this->mobileEcg.getChannels();
	  uint8_t size = channels * 4;
	  Serial.write(size);
	  Serial.write((byte*)values, channels * 4);
}*/