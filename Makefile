GRADLE=./gradlew

.PHONY: help setup build run-server clean test jar-info

help:
	@printf "Targets:\n"
	@printf "  make setup      - Verify Java and Gradle wrapper\n"
	@printf "  make build      - Build shaded mod jar\n"
	@printf "  make run-server - Run dedicated dev server\n"
	@printf "  make test       - Run tests\n"
	@printf "  make clean      - Clean build artifacts\n"
	@printf "  make jar-info   - List jars in build/libs\n"

setup:
	@java -version
	@$(GRADLE) --version

build:
	@$(GRADLE) clean build

run-server:
	@$(GRADLE) runServer

test:
	@$(GRADLE) test

clean:
	@$(GRADLE) clean

jar-info:
	@ls -l build/libs || true
