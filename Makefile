NAME = Serendipity

all:
	mvn -f ./Serendipity package
	cp ./Serendipity/target/Serendipity-Stable.jar ./$(NAME).jar
