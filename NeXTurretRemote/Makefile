CXX = g++
CXXFLAGS = -W -Wall -ansi -pedantic
LDFLAGS = -lusb-1.0
EXEC = remote
SRC = 
OBJ = $(SRC:.cpp=.o)

all: $(EXEC)

remote: nxt.cpp main.cpp
	$(CXX) -o $@ $^ $(LDFLAGS)
	
main.o : nxt.h
	
%.o: %.cpp
	$(CXX) -o $@ -c $< $(CXXFLAGS)
	
clean:
	rm -rf *.o
	
mrproper: clean
	rm -rf $(EXEC)