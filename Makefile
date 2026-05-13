GRADLE=./gradlew
LOCAL_SERVER_DIR=.local-neoforge-server
DEFAULT_MC_VERSION=$(shell sed -n 's/^minecraft_version=//p' gradle.properties)
DEFAULT_NEO_VERSION=$(shell sed -n 's/^neo_version=//p' gradle.properties)
MOD_VERSION=$(shell sed -n 's/^mod_version=//p' gradle.properties)
MC_VERSION?=$(DEFAULT_MC_VERSION)
NEO_VERSION?=$(strip $(shell case "$(MC_VERSION)" in \
1.21.1) printf "21.1.229" ;; \
1.21.2) printf "21.2.1-beta" ;; \
1.21.3) printf "21.3.96" ;; \
1.21.4) printf "21.4.157" ;; \
1.21.5) printf "21.5.97" ;; \
1.21.6) printf "21.6.20-beta" ;; \
1.21.7) printf "21.7.25-beta" ;; \
1.21.8) printf "21.8.53" ;; \
1.21.9) printf "21.9.16-beta" ;; \
1.21.10) printf "21.10.64" ;; \
1.21.11) printf "21.11.42" ;; \
*) printf "" ;; \
esac))
SUPPORTED_MATRIX=1.21.1:21.1.229 1.21.2:21.2.1-beta 1.21.3:21.3.96 1.21.4:21.4.157 1.21.5:21.5.97 1.21.6:21.6.20-beta 1.21.7:21.7.25-beta 1.21.8:21.8.53 1.21.9:21.9.16-beta 1.21.10:21.10.64 1.21.11:21.11.42
NEOFORGE_INSTALLER=neoforge-$(NEO_VERSION)-installer.jar
NEOFORGE_INSTALLER_URL=https://maven.neoforged.net/releases/net/neoforged/neoforge/$(NEO_VERSION)/$(NEOFORGE_INSTALLER)
LOCAL_SERVER_VERSION_DIR=$(LOCAL_SERVER_DIR)/mc$(MC_VERSION)
MOD_JAR=build/libs/mcdiscordbridge-$(MOD_VERSION).jar

.PHONY: help setup build build-one build-all run-server run-prod-local run-local-one local-server-setup clean test jar-info validate-version-selection

help:
	@printf "Targets:\n"
	@printf "  make setup         - Verify Java and Gradle wrapper\n"
	@printf "  make build         - Build one version (uses MC_VERSION/NEO_VERSION)\n"
	@printf "  make build-one     - Build one version (uses MC_VERSION/NEO_VERSION)\n"
	@printf "  make build-all     - Build all supported MC/NeoForge pairs\n"
	@printf "  make run-server    - Run dedicated dev server\n"
	@printf "  make run-prod-local - Run local production-style server (one version)\n"
	@printf "  make run-local-one - Run local production-style server (one version)\n"
	@printf "  make test          - Run tests\n"
	@printf "  make clean         - Clean build artifacts\n"
	@printf "  make jar-info      - List jars in build/libs\n"
	@printf "Variables:\n"
	@printf "  MC_VERSION=%s (default)\n" "$(DEFAULT_MC_VERSION)"
	@printf "  NEO_VERSION=auto from MC_VERSION (override optional)\n"

validate-version-selection:
	@if [ -z "$(NEO_VERSION)" ]; then \
		printf "Unsupported MC_VERSION '%s'. Supported: %s\n" "$(MC_VERSION)" "$(SUPPORTED_MATRIX)"; \
		exit 1; \
	fi

setup:
	@java -version
	@$(GRADLE) --version

build: build-one

build-one: validate-version-selection
	@printf "Building for MC %s with NeoForge %s\n" "$(MC_VERSION)" "$(NEO_VERSION)"
	@$(GRADLE) clean build \
		-Pminecraft_version=$(MC_VERSION) \
		-Pminecraft_version_range='[1.21,1.22)' \
		-Pneo_version=$(NEO_VERSION)

build-all:
	@set -e; \
	for pair in $(SUPPORTED_MATRIX); do \
		mc=$${pair%%:*}; \
		neo=$${pair##*:}; \
		printf "Building for MC %s with NeoForge %s\n" "$$mc" "$$neo"; \
		$(GRADLE) clean build \
			-Pminecraft_version=$$mc \
			-Pminecraft_version_range='[1.21,1.22)' \
			-Pneo_version=$$neo; \
	done

run-server:
	@$(GRADLE) runServer

local-server-setup: build-one validate-version-selection
	@mkdir -p "$(LOCAL_SERVER_VERSION_DIR)"
	@if [ ! -f "$(LOCAL_SERVER_VERSION_DIR)/$(NEOFORGE_INSTALLER)" ]; then \
		printf "Downloading %s\n" "$(NEOFORGE_INSTALLER_URL)"; \
		curl -fsSL "$(NEOFORGE_INSTALLER_URL)" -o "$(LOCAL_SERVER_VERSION_DIR)/$(NEOFORGE_INSTALLER)"; \
	fi
	@if ! ls "$(LOCAL_SERVER_VERSION_DIR)"/libraries/net/neoforged/neoforge/*/unix_args.txt >/dev/null 2>&1; then \
		printf "Installing NeoForge server into %s\n" "$(LOCAL_SERVER_VERSION_DIR)"; \
		cd "$(LOCAL_SERVER_VERSION_DIR)" && java -jar "$(NEOFORGE_INSTALLER)" --installServer; \
	fi
	@if [ ! -f "$(MOD_JAR)" ]; then \
		printf "Expected mod jar not found: %s\n" "$(MOD_JAR)"; \
		exit 1; \
	fi
	@mkdir -p "$(LOCAL_SERVER_VERSION_DIR)/mods"
	@cp -f "$(MOD_JAR)" "$(LOCAL_SERVER_VERSION_DIR)/mods/"
	@printf "eula=true\n" > "$(LOCAL_SERVER_VERSION_DIR)/eula.txt"

run-prod-local: run-local-one

run-local-one: local-server-setup
	@printf "Starting local production-style NeoForge server from %s\n" "$(LOCAL_SERVER_VERSION_DIR)"
	@cd "$(LOCAL_SERVER_VERSION_DIR)" && ARGS_FILE=$$(ls libraries/net/neoforged/neoforge/*/unix_args.txt | head -n 1) && java @user_jvm_args.txt @$$ARGS_FILE nogui

test:
	@$(GRADLE) test

clean:
	@$(GRADLE) clean

jar-info:
	@ls -l build/libs || true
