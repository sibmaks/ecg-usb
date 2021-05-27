#include "ECGAdapter.h"
#define DATA_APPROVE_AWAITING 50
#ifndef ECG_DEBUG_MODE_H_
#define ECG_DEBUG_MODE_H_ false
#endif

ECGAdapter::ECGAdapter(const char* name, uint32_t channels, const char* version, int32_t minValue, int32_t maxValue, int32_t maxDataToSend) : 
	name(name), channels(channels), version(version), minValue(minValue),
	maxValue(maxValue), maxDataToSend(maxDataToSend) {
	Serial.begin(460800);
	this->output_on = false;
	this->data_sent = false;
	this->data_sent_id = 0;
}

void ECGAdapter::loop() {
	if (Serial.available() >= 3) {
	const char* command = commandReader.readCommand();
	if (command == NULL) {
	  return;
	}
	if (strcmp(command, "GET_PARAMETER") == 0) {
	  const char* parameter = NULL;
	  do {
		parameter = commandReader.readCommand();
	  } while (parameter == NULL);
	  if (strcmp(parameter, "MODEL") == 0) {
		Serial.print("\nMODEL ");
		Serial.print(this->name);
	  } else if (strcmp(parameter, "CHANNELS_COUNT") == 0) {
		Serial.print("\nCHS_CT ");
		Serial.print(this->channels);
	  } else if (strcmp(parameter, "VERSION") == 0) {
		Serial.print("\nVERSION ");
		Serial.print(this->version);
	  } else if (strcmp(parameter, "MIN_VALUE") == 0) {
		Serial.print("\nMIN_VALUE ");
		Serial.print(this->minValue);
	  } else if (strcmp(parameter, "MAX_VALUE") == 0) {
		Serial.print("\nMAX_VALUE ");
		Serial.print(this->maxValue);
	  } else if (strcmp(parameter, "MAX_DATA_TO_SEND") == 0) {
		Serial.print("\nMAX_DTS ");
		Serial.print(this->maxDataToSend);
	  }
	  Serial.print("\nEND");
	} else if (strcmp(command, "ON_DF") == 0) {
	  output_on = true;
	  Serial.print("\nDF_ON \nEND");
	} else if (strcmp(command, "OFF_DF") == 0) {
	  output_on = false;
	  Serial.print("\nDF_OFF \nEND");
	}
  }
}

boolean ECGAdapter::isOutputOn() const {
	return output_on;
}

#if ECG_DEBUG_MODE_H_
void ECGAdapter::send(int32_t* values) const {
	  uint32_t hash = this->channels;
      for(uint32_t c = 0; c < this->channels; c++) {
	    Serial.print((values[c]);
	    Serial.print(",");
		hash ^= values[c];
      }
	  Serial.println();
}
#else
void ECGAdapter::send(int32_t* values) const {
	  Serial.print("\nDATA ");
	  uint32_t hash = this->channels;
      for(uint32_t c = 0; c < this->channels; c++) {
	    Serial.write((byte*)(values + c), 4);
		hash ^= values[c];
      }
	  Serial.write((byte*)&hash, 4);
	  Serial.print("\nEND");
}
#endif