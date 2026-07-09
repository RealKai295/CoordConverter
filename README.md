# CoordConvert

Convert coordinates and Xaero's Minimap waypoints between Minecraft dimensions with a single command.

CoordConvert is a lightweight client-side Fabric mod for Minecraft **1.21.x** (built and tested on **1.21.11**). It handles overworld/nether scaling for you and can drop the result straight into Xaero as a new waypoint.

## Features

- `/convert` command with tab completion for dimensions and waypoint names
- Convert raw coordinates or existing Xaero waypoints
- Automatic 8:1 overworld/nether conversion
- Creates or updates a waypoint in the target dimension
- Works across multiple Xaero versions via reflection (no hard API dependency)

## Requirements

| Dependency | Required |
| --- | --- |
| Minecraft Java Edition 1.21.x | Yes |
| Fabric Loader | Yes |
| Fabric API | Yes |
| [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) | Required for waypoint lookup and creation |

Coordinate conversion works without Xaero if you pass raw `x y z` values, but waypoint lookup and automatic waypoint saving need Xaero installed.

## Installation

1. Install Fabric Loader for Minecraft 1.21.x.
2. Download the latest `coordconvert` jar from [Releases](https://github.com/kai/CoordConvert/releases).
3. Place it in your `mods` folder alongside Fabric API and Xaero's Minimap.
4. Launch the game.

## Usage

```
/convert <waypoint> <dimension>
/convert <x> <y> <z> <dimension>
```

Press `Tab` to autocomplete waypoint names and dimensions.

### Examples

```
/convert Base nether
/convert "My Base" nether
/convert 800 64 800 nether
/convert -120 70 240 overworld
```

### Dimensions

| Dimension | Names |
| --- | --- |
| Overworld | `overworld`, `ow`, `world` |
| Nether | `nether`, `n`, `hell` |
| The End | `end`, `e`, `the_end` |

### Conversion rules

- **Overworld ↔ Nether:** divide or multiply by 8 on X and Z
- **The End:** X and Z stay the same
- **Y** is never scaled

Example: overworld `800 64 800` becomes nether `100 64 100`.

### Output

```
Converted Base: Overworld 800 64 800 -> Nether 100 64 100. Added waypoint Base (Nether)
```

Waypoint names and coordinates are shown in bold. Errors are plain text with a short explanation.

## Building

```bash
git clone https://github.com/kai/CoordConvert.git
cd CoordConvert
./gradlew build
```

The built jar is at `build/libs/coordconvert-<version>.jar`.

You need **Java 21** or newer.

## Project layout

```
src/client/java/com/coordconvert/
├── command/
├── conversion/
├── util/
└── xaero/
```

## License

WTFPL - see [LICENSE](LICENSE).

## Credits

- [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) by thexaero
- Built with [Fabric](https://fabricmc.net/)
