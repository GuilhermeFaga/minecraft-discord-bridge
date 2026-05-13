package com.faga.mcdiscordbridge.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BridgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> DISCORD_API_KEY = BUILDER
            .comment("Discord bot token or env:DISCORD_BOT_TOKEN")
            .define("discordApiKey", "");

    public static final ModConfigSpec.ConfigValue<String> WHITELIST_GUILD_ID = BUILDER
            .comment("Only messages from this guild are accepted")
            .define("whitelistGuildId", "");

    public static final ModConfigSpec.ConfigValue<String> CHAT_CHANNEL_ID = BUILDER
            .comment("Discord channel for chat bridge")
            .define("chatChannelId", "");

    public static final ModConfigSpec.ConfigValue<String> ADMIN_LOG_CHANNEL_ID = BUILDER
            .comment("Discord channel for admin logs")
            .define("adminLogChannelId", "");

    public static final ModConfigSpec.BooleanValue ENABLE_JOIN_LEAVE = BUILDER.define("enableJoinLeave", true);
    public static final ModConfigSpec.BooleanValue ENABLE_DEATHS = BUILDER.define("enableDeaths", true);
    public static final ModConfigSpec.BooleanValue ENABLE_ADVANCEMENTS = BUILDER.define("enableAdvancements", true);
    public static final ModConfigSpec.BooleanValue ENABLE_MINECRAFT_CHAT_TO_DISCORD = BUILDER.define("enableMinecraftChatToDiscord", true);
    public static final ModConfigSpec.BooleanValue ENABLE_DISCORD_CHAT_TO_MINECRAFT = BUILDER.define("enableDiscordChatToMinecraft", true);
    public static final ModConfigSpec.BooleanValue ENABLE_ADMIN_LOGS = BUILDER.define("enableAdminLogs", true);
    public static final ModConfigSpec.BooleanValue ENABLE_MESSAGE_CONTENT_INTENT = BUILDER
            .comment("Enable only if Message Content Intent is allowed for your bot in Discord Developer Portal")
            .define("enableMessageContentIntent", false);

    public static final ModConfigSpec.BooleanValue REDACT_COMMAND_ARGUMENTS = BUILDER.define("redactCommandArguments", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REDACTED_COMMANDS = BUILDER
            .defineListAllowEmpty("redactedCommands", List.of("login", "register", "password", "token", "discord", "op", "deop"), () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private BridgeConfig() {
    }

    public static String resolveToken() {
        String raw = DISCORD_API_KEY.get().trim();
        if (raw.startsWith("env:")) {
            String key = raw.substring("env:".length());
            String env = System.getenv(key);
            return env == null ? "" : env.trim();
        }
        return raw;
    }
}
