package io.github.johager.waypoints;

/** Ein gefundener Wegpunkt plus seine Art (eigener, Todespunkt oder Server-Wegpunkt). */
public record Ref(Kind kind, Waypoint waypoint) {

    public static final String SERVER_PREFIX = "#server:";

    public enum Kind { OWN, DEATH, SERVER }

    /** Kennung, unter der eine laufende Navigation gespeichert wird. */
    public String id() {
        return switch (kind) {
            case OWN -> waypoint.name();
            case DEATH -> PlayerData.DEATH_ID;
            case SERVER -> SERVER_PREFIX + waypoint.name();
        };
    }
}
