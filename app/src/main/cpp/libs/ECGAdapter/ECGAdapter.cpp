#include "ECGAdapter.h"
#define DATA_APPROVE_AWAITING 75

ECGAdapter::ECGAdapter(const char* name, uint32_t channels, const char* version, int32_t minValue, int32_t maxValue, int32_t maxDataToSend) : 
	name(name), channels(channels), version(version), minValue(minValue),
	maxValue(maxValue), maxDataToSend(maxDataToSend) {
	Serial.begin(460800);
	this->output_on = false;
	this->data_sent = false;
	this->data_sent_id = 0;
}

void ECGAdapter::loop() {
	if (Serial.available()) {
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
	  dataLine.clear();
	  Serial.print("\nDF_OFF \nEND");
	} else if (strcmp(command, "DA_RD") == 0) {
	  const char* id_line = NULL;
	  do {
		id_line = commandReader.readCommand();
	  } while (id_line == NULL);
	  if (data_sent_id >= atoi(id_line)) {
		data_sent = false;
		dataLine.removes(this->channels * this->data_sent_count);
		Serial.print("\nDR_OK ");
	  } else {
		Serial.print("\nDR_II");
		Serial.print(data_sent_id);
		Serial.print(" ");
		Serial.print(id_line);
	  }
	  Serial.print("\nEND");
	}
  }
  if (dataLine.getSize() > 0) {
	long t;
	uint32_t s = dataLine.getSize() / 3;
	if (!data_sent || ((t = millis()) - data_sent_time) >= DATA_APPROVE_AWAITING) {
	  if (!data_sent) {
		data_sent_id++;
	  }

	  data_sent_count = min(s, (uint32_t)this->maxDataToSend);
	  const int* vals = dataLine.getValues();
	  Serial.print("\nDATA ");
	  uint32_t hash = data_sent_id;
	  hash ^= data_sent_count;
	  Serial.write((byte*)&data_sent_id, 4);
	  Serial.write((byte*)&data_sent_count, 4);
		//	Serial.print(data_sent_id);
			//Serial.print(" ");
			//Serial.print(data_sent_count);
			//Serial.print(" ");
	  for (uint32_t i = 0; i < data_sent_count; i++) {
		  for(uint32_t c = 0; c < this->channels; c++) {
			Serial.write((byte*)(vals + i * this->channels + c), 4);
			//Serial.print(vals[i][c]);
			//Serial.print(" ");
			hash ^= vals[i * this->channels + c];
		  }
	  }
			//Serial.print(hash);
			//Serial.print(" ");
	  Serial.write((byte*)&hash, 4);
	  Serial.print("\nEND");
	  data_sent_time = t;
	  data_sent = true;
	}
  }
}

boolean ECGAdapter::isOutputOn() const {
	return output_on;
}

void ECGAdapter::add(int value) {
	dataLine.add(value);
}


const DataLine<int>* ECGAdapter::getDataLine() const {
    return &(this->dataLine);
}