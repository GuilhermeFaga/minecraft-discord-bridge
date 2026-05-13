package com.faga.mcdiscordbridge.link;

public record PendingLinkCode(
        String code,
        String discordUserId,
        String discordTag,
        long createdAtEpochSeconds,
        long expiresAtEpochSeconds
) {
    public boolean isExpired(long nowEpochSeconds) {
        return nowEpochSeconds >= expiresAtEpochSeconds;
    }
}
