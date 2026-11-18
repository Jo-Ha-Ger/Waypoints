package io.github.johager.waypoints;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lädt und speichert Spielerdaten. Jede Datei heißt {@code <UUID>.yml}, damit die
 * Wegpunkte auch nach einer Namensänderung erhalten bleiben.
 */
public final class DataStore {

    private final File folder;
    private final File serverFile;
    private final Logger logger;
    /** Server-Wegpunkte, die jeder sieht. Nur im Hauptthread verändern. */
    private final List<Waypoint> serverWaypoints = new ArrayList<>();
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    /** Ein einzelner Schreib-Thread hält die Reihenfolge der Speichervorgänge ein. */
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Waypoints-Saver");
        t.setDaemon(true);
        return t;
    });

    public DataStore(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "players");
        this.serverFile = new File(dataFolder, "server.yml");
        this.logger = logger;
        // Ordner aus Version 1.0.0 übernehmen
        File old = new File(dataFolder, "spieler");
        if (old.isDirectory() && !folder.exists() && !old.renameTo(folder)) {
            logger.warning("Could not rename " + old + " to " + folder);
        }
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("Could not create folder: " + folder);
        }
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData load(UUID uuid, String name) {
        PlayerData data = new PlayerData(uuid, name);
        File file = file(uuid);
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            data.navigation(yaml.getString("navigation"));
            data.language(Lang.parse(yaml.getString("language")));
            data.deathEnabled(yaml.getBoolean("death-point", true));
            // "wegpunkte" = Schlüssel aus Version 1.0.0
            List<Map<?, ?>> list = yaml.getMapList("waypoints");
            if (list.isEmpty()) {
                list = yaml.getMapList("wegpunkte");
            }
            for (Map<?, ?> map : list) {
                Waypoint wp = read(map, file);
                if (wp != null) {
                    data.rawWaypoints().add(wp);
                }
            }
            for (Map<?, ?> map : yaml.getMapList("death")) {
                data.death(read(map, file));
            }
            for (Map<?, ?> map : yaml.getMapList("inbox")) {
                Waypoint wp = read(map, file);
                Object from = map.get("from");
                if (wp != null) {
                    data.addShared(from == null ? "?" : String.valueOf(from), wp);
                }
            }
            String nav = data.navigation();
            if (nav != null && !nav.startsWith(Ref.SERVER_PREFIX) && data.find(nav) == null) {
                data.navigation(null);
            }
        }
        data.lastName(name);
        cache.put(uuid, data);
        return data;
    }

    private Waypoint read(Map<?, ?> map, File file) {
        try {
            return new Waypoint(
                    String.valueOf(map.get("name")),
                    String.valueOf(first(map, "world", "welt")),
                    String.valueOf(map.get("dimension")),
                    ((Number) map.get("x")).doubleValue(),
                    ((Number) map.get("y")).doubleValue(),
                    ((Number) map.get("z")).doubleValue(),
                    first(map, "created", "erstellt") instanceof Number n ? n.longValue() : 0L);
        } catch (RuntimeException ex) {
            logger.warning("Skipping broken waypoint in " + file.getName() + ": " + map);
            return null;
        }
    }

    private static Object first(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value != null ? value : map.get(fallback);
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    /** Bereitet die Datei im Hauptthread vor und schreibt sie im Hintergrund. */
    public void save(PlayerData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "Waypoints of / Wegpunkte von " + data.lastName(),
                "File is named after the player's UUID so it survives name changes."));
        yaml.set("player-name", data.lastName());
        yaml.set("language", data.language() == null ? "auto" : data.language().name().toLowerCase(Locale.ROOT));
        yaml.set("death-point", data.deathEnabled());
        yaml.set("navigation", data.navigation());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Waypoint wp : data.waypoints()) {
            list.add(write(wp));
        }
        yaml.set("waypoints", list);
        yaml.set("death", data.death() == null ? List.of() : List.of(write(data.death())));
        List<Map<String, Object>> inbox = new ArrayList<>();
        for (Shared shared : data.inbox()) {
            Map<String, Object> map = write(shared.waypoint());
            map.put("from", shared.from());
            inbox.add(map);
        }
        yaml.set("inbox", inbox);
        String content = yaml.saveToString();
        Path target = file(data.uuid()).toPath();
        writer.execute(() -> write(target, content));
    }

    // ------------------------------------------------------------------ Server-Wegpunkte

    public void loadServerWaypoints() {
        serverWaypoints.clear();
        if (serverFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(serverFile);
            for (Map<?, ?> map : yaml.getMapList("waypoints")) {
                Waypoint wp = read(map, serverFile);
                if (wp != null) {
                    serverWaypoints.add(wp);
                }
            }
        }
    }

    /** Sortierte Kopie der Server-Wegpunkte. */
    public List<Waypoint> serverWaypoints() {
        List<Waypoint> copy = new ArrayList<>(serverWaypoints);
        copy.sort(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    public Waypoint findServer(String name) {
        if (name == null) {
            return null;
        }
        for (Waypoint wp : serverWaypoints) {
            if (wp.isNamed(name)) {
                return wp;
            }
        }
        return null;
    }

    public void putServer(Waypoint waypoint) {
        serverWaypoints.removeIf(wp -> wp.isNamed(waypoint.name()));
        serverWaypoints.add(waypoint);
        saveServer();
    }

    public Waypoint removeServer(String name) {
        Waypoint wp = findServer(name);
        if (wp != null) {
            serverWaypoints.remove(wp);
            saveServer();
        }
        return wp;
    }

    private void saveServer() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("Server waypoints - visible to every player"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Waypoint wp : serverWaypoints()) {
            list.add(write(wp));
        }
        yaml.set("waypoints", list);
        String content = yaml.saveToString();
        Path target = serverFile.toPath();
        writer.execute(() -> write(target, content));
    }

    private static Map<String, Object> write(Waypoint wp) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", wp.name());
        map.put("world", wp.world());
        map.put("dimension", wp.dimension());
        map.put("x", round(wp.x()));
        map.put("y", round(wp.y()));
        map.put("z", round(wp.z()));
        map.put("created", wp.created());
        return map;
    }

    private void write(Path target, String content) {
        try {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicNotSupported) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not save waypoints: " + target, ex);
        }
    }

    /** Speichert alles und wartet, bis die Dateien geschrieben sind. */
    public void shutdown() {
        for (PlayerData data : cache.values()) {
            save(data);
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Saving took too long.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
