package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.config.BridgeConfigService;
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

public final class DiscordBot {
    private JDA jda;
    private MinecraftServer server;
    private final DiscordActivityRotator activityRotator = new DiscordActivityRotator();
    private final DiscordLinkService linkService = new DiscordLinkService();

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
            } else if (BridgeConfig.ENABLE_DISCORD_CHAT_TO_MINECRAFT.get()) {
                DiscordBridgeMod.LOGGER.warn("Discord->Minecraft chat is enabled but Message Content Intent is disabled in config.");
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
        sendToChannel(BridgeConfig.CHAT_CHANNEL_ID.get(), text, payload);
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
            activityRotator.start(jda);
            registerSlashCommands();
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
        var leaderboardCommand = Commands.slash("leaderboard", "Show Minecraft leaderboard by category")
                .addOptions(categoryOption, pageOption);

        String guildId = BridgeConfig.WHITELIST_GUILD_ID.get().trim();
        if (!guildId.isBlank() && jda.getGuildById(guildId) != null) {
            var guild = jda.getGuildById(guildId);
            guild.upsertCommand(linkCommand).queue();
            guild.upsertCommand(leaderboardCommand).queue();
            return;
        }
        jda.upsertCommand(linkCommand).queue();
        jda.upsertCommand(leaderboardCommand).queue();
    }

    private void sendToChannel(String channelId, String text, DiscordEmbedPayload payload) {
        if (jda == null || channelId == null || channelId.isBlank()) {
            return;
        }
        TextChannel channel = jda.getTextChannelById(channelId.trim());
        if (channel == null) {
            if (jda.getStatus() != Status.CONNECTED) {
                DiscordBridgeMod.LOGGER.info("Discord not ready yet; skipping send to channel {}", channelId);
            } else {
                DiscordBridgeMod.LOGGER.warn("Configured Discord channel not found: {}", channelId);
            }
            return;
        }
        if (BridgeConfig.ENABLE_EMBEDS.get() && payload != null) {
            MessageEmbed embed = DiscordEmbedFactory.build(payload);
            channel.sendMessageEmbeds(embed).queue(
                    ignored -> {},
                    error -> channel.sendMessage(text).queue()
            );
            return;
        }
        channel.sendMessage(text).queue();
    }
}
