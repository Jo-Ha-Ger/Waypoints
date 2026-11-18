package io.github.johager.waypoints;

import net.kyori.adventure.bossbar.BossBar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Alle Wegpunkte eines Spielers plus Einstellungen, Posteingang und Navigationszustand. */
public final class PlayerData {

    /** Interne Kennung des Todespunkts (Namen dürfen nicht mit # beginnen). */
    public static final String DEATH_ID = "#death";
    /** Maximale Anzahl wartender geteilter Wegpunkte. */
    public static final int INBOX_LIMIT = 18;

    private final UUID uuid;
    private String lastName;
    private final List<Waypoint> waypoints = new ArrayList<>();
    private Waypoint death;
    private final List<Shared> inbox = new ArrayList<>();
    private int nextShareId = 1;

    /** Kennung des aktiven Ziels (siehe {@link Ref#id()}) oder {@code null}. Wird mitgespeichert. */
    private String navigation;
    /** Gewählte Sprache oder {@code null} = automatisch. */
    private Lang language;
    private boolean deathEnabled = true;

    // Laufzeitzustand (wird nicht gespeichert)
    BossBar bar;
    double startDistance = -1;
    boolean lastConverted;

    public PlayerData(UUID uuid, String lastName) {
        this.uuid = uuid;
        this.lastName = lastName;
    }

    public UUID uuid() {
        return uuid;
    }

    public String lastName() {
        return lastName;
    }

    public void lastName(String name) {
        this.lastName = name;
    }

    /** Sortierte Kopie der eigenen Wegpunkte (ohne Todespunkt). */
    public List<Waypoint> waypoints() {
        List<Waypoint> copy = new ArrayList<>(waypoints);
        copy.sort(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    public int count() {
        return waypoints.size();
    }

    /** Sucht einen eigenen Wegpunkt; {@link #DEATH_ID} liefert den Todespunkt. */
    public Waypoint find(String name) {
        if (name == null) {
            return null;
        }
        if (DEATH_ID.equalsIgnoreCase(name.trim())) {
            return death;
        }
        for (Waypoint wp : waypoints) {
            if (wp.isNamed(name)) {
                return wp;
            }
        }
        return null;
    }

    /** Fügt hinzu oder ersetzt einen Wegpunkt mit gleichem Namen. */
    public void put(Waypoint waypoint) {
        waypoints.removeIf(wp -> wp.isNamed(waypoint.name()));
        waypoints.add(waypoint);
    }

    /** Entfernt einen eigenen Wegpunkt (oder mit {@link #DEATH_ID} den Todespunkt). */
    public Waypoint remove(String name) {
        Waypoint wp = find(name);
        if (wp == null) {
            return null;
        }
        if (wp == death) {
            death = null;
        } else {
            waypoints.remove(wp);
        }
        if (navigation != null && !navigation.startsWith(Ref.SERVER_PREFIX) && find(navigation) == null) {
            navigation = null;
        }
        return wp;
    }

    /** Freier Name: "Basis", sonst "Basis (2)", "Basis (3)", ... */
    public String uniqueName(String base) {
        String name = base.length() > 32 ? base.substring(0, 32) : base;
        int n = 2;
        while (find(name) != null) {
            String suffix = " (" + n++ + ")";
            String cut = base.length() + suffix.length() > 32 ? base.substring(0, 32 - suffix.length()) : base;
            name = cut + suffix;
        }
        return name;
    }

    public Waypoint death() {
        return death;
    }

    public void death(Waypoint death) {
        this.death = death;
        if (death == null && DEATH_ID.equals(navigation)) {
            navigation = null;
        }
    }

    // ------------------------------------------------------------------ Posteingang

    public List<Shared> inbox() {
        return List.copyOf(inbox);
    }

    /** Legt einen geteilten Wegpunkt ab. Gibt {@code null} zurück, wenn der Posteingang voll ist. */
    public Shared addShared(String from, Waypoint wp) {
        if (inbox.size() >= INBOX_LIMIT) {
            return null;
        }
        Shared shared = new Shared(nextShareId++, from, wp);
        inbox.add(shared);
        return shared;
    }

    public Shared takeShared(int id) {
        for (int i = 0; i < inbox.size(); i++) {
            if (inbox.get(i).id() == id) {
                return inbox.remove(i);
            }
        }
        return null;
    }

    public void clearInbox() {
        inbox.clear();
    }

    // ------------------------------------------------------------------ Einstellungen

    public boolean isNavigatingTo(String id) {
        return navigation != null && id != null && navigation.equalsIgnoreCase(id);
    }

    public String navigation() {
        return navigation;
    }

    public void navigation(String id) {
        this.navigation = id;
        this.startDistance = -1;
    }

    public Lang language() {
        return language;
    }

    public void language(Lang language) {
        this.language = language;
    }

    public boolean deathEnabled() {
        return deathEnabled;
    }

    public void deathEnabled(boolean enabled) {
        this.deathEnabled = enabled;
    }

    List<Waypoint> rawWaypoints() {
        return waypoints;
    }
}
