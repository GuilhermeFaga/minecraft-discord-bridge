package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.config.BridgeConfigService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class DiscordSetupWizard {
    private final Map<String, Integer> stageByUser = new ConcurrentHashMap<>();
    private final DiscordBot bot;

    public DiscordSetupWizard(DiscordBot bot) {
        this.bot = bot;
    }

    public void startOrContinue(MessageReceivedEvent event, boolean isAdmin) {
        if (!isAdmin) {
            event.getMessage().reply("You need Administrator permission to run setup.").queue();
            return;
        }
        String userId = event.getAuthor().getId();
        int stage = stageByUser.getOrDefault(userId, 0);
        if (stage == 0) {
            stageByUser.put(userId, 1);
            event.getMessage().reply("Setup started. Reply with: `!bridge chat <channelId>`").queue();
            return;
        }
        event.getMessage().reply("Setup already running. Use `!bridge chat <id>` then `!bridge admin <id>`.").queue();
    }

    public boolean hasActiveSession(String userId) {
        return stageByUser.getOrDefault(userId, 0) > 0;
    }

    public boolean handleStep(MessageReceivedEvent event, boolean isAdmin) {
        if (!isAdmin) {
            return false;
        }
        String userId = event.getAuthor().getId();
        if (!hasActiveSession(userId)) {
            return false;
        }

        String content = event.getMessage().getContentRaw().trim();
        if (content.startsWith("!bridge chat ")) {
            String channelId = content.substring("!bridge chat ".length()).trim();
            Optional<String> validation = validateAndSave(channelId, true, event);
            if (validation.isPresent()) {
                event.getMessage().reply(validation.get()).queue();
                return true;
            }
            stageByUser.put(userId, 2);
            event.getMessage().reply("Saved chat channel to server config. Next: `!bridge admin <channelId>`").queue();
            return true;
        }
        if (content.startsWith("!bridge admin ")) {
            String channelId = content.substring("!bridge admin ".length()).trim();
            Optional<String> validation = validateAndSave(channelId, false, event);
            if (validation.isPresent()) {
                event.getMessage().reply(validation.get()).queue();
                return true;
            }
            stageByUser.remove(userId);
            event.getMessage().reply("Saved admin channel to server config. Setup complete.").queue();
            return true;
        }
        return false;
    }

    private Optional<String> validateAndSave(String channelId, boolean chat, MessageReceivedEvent event) {
        TextChannel channel = event.getJDA().getTextChannelById(channelId);
        if (channel == null) {
            return Optional.of("Channel ID is invalid or I cannot access that channel.");
        }
        if (!channel.getGuild().getId().equals(event.getGuild().getId())) {
            return Optional.of("Channel must belong to this guild.");
        }
        Optional<String> updateError = chat
                ? BridgeConfigService.updateChatChannel(channelId, event.getGuild().getId())
                : BridgeConfigService.updateAdminChannel(channelId, event.getGuild().getId());
        if (updateError.isPresent()) {
            return updateError;
        }
        if (chat && bot.getJda() != null && BridgeConfig.CHAT_CHANNEL_ID.get().isBlank()) {
            return Optional.of("Unexpected save error for chat channel.");
        }
        return Optional.empty();
    }
}
