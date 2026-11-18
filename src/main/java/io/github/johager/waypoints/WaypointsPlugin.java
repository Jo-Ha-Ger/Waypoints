package io.github.johager.waypoints;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WaypointsPlugin extends JavaPlugin {

    public static final String PERM_USE = "waypoints.use";
    public static final String PERM_SHARE = "waypoints.share";
    public static final String PERM_ADMIN = "waypoints.admin";

    /** Ort, der auf einen Namen aus dem Chat wartet (Knopf „Aktuellen Ort speichern“). */
    record PendingName(Location location, long expiresAt) {
    }

    /** Ein per /wp show im Chat gezeigter Wegpunkt, den andere per Klick speichern können. */
    record ShownWaypoint(Waypoint waypoint, long expiresAt) {
    }

    private static final long SHOW_LIFETIME_MS = 15 * 60_000L;
    private static final long SHOW_COOLDOWN_MS = 10_000L;

    private DataStore store;
    private Navigation navigation;
    private final Map<UUID, PendingName> pendingNames = new ConcurrentHashMap<>();
    private final Map<Integer, ShownWaypoint> shown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShow = new ConcurrentHashMap<>();
    private int nextShownId = 1;

    private Lang defaultLang;
    private boolean autoLanguage;
    private int maxWaypoints;
    private boolean deathWaypoint;
    private double arrivalDistance;
    private boolean stopOnArrival;

    @Override
    public void onLoad() {
        // Daten aus der Zeit, als das Plugin noch "Wegpunkte" hieß, übernehmen
        File folder = getDataFolder();
        File old = new File(folder.getParentFile(), "Wegpunkte");
        if (old.isDirectory() && !folder.exists()) {
            if (old.renameTo(folder)) {
                getLogger().info("Migrated data from plugins/Wegpunkte to plugins/" + folder.getName());
            } else {
                getLogger().warning("Could not move " + old + " to " + folder);
            }
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Lang configured = Lang.parse(getConfig().getString("default-language", "en"));
        defaultLang = configured != null ? configured : Lang.EN;
        autoLanguage = getConfig().getBoolean("auto-language", true);
        maxWaypoints = Math.max(1, getConfig().getInt("max-waypoints", 100));
        deathWaypoint = getConfig().getBoolean("death-point", true);
        arrivalDistance = Math.max(1.0, getConfig().getDouble("arrival-distance", 5.0));
        stopOnArrival = getConfig().getBoolean("stop-on-arrival", true);
        long interval = Math.max(1L, getConfig().getLong("display-interval-ticks", 4L));

        store = new DataStore(getDataFolder(), getLogger());
        store.loadServerWaypoints();
        navigation = new Navigation(this);

        WaypointCommand command = new WaypointCommand(this);
        PluginCommand pluginCommand = getCommand("waypoint");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        // Falls per Neustart/Reload Spieler schon online sind
        for (Player player : Bukkit.getOnlinePlayers()) {
            store.load(player.getUniqueId(), player.getName());
        }

        Runnable hudTask = navigation::tick;
        Bukkit.getScheduler().runTaskTimer(this, hudTask, 20L, interval);
    }

    @Override
    public void onDisable() {
        if (store == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = store.get(player.getUniqueId());
            if (data != null) {
                navigation.hide(player, data);
            }
        }
        store.shutdown();
    }

    // ------------------------------------------------------------------ Sprache

    /** Sprache eines Spielers: eigene Wahl → Spielsprache (falls aktiv) → Server-Standard. */
    public Lang lang(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return defaultLang;
        }
        PlayerData data = store.get(player.getUniqueId());
        if (data != null && data.language() != null) {
            return data.language();
        }
        if (autoLanguage) {
            Lang fromClient = Lang.fromLocale(player.locale());
            if (fromClient != null) {
                return fromClient;
            }
        }
        return defaultLang;
    }

    public String t(CommandSender sender, String key, Object... args) {
        return I18n.t(lang(sender), key, args);
    }

    public void info(CommandSender sender, String key, Object... args) {
        Lang lang = lang(sender);
        sender.sendMessage(Text.info(lang, I18n.t(lang, key, args)));
    }

    public void success(CommandSender sender, String key, Object... args) {
        Lang lang = lang(sender);
        sender.sendMessage(Text.success(lang, I18n.t(lang, key, args)));
    }

    public void error(CommandSender sender, String key, Object... args) {
        Lang lang = lang(sender);
        sender.sendMessage(Text.error(lang, I18n.t(lang, key, args)));
    }

    // ------------------------------------------------------------------ Wegpunkte finden

    /** Anzeigename (der Todespunkt wird übersetzt). */
    public String displayName(Player player, Ref ref) {
        return ref.kind() == Ref.Kind.DEATH ? t(player, "death.name") : ref.waypoint().name();
    }

    /** Sucht anhand einer gespeicherten Kennung (siehe {@link Ref#id()}). */
    public Ref lookup(PlayerData data, String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith(Ref.SERVER_PREFIX)) {
            Waypoint wp = store.findServer(id.substring(Ref.SERVER_PREFIX.length()));
            return wp == null ? null : new Ref(Ref.Kind.SERVER, wp);
        }
        if (PlayerData.DEATH_ID.equalsIgnoreCase(id)) {
            return data.death() == null ? null : new Ref(Ref.Kind.DEATH, data.death());
        }
        Waypoint wp = data.find(id);
        return wp == null ? null : new Ref(Ref.Kind.OWN, wp);
    }

    /**
     * Sucht anhand einer Eingabe: erst eigene Wegpunkte, dann Server-Wegpunkte,
     * dann der Todespunkt ("death" oder sein übersetzter Name).
     */
    public Ref resolve(PlayerData data, String input) {
        String n = input.trim();
        if (n.startsWith(Ref.SERVER_PREFIX) || n.equalsIgnoreCase(PlayerData.DEATH_ID)) {
            return lookup(data, n);
        }
        Waypoint own = data.find(n);
        if (own != null) {
            return new Ref(Ref.Kind.OWN, own);
        }
        Waypoint server = store.findServer(n);
        if (server != null) {
            return new Ref(Ref.Kind.SERVER, server);
        }
        if (data.death() != null && (n.equalsIgnoreCase("death")
                || n.equalsIgnoreCase(I18n.t(Lang.EN, "death.name"))
                || n.equalsIgnoreCase(I18n.t(Lang.DE, "death.name")))) {
            return new Ref(Ref.Kind.DEATH, data.death());
        }
        return null;
    }

    /** Alle Einträge für Menü und Liste: Todespunkt, Server-Wegpunkte, eigene Wegpunkte. */
    public List<Ref> allRefs(PlayerData data) {
        List<Ref> refs = new ArrayList<>();
        if (data.death() != null) {
            refs.add(new Ref(Ref.Kind.DEATH, data.death()));
        }
        for (Waypoint wp : store.serverWaypoints()) {
            refs.add(new Ref(Ref.Kind.SERVER, wp));
        }
        for (Waypoint wp : data.waypoints()) {
            refs.add(new Ref(Ref.Kind.OWN, wp));
        }
        return refs;
    }

    // ------------------------------------------------------------------ Eigene Wegpunkte

    private Waypoint at(String name, Location location) {
        return new Waypoint(name, location.getWorld().getName(),
                location.getWorld().getEnvironment().name(),
                location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());
    }

    private String coords(Waypoint wp) {
        return NavMath.coords(wp.x(), wp.y(), wp.z());
    }

    public void createWaypoint(Player player, PlayerData data, String rawName, Location location) {
        String name = rawName.trim();
        String error = NavMath.validateName(name);
        if (error != null) {
            error(player, error);
            return;
        }
        if (data.find(name) != null) {
            error(player, "wp.exists", name);
            return;
        }
        if (data.count() >= maxWaypoints) {
            error(player, "wp.limit", maxWaypoints);
            return;
        }
        Waypoint wp = at(name, location);
        data.put(wp);
        store.save(data);
        success(player, "wp.saved", name, coords(wp), I18n.dimension(lang(player), wp.dimension()));
    }

    /** Löscht einen eigenen Wegpunkt oder den Todespunkt (Server-Wegpunkte: siehe {@link #deleteServerWaypoint}). */
    public void deleteWaypoint(Player player, PlayerData data, String name) {
        Ref ref = resolve(data, name);
        if (ref == null || ref.kind() == Ref.Kind.SERVER) {
            if (ref != null && player.hasPermission(PERM_ADMIN)) {
                deleteServerWaypoint(player, ref.waypoint().name());
            } else {
                error(player, "wp.not-found", name);
            }
            return;
        }
        boolean wasNavigating = data.isNavigatingTo(ref.id());
        data.remove(ref.id());
        if (wasNavigating) {
            navigation.hide(player, data);
        }
        store.save(data);
        success(player, "wp.deleted", displayName(player, ref));
    }

    /** Kopiert einen fremden Wegpunkt in die eigene Liste (Name wird bei Bedarf eindeutig gemacht). */
    public boolean adoptWaypoint(Player player, PlayerData data, Waypoint source) {
        if (data.count() >= maxWaypoints) {
            error(player, "wp.limit", maxWaypoints);
            return false;
        }
        String base = source.name();
        if (NavMath.validateName(base) != null) {
            base = t(player, "death.name");
        }
        String name = data.uniqueName(base);
        data.put(new Waypoint(name, source.world(), source.dimension(),
                source.x(), source.y(), source.z(), System.currentTimeMillis()));
        store.save(data);
        success(player, "share.accepted", name);
        return true;
    }

    // ------------------------------------------------------------------ Todespunkt

    public void saveDeathPoint(Player player, Location location) {
        if (!deathWaypoint) {
            return;
        }
        PlayerData data = store.get(player.getUniqueId());
        if (data == null || !data.deathEnabled()) {
            return;
        }
        data.death(at(PlayerData.DEATH_ID, location));
        store.save(data);
    }

    public void toggleDeathPoint(Player player, PlayerData data) {
        if (!deathWaypoint) {
            error(player, "death.server-off");
            return;
        }
        boolean enabled = !data.deathEnabled();
        data.deathEnabled(enabled);
        if (!enabled) {
            if (data.isNavigatingTo(PlayerData.DEATH_ID)) {
                navigation.hide(player, data);
            }
            data.death(null);
        }
        store.save(data);
        success(player, enabled ? "death.on" : "death.off");
    }

    // ------------------------------------------------------------------ Server-Wegpunkte

    public void createServerWaypoint(Player player, String rawName) {
        if (!player.hasPermission(PERM_ADMIN)) {
            error(player, "server.no-permission");
            return;
        }
        String name = rawName.trim();
        String error = NavMath.validateName(name);
        if (error != null) {
            error(player, error);
            return;
        }
        if (store.findServer(name) != null) {
            error(player, "server.exists", name);
            return;
        }
        Waypoint wp = at(name, player.getLocation());
        store.putServer(wp);
        success(player, "server.saved", name, coords(wp), I18n.dimension(lang(player), wp.dimension()));
    }

    public void deleteServerWaypoint(Player player, String name) {
        if (!player.hasPermission(PERM_ADMIN)) {
            error(player, "server.no-permission");
            return;
        }
        Waypoint removed = store.removeServer(name);
        if (removed == null) {
            error(player, "server.not-found", name);
            return;
        }
        success(player, "server.deleted", removed.name());
        // Laufende Navigationen anderer Spieler beenden sich beim nächsten Update von selbst.
    }

    // ------------------------------------------------------------------ Teilen

    public void share(Player sender, PlayerData data, String targetName, String waypointName) {
        if (!sender.hasPermission(PERM_SHARE)) {
            error(sender, "share.no-permission");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            error(sender, "share.offline", targetName);
            return;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            error(sender, "share.self");
            return;
        }
        Ref ref = resolve(data, waypointName);
        if (ref == null) {
            error(sender, "wp.not-found", waypointName);
            return;
        }
        PlayerData targetData = store.get(target.getUniqueId());
        if (targetData == null) {
            targetData = store.load(target.getUniqueId(), target.getName());
        }
        Waypoint wp = ref.waypoint();
        Waypoint copy = new Waypoint(displayName(sender, ref), wp.world(), wp.dimension(),
                wp.x(), wp.y(), wp.z(), wp.created());
        Shared shared = targetData.addShared(sender.getName(), copy);
        if (shared == null) {
            error(sender, "share.inbox-full", target.getName());
            return;
        }
        store.save(targetData);
        success(sender, "share.sent", copy.name(), target.getName());

        Lang lang = lang(target);
        Component message = Text.info(lang, I18n.t(lang, "share.received", sender.getName(), copy.name(),
                        coords(copy), I18n.dimension(lang, copy.dimension())))
                .append(Text.button(I18n.t(lang, "share.accept-btn"), NamedTextColor.GREEN,
                        "/waypoint accept " + shared.id(), I18n.t(lang, "share.accept-hover")))
                .append(Text.of(" ", NamedTextColor.GRAY))
                .append(Text.button(I18n.t(lang, "share.decline-btn"), NamedTextColor.RED,
                        "/waypoint decline " + shared.id(), I18n.t(lang, "share.decline-hover")));
        target.sendMessage(message);
        info(target, "share.inbox-hint");
    }

    public void acceptShared(Player player, PlayerData data, int id) {
        Shared shared = data.takeShared(id);
        if (shared == null) {
            error(player, "share.not-found");
            return;
        }
        if (!adoptWaypoint(player, data, shared.waypoint())) {
            // Limit erreicht: zurücklegen, damit nichts verloren geht
            data.addShared(shared.from(), shared.waypoint());
        }
        store.save(data);
    }

    public void declineShared(Player player, PlayerData data, int id) {
        Shared shared = data.takeShared(id);
        if (shared == null) {
            error(player, "share.not-found");
            return;
        }
        store.save(data);
        info(player, "share.declined", shared.waypoint().name());
    }

    /** Zeigt einen Wegpunkt allen Spielern im Chat, jeweils in deren Sprache. */
    public void show(Player sender, PlayerData data, String waypointName) {
        if (!sender.hasPermission(PERM_SHARE)) {
            error(sender, "share.no-permission");
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastShow.get(sender.getUniqueId());
        if (last != null && now - last < SHOW_COOLDOWN_MS) {
            error(sender, "show.cooldown");
            return;
        }
        Ref ref = resolve(data, waypointName);
        if (ref == null) {
            error(sender, "wp.not-found", waypointName);
            return;
        }
        lastShow.put(sender.getUniqueId(), now);
        shown.values().removeIf(s -> s.expiresAt() < now);

        Waypoint wp = ref.waypoint();
        String name = displayName(sender, ref);
        Waypoint copy = new Waypoint(name, wp.world(), wp.dimension(), wp.x(), wp.y(), wp.z(), wp.created());
        int id = nextShownId++;
        shown.put(id, new ShownWaypoint(copy, now + SHOW_LIFETIME_MS));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(PERM_USE)) {
                continue;
            }
            Lang lang = lang(player);
            Component message = Text.info(lang, I18n.t(lang, "show.broadcast", sender.getName(), name,
                    coords(copy), I18n.dimension(lang, copy.dimension())));
            if (!player.getUniqueId().equals(sender.getUniqueId())) {
                message = message.append(Text.button(I18n.t(lang, "show.save-btn"), NamedTextColor.GREEN,
                        "/waypoint saveshown " + id, I18n.t(lang, "show.save-hover")));
            }
            player.sendMessage(message);
        }
    }

    public void saveShown(Player player, PlayerData data, int id) {
        ShownWaypoint entry = shown.get(id);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            error(player, "show.expired");
            return;
        }
        adoptWaypoint(player, data, entry.waypoint());
    }

    // ------------------------------------------------------------------ Sonstiges

    public void setLanguage(Player player, PlayerData data, Lang lang) {
        data.language(lang);
        store.save(data);
        success(player, lang == null ? "lang.auto" : "lang.set");
    }

    public void askForName(Player player) {
        pendingNames.put(player.getUniqueId(),
                new PendingName(player.getLocation(), System.currentTimeMillis() + 60_000L));
        info(player, "ask.line1");
        info(player, "ask.line2");
    }

    /** Liefert und entfernt eine gültige wartende Eingabe, sonst {@code null}. Thread-sicher. */
    PendingName takePendingName(UUID uuid) {
        PendingName pending = pendingNames.remove(uuid);
        if (pending == null || pending.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return pending;
    }

    void clearPendingName(UUID uuid) {
        pendingNames.remove(uuid);
        lastShow.remove(uuid);
    }

    public PlayerData data(Player player) {
        PlayerData data = store.get(player.getUniqueId());
        return data != null ? data : store.load(player.getUniqueId(), player.getName());
    }

    // ------------------------------------------------------------------ Getter

    public DataStore store() {
        return store;
    }

    public Navigation navigation() {
        return navigation;
    }

    public int maxWaypoints() {
        return maxWaypoints;
    }

    public boolean deathWaypointEnabled() {
        return deathWaypoint;
    }

    public double arrivalDistance() {
        return arrivalDistance;
    }

    public boolean stopOnArrival() {
        return stopOnArrival;
    }
}
