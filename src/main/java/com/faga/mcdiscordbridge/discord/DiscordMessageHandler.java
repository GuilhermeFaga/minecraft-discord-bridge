package com.faga.mcdiscordbridge.discord;

import com.faga.mcdiscordbridge.config.BridgeConfig;
import com.faga.mcdiscordbridge.config.BridgeConfigService;
import com.faga.mcdiscordbridge.leaderboard.LeaderboardCategory;
import com.faga.mcdiscordbridge.leaderboard.LeaderboardEntry;
import com.faga.mcdiscordbridge.leaderboard.LeaderboardService;
import com.faga.mcdiscordbridge.link.PendingLinkCode;
import com.faga.mcdiscordbridge.util.PermissionUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class DiscordMessageHandler extends ListenerAdapter {
    private static final int PAGE_SIZE = 10;

    private final DiscordBot bot;
    private final LeaderboardService leaderboardService;

    public DiscordMessageHandler(DiscordBot bot) {
        this.bot = bot;
        this.leaderboardService = new LeaderboardService();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!isWhitelistedGuild(event)) {
            event.reply("This command is not enabled in this server.").setEphemeral(true).queue();
            return;
        }

        if ("link".equals(event.getName())) {
            handleLinkCommand(event);
            return;
        }
        if ("leaderboard".equals(event.getName())) {
            handleLeaderboardCommand(event);
            return;
        }
        if ("mc".equals(event.getName())) {
            handleMinecraftRelayCommand(event);
            return;
        }
        if ("config".equals(event.getName())) {
            handleConfigCommand(event);
        }
    }

    private void handleConfigCommand(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.isDiscordAdmin(event.getMember())) {
            event.reply("You need Administrator permission to use this command.").setEphemeral(true).queue();
            return;
        }
        String chatChannelId = option(event, "chat_channel_id");
        String adminChannelId = option(event, "admin_channel_id");
        String whitelistGuildId = option(event, "whitelist_guild_id");
        if (chatChannelId == null && adminChannelId == null && whitelistGuildId == null) {
            event.reply("Provide at least one option: `chat_channel_id`, `admin_channel_id`, or `whitelist_guild_id`.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        List<String> updates = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String guildId = event.getGuild() == null ? "" : event.getGuild().getId();

        if (chatChannelId != null) {
            if (!isNumericSnowflake(chatChannelId)) {
                errors.add("`chat_channel_id` must be numeric.");
            } else {
                TextChannel channel = event.getJDA().getTextChannelById(chatChannelId);
                if (channel == null || event.getGuild() == null || !channel.getGuild().getId().equals(guildId)) {
                    errors.add("`chat_channel_id` is invalid or not in this guild.");
                } else if (!channel.canTalk()) {
                    errors.add("Bot cannot send messages to `chat_channel_id`.");
                } else {
                    BridgeConfigService.updateChatChannel(chatChannelId, guildId)
                            .ifPresentOrElse(errors::add, () -> updates.add("chat channel updated"));
                }
            }
        }

        if (adminChannelId != null) {
            if (!isNumericSnowflake(adminChannelId)) {
                errors.add("`admin_channel_id` must be numeric.");
            } else {
                TextChannel channel = event.getJDA().getTextChannelById(adminChannelId);
                if (channel == null || event.getGuild() == null || !channel.getGuild().getId().equals(guildId)) {
                    errors.add("`admin_channel_id` is invalid or not in this guild.");
                } else if (!channel.canTalk()) {
                    errors.add("Bot cannot send messages to `admin_channel_id`.");
                } else {
                    BridgeConfigService.updateAdminChannel(adminChannelId, guildId)
                            .ifPresentOrElse(errors::add, () -> updates.add("admin channel updated"));
                }
            }
        }

        if (whitelistGuildId != null) {
            if (!isNumericSnowflake(whitelistGuildId)) {
                errors.add("`whitelist_guild_id` must be numeric.");
            } else {
                BridgeConfigService.updateWhitelistGuild(whitelistGuildId)
                        .ifPresentOrElse(errors::add, () -> updates.add("whitelist guild updated"));
            }
        }

        if (updates.isEmpty() && !errors.isEmpty()) {
            event.reply(String.join("\n", errors)).setEphemeral(true).queue();
            return;
        }

        StringBuilder reply = new StringBuilder();
        if (!updates.isEmpty()) {
            reply.append("Updated: ").append(String.join(", ", updates)).append(".");
        }
        if (!errors.isEmpty()) {
            if (!reply.isEmpty()) {
                reply.append("\n");
            }
            reply.append("Issues:\n").append(String.join("\n", errors));
        }
        event.reply(reply.toString()).setEphemeral(true).queue();
    }

    private void handleMinecraftRelayCommand(SlashCommandInteractionEvent event) {
        if (!BridgeConfig.ENABLE_DISCORD_CHAT_TO_MINECRAFT.get()) {
            event.reply("Discord to Minecraft relay is disabled by server configuration.").setEphemeral(true).queue();
            return;
        }
        if (!event.getChannel().getId().equals(BridgeConfig.CHAT_CHANNEL_ID.get().trim())) {
            event.reply("Use this command in the configured bridge chat channel.").setEphemeral(true).queue();
            return;
        }
        if (bot.getServer() == null) {
            event.reply("Server is not ready yet.").setEphemeral(true).queue();
            return;
        }
        String message = event.getOption("message", OptionMapping::getAsString);
        if (message == null || message.isBlank()) {
            event.reply("Message cannot be empty.").setEphemeral(true).queue();
            return;
        }

        String senderName = bot.getLinkService()
                .getByDiscordUserId(event.getUser().getId())
                .map(linked -> linked.minecraftName())
                .orElse(event.getUser().getName());
        String safe = sanitize(message.trim());
        bot.getServer().execute(() -> bot.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("[Discord] <" + senderName + "> " + safe),
                false
        ));
        event.getChannel().sendMessage("**" + senderName + ":** " + safe).queue(
                ignored -> {},
                error -> {}
        );
        event.deferReply(true).queue(hook -> hook.deleteOriginal().queue());
    }

    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        if (!BridgeConfig.ENABLE_ACCOUNT_LINKING.get()) {
            event.reply("Account linking is disabled by server configuration.").setEphemeral(true).queue();
            return;
        }

        PendingLinkCode code = bot.getLinkService().createOrReplaceCode(event.getUser().getId(), event.getUser().getAsTag());
        long seconds = Math.max(1L, code.expiresAtEpochSeconds() - code.createdAtEpochSeconds());
        event.reply("Your link code is: `" + code.code() + "`\nRun `/bridge link " + code.code()
                        + "` in Minecraft chat.\nThis code expires in " + seconds + " seconds.")
                .setEphemeral(true)
                .queue();
    }

    private void handleLeaderboardCommand(SlashCommandInteractionEvent event) {
        if (bot.getServer() == null) {
            event.reply("Server is not ready yet.").queue();
            return;
        }
        String categoryKey = event.getOption("category", OptionMapping::getAsString);
        LeaderboardCategory category = LeaderboardCategory.fromKey(categoryKey);
        if (category == null) {
            event.reply("Unknown category: `" + categoryKey + "`.").queue();
            return;
        }

        int page = Math.max(1, event.getOption("page", 1, OptionMapping::getAsInt));
        String visibility = event.getOption("visibility", "private", OptionMapping::getAsString);
        boolean isPublic = "public".equalsIgnoreCase(visibility);
        List<LeaderboardEntry> entries = leaderboardService.collect(bot.getServer(), category);
        if (entries.isEmpty()) {
            event.reply("No stats found for `" + category.key() + "` yet.").setEphemeral(!isPublic).queue();
            return;
        }

        int pageCount = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(page, pageCount);
        int start = (safePage - 1) * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);

        StringBuilder description = new StringBuilder();
        for (int i = start; i < end; i++) {
            LeaderboardEntry entry = entries.get(i);
            String displayName = entry.playerName();
            var linked = bot.getLinkService().getByMinecraftUuid(entry.playerUuid().toString());
            if (linked.isPresent()) {
                displayName = "<@" + linked.get().discordUserId() + ">";
            }
            description.append("#")
                    .append(i + 1)
                    .append(" ")
                    .append(displayName)
                    .append(" - ")
                    .append(category.format(entry.value()))
                    .append("\n");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Leaderboard: " + category.displayName())
                .setDescription(description.toString())
                .setTimestamp(Instant.now())
                .setFooter("Page " + safePage + "/" + pageCount + " • " + entries.size() + " players");

        event.replyEmbeds(embed.build()).setEphemeral(!isPublic).queue();
    }

    private boolean isWhitelistedGuild(SlashCommandInteractionEvent event) {
        String whitelistGuildId = BridgeConfig.WHITELIST_GUILD_ID.get().trim();
        return whitelistGuildId.isBlank() || (event.getGuild() != null && event.getGuild().getId().equals(whitelistGuildId));
    }

    private String option(SlashCommandInteractionEvent event, String name) {
        OptionMapping option = event.getOption(name);
        if (option == null) {
            return null;
        }
        String value = option.getAsString().trim();
        return value.isBlank() ? null : value;
    }

    private boolean isNumericSnowflake(String value) {
        return value != null && value.matches("\\d{10,}");
    }

    private String sanitize(String text) {
        return text.replace("@everyone", "everyone").replace("@here", "here");
    }
}
