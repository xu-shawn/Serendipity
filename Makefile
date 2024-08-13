EXE = Serendipity-Dev

all:
ifdef EVALFILE
	cp $(EVALFILE) ./Serendipity/src/main/resources/embedded.nnue
endif
	mvn -f ./Serendipity/pom.xml package
	echo "#!/usr/bin/env bash" > $(EXE)
	echo 'java -jar -Xmx128m $(shell pwd)/Serendipity/target/Serendipity-Test.jar "$$@"' >> $(EXE)
	chmod +x $(EXE)
