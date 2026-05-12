package com.faga.mcdiscordbridge.minecraft;

import com.faga.mcdiscordbridge.discord.DiscordBot;

public final class AdminLogHandler {
    private final DiscordBot bot;

    public AdminLogHandler(DiscordBot bot) {
        this.bot = bot;
    }

    public void log(String message) {
        bot.sendAdminMessage("[ADMIN] " + message);
    }
}
