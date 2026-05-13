package com.faga.mcdiscordbridge.link;

public record LinkedAccount(
        String minecraftUuid,
        String minecraftName,
        String discordUserId,
        String discordTag,
        long linkedAtEpochSeconds
) {
}
