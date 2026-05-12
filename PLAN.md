# MC Discord Bridge - Implementation Plan

## Project Metadata
- Group: `com.faga`
- Mod ID: `mcdiscordbridge`
- Artifact: `mcdiscordbridge`
- Target: NeoForge on Minecraft `1.21.1` stable line
- Java: `21`
- Packaging: single deployable mod `.jar` with embedded JDA (relocated/shaded)
- Runtime role: dedicated server-side mod only (no client requirement)

## Goals
1. Build a server-only NeoForge mod that starts/stops a Discord bot with server lifecycle.
2. Bridge key Minecraft events to Discord channels.
3. Bridge Discord chat messages to Minecraft chat safely (thread-safe, loop-safe).
4. Provide a setup wizard (`!bridge setup`) in whitelisted Discord guild for admins.
5. Enforce secure config practices and safe failure behavior.
6. Provide developer ergonomics with Gradle, Makefile, and README.

## Scope (v1)

### In Scope
- Minecraft -> Discord:
  - Server started/stopped
  - Player join/leave
  - Death messages
  - Advancement announcements
  - Player chat relay (toggle)
- Discord -> Minecraft:
  - One configured channel relay to in-game chat
- Admin logs:
  - Server lifecycle
  - Player join/leave
  - Command execution logs with redaction
  - Kicks/bans if exposed cleanly by NeoForge events
- Setup wizard:
  - `!bridge setup`
  - Admin-only, whitelisted guild only
  - Configure channels and key toggles

### Out of Scope
- Slash commands
- Account linking
- Role sync
- Whitelist management via Discord
- Multi-server orchestration
- Rich embeds everywhere

## Architecture

```text
Minecraft Dedicated Server
 └─ NeoForge mod (mcdiscordbridge)
     ├─ Config (server config, env token support)
     ├─ Minecraft event listeners
     ├─ Discord bot service (JDA)
     ├─ Discord message listener + setup wizard
     ├─ Channel router (chat/admin)
     └─ Formatter + redactor utilities
```

## Package Layout

```text
com.faga.mcdiscordbridge
 ├─ DiscordBridgeMod.java
 ├─ config/
 │   └─ BridgeConfig.java
 ├─ discord/
 │   ├─ DiscordBot.java
 │   ├─ DiscordMessageHandler.java
 │   ├─ DiscordChannelRouter.java
 │   └─ DiscordSetupWizard.java
 ├─ minecraft/
 │   ├─ MinecraftEventHandler.java
 │   ├─ MinecraftMessageSender.java
 │   └─ AdminLogHandler.java
 └─ util/
     ├─ MessageFormatter.java
     └─ CommandRedactor.java
```

## Configuration Design

### Required Server Config
- `discordApiKey` (literal value or `env:DISCORD_BOT_TOKEN`)
- `whitelistGuildId` (only this guild accepted for setup and runtime)

### Channel Config
- `chatChannelId`
- `adminLogChannelId`

### Feature Toggles
- `enableJoinLeave`
- `enableDeaths`
- `enableAdvancements`
- `enableMinecraftChatToDiscord`
- `enableDiscordChatToMinecraft`
- `enableAdminLogs`

### Security/Redaction
- `redactCommandArguments` (default `true`)
- `redactedCommands` defaults:
  - `login`, `register`, `password`, `token`, `discord`, `op`, `deop`

## Threading and Safety Rules
1. Discord -> Minecraft always scheduled onto server thread via `server.execute(...)`.
2. Minecraft -> Discord sends asynchronously with JDA `queue()`.
3. No blocking IO on Minecraft server thread.
4. Bot failures never crash server startup.
5. Invalid channel/guild routes are disabled with explicit logs.

## Setup Wizard (`!bridge setup`) Specification

### Entry Conditions
- Message must be in `whitelistGuildId`.
- Sender must be admin.
- Ignore bot and webhook senders.

### Flow
1. Admin runs `!bridge setup`.
2. Bot sends guided prompts for chat/admin channels and key toggles.
3. Validate every ID belongs to whitelisted guild.
4. Persist values to server config.
5. Confirm setup completion.

### Guardrails
- Timeout/abort support.
- Unauthorized users are rejected.
- Idempotent rerun support.

## Build and Dependency Plan
1. Bootstrap NeoForge Gradle project for 1.21.1.
2. Configure Java toolchain to 21.
3. Add JDA dependency.
4. Exclude voice dependency (`opus-java`) unless needed.
5. Shade and relocate JDA/transitives into final jar.
6. Validate final jar loads on dedicated server.

## Milestones
1. Scaffold project.
2. Core lifecycle.
3. Server config.
4. Discord service.
5. MC -> Discord baseline events.
6. Discord -> MC chat.
7. Deaths/advancements/MC chat relay.
8. Admin logs + redaction.
9. Setup wizard.
10. Docs + Makefile + validation.

## Makefile Plan
- `help`
- `setup`
- `build`
- `run-server`
- `test`
- `clean`
- `jar-info`

## Definition of Done
- Mod builds and runs on dedicated server with Java 21.
- Bot lifecycle integrates with server lifecycle.
- Required bridge flows work end-to-end.
- Setup wizard configures channels/settings securely.
- Discord failures do not crash server.
- README and Makefile are complete.
