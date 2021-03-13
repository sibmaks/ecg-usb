#ifndef COMMAND_READER_H_
#define COMMAND_READER_H_

class CommandReader {
	private:
		unsigned int maxLength;
		char* command;
		unsigned int commandLength;
		
		void clearCommand();
	public:
		CommandReader(unsigned int maxLength = 32);
		
		const char* readCommand();
		
		~CommandReader();
};

#endif