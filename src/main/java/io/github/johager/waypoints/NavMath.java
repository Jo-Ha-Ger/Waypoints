package io.github.johager.waypoints;


/** Reine Rechenlogik ohne Server-Abhängigkeiten (dadurch einfach testbar). */
public final class NavMath {

    private NavMath() {
    }

    /** Pfeile im Uhrzeigersinn, beginnend mit "geradeaus". */
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    /** Ergebnis der Zielberechnung für die aktuelle Dimension des Spielers. */
    public record Target(double x, double y, double z, boolean converted) {
    }

    /**
     * Rechnet ein Ziel in die Dimension des Spielers um.
     *
     * @return das Ziel oder {@code null}, wenn es in einer nicht erreichbaren Dimension liegt
     */
    public static Target resolve(String playerWorld, String playerDim, Waypoint wp) {
        if (playerWorld.equals(wp.world())) {
            return new Target(wp.x(), wp.y(), wp.z(), false);
        }
        if ("NETHER".equals(playerDim) && "NORMAL".equals(wp.dimension())) {
            return new Target(wp.x() / 8.0, wp.y(), wp.z() / 8.0, true);
        }
        if ("NORMAL".equals(playerDim) && "NETHER".equals(wp.dimension())) {
            return new Target(wp.x() * 8.0, wp.y(), wp.z() * 8.0, true);
        }
        return null;
    }

    /**
     * Pfeil relativ zur Blickrichtung.
     * Minecraft-Yaw: 0 = Süden (+Z), 90 = Westen (-X), 180 = Norden, 270 = Osten.
     */
    public static String arrow(double fromX, double fromZ, float playerYaw, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        if (dx * dx + dz * dz < 0.25) {
            return "●";
        }
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = normalize(targetYaw - playerYaw);
        int index = (int) Math.floorMod(Math.round(rel / 45.0), 8L);
        return ARROWS[index];
    }

    /** Normalisiert einen Winkel auf [-180, 180). */
    static double normalize(double angle) {
        double a = angle % 360.0;
        if (a >= 180.0) {
            a -= 360.0;
        } else if (a < -180.0) {
            a += 360.0;
        }
        return a;
    }

    public static String distance(double meters, Lang lang) {
        if (meters < 1000) {
            return Math.round(meters) + " m";
        }
        return String.format(lang.locale(), "%.1f km", meters / 1000.0);
    }

    public static String coords(double x, double y, double z) {
        return "X " + (long) Math.floor(x) + "  Y " + (long) Math.floor(y) + "  Z " + (long) Math.floor(z);
    }

    /** Prüft einen Namen. Gibt den Schlüssel der Fehlermeldung zurück oder {@code null}, wenn alles passt. */
    public static String validateName(String name) {
        if (name == null || name.isBlank()) {
            return "name.empty";
        }
        if (name.length() > 32) {
            return "name.too-long";
        }
        if (name.indexOf('§') >= 0 || name.startsWith("/") || name.startsWith("#")) {
            return "name.invalid";
        }
        return null;
    }
}
