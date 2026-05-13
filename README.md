# MC Discord Bridge

Server-side NeoForge mod for Minecraft `1.21.1` that runs an embedded Discord bot and bridges events/chat.

## Features (v1)
- Minecraft -> Discord: server start/stop, player join/leave, deaths
- Discord -> Minecraft chat relay from a configured channel
- Admin channel logging with command redaction
- Guild text setup wizard command: `!bridge setup`

## Requirements
- Java 21
- NeoForge server compatible with `21.1.229`
- Discord bot token and bot invited to your server

## Discord Bot Intents/Permissions
Enable in Discord Developer Portal:
- Message Content Intent

Bot permissions:
- Read Messages / View Channels
- Send Messages
- Read Message History

## Configuration
This mod uses server config values:
- `discordApiKey` (literal token or `env:DISCORD_BOT_TOKEN`)
- `whitelistGuildId` (required whitelist guild)
- `chatChannelId`
- `adminLogChannelId`
- `enableMessageContentIntent` (set `true` only if enabled in Discord Developer Portal)
- `enableEmbeds` (send rich embeds; falls back to plain text on errors)
- `embedColorHex` (hex color for embeds, ex: `#57A5FF`)
- `includePlayerHeadInEmbeds` (include player thumbnail from configured template)
- `playerHeadUrlTemplate` (must include `%uuid%`, default uses Crafatar)
- `enableBotActivityRotation`
- `botActivities` (list of activity strings to rotate)
- `botActivityRotateSeconds` (rotation interval, minimum 5)
- `botActivityType` (`PLAYING`, `WATCHING`, `LISTENING`, `COMPETING`)
- `dayMilestoneGap` (send day milestone every N days; `0` disables, `10` -> days 10/20/30)
- `enableAccountLinking` (enable Discord-to-Minecraft account linking)
- `linkCodeExpirySeconds` (one-time code lifetime)
- `linkCodeLength` (code size, 4 to 12)

First run creates config in the server config directory.

### Token via environment variable
Set:
```bash
export DISCORD_BOT_TOKEN="your_token"
```

Then set in config:
```toml
discordApiKey = "env:DISCORD_BOT_TOKEN"
```

## Setup Wizard
In your whitelisted Discord server, run:
```text
!bridge setup
```

Then complete:
- `!bridge chat <channelId>`
- `!bridge admin <channelId>`

Only Discord admins can run setup.

## Account Linking
- On Discord, run slash command: `/bridge-link`
- The bot replies ephemerally with a one-time code
- In Minecraft, run: `/bridge link <code>`
- `linkCodeExpirySeconds` controls how long the code remains valid

## Development
Use Makefile helpers:
```bash
make setup
make build
make run-server
make run-prod-local
make test
make jar-info
```

Or directly:
```bash
./gradlew build
```

Output jar is in `build/libs`.

`make run-prod-local` installs a local NeoForge server in `.local-neoforge-server`, copies the built mod jar into `mods/`, accepts EULA for local testing, and starts the server using NeoForge's production launch args.

## Notes
- This is server-only. Do not install on clients.
- Discord failures are logged and do not intentionally crash server startup.
- Avoid logging secrets in command usage; redaction is enabled by default.
