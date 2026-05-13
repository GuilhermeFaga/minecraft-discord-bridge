package com.faga.mcdiscordbridge.leaderboard;

import net.minecraft.stats.Stats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;

public enum LeaderboardCategory {
    PLAYTIME("playtime", "Play Time") {
        @Override
        public int getValue(ServerPlayer player) {
            return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        }

        @Override
        public int getValue(ServerStatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        }

        @Override
        public String format(int value) {
            long seconds = value / 20L;
            long hours = seconds / 3600L;
            long minutes = (seconds % 3600L) / 60L;
            return hours + "h " + minutes + "m";
        }
    },
    DEATHS("deaths", "Deaths") {
        @Override
        public int getValue(ServerPlayer player) {
            return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        }

        @Override
        public int getValue(ServerStatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
        }
    },
    PLAYER_KILLS("player_kills", "Player Kills") {
        @Override
        public int getValue(ServerPlayer player) {
            return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        }

        @Override
        public int getValue(ServerStatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        }
    },
    MOB_KILLS("mob_kills", "Mob Kills") {
        @Override
        public int getValue(ServerPlayer player) {
            return player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
        }

        @Override
        public int getValue(ServerStatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
        }
    },
    MINED_BLOCKS("mined_blocks", "Mined Blocks") {
        @Override
        public int getValue(ServerPlayer player) {
            int total = 0;
            for (var block : BuiltInRegistries.BLOCK) {
                total += player.getStats().getValue(Stats.BLOCK_MINED.get(block));
            }
            return total;
        }

        @Override
        public int getValue(ServerStatsCounter stats) {
            int total = 0;
            for (var block : BuiltInRegistries.BLOCK) {
                total += stats.getValue(Stats.BLOCK_MINED.get(block));
            }
            return total;
        }
    },
    DISTANCE_WALKED("distance_walked", "Distance Walked") {
        @Override
        public int getValue(ServerPlayer player) {
            return player.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM));
        }

        @Override
        public int getValue(ServerStatsCounter stats) {
            return stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM));
        }

        @Override
        public String format(int value) {
            double meters = value / 100.0;
            if (meters >= 1000.0) {
                return String.format("%.2f km", meters / 1000.0);
            }
            return String.format("%.1f m", meters);
        }
    };

    private final String key;
    private final String displayName;

    LeaderboardCategory(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public abstract int getValue(ServerPlayer player);

    public abstract int getValue(ServerStatsCounter stats);

    public String format(int value) {
        return Integer.toString(value);
    }

    public static LeaderboardCategory fromKey(String key) {
        for (LeaderboardCategory category : values()) {
            if (category.key.equalsIgnoreCase(key)) {
                return category;
            }
        }
        return null;
    }
}
