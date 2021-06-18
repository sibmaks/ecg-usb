#ifndef ECG_ADAPTER_H_
#define ECG_ADAPTER_H_

#include <Arduino.h>
#include "CommandReader.h"
#include "DataLine.h"
#include "MobileECG.h"

class ECGAdapter {
	private:
		MobileECG& mobileEcg;
		const char* version;
		int32_t maxDataToSend;

		boolean output_on;
		boolean data_sent;
		uint32_t data_sent_id;
		uint32_t data_sent_count;
		unsigned long data_sent_time;
		
		CommandReader commandReader = CommandReader();
	public:
		ECGAdapter(MobileECG& mobileEcg, const char* version, uint8_t maxDataToSend);
		
		const char* loop();
		
		boolean isOutputOn() const;
		
		void send(int32_t* values) const;
};

#endif