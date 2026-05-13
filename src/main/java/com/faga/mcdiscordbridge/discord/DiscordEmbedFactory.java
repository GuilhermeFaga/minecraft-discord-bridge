package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import java.awt.Color;
import java.time.Instant;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public final class DiscordEmbedFactory {
    private DiscordEmbedFactory() {
    }

    public static MessageEmbed build(DiscordEmbedPayload payload) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(orDefault(payload.title(), "Minecraft Event"));
        if (payload.description() != null && !payload.description().isBlank()) {
            builder.setDescription(payload.description());
        }
        builder.setTimestamp(Instant.now());
        String selectedColor = payload.colorHex() != null && !payload.colorHex().isBlank()
                ? payload.colorHex()
                : BridgeConfig.EMBED_COLOR_HEX.get();
        builder.setColor(parseColor(selectedColor));

        if (payload.playerName() != null && !payload.playerName().isBlank()) {
            builder.addField("Player", payload.playerName(), true);
        }
        if (payload.playerUuid() != null && !payload.playerUuid().isBlank()) {
            builder.addField("UUID", payload.playerUuid(), true);
        }
        for (Map.Entry<String, String> entry : payload.fields().entrySet()) {
            builder.addField(entry.getKey(), entry.getValue(), false);
        }

        if (BridgeConfig.INCLUDE_PLAYER_HEAD_IN_EMBEDS.get() && payload.playerUuid() != null && !payload.playerUuid().isBlank()) {
            String template = BridgeConfig.PLAYER_HEAD_URL_TEMPLATE.get();
            if (template != null && template.contains("%uuid%")) {
                builder.setThumbnail(template.replace("%uuid%", payload.playerUuid()));
            }
        }

        return builder.build();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Color parseColor(String hex) {
        if (hex == null) {
            return new Color(0x57A5FF);
        }
        String normalized = hex.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        try {
            return new Color(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException e) {
            return new Color(0x57A5FF);
        }
    }
}
