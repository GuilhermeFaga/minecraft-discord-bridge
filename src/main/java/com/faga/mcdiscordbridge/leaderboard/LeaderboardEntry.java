package com.faga.mcdiscordbridge.leaderboard;

import java.util.UUID;

public record LeaderboardEntry(UUID playerUuid, String playerName, int value) {
}
