package io.github.johager.waypoints;

/**
 * Ein gespeicherter Wegpunkt.
 *
 * @param name      Anzeigename (eindeutig pro Spieler, Groß-/Kleinschreibung egal)
 * @param world     Name der Welt, z. B. "world" oder "world_nether"
 * @param dimension NORMAL, NETHER, THE_END oder CUSTOM
 * @param created   Zeitpunkt der Erstellung (Millisekunden)
 */
public record Waypoint(String name, String world, String dimension,
                       double x, double y, double z, long created) {

    public boolean isNamed(String other) {
        return name.equalsIgnoreCase(other.trim());
    }
}
