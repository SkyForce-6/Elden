# Elden

**Elden** is a Paper plugin for Minecraft that brings **Elden Ring-inspired gameplay systems** into your server.  
It adds mechanics such as **Sites of Grace, runes, classes, leveling, stamina-based combat, flasks, Torrent, spirit springs, custom bosses, enemies, weapons, armor, spells, talismans, and more**.

## Features

- **Sites of Grace** for activation, attunement, and fast travel
- **Rune system** for progression and upgrades
- **Class system** inspired by Elden Ring starting classes
- **Leveling system** with customizable attributes and progression
- **Stamina-based combat** with dodge rolls, cooldowns, poise, and i-frames
- **Custom weapons and armor**
- **Ashes of War, spells, and Spirit Ashes**
- **Torrent mount system** with whistle, movement tuning, healing items, and spirit springs
- **Flask system** inspired by Crimson and Cerulean Flasks
- **Boss and enemy systems** with spawn and portal management
- **Talismans** and player profile/stat systems
- Highly configurable through YAML configuration files

## Tech Stack

- **Language:** Java
- **Build Tool:** Gradle Kotlin DSL
- **Platform:** Paper API
- **Database:** SQLite
- **Shading:** Shadow plugin

## Requirements

- **Java 25**
- A **Paper** server compatible with the configured API version
- Gradle (or use the included Gradle wrapper)

## Installation

Clone the repository:

```bash
git clone https://github.com/SkyForce-6/Elden.git
cd Elden
```

Build the plugin:

```bash
./gradlew build
```

On Windows:

```bash
gradlew.bat build
```

After building, use the generated plugin jar from the build output and place it into your server's `plugins` folder.

## Development

Run a local Paper development server with Gradle:

```bash
./gradlew runServer
```

This project uses the `run-paper` Gradle plugin for local testing.

## Configuration

The plugin provides multiple configuration files inside `src/main/resources`:

- `plugin.yml` — plugin metadata, commands, and permissions
- `config.yml` — gameplay systems and balancing configuration
- `messages.yml` — customizable messages

You can adjust many gameplay systems, including:

- grace activation radius
- visual effects
- boss spawns and portals
- rune display settings
- smithing settings
- custom model data
- combat balancing
- stamina and dodge behavior
- compass and bossbar settings
- Torrent movement and healing
- flask values and cooldowns
- class defaults
- level caps and attribute scaling
- spirit spring locations
- grace locations

## Commands

The plugin currently includes commands such as:

- `/grace`
- `/runes`
- `/eldenclass`
- `/level`
- `/weapon`
- `/smithing`
- `/armor`
- `/profile`
- `/ash`
- `/spell`
- `/spiritash`
- `/spiritspring`
- `/talisman`
- `/torrent`
- `/boss`
- `/enemy`

## Permissions

The plugin defines permissions for both regular players and admins, including systems like:

- grace usage and administration
- rune management
- combat access
- class usage and reset
- weapon, armor, ash, spell, and talisman administration
- spirit summon usage
- Torrent usage
- boss and enemy administration

Example permission nodes:

- `elden.grace.use`
- `elden.grace.admin`
- `elden.runes.use`
- `elden.runes.admin`
- `elden.class.use`
- `elden.class.admin`
- `elden.level.use`
- `elden.weapon.admin`
- `elden.armor.admin`
- `elden.spirit.admin`
- `elden.mount.torrent`
- `elden.boss.admin`
- `elden.enemy.admin`

## Project Structure

```bash
Elden/
├── src/main/java/         # Plugin source code
├── src/main/resources/    # plugin.yml, config.yml, messages.yml
├── gradle/                # Gradle wrapper files
├── build.gradle.kts       # Build configuration
├── settings.gradle.kts    # Project settings
├── gradle.properties      # Gradle properties
├── gradlew
├── gradlew.bat
└── README.md
```

## Goals

The goal of this project is to recreate the feel of **Elden Ring** inside Minecraft by combining:

- exploration and checkpoints
- combat depth
- RPG progression
- class identity
- boss encounters
- mobility systems
- collectible and upgradeable equipment

## Contributing

Contributions, suggestions, and improvements are welcome.  
If you want to help, feel free to fork the repository and open a pull request.

## License

Add your preferred license here.  
If you do not have one yet, you may want to choose a license such as **MIT**.

## Author

Created by [SkyForce-6](https://github.com/SkyForce-6)
