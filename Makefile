EXE = Serendipity-Dev

all:
ifdef EVALFILE
	cp $(EVALFILE) ./Serendipity/src/main/resources/embedded.nnue
endif
ifdef JAVA_HOME
	JAVA_HOME=$(JAVA_HOME) mvn -f ./Serendipity/pom.xml package
else
	mvn -f ./Serendipity/pom.xml package
endif
	cat header.sh ./Serendipity/target/Serendipity-Test.jar > $(EXE)
	chmod +x $(EXE)
