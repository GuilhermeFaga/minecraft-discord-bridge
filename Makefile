GRADLE=./gradlew
LOCAL_SERVER_DIR=.local-neoforge-server
NEO_VERSION=$(shell sed -n 's/^neo_version=//p' gradle.properties)
NEOFORGE_INSTALLER=neoforge-$(NEO_VERSION)-installer.jar
NEOFORGE_INSTALLER_URL=https://maven.neoforged.net/releases/net/neoforged/neoforge/$(NEO_VERSION)/$(NEOFORGE_INSTALLER)

.PHONY: help setup build run-server run-prod-local local-server-setup clean test jar-info

help:
	@printf "Targets:\n"
	@printf "  make setup      - Verify Java and Gradle wrapper\n"
	@printf "  make build      - Build shaded mod jar\n"
	@printf "  make run-server - Run dedicated dev server\n"
	@printf "  make run-prod-local - Run local production-style server\n"
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

local-server-setup: build
	@mkdir -p "$(LOCAL_SERVER_DIR)"
	@if [ ! -f "$(LOCAL_SERVER_DIR)/$(NEOFORGE_INSTALLER)" ]; then \
		printf "Downloading %s\n" "$(NEOFORGE_INSTALLER_URL)"; \
		curl -fsSL "$(NEOFORGE_INSTALLER_URL)" -o "$(LOCAL_SERVER_DIR)/$(NEOFORGE_INSTALLER)"; \
	fi
	@if ! ls "$(LOCAL_SERVER_DIR)"/libraries/net/neoforged/neoforge/*/unix_args.txt >/dev/null 2>&1; then \
		printf "Installing NeoForge server into %s\n" "$(LOCAL_SERVER_DIR)"; \
		cd "$(LOCAL_SERVER_DIR)" && java -jar "$(NEOFORGE_INSTALLER)" --installServer; \
	fi
	@mkdir -p "$(LOCAL_SERVER_DIR)/mods"
	@cp -f build/libs/mcdiscordbridge-0.1.0.jar "$(LOCAL_SERVER_DIR)/mods/"
	@printf "eula=true\n" > "$(LOCAL_SERVER_DIR)/eula.txt"

run-prod-local: local-server-setup
	@printf "Starting local production-style NeoForge server from %s\n" "$(LOCAL_SERVER_DIR)"
	@cd "$(LOCAL_SERVER_DIR)" && ARGS_FILE=$$(ls libraries/net/neoforged/neoforge/*/unix_args.txt | head -n 1) && java @user_jvm_args.txt @$$ARGS_FILE nogui

test:
	@$(GRADLE) test

clean:
	@$(GRADLE) clean

jar-info:
	@ls -l build/libs || true
