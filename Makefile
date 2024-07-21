EXE = Serendipity-Dev

all:
ifdef EVALFILE
	cp $(EVALFILE) ./Serendipity/src/main/resources/embedded.nnue
endif
	mvn -f ./Serendipity package
	echo "#!/usr/bin/env -S java -jar -Xmx128m" > $(EXE)
	cat ./Serendipity/target/Serendipity-Test.jar >> $(EXE)
	chmod +x $(EXE)
