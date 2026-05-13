package com.faga.mcdiscordbridge.link;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PendingLinkCodeTest {
    @Test
    void reportsExpiredWhenNowReachedExpiry() {
        PendingLinkCode code = new PendingLinkCode("ABC123", "1", "user#0001", 100, 200);

        assertFalse(code.isExpired(199));
        assertTrue(code.isExpired(200));
        assertTrue(code.isExpired(250));
    }
}
