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
		boolean outputOn;
		CommandReader commandReader = CommandReader();
	public:
		ECGAdapter(MobileECG& mobileEcg, const char* version);
		
		const char* loop();
		
		boolean isOutputOn() const;
		
		void send(int32_t* values) const;
};

#endif