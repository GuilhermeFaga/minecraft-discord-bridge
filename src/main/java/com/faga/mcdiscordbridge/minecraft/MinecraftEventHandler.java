package com.faga.mcdiscordbridge.minecraft;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.discord.DiscordBot;
import com.faga.mcdiscordbridge.discord.DiscordEmbedPayload;
import com.faga.mcdiscordbridge.link.LinkedAccount;
import com.faga.mcdiscordbridge.util.CommandRedactor;
import com.faga.mcdiscordbridge.util.MessageFormatter;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class MinecraftEventHandler {
    private static final String COLOR_SERVER = "#3B82F6";
    private static final String COLOR_JOIN = "#22C55E";
    private static final String COLOR_LEAVE = "#F97316";
    private static final String COLOR_DEATH = "#EF4444";
    private static final String COLOR_ADVANCEMENT = "#EAB308";
    private static final String COLOR_DAY_MILESTONE = "#06B6D4";
    private static final String COLOR_ADMIN = "#8B5CF6";

    private final DiscordBot discordBot;
    private long lastCheckedDay = -1;
    private long lastAnnouncedDay = -1;

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
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("bridge")
                        .then(Commands.literal("link")
                                .then(Commands.argument("code", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!BridgeConfig.ENABLE_ACCOUNT_LINKING.get()) {
                                                ctx.getSource().sendFailure(Component.literal("Account linking is disabled."));
                                                return 0;
                                            }
                                            if (ctx.getSource().getPlayer() == null) {
                                                ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                                                return 0;
                                            }
                                            String code = StringArgumentType.getString(ctx, "code").trim().toUpperCase();
                                            var player = ctx.getSource().getPlayer();
                                            var linked = discordBot.getLinkService().consumeAndLink(
                                                    code,
                                                    player.getStringUUID(),
                                                    player.getGameProfile().getName()
                                            );
                                            if (linked.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Invalid or expired link code."));
                                                return 0;
                                            }
                                            LinkedAccount account = linked.get();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Linked to Discord user " + account.discordTag() + " successfully."),
                                                    false
                                            );
                                            discordBot.sendAdminMessage(
                                                    "[ADMIN] Linked Minecraft user " + account.minecraftName() + " (" + account.minecraftUuid() + ") to Discord " + account.discordTag(),
                                                    DiscordEmbedPayload.builder("Account Linked")
                                                            .description("Discord/Minecraft account link created")
                                                            .field("Minecraft", account.minecraftName() + " (" + account.minecraftUuid() + ")")
                                                            .field("Discord", account.discordTag())
                                                            .color(COLOR_ADMIN)
                                                            .build()
                                            );
                                            return 1;
                                        })))
        );
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        long currentDay = event.getServer().overworld().getDayTime() / 24000L;
        lastCheckedDay = currentDay;
        discordBot.sendChatMessage(":white_check_mark: Server started",
                DiscordEmbedPayload.builder("Server Status")
                        .description("Server started")
                        .color(COLOR_SERVER)
                        .build());
        discordBot.sendAdminMessage("[ADMIN] Server started",
                DiscordEmbedPayload.builder("Admin Event")
                        .description("Server started")
                        .field("State", "Started")
                        .color(COLOR_ADMIN)
                        .build());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        discordBot.sendChatMessage(":octagonal_sign: Server stopping",
                DiscordEmbedPayload.builder("Server Status")
                        .description("Server stopping")
                        .color(COLOR_SERVER)
                        .build());
        discordBot.sendAdminMessage("[ADMIN] Server stopping",
                DiscordEmbedPayload.builder("Admin Event")
                        .description("Server stopping")
                        .field("State", "Stopping")
                        .color(COLOR_ADMIN)
                        .build());
        discordBot.stop();
        lastCheckedDay = -1;
        lastAnnouncedDay = -1;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int gap = BridgeConfig.DAY_MILESTONE_GAP.get();
        if (gap <= 0) {
            return;
        }
        long day = event.getServer().overworld().getDayTime() / 24000L;
        if (day == lastCheckedDay) {
            return;
        }
        lastCheckedDay = day;
        if (day <= 0 || day % gap != 0 || day == lastAnnouncedDay) {
            return;
        }
        lastAnnouncedDay = day;
        String text = ":calendar_spiral: World reached day " + day;
        discordBot.sendChatMessage(text,
                DiscordEmbedPayload.builder(text)
                        .color(COLOR_DAY_MILESTONE)
                        .build());
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!BridgeConfig.ENABLE_JOIN_LEAVE.get()) {
            return;
        }
        String name = event.getEntity().getGameProfile().getName();
        String uuid = event.getEntity().getStringUUID();
        String text = MessageFormatter.join(name);
        discordBot.sendChatMessage(MessageFormatter.join(name),
                DiscordEmbedPayload.builder(text)
                        .color(COLOR_JOIN)
                        .build());
        discordBot.sendAdminMessage("[ADMIN] Join " + name + " " + uuid,
                DiscordEmbedPayload.builder("Admin Event")
                        .description("Player joined")
                        .player(name, uuid)
                        .field("Event", "Join")
                        .color(COLOR_ADMIN)
                        .build());
    }

    @SubscribeEvent
    public void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!BridgeConfig.ENABLE_JOIN_LEAVE.get()) {
            return;
        }
        String name = event.getEntity().getGameProfile().getName();
        String uuid = event.getEntity().getStringUUID();
        String text = MessageFormatter.leave(name);
        discordBot.sendChatMessage(MessageFormatter.leave(name),
                DiscordEmbedPayload.builder(text)
                        .color(COLOR_LEAVE)
                        .build());
        discordBot.sendAdminMessage("[ADMIN] Leave " + name + " " + uuid,
                DiscordEmbedPayload.builder("Admin Event")
                        .description("Player left")
                        .player(name, uuid)
                        .field("Event", "Leave")
                        .color(COLOR_ADMIN)
                        .build());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!BridgeConfig.ENABLE_DEATHS.get() || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        String death = player.getCombatTracker().getDeathMessage().getString();
        String text = MessageFormatter.death(death);
        discordBot.sendChatMessage(text,
                DiscordEmbedPayload.builder(text)
                        .color(COLOR_DEATH)
                        .build());
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!BridgeConfig.ENABLE_ADMIN_LOGS.get() || event.getParseResults() == null) {
            return;
        }
        String sourceName = event.getParseResults().getContext().getSource().getTextName();
        String input = event.getParseResults().getReader().getString();
        String redacted = CommandRedactor.redact(input);
        discordBot.sendAdminMessage("[ADMIN] " + sourceName + " ran command: " + redacted,
                DiscordEmbedPayload.builder("Admin Command")
                        .description("Command executed")
                        .field("Source", sourceName)
                        .field("Command", redacted)
                        .color(COLOR_ADMIN)
                        .build());
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!BridgeConfig.ENABLE_ADVANCEMENTS.get()) {
            return;
        }
        var advancement = event.getAdvancement();
        if (advancement.value().display().isEmpty() || !advancement.value().display().get().shouldAnnounceChat()) {
            return;
        }
        String player = event.getEntity().getGameProfile().getName();
        String advancementName = advancement.value().display().get().getTitle().getString();
        String text = ":trophy: " + player + " has made the advancement " + advancementName;
        discordBot.sendChatMessage(text,
                DiscordEmbedPayload.builder(text)
                        .color(COLOR_ADVANCEMENT)
                        .build());
    }

    @SubscribeEvent
    public void onMinecraftChat(ServerChatEvent event) {
        if (!BridgeConfig.ENABLE_MINECRAFT_CHAT_TO_DISCORD.get()) {
            return;
        }
        String player = event.getPlayer().getGameProfile().getName();
        String message = event.getRawText();
        if (message.startsWith("[Discord] <")) {
            return;
        }
        discordBot.sendChatMessage("**" + player + ":** " + message.replace("@", "@\u200B"));
    }
}
