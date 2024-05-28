EXE = Serendipity-Dev

all:
	mvn -f ./Serendipity package
	echo "#!/usr/bin/env -S java -jar -Xmx1024m" > $(EXE)
	cat ./Serendipity/target/Serendipity-Test.jar >> $(EXE)
	chmod +x $(EXE)
