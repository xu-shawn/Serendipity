EXE = Serendipity-Dev
EMBEDDED_NET_LOCATION = ./Serendipity/src/main/resources/embedded.nnue
NET_NAME = net1.nnue
ifndef MAVEN_EXE
	MAVEN_EXE = mvn
endif

all:
ifdef EVALFILE
	cp $(EVALFILE) $(EMBEDDED_NET_LOCATION)
else
	$(MAKE) net
endif
ifdef JAVA_HOME
	JAVA_HOME=$(JAVA_HOME) $(MAVEN_EXE) -f ./Serendipity/pom.xml package
else
	$(MAVEN_EXE) -f ./Serendipity/pom.xml package
endif
	cat header.sh ./Serendipity/target/Serendipity-Test.jar > $(EXE)
	chmod +x $(EXE)
	cp ./Serendipity/target/Serendipity-Test.jar Serendipity.jar
net:
	wget -O $(EMBEDDED_NET_LOCATION) https://github.com/xu-shawn/Serendipity-Networks/raw/main/$(NET_NAME)
