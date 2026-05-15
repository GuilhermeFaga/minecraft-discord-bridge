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
    public static final ModConfigSpec.BooleanValue ENABLE_EMBEDS = BUILDER
            .comment("Send Discord embeds for richer event logs")
            .define("enableEmbeds", true);
    public static final ModConfigSpec.ConfigValue<String> EMBED_COLOR_HEX = BUILDER
            .comment("Hex color for embeds, for example #57A5FF")
            .define("embedColorHex", "#57A5FF");
    public static final ModConfigSpec.BooleanValue INCLUDE_PLAYER_HEAD_IN_EMBEDS = BUILDER
            .comment("Include player head thumbnail in embeds when UUID is available")
            .define("includePlayerHeadInEmbeds", true);
    public static final ModConfigSpec.ConfigValue<String> PLAYER_HEAD_URL_TEMPLATE = BUILDER
            .comment("URL template with %uuid% placeholder")
            .define("playerHeadUrlTemplate", "https://crafatar.com/avatars/%uuid%?size=128&overlay");
    public static final ModConfigSpec.BooleanValue ENABLE_BOT_ACTIVITY_ROTATION = BUILDER
            .comment("Rotate bot activity text from botActivities")
            .define("enableBotActivityRotation", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOT_ACTIVITIES = BUILDER
            .defineListAllowEmpty("botActivities", List.of("Watching the server", "Bridging chat", "Tracking events"), () -> "", o -> o instanceof String);
    public static final ModConfigSpec.IntValue BOT_ACTIVITY_ROTATE_SECONDS = BUILDER
            .defineInRange("botActivityRotateSeconds", 20, 5, 3600);
    public static final ModConfigSpec.ConfigValue<String> BOT_ACTIVITY_TYPE = BUILDER
            .comment("PLAYING, WATCHING, LISTENING, COMPETING")
            .define("botActivityType", "WATCHING");
    public static final ModConfigSpec.IntValue DAY_MILESTONE_GAP = BUILDER
            .comment("Send day milestone message every N minecraft days (0 disables)")
            .defineInRange("dayMilestoneGap", 0, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue ENABLE_ACCOUNT_LINKING = BUILDER
            .comment("Enable Discord account linking to Minecraft players")
            .define("enableAccountLinking", true);
    public static final ModConfigSpec.IntValue LINK_CODE_EXPIRY_SECONDS = BUILDER
            .comment("How long link codes remain valid")
            .defineInRange("linkCodeExpirySeconds", 600, 30, 86400);
    public static final ModConfigSpec.IntValue LINK_CODE_LENGTH = BUILDER
            .comment("Length of one-time account link code")
            .defineInRange("linkCodeLength", 6, 4, 12);

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
