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
            if (BridgeConfig.ENABLE_MESSAGE_CONTENT_INTENT.get()) {
                builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
            }
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

        String guildId = BridgeConfig.WHITELIST_GUILD_ID.get().trim();
        if (!guildId.isBlank() && jda.getGuildById(guildId) != null) {
            var guild = jda.getGuildById(guildId);
            guild.upsertCommand(linkCommand).queue();
            guild.upsertCommand(leaderboardCommand).queue();
            guild.upsertCommand(mcCommand).queue();
            return;
        }
        jda.upsertCommand(linkCommand).queue();
        jda.upsertCommand(leaderboardCommand).queue();
        jda.upsertCommand(mcCommand).queue();
    }

    private void sendToChannel(String channelId, String text, DiscordEmbedPayload payload) {
        if (jda == null || channelId == null || channelId.isBlank()) {
            return;
        }
        TextChannel channel = jda.getTextChannelById(channelId.trim());
        if (channel == null) {
            if (jda.getStatus() != Status.CONNECTED) {
                synchronized (pendingMessages) {
                    pendingMessages.add(new PendingMessage(channelId, text, payload));
                }
                DiscordBridgeMod.LOGGER.info("Discord not ready yet; queued message for channel {}", channelId);
            } else {
                DiscordBridgeMod.LOGGER.warn("Configured Discord channel not found: {}", channelId);
            }
            return;
        }
        if (BridgeConfig.ENABLE_EMBEDS.get() && payload != null) {
            MessageEmbed embed = DiscordEmbedFactory.build(withLinkedPlayerMention(payload));
            channel.sendMessageEmbeds(embed).queue(
                    ignored -> {},
                    error -> channel.sendMessage(text).queue()
            );
            return;
        }
        channel.sendMessage(text).queue();
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
