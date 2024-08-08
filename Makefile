EXE = Serendipity-Dev

all:
ifdef EVALFILE
	cp $(EVALFILE) ./Serendipity/src/main/resources/embedded.nnue
endif
	mvn -f ./Serendipity/pom.xml package
	echo "#!/usr/bin/java -jar" > $(EXE)
	cat ./Serendipity/target/Serendipity-Test.jar >> $(EXE)
	chmod +x $(EXE)
	./$(EXE) bench
