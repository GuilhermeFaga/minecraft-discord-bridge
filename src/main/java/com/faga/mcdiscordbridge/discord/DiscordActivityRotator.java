package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public final class DiscordActivityRotator {
    private ScheduledExecutorService scheduler;
    private final AtomicInteger index = new AtomicInteger(0);

    public void start(JDA jda) {
        stop();
        if (!BridgeConfig.ENABLE_BOT_ACTIVITY_ROTATION.get()) {
            return;
        }
        List<String> activities = new ArrayList<>();
        for (String value : BridgeConfig.BOT_ACTIVITIES.get()) {
            if (value != null && !value.trim().isEmpty()) {
                activities.add(value.trim());
            }
        }
        if (activities.isEmpty()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "discord-activity-rotator");
            t.setDaemon(true);
            return t;
        });

        Runnable update = () -> {
            int i = Math.floorMod(index.getAndIncrement(), activities.size());
            jda.getPresence().setActivity(toActivity(activities.get(i)));
        };
        update.run();
        scheduler.scheduleAtFixedRate(update, BridgeConfig.BOT_ACTIVITY_ROTATE_SECONDS.get(), BridgeConfig.BOT_ACTIVITY_ROTATE_SECONDS.get(), TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private Activity toActivity(String text) {
        String type = BridgeConfig.BOT_ACTIVITY_TYPE.get().trim().toUpperCase();
        return switch (type) {
            case "PLAYING" -> Activity.playing(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            default -> Activity.watching(text);
        };
    }
}
