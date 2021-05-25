#ifndef DATA_LINE_H_
#define DATA_LINE_H_

#ifndef DATA_LINE_SIZE
#define DATA_LINE_SIZE 1024
#endif

#include <Arduino.h>

template<class T> 
class DataLine
{
	private:
		uint32_t size;
		T values[DATA_LINE_SIZE];
		
	public:
		DataLine() {
			this->size = 0;
		}
		
		bool add(T value) {
			if(this->size != DATA_LINE_SIZE) {
				this->values[size++] = value;
				return true;
			}
			return false;
		}
		
		bool remove(uint32_t index) {
			if(index >= this->size) {
				return false;
			}
			for(uint32_t i = index; i <  this->size; i++) {
				this->values[i] = this->values[i + 1];
			}
			this->size--;
			return true;
		}
		
		bool removes(uint32_t count) {
			if(count > this->size) {
				return false;
			}
			for(uint32_t i = 0; i < this->size - count; i++) {
				this->values[i] = this->values[i + count];
			}
			this->size -= count;
			return true;
		}
		
		uint32_t getSize() const {
			return this->size;
		}
			
		const T* getValues() const {
			return this->values;
		}
			
		void clear() {
			this->size = 0;
		}

		bool hasSpace(uint32_t space) const {
		    return (this->size + space) <= DATA_LINE_SIZE;
		}
};

#endif