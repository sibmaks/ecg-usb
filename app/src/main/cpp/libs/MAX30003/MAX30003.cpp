#include "MAX30003.h"
#include <SPI.h>

MAX30003::MAX30003(int csPin, int ioDelay) : csPin(csPin), ioDelay(ioDelay)
{
}

void MAX30003::switchClock(bool on)
{
	digitalWrite(csPin, on ? LOW : HIGH);
	delayMicroseconds(ioDelay);
}

void MAX30003::begin()
{
	pinMode(csPin, OUTPUT);
	digitalWrite(csPin, HIGH);

	SPI.begin();
	SPI.setBitOrder(MSBFIRST);
	SPI.setDataMode(SPI_MODE0);
}

void MAX30003::swReset()
{
	this->writeRegister(Registers_e::SW_RST, 0x000000);
}

void MAX30003::synch()
{
	this->writeRegister(Registers_e::SYNCH, 0x000000);
}

void MAX30003::fifoReset()
{
	this->writeRegister(Registers_e::FIFO_RST, 0x000000);
}

void MAX30003::writeRegister(const Registers_e address, unsigned long data)
{
	byte dataToSend = (address << 1) | WREG;

	this->switchClock(true);

	SPI.transfer(dataToSend);
	SPI.transfer(data >> 16);
	SPI.transfer(data >> 8);
	SPI.transfer(data);

	this->switchClock(false);
}

void MAX30003::readConfig(const Registers_e address, uint32_t& all) {
  all = this->readUint32(address);
}

uint8_t *MAX30003::readRegister(const Registers_e address)
{
	this->switchClock(true);

	uint8_t SPI_TX_Buff = (address << 1) | RREG;
	SPI.transfer(SPI_TX_Buff);

	for (int i = 0; i < 3; i++)
	{
		buff[i] = SPI.transfer(0xff);
	}
	this->switchClock(false);

	return buff;
}

uint32_t MAX30003::readUint32(const Registers_e address) {
  uint8_t* buff = this->readRegister(address);

  uint32_t data = (uint32_t)buff[0];
  data <<= 8;
  data |= ((uint32_t)buff[1]);
  data <<= 8;
  data |= ((uint32_t)buff[2]);

  return data;
}

Status_u MAX30003::readStatus() {
  return Status_u{ this->readUint32(Registers_e::STATUS) };
}