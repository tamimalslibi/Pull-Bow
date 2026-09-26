# Pull Bow Plugin

A Minecraft plugin for Paper/Spigot 1.21 that adds a special enchanted bow that pulls hit players directly to the shooter.

## Features

✅ **Pull Mechanic**: Pulls hit players directly to the shooter's front position  
✅ **15-Second Cooldown**: Per-player cooldown system  
✅ **OP Only**: Only server operators can obtain the Pull Bow  
✅ **Enchanted Bow**: Red name with full enchantments (Infinity, Power V, Unbreaking, Mending, Flame)  
✅ **Custom Lore**: Descriptive lore with enchantment list  

## Installation

1. Download or compile the plugin `.jar` file
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Use `/getpullbow` as an OP to obtain the bow

## Commands

- `/getpullbow` - Get a Pull Bow (requires OP)

## Permissions

- `pullbow.get` - Allows getting the Pull Bow (default: op)

## Building from Source

### Requirements
- Java 21 or higher
- Maven 3.6+

### Steps

```bash
# Clone the repository
git clone https://github.com/yourusername/PullBowPlugin.git
cd PullBowPlugin

# Compile with Maven
mvn clean package

# The compiled .jar will be in target/PullBowPlugin-1.0.jar
```

## How It Works

1. Operator uses `/getpullbow` to get the special bow
2. Shoot another player with the Pull Bow
3. When arrow hits, target is pulled directly to shooter's front
4. 15-second cooldown activates (whether hit or miss)
5. Cooldown is tracked separately for each player

## Technical Details

- **Server**: Paper/Spigot 1.21+
- **API Version**: 1.21
- **Java Version**: 21
- **Pull Duration**: Up to 2 seconds (40 ticks)
- **Final Distance**: 2 blocks in front of shooter

## License

This plugin is provided as-is for use on Minecraft servers.

## Support

For issues or questions, please open an issue on GitHub.
