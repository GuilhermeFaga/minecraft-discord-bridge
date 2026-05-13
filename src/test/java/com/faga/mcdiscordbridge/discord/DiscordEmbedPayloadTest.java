package com.faga.mcdiscordbridge.discord;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DiscordEmbedPayloadTest {
    @Test
    void builderStoresFieldsAndPlayer() {
        DiscordEmbedPayload payload = DiscordEmbedPayload.builder("Title")
                .description("Desc")
                .player("Steve", "uuid-1")
                .field("Event", "Join")
                .build();

        assertEquals("Title", payload.title());
        assertEquals("Desc", payload.description());
        assertEquals("Steve", payload.playerName());
        assertEquals("uuid-1", payload.playerUuid());
        assertEquals("Join", payload.fields().get("Event"));
    }
}
