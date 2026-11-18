package io.github.johager.waypoints;

/** Ein Wegpunkt, den ein anderer Spieler geteilt hat und der auf Annahme wartet. */
public record Shared(int id, String from, Waypoint waypoint) {
}
