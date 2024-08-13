EXE = Serendipity-Dev

all:
ifdef EVALFILE
	cp $(EVALFILE) ./Serendipity/src/main/resources/embedded.nnue
endif
	mvn -f ./Serendipity/pom.xml package
	cat header.sh ./Serendipity/target/Serendipity-Test.jar > $(EXE)
	chmod +x $(EXE)
