package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import com.faga.mcdiscordbridge.config.BridgeConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.server.MinecraftServer;

public final class DiscordBot {
    private JDA jda;
    private MinecraftServer server;

    public void start(MinecraftServer minecraftServer) {
        this.server = minecraftServer;
        String token = BridgeConfig.resolveToken();
        if (token.isBlank()) {
            DiscordBridgeMod.LOGGER.warn("Discord bridge disabled: missing discordApiKey");
            return;
        }
        try {
            this.jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .addEventListeners(new DiscordMessageHandler(this))
                    .build();
            DiscordBridgeMod.LOGGER.info("Discord bot starting");
        } catch (Exception e) {
            DiscordBridgeMod.LOGGER.error("Discord bot failed to start", e);
        }
    }

    public void stop() {
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

    public void sendChatMessage(String text) {
        sendToChannel(BridgeConfig.CHAT_CHANNEL_ID.get(), text);
    }

    public void sendAdminMessage(String text) {
        if (BridgeConfig.ENABLE_ADMIN_LOGS.get()) {
            sendToChannel(BridgeConfig.ADMIN_LOG_CHANNEL_ID.get(), text);
        }
    }

    private void sendToChannel(String channelId, String text) {
        if (jda == null || channelId == null || channelId.isBlank()) {
            return;
        }
        TextChannel channel = jda.getTextChannelById(channelId.trim());
        if (channel == null) {
            DiscordBridgeMod.LOGGER.warn("Configured Discord channel not found: {}", channelId);
            return;
        }
        channel.sendMessage(text).queue();
    }
}
