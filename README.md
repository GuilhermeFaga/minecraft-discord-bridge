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

## Development
Use Makefile helpers:
```bash
make setup
make build
make run-server
make test
make jar-info
```

Or directly:
```bash
./gradlew build
```

Output jar is in `build/libs`.

## Notes
- This is server-only. Do not install on clients.
- Discord failures are logged and do not intentionally crash server startup.
- Avoid logging secrets in command usage; redaction is enabled by default.
