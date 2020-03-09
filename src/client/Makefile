# Networks 1 Client Makefile
# Dikabelo Ramashala
# 5th March 2020
# RMSDIK001

JAVAC = javac
JFLAGS = -g
SRC = src/
DOC = doc/
BIN = bin/
JAVADOC = javadoc
CLIENT = client

default:
	$(JAVAC) -cp $(JFLAGS) ./*.java
	
docs:
	$(JAVADOC) -d ../../$(DOC)$(CLIENT) ./*.java
	
clean:
	rm -f *.class