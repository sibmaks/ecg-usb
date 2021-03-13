#include "CommandReader.h"
#include <Arduino.h>

CommandReader::CommandReader(unsigned int maxLength) : maxLength(maxLength) {
	command = new char[maxLength];
}

CommandReader::~CommandReader() {
	delete command;
}

void CommandReader::clearCommand() {
  commandLength = 0;
  for (int i = 0; i < maxLength; i++) {
    command[i] = '\0';
  }
}

const char* CommandReader::readCommand() {
    int c;
    do {
      c = Serial.read();
    } while (isSpace(c));
    if (c == -1) {
      return NULL;
    }
    clearCommand();
    command[commandLength++] = c;

    while (!isSpace((c = Serial.read()))) {
      if (c == -1) {
        break;
      }
      command[commandLength++] = c;
      if (commandLength >= maxLength - 1) {
        Serial.print("Command is too long: ");
        Serial.print(command);
        while (!isSpace(c = Serial.read()) && c != -1) {
          Serial.print((char) c);
        }
        Serial.println();
        return NULL;
      }
	}
	return command;
}