package com.faga.mcdiscordbridge.leaderboard;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                byUuid.put(player.getUUID(), new LeaderboardEntry(player.getUUID(), player.getName().getString(), value));
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
        Map<UUID, String> cachedNames = loadUserCacheNames(server);

        try (var stream = Files.list(statsDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> loadPlayerStatFile(server, category, byUuid, path, cachedNames));
        } catch (IOException e) {
            DiscordBridgeMod.LOGGER.warn("Failed to read stats directory for leaderboard", e);
        }
    }

    private void loadPlayerStatFile(MinecraftServer server, LeaderboardCategory category, Map<UUID, LeaderboardEntry> byUuid, Path path,
                                    Map<UUID, String> cachedNames) {
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
        String resolvedName = cachedNames.getOrDefault(uuid, uuid.toString().substring(0, 8));
        byUuid.putIfAbsent(uuid, new LeaderboardEntry(uuid, resolvedName, value));
    }

    private Map<UUID, String> loadUserCacheNames(MinecraftServer server) {
        Map<UUID, String> names = new HashMap<>();
        Path usercachePath = server.getServerDirectory().resolve("usercache.json");
        if (!Files.isRegularFile(usercachePath)) {
            return names;
        }
        try {
            String raw = Files.readString(usercachePath);
            JsonElement root = JsonParser.parseString(raw);
            if (!root.isJsonArray()) {
                return names;
            }
            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("uuid") || !obj.has("name")) {
                    continue;
                }
                String uuidRaw = obj.get("uuid").getAsString();
                String name = obj.get("name").getAsString();
                if (uuidRaw == null || uuidRaw.isBlank() || name == null || name.isBlank()) {
                    continue;
                }
                try {
                    names.put(UUID.fromString(uuidRaw), name);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            DiscordBridgeMod.LOGGER.warn("Failed to read usercache.json for leaderboard names", e);
        }
        return names;
    }
}
