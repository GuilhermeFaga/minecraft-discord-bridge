package com.faga.mcdiscordbridge.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean isDiscordAdmin(Member member) {
        return member != null && member.hasPermission(Permission.ADMINISTRATOR);
    }
}
