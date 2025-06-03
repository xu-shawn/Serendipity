# This file is part of Serendipity, an UCI chess engine written in Java.
#
# Copyright (C) 2024-2025  Shawn Xu <shawn@shawnxu.org>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

EXE ?= Serendipity-Dev
EMBEDDED_NET_LOCATION := ./Serendipity/src/main/resources/embedded.nnue
NET_NAME := net1.nnue
MAVEN_EXE ?= mvn

MAVEN_COMMAND_PREFIX :=
ifdef JAVA_HOME
    MAVEN_COMMAND_PREFIX := JAVA_HOME=$(JAVA_HOME)
endif

.PHONY: all net

all: net
	$(MAVEN_COMMAND_PREFIX) $(MAVEN_EXE) -f ./Serendipity/pom.xml package
	cat header.sh ./Serendipity/target/Serendipity-Test.jar > $(EXE)
	chmod +x $(EXE)
	cp ./Serendipity/target/Serendipity-Test.jar Serendipity.jar

net:
	mkdir -p $(dir $(EMBEDDED_NET_LOCATION))
ifdef EVALFILE
	cp $(EVALFILE) $(EMBEDDED_NET_LOCATION)
else
	wget -O $(EMBEDDED_NET_LOCATION) https://github.com/xu-shawn/Serendipity-Networks/raw/main/$(NET_NAME)
