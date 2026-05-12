package com.faga.mcdiscordbridge.util;

import com.faga.mcdiscordbridge.config.BridgeConfig;

public final class CommandRedactor {
    private CommandRedactor() {
    }

    public static String redact(String commandInput) {
        if (!BridgeConfig.REDACT_COMMAND_ARGUMENTS.get()) {
            return commandInput;
        }
        String trimmed = commandInput.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        String[] parts = withoutSlash.split("\\s+");
        if (parts.length == 0) {
            return trimmed;
        }
        String root = parts[0].toLowerCase();
        for (String cmd : BridgeConfig.REDACTED_COMMANDS.get()) {
            if (root.equals(cmd.toLowerCase())) {
                return "/" + root + " ...";
            }
        }
        return commandInput;
    }
}
