package com.faga.mcdiscordbridge.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class MinecraftMessageSender {
    private final MinecraftServer server;

    public MinecraftMessageSender(MinecraftServer server) {
        this.server = server;
    }

    public void broadcastSystem(String message) {
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.literal(message), false));
    }
}
