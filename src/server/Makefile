# Networks 1 Server Makefile
# Dikabelo Ramashala
# 5th March 2020
# RMSDIK001
JAVAC = javac
JFLAGS = -g
SRC = src/
DOC = doc/
BIN = bin/
JAVADOC = javadoc
SERVER = server

default:
	$(JAVAC) -cp $(JFLAGS) User.java
	$(JAVAC) -cp $(JFLAGS) FileDetails.java
	$(JAVAC) -cp $(JFLAGS) FileOperation.java
	$(JAVAC) -cp $(JFLAGS) Server.java
	$(JAVAC) -cp $(JFLAGS) ServerMain.java

docs:
	$(JAVADOC) -d ../../$(DOC)$(SERVER) ./*.java