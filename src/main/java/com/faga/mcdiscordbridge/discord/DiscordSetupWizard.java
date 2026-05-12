package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public final class DiscordSetupWizard {
    private final Map<String, Integer> stageByUser = new ConcurrentHashMap<>();

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

    public static boolean handleStep(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw().trim();
        if (content.startsWith("!bridge chat ")) {
            String channelId = content.substring("!bridge chat ".length()).trim();
            BridgeConfig.CHAT_CHANNEL_ID.set(channelId);
            event.getMessage().reply("Saved chat channel. Next: `!bridge admin <channelId>`").queue();
            return true;
        }
        if (content.startsWith("!bridge admin ")) {
            String channelId = content.substring("!bridge admin ".length()).trim();
            BridgeConfig.ADMIN_LOG_CHANNEL_ID.set(channelId);
            event.getMessage().reply("Saved admin channel. Setup complete.").queue();
            return true;
        }
        return false;
    }
}
