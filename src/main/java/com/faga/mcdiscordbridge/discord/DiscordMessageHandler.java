package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.util.PermissionUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class DiscordMessageHandler extends ListenerAdapter {
    private final DiscordBot bot;
    private final DiscordSetupWizard wizard;

    public DiscordMessageHandler(DiscordBot bot) {
        this.bot = bot;
        this.wizard = new DiscordSetupWizard(bot);
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
            boolean isAdmin = PermissionUtil.isDiscordAdmin(event.getMember());
            wizard.startOrContinue(event, isAdmin);
            return;
        }
        boolean isAdmin = PermissionUtil.isDiscordAdmin(event.getMember());
        if (wizard.handleStep(event, isAdmin)) {
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
