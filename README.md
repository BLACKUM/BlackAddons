# BlackAddons

BlackAddons is a utility and quality-of-life mod for Minecraft, designed to enhance the player experience with advanced personalization, privacy tools, and dungeon utilities. I totally didn't make readme with ai. Totally.

## Features

### Utility

- **Profile Viewer**: Inspect player stats directly in-game without using external sites. View dungeon stats, skills, slayers, and more with visualized graphs for class levels and floor completions.
- **Dungeon Party Finder**: A dedicated menu for finding and creating dungeon groups. It integrates with external services to provide a more reliable experience than the standard party finder.
- **IRC Chat**: A global chat system that lets you communicate with other mod users across different servers. It features an emoji selector and handles media previews directly in the interface.
- **RNG Tracker**: Automatically tracks rare drops and displays them on screen. Includes options to simulate drops for testing purposes.
- **Bot Integration**: Seamlessly syncs daily stats and integrates with external bots for leaderboard tracking.
- https://github.com/BLACKUM/rtca-bot-hypixel (bot)

### Privacy & Mod Hider

- **Spoof Mode**: Diverse control over how your client identifies itself to servers.
    - **Vanilla**: Pretend to be a completely vanilla client.
    - **Modded**: Identify as modded but hide specific mods from the list.
    - **Custom**: Set a custom client brand name, hide mods, and disable payloads.
- **Hide Mods**: Prevent servers from querying your detailed mod list.
- **Payload Control**: Block or whitelist custom payload channels to prevent server-side mod detection hacks and improve security. Yes, even modanouncer from Firmament.

### Cheats (AtkLxve is maintaining that, i don't take any accountability if something goes wrong)

- **AutoTNT**: Automatically interacts with specific blocks (Cracked Stone Bricks, Smooth Stone Slab) using TNT.
    - **Smart Delay**: Configurable tick delays for interactions.
    - **Safety**: Built-in anti-spam to prevent accidental multiple clicks and auto-unequip functionality.

### Quality of Life

- **Fullbright**: Toggle permanent Night Vision for better visibility in dark areas.
- **Custom GUI**: A clean, modern card-based interface for settings with movable and resizable elements.
- **Notifications**: In-game toast notifications for important events and updates.

## Commands

- `/b`, `/black`, or `/blackaddons`: Open the main settings menu.
- `/b pf`: Open the Dungeon Party Finder.
- `/b irc`: Open the IRC chat interface.
- `/b pv [player] [force]`: Open the Profile Viewer for a specific player. Use the force argument to refresh cached data.
- `/b preview [url]`: Open a full-screen preview for a direct image or Discord media link.
- `/b commandaliases add [alias] [original command]`: Create a custom command alias.
- `/b commandaliases del [alias]`: Remove a command alias.
- `/b commandaliases list`: List all currently configured aliases.

## Credits

- **Blackum**: Erm that's me hello
- **AtkLxve**: Cheats
- **Autismo**: Helped me with rewriting, understanding java and a lot more stuff
- **adjectiven0un**: Original XP calculation logic and API inspiration from [adjectils](https://adjectils.com/dungeon.html).

## Powered By

- **Player Data**: UUID lookup provided by [PlayerDB](https://playerdb.co/).
- **Hypixel Data**: Official [Hypixel API](https://api.hypixel.net/) for Bazaar prices.
- **Auction House**: Price data provided by [Moulberry's Codes](https://moulberry.codes/).
- **Pricing for stuff like Shiny necron's handle**: Item market data from [Coflnet](https://sky.coflnet.com/).
