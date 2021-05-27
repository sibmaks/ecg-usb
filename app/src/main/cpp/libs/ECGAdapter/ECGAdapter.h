#ifndef ECG_ADAPTER_H_
#define ECG_ADAPTER_H_

#include <Arduino.h>
#include "CommandReader.h"
#include "DataLine.h"

class ECGAdapter {
	private:
		const char* name;
		uint32_t channels;
		const char* version;
		int32_t minValue;
		int32_t maxValue;
		int32_t maxDataToSend;
		
		
		boolean output_on;
		boolean data_sent;
		uint32_t data_sent_id;
		uint32_t data_sent_count;
		unsigned long data_sent_time;
		
		CommandReader commandReader = CommandReader();
	public:
		ECGAdapter(const char* name, uint32_t channels, const char* version, int32_t minValue, int32_t maxValue, int32_t maxDataToSend);
		
		void loop();
		
		boolean isOutputOn() const;
		
		void send(int32_t* values) const;
};

#endif