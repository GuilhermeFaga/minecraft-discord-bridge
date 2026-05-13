package com.faga.mcdiscordbridge.link;

import com.faga.mcdiscordbridge.DiscordBridgeMod;
import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

public final class DiscordLinkService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Type LINK_LIST_TYPE = new TypeToken<List<LinkedAccount>>() {
    }.getType();

    private final SecureRandom random = new SecureRandom();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, PendingLinkCode> pendingByCode = new ConcurrentHashMap<>();
    private final Map<String, String> activeCodeByDiscordUser = new ConcurrentHashMap<>();
    private final Map<String, LinkedAccount> linkedByMinecraftUuid = new ConcurrentHashMap<>();

    private Path storagePath;

    public void initialize(MinecraftServer server) {
        this.storagePath = server.getServerDirectory().resolve("serverconfig").resolve("mcdiscordbridge-links.json");
        loadLinkedAccounts();
    }

    public PendingLinkCode createOrReplaceCode(String discordUserId, String discordTag) {
        cleanupExpired();
        String existing = activeCodeByDiscordUser.remove(discordUserId);
        if (existing != null) {
            pendingByCode.remove(existing);
        }

        String code = generateUniqueCode();
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + BridgeConfig.LINK_CODE_EXPIRY_SECONDS.get();
        PendingLinkCode pending = new PendingLinkCode(code, discordUserId, discordTag, now, expiresAt);
        pendingByCode.put(code, pending);
        activeCodeByDiscordUser.put(discordUserId, code);
        return pending;
    }

    public Optional<LinkedAccount> consumeAndLink(String code, String minecraftUuid, String minecraftName) {
        cleanupExpired();
        PendingLinkCode pending = pendingByCode.remove(code);
        if (pending == null || pending.isExpired(Instant.now().getEpochSecond())) {
            return Optional.empty();
        }
        activeCodeByDiscordUser.remove(pending.discordUserId(), code);

        LinkedAccount account = new LinkedAccount(
                minecraftUuid,
                minecraftName,
                pending.discordUserId(),
                pending.discordTag(),
                Instant.now().getEpochSecond()
        );
        linkedByMinecraftUuid.put(minecraftUuid, account);
        saveLinkedAccounts();
        return Optional.of(account);
    }

    public Optional<LinkedAccount> getByMinecraftUuid(String uuid) {
        return Optional.ofNullable(linkedByMinecraftUuid.get(uuid));
    }

    private String generateUniqueCode() {
        int length = BridgeConfig.LINK_CODE_LENGTH.get();
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode(length);
            if (!pendingByCode.containsKey(code)) {
                return code;
            }
        }
        return randomCode(length + 2);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private void cleanupExpired() {
        long now = Instant.now().getEpochSecond();
        for (PendingLinkCode pending : new ArrayList<>(pendingByCode.values())) {
            if (pending.isExpired(now)) {
                pendingByCode.remove(pending.code());
                activeCodeByDiscordUser.remove(pending.discordUserId(), pending.code());
            }
        }
    }

    private void loadLinkedAccounts() {
        linkedByMinecraftUuid.clear();
        if (storagePath == null || !Files.exists(storagePath)) {
            return;
        }
        try {
            String json = Files.readString(storagePath);
            List<LinkedAccount> links = gson.fromJson(json, LINK_LIST_TYPE);
            if (links == null) {
                return;
            }
            for (LinkedAccount link : links) {
                linkedByMinecraftUuid.put(link.minecraftUuid(), link);
            }
            DiscordBridgeMod.LOGGER.info("Loaded {} linked account(s)", links.size());
        } catch (IOException e) {
            DiscordBridgeMod.LOGGER.error("Failed to load account links", e);
        }
    }

    private void saveLinkedAccounts() {
        if (storagePath == null) {
            return;
        }
        try {
            Files.createDirectories(storagePath.getParent());
            String json = gson.toJson(new ArrayList<>(linkedByMinecraftUuid.values()), LINK_LIST_TYPE);
            Files.writeString(storagePath, json);
        } catch (IOException e) {
            DiscordBridgeMod.LOGGER.error("Failed to persist account links", e);
        }
    }
}
