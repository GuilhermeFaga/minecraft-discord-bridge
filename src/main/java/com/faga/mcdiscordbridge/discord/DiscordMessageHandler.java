package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class DiscordMessageHandler extends ListenerAdapter {
    private final DiscordBot bot;
    private final DiscordSetupWizard wizard = new DiscordSetupWizard();

    public DiscordMessageHandler(DiscordBot bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }
        String whitelistGuildId = BridgeConfig.WHITELIST_GUILD_ID.get().trim();
        if (!whitelistGuildId.isBlank() && !event.getGuild().getId().equals(whitelistGuildId)) {
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        if (content.equalsIgnoreCase("!bridge setup")) {
            boolean isAdmin = event.getMember() != null && event.getMember().hasPermission(Permission.ADMINISTRATOR);
            wizard.startOrContinue(event, isAdmin);
            return;
        }
        if (DiscordSetupWizard.handleStep(event)) {
            return;
        }

        if (!BridgeConfig.ENABLE_DISCORD_CHAT_TO_MINECRAFT.get()) {
            return;
        }
        if (!event.getChannel().getId().equals(BridgeConfig.CHAT_CHANNEL_ID.get().trim())) {
            return;
        }
        if (content.startsWith("!") || content.startsWith("/") || content.isBlank()) {
            return;
        }
        String safe = sanitize(content);
        if (bot.getServer() != null) {
            bot.getServer().execute(() -> bot.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("[Discord] <" + event.getAuthor().getName() + "> " + safe),
                    false
            ));
        }
    }

    private String sanitize(String text) {
        return text.replace("@everyone", "everyone").replace("@here", "here");
    }
}
