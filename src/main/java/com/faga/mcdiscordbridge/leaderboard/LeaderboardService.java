package com.faga.mcdiscordbridge.leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class LeaderboardService {
    public List<LeaderboardEntry> collect(MinecraftServer server, LeaderboardCategory category) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int value = category.getValue(player);
            if (value > 0) {
                entries.add(new LeaderboardEntry(player.getGameProfile().getName(), value));
            }
        }
        entries.sort(Comparator.comparingInt(LeaderboardEntry::value).reversed().thenComparing(LeaderboardEntry::playerName));
        return entries;
    }
}
