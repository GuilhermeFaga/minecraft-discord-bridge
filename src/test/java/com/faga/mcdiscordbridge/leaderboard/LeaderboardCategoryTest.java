package com.faga.mcdiscordbridge.leaderboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LeaderboardCategoryTest {
    @Test
    void categoryLookupWorksCaseInsensitive() {
        assertEquals(LeaderboardCategory.PLAYTIME, LeaderboardCategory.fromKey("playtime"));
        assertEquals(LeaderboardCategory.MOB_KILLS, LeaderboardCategory.fromKey("MOB_KILLS"));
        assertNull(LeaderboardCategory.fromKey("unknown"));
    }

    @Test
    void playtimeFormattingUsesHoursAndMinutes() {
        int ticks = 20 * (2 * 3600 + 15 * 60);
        assertEquals("2h 15m", LeaderboardCategory.PLAYTIME.format(ticks));
    }

    @Test
    void distanceFormattingUsesMetersAndKilometers() {
        assertEquals("250.0 m", LeaderboardCategory.DISTANCE_WALKED.format(25000));
        assertEquals("1.25 km", LeaderboardCategory.DISTANCE_WALKED.format(125000));
    }
}
