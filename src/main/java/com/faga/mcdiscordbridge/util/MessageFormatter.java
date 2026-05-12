package com.faga.mcdiscordbridge.util;

public final class MessageFormatter {
    private MessageFormatter() {
    }

    public static String join(String player) {
        return ":green_circle: " + player + " joined the server";
    }

    public static String leave(String player) {
        return ":red_circle: " + player + " left the server";
    }

    public static String death(String deathMessage) {
        return ":skull: " + deathMessage;
    }
}
