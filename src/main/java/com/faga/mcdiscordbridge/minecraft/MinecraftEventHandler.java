package com.faga.mcdiscordbridge.minecraft;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.discord.DiscordBot;
import com.faga.mcdiscordbridge.util.CommandRedactor;
import com.faga.mcdiscordbridge.util.MessageFormatter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class MinecraftEventHandler {
    private final DiscordBot discordBot;

    public MinecraftEventHandler(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public void register() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        discordBot.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        discordBot.sendChatMessage(":white_check_mark: Server started");
        discordBot.sendAdminMessage("[ADMIN] Server started");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        discordBot.sendChatMessage(":octagonal_sign: Server stopping");
        discordBot.sendAdminMessage("[ADMIN] Server stopping");
        discordBot.stop();
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!BridgeConfig.ENABLE_JOIN_LEAVE.get()) {
            return;
        }
        String name = event.getEntity().getGameProfile().getName();
        discordBot.sendChatMessage(MessageFormatter.join(name));
        discordBot.sendAdminMessage("[ADMIN] Join " + name + " " + event.getEntity().getStringUUID());
    }

    @SubscribeEvent
    public void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!BridgeConfig.ENABLE_JOIN_LEAVE.get()) {
            return;
        }
        String name = event.getEntity().getGameProfile().getName();
        discordBot.sendChatMessage(MessageFormatter.leave(name));
        discordBot.sendAdminMessage("[ADMIN] Leave " + name + " " + event.getEntity().getStringUUID());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!BridgeConfig.ENABLE_DEATHS.get() || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        String death = player.getCombatTracker().getDeathMessage().getString();
        discordBot.sendChatMessage(MessageFormatter.death(death));
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!BridgeConfig.ENABLE_ADMIN_LOGS.get() || event.getParseResults() == null) {
            return;
        }
        String sourceName = event.getParseResults().getContext().getSource().getTextName();
        String input = event.getParseResults().getReader().getString();
        discordBot.sendAdminMessage("[ADMIN] " + sourceName + " ran command: " + CommandRedactor.redact(input));
    }
}
