package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.config.BridgeConfigService;
import com.faga.mcdiscordbridge.link.LinkedAccount;
import com.faga.mcdiscordbridge.link.DiscordLinkService;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DiscordBot {
    private JDA jda;
    private MinecraftServer server;
    private final DiscordActivityRotator activityRotator = new DiscordActivityRotator();
    private final DiscordLinkService linkService = new DiscordLinkService();
    private final List<PendingMessage> pendingMessages = new ArrayList<>();

    public void start(MinecraftServer minecraftServer) {
        this.server = minecraftServer;
        BridgeConfigService.setServer(minecraftServer);
        linkService.initialize(minecraftServer);
        DiscordBridgeMod.LOGGER.info("Bridge config whitelistGuildId={}, chatChannelId={}, adminLogChannelId={}",
                BridgeConfig.WHITELIST_GUILD_ID.get(),
                BridgeConfig.CHAT_CHANNEL_ID.get(),
                BridgeConfig.ADMIN_LOG_CHANNEL_ID.get());
        String token = BridgeConfig.resolveToken();
        if (token.isBlank()) {
            DiscordBridgeMod.LOGGER.warn("Discord bridge disabled: missing discordApiKey");
            return;
        }
        try {
            JDABuilder builder = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .addEventListeners(new DiscordMessageHandler(this));
            this.jda = builder.build();
            this.jda.addEventListener(new DiscordReadyListener(this));
            DiscordBridgeMod.LOGGER.info("Discord bot starting");
        } catch (NoClassDefFoundError e) {
            DiscordBridgeMod.LOGGER.error("Discord bridge disabled: missing runtime dependency ({}).", e.getMessage());
        } catch (Exception e) {
            DiscordBridgeMod.LOGGER.error("Discord bot failed to start", e);
        }
    }

    public void stop() {
        activityRotator.stop();
        synchronized (pendingMessages) {
            pendingMessages.clear();
        }
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
    }

    public MinecraftServer getServer() {
        return server;
    }

    public JDA getJda() {
        return jda;
    }

    public DiscordLinkService getLinkService() {
        return linkService;
    }

    public void sendChatMessage(String text) {
        sendToChannel(BridgeConfig.CHAT_CHANNEL_ID.get(), text, null);
    }

    public void sendChatMessage(String text, DiscordEmbedPayload payload) {
        sendToChannel(BridgeConfig.CHAT_CHANNEL_ID.get(), text, withoutDescription(payload));
    }

    public void sendAdminMessage(String text) {
        if (BridgeConfig.ENABLE_ADMIN_LOGS.get()) {
            sendToChannel(BridgeConfig.ADMIN_LOG_CHANNEL_ID.get(), text, null);
        }
    }

    public void sendAdminMessage(String text, DiscordEmbedPayload payload) {
        if (BridgeConfig.ENABLE_ADMIN_LOGS.get()) {
            sendToChannel(BridgeConfig.ADMIN_LOG_CHANNEL_ID.get(), text, payload);
        }
    }

    public void onReady() {
        if (jda != null) {
            activityRotator.start(jda, server);
            registerSlashCommands();
            flushPendingMessages();
        }
    }

    private void flushPendingMessages() {
        List<PendingMessage> copy;
        synchronized (pendingMessages) {
            if (pendingMessages.isEmpty()) {
                return;
            }
            copy = new ArrayList<>(pendingMessages);
            pendingMessages.clear();
        }
        for (PendingMessage msg : copy) {
            sendToChannel(msg.channelId(), msg.text(), msg.payload());
        }
    }

    private void registerSlashCommands() {
        var linkCommand = Commands.slash("link", "Generate one-time code to link Discord to Minecraft");
        OptionData categoryOption = new OptionData(OptionType.STRING, "category", "Leaderboard category", true)
                .addChoices(
                        new Choice("playtime", "playtime"),
                        new Choice("deaths", "deaths"),
                        new Choice("player_kills", "player_kills"),
                        new Choice("mob_kills", "mob_kills"),
                        new Choice("mined_blocks", "mined_blocks"),
                        new Choice("distance_walked", "distance_walked")
                );
        OptionData pageOption = new OptionData(OptionType.INTEGER, "page", "Page number (default 1)", false).setMinValue(1);
        OptionData visibilityOption = new OptionData(OptionType.STRING, "visibility", "Leaderboard visibility (private by default)", false)
                .addChoices(
                        new Choice("private", "private"),
                        new Choice("public", "public")
                );
        var leaderboardCommand = Commands.slash("leaderboard", "Show Minecraft leaderboard by category")
                .addOptions(categoryOption, pageOption, visibilityOption);
        OptionData mcMessageOption = new OptionData(OptionType.STRING, "message", "Message to relay to Minecraft", true);
        var mcCommand = Commands.slash("mc", "Send a message to Minecraft chat")
                .addOptions(mcMessageOption);
        var configCommand = Commands.slash("config", "Configure Discord bridge channels and whitelist guild")
                .addOption(OptionType.STRING, "chat_channel_id", "Bridge chat channel ID", false)
                .addOption(OptionType.STRING, "admin_channel_id", "Admin log channel ID", false)
                .addOption(OptionType.STRING, "whitelist_guild_id", "Guild ID allowed to use slash commands", false);

        String guildId = BridgeConfig.WHITELIST_GUILD_ID.get().trim();
        if (!guildId.isBlank() && jda.getGuildById(guildId) != null) {
            var guild = jda.getGuildById(guildId);
            guild.upsertCommand(linkCommand).queue();
            guild.upsertCommand(leaderboardCommand).queue();
            guild.upsertCommand(mcCommand).queue();
            guild.upsertCommand(configCommand).queue();
            return;
        }
        jda.upsertCommand(linkCommand).queue();
        jda.upsertCommand(leaderboardCommand).queue();
        jda.upsertCommand(mcCommand).queue();
        jda.upsertCommand(configCommand).queue();
    }

    private void sendToChannel(String channelId, String text, DiscordEmbedPayload payload) {
        if (jda == null || channelId == null || channelId.isBlank()) {
            return;
        }
        String trimmedChannelId = channelId.trim();
        TextChannel channel = jda.getTextChannelById(trimmedChannelId);
        if (channel == null) {
            if (jda.getStatus() != Status.CONNECTED) {
                synchronized (pendingMessages) {
                    pendingMessages.add(new PendingMessage(trimmedChannelId, text, payload));
                }
                DiscordBridgeMod.LOGGER.info("Discord not ready yet; queued message for channel {}", trimmedChannelId);
            } else {
                DiscordBridgeMod.LOGGER.warn("Configured Discord channel not found: {}", trimmedChannelId);
            }
            return;
        }
        if (!channel.canTalk()) {
            DiscordBridgeMod.LOGGER.warn("Discord channel is not writable by bot: {}", trimmedChannelId);
            return;
        }
        if (BridgeConfig.ENABLE_EMBEDS.get() && payload != null) {
            MessageEmbed embed = DiscordEmbedFactory.build(withLinkedPlayerMention(payload));
            try {
                channel.sendMessageEmbeds(embed).queue(
                        ignored -> {},
                        error -> safeSendTextFallback(channel, text, trimmedChannelId, error)
                );
            } catch (RuntimeException e) {
                if (!isRecoverableSendError(e)) {
                    throw e;
                }
                DiscordBridgeMod.LOGGER.warn("Failed to send Discord embed to channel {}", trimmedChannelId, e);
            }
            return;
        }
        try {
            channel.sendMessage(text).queue(
                    ignored -> {},
                    error -> DiscordBridgeMod.LOGGER.warn("Failed to send Discord message to channel {}", trimmedChannelId, error)
            );
        } catch (RuntimeException e) {
            if (!isRecoverableSendError(e)) {
                throw e;
            }
            DiscordBridgeMod.LOGGER.warn("Failed to send Discord message to channel {}", trimmedChannelId, e);
        }
    }

    private void safeSendTextFallback(TextChannel channel, String text, String channelId, Throwable embedError) {
        DiscordBridgeMod.LOGGER.warn("Failed to send Discord embed to channel {}; falling back to plain text", channelId, embedError);
        try {
            channel.sendMessage(text).queue(
                    ignored -> {},
                    error -> DiscordBridgeMod.LOGGER.warn("Failed to send plain-text fallback to channel {}", channelId, error)
            );
        } catch (RuntimeException e) {
            if (!isRecoverableSendError(e)) {
                throw e;
            }
            DiscordBridgeMod.LOGGER.warn("Failed to send plain-text fallback to channel {}", channelId, e);
        }
    }

    private boolean isRecoverableSendError(Throwable error) {
        if (error instanceof java.util.concurrent.RejectedExecutionException) {
            return true;
        }
        if (error instanceof ErrorResponseException responseException) {
            ErrorResponse response = responseException.getErrorResponse();
            return response == ErrorResponse.MISSING_ACCESS
                    || response == ErrorResponse.MISSING_PERMISSIONS
                    || response == ErrorResponse.UNKNOWN_CHANNEL
                    || response == ErrorResponse.UNKNOWN_MESSAGE
                    || response == ErrorResponse.CANNOT_SEND_TO_USER;
        }
        return false;
    }

    private DiscordEmbedPayload withLinkedPlayerMention(DiscordEmbedPayload payload) {
        if (payload.playerUuid() == null || payload.playerUuid().isBlank()) {
            return payload;
        }
        LinkedAccount linked = linkService.getByMinecraftUuid(payload.playerUuid()).orElse(null);
        if (linked == null) {
            return payload;
        }
        String mention = "<@" + linked.discordUserId() + ">";
        String minecraftName = linked.minecraftName();
        return DiscordEmbedPayload.builder(payload.title())
                .description(replacePlayerName(payload.description(), minecraftName, mention))
                .player(mention, payload.playerUuid())
                .color(payload.colorHex())
                .fields(replacePlayerNameInFields(payload.fields(), minecraftName, mention))
                .build();
    }

    private Map<String, String> replacePlayerNameInFields(Map<String, String> fields, String playerName, String replacement) {
        if (fields == null || fields.isEmpty()) {
            return fields;
        }
        Map<String, String> replaced = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            replaced.put(entry.getKey(), replacePlayerName(entry.getValue(), playerName, replacement));
        }
        return replaced;
    }

    private String replacePlayerName(String text, String playerName, String replacement) {
        if (text == null || text.isBlank() || playerName == null || playerName.isBlank()) {
            return text;
        }
        return text.replace(playerName, replacement);
    }

    private DiscordEmbedPayload withoutDescription(DiscordEmbedPayload payload) {
        if (payload == null) {
            return null;
        }
        return DiscordEmbedPayload.builder(payload.title())
                .player(payload.playerName(), payload.playerUuid())
                .color(payload.colorHex())
                .fields(payload.fields())
                .build();
    }

    private record PendingMessage(String channelId, String text, DiscordEmbedPayload payload) {
    }
}
