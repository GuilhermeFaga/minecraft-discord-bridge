package com.faga.mcdiscordbridge.discord;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public final class DiscordReadyListener extends ListenerAdapter {
    private final DiscordBot bot;

    public DiscordReadyListener(DiscordBot bot) {
        this.bot = bot;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        bot.onReady();
    }
}
