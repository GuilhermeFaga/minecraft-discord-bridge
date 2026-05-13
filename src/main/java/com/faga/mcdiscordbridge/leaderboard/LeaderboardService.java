package com.faga.mcdiscordbridge.leaderboard;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;

public final class LeaderboardService {
    public List<LeaderboardEntry> collect(MinecraftServer server, LeaderboardCategory category) {
        Map<UUID, LeaderboardEntry> byUuid = new HashMap<>();

        loadOfflinePlayers(server, category, byUuid);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int value = category.getValue(player);
            if (value > 0) {
                byUuid.put(player.getUUID(), new LeaderboardEntry(player.getGameProfile().getName(), value));
            }
        }

        List<LeaderboardEntry> entries = new ArrayList<>(byUuid.values());
        entries.sort(Comparator.comparingInt(LeaderboardEntry::value).reversed().thenComparing(LeaderboardEntry::playerName));
        return entries;
    }

    private void loadOfflinePlayers(MinecraftServer server, LeaderboardCategory category, Map<UUID, LeaderboardEntry> byUuid) {
        Path statsDir = server.getWorldPath(LevelResource.ROOT).resolve("stats");
        if (!Files.isDirectory(statsDir)) {
            return;
        }

        try (var stream = Files.list(statsDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> loadPlayerStatFile(server, category, byUuid, path));
        } catch (IOException e) {
            DiscordBridgeMod.LOGGER.warn("Failed to read stats directory for leaderboard", e);
        }
    }

    private void loadPlayerStatFile(MinecraftServer server, LeaderboardCategory category, Map<UUID, LeaderboardEntry> byUuid, Path path) {
        String fileName = path.getFileName().toString();
        String uuidPart = fileName.substring(0, fileName.length() - ".json".length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidPart);
        } catch (IllegalArgumentException e) {
            return;
        }

        ServerStatsCounter statsCounter = new ServerStatsCounter(server, path.toFile());
        int value = category.getValue(statsCounter);
        if (value <= 0) {
            return;
        }
        byUuid.putIfAbsent(uuid, new LeaderboardEntry(resolveName(server, uuid), value));
    }

    private String resolveName(MinecraftServer server, UUID uuid) {
        Optional<GameProfile> profile = server.getProfileCache().get(uuid);
        if (profile.isPresent()) {
            return profile.get().getName();
        }
        return uuid.toString().substring(0, 8);
    }
}
