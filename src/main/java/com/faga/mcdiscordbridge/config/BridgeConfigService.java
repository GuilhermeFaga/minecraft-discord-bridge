package com.faga.mcdiscordbridge.config;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class BridgeConfigService {
    private static volatile MinecraftServer server;

    private BridgeConfigService() {
    }

    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    public static Optional<String> updateChatChannel(String channelId, String guildId) {
        return updateChannel("chatChannelId", channelId, guildId);
    }

    public static Optional<String> updateAdminChannel(String channelId, String guildId) {
        return updateChannel("adminLogChannelId", channelId, guildId);
    }

    public static Optional<String> updateWhitelistGuild(String guildId) {
        String trimmed = guildId == null ? "" : guildId.trim();
        if (!trimmed.matches("\\d{10,}")) {
            return Optional.of("Guild ID must be numeric.");
        }
        BridgeConfig.WHITELIST_GUILD_ID.set(trimmed);
        try {
            persistCommonToml("whitelistGuildId", trimmed);
        } catch (IOException e) {
            DiscordBridgeMod.LOGGER.error("Failed to persist whitelistGuildId", e);
            return Optional.of("Saved in memory, but failed to persist to disk.");
        }
        return Optional.empty();
    }

    private static Optional<String> updateChannel(String key, String channelId, String guildId) {
        String trimmed = channelId == null ? "" : channelId.trim();
        if (!trimmed.matches("\\d{10,}")) {
            return Optional.of("Channel ID must be numeric.");
        }
        String whitelistGuild = BridgeConfig.WHITELIST_GUILD_ID.get().trim();
        if (!whitelistGuild.isBlank() && !whitelistGuild.equals(guildId)) {
            return Optional.of("This guild is not whitelisted for setup.");
        }

        if ("chatChannelId".equals(key)) {
            BridgeConfig.CHAT_CHANNEL_ID.set(trimmed);
        } else {
            BridgeConfig.ADMIN_LOG_CHANNEL_ID.set(trimmed);
        }

        try {
            persistCommonToml(key, trimmed);
        } catch (IOException e) {
            DiscordBridgeMod.LOGGER.error("Failed to persist {}", key, e);
            return Optional.of("Saved in memory, but failed to persist to disk.");
        }
        return Optional.empty();
    }

    private static void persistCommonToml(String key, String value) throws IOException {
        MinecraftServer currentServer = server;
        if (currentServer == null) {
            return;
        }
        Path configPath = currentServer.getServerDirectory()
                .resolve("config")
                .resolve(DiscordBridgeMod.MOD_ID + "-common.toml");
        Files.createDirectories(configPath.getParent());

        List<String> lines = Files.exists(configPath)
                ? Files.readAllLines(configPath, StandardCharsets.UTF_8)
                : new ArrayList<>();

        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith(key + " = ") || line.startsWith(key + "=")) {
                lines.set(i, key + " = \"" + value + "\"");
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lines.add(key + " = \"" + value + "\"");
        }
        Files.writeString(configPath, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }
}
