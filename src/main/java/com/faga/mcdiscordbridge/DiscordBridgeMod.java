package com.faga.mcdiscordbridge;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.discord.DiscordBot;
import com.faga.mcdiscordbridge.minecraft.MinecraftEventHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(DiscordBridgeMod.MOD_ID)
public class DiscordBridgeMod {
    public static final String MOD_ID = "mcdiscordbridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final DiscordBot discordBot;

    public DiscordBridgeMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BridgeConfig.SPEC);
        this.discordBot = new DiscordBot();
        MinecraftEventHandler eventHandler = new MinecraftEventHandler(discordBot);
        eventHandler.register();
    }
}
