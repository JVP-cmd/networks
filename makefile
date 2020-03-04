# A makefile

JC = javac
JFLAGS = -g
BINDIR=./bin
SRCDIR=./src
DOCDIR=./javadocs

.SUFFIXES: .java .class

${BINDIR}/%.class: ${SRCDIR}/%.java
	javac $< -cp ${BINDIR} -d ${BINDIR}

${BINDIR}/SeverMain.class: ${BINDIR}/Client.class ${BINDIR}/ClientMain.class ${BINDIR}/Server.class ${SRCDIR}/ServerMain.java


clean:
	rm -f ${BINDIR}/*.class

run:
	java -cp ./bin ServerMain

docs:
	javadoc  -classpath ${BINDIR} -d ${DOCDIR} ${SRCDIR}/*.java

cleandocs:
	rm -rf ${DOCDIR}/*

#Code From:
#www.cs.swarthmore.edu
