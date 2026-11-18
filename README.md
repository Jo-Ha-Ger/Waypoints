# Waypoints

A lightweight [Paper](https://papermc.io) plugin to save locations, share them with friends and navigate to them with a live on-screen display. English and German included.

*Deutsche Beschreibung weiter unten.*

**Requires:** Paper 26.3 or newer, Java 25

## Features

- **Save locations** with a name, either with `/wp set <name>` or from the menu.
- **Navigation display** at the top of the screen: name, a direction arrow that turns with your view, distance, height difference and coordinates. The bar fills up as you get closer.
- **Menu** (`/wp`) with all waypoints. It works for Bedrock players via Geyser too.
- **Server waypoints** that admins create for everyone, e.g. Spawn or Town.
- **Share** waypoints with a player (`/wp share`). The recipient gets an inbox and accept/decline buttons.
- **Show** a waypoint to everyone in chat (`/wp show`). Others can save it with one click.
- **Death point** is saved automatically when you die, with a "navigate there" button. Every player can turn it off for themselves.
- **Nether conversion**: in the Nether, Overworld coordinates are divided by 8 automatically, and the other way round.
- **English and German**, picked automatically from the player's game language. Each player can change it.
- Data is stored per player in `plugins/Waypoints/players/<UUID>.yml`, so it survives name changes.

## Commands

| Command | Description |
|---|---|
| `/wp` | Open the menu |
| `/wp set <name>` | Save your current location |
| `/wp nav <name>` | Start navigation (own, server waypoint or `death`) |
| `/wp stop` | Stop navigation |
| `/wp list` | List all waypoints in chat (clickable) |
| `/wp del <name>` | Delete a waypoint |
| `/wp share <player> <name>` | Send a waypoint to a player |
| `/wp show <name>` | Show a waypoint to everyone in chat |
| `/wp inbox` | Waypoints others shared with you |
| `/wp death` | Turn the death point on/off for yourself |
| `/wp language <en\|de\|auto>` | Change language |
| `/wp server set <name>` | Create a server waypoint (admin) |
| `/wp server del <name>` | Delete a server waypoint (admin) |

Aliases: `/waypoint`, `/waypoints`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `waypoints.use` | everyone | Save, navigate, list, delete |
| `waypoints.share` | everyone | `/wp share` and `/wp show` |
| `waypoints.admin` | OP | Create and delete server waypoints |
| `waypoints.*` | OP | All of the above |

Example with LuckPerms: `/lp group admin permission set waypoints.admin true`

## Configuration

`plugins/Waypoints/config.yml`:

```yaml
default-language: en      # en or de
auto-language: true       # use each player's game language
max-waypoints: 100        # per player (death point not counted)
death-point: true         # false = disable the death point for everyone
arrival-distance: 5       # blocks
stop-on-arrival: true
display-interval-ticks: 4 # 20 ticks = 1 second
```

## Building

```bash
mvn package
```

The JAR is written to `target/Waypoints-<version>.jar`. GitHub Actions also builds every push automatically.

---

## Deutsch

Ein schlankes Paper-Plugin: Orte speichern, mit Freunden teilen und per Anzeige oben am Bildschirm hinnavigieren.

**Voraussetzung:** Paper 26.3 oder neuer, Java 25

- Orte mit Namen speichern (`/wp set <Name>` oder im Menü `/wp`).
- Anzeige oben am Bildschirm mit Richtungspfeil, Entfernung und Koordinaten.
- Server-Wegpunkte für alle, z. B. Spawn oder Stadt. Anlegen nur mit `waypoints.admin`.
- Wegpunkte teilen (`/wp share <Spieler> <Name>`) oder allen im Chat zeigen (`/wp show <Name>`). Geteilte Wegpunkte landen im Posteingang (`/wp inbox`).
- Todespunkt wird automatisch gespeichert und ist pro Spieler abschaltbar (`/wp death`).
- Automatische Umrechnung der Koordinaten zwischen Oberwelt und Nether.
- Deutsch und Englisch, automatisch passend zur Spielsprache, umstellbar mit `/wp language de`.

Alle Befehle und Rechte stehen in den Tabellen oben.

## License

[MIT](LICENSE) © 2026 Jo-Ha-Ger
