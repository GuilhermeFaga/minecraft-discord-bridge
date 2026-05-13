package com.faga.mcdiscordbridge.discord;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DiscordEmbedPayload(
        String title,
        String description,
        String playerName,
        String playerUuid,
        String colorHex,
        Map<String, String> fields
) {
    public DiscordEmbedPayload {
        fields = fields == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final String title;
        private String description;
        private String playerName;
        private String playerUuid;
        private String colorHex;
        private final Map<String, String> fields = new LinkedHashMap<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder player(String name, String uuid) {
            this.playerName = name;
            this.playerUuid = uuid;
            return this;
        }

        public Builder color(String hex) {
            this.colorHex = hex;
            return this;
        }

        public Builder field(String key, String value) {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                this.fields.put(key, value);
            }
            return this;
        }

        public DiscordEmbedPayload build() {
            return new DiscordEmbedPayload(title, description, playerName, playerUuid, colorHex, fields);
        }
    }
}
