package io.github.johager.waypoints;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Zeigt das aktive Ziel als Leiste oben am Bildschirm (BossBar) an:
 * Name, Richtungspfeil, Entfernung und Koordinaten. Die Leiste füllt sich,
 * je näher man dem Ziel kommt.
 */
public final class Navigation {

    private final WaypointsPlugin plugin;

    public Navigation(WaypointsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, PlayerData data, Ref ref) {
        data.navigation(ref.id());
        plugin.store().save(data);
        update(player, data);
        plugin.success(player, "nav.started", plugin.displayName(player, ref));
    }

    public void stop(Player player, PlayerData data, boolean message) {
        boolean wasActive = data.navigation() != null;
        data.navigation(null);
        hide(player, data);
        plugin.store().save(data);
        if (message && wasActive) {
            plugin.info(player, "nav.stopped");
        }
    }

    public void hide(Player player, PlayerData data) {
        if (data.bar != null) {
            player.hideBossBar(data.bar);
            data.bar = null;
        }
    }

    /** Wird regelmäßig für alle Spieler aufgerufen. */
    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.store().get(player.getUniqueId());
            if (data != null && data.navigation() != null) {
                update(player, data);
            }
        }
    }

    public void update(Player player, PlayerData data) {
        Ref ref = plugin.lookup(data, data.navigation());
        if (ref == null) {
            stop(player, data, false);
            return;
        }
        Waypoint wp = ref.waypoint();
        Lang lang = plugin.lang(player);
        String name = plugin.displayName(player, ref);

        Location loc = player.getLocation();
        String playerDim = player.getWorld().getEnvironment().name();
        NavMath.Target target = NavMath.resolve(player.getWorld().getName(), playerDim, wp);

        Component title;
        float progress;
        BossBar.Color color;

        if (target == null) {
            title = Text.bold(name, NamedTextColor.GOLD)
                    .append(Text.of(I18n.t(lang, "nav.other-dim", I18n.dimension(lang, wp.dimension())), NamedTextColor.RED))
                    .append(Text.of(NavMath.coords(wp.x(), wp.y(), wp.z()), NamedTextColor.GRAY));
            progress = 0f;
            color = BossBar.Color.RED;
            data.startDistance = -1;
        } else {
            double dx = target.x() - loc.getX();
            double dy = target.y() - loc.getY();
            double dz = target.z() - loc.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double distance = Math.sqrt(horizontal * horizontal + dy * dy);

            if (distance <= plugin.arrivalDistance() && plugin.stopOnArrival()) {
                plugin.success(player, "nav.arrived", name);
                stop(player, data, false);
                return;
            }

            // Fortschritt neu messen, wenn die Navigation startet oder die Dimension wechselt
            if (data.startDistance < 0 || data.lastConverted != target.converted()) {
                data.startDistance = Math.max(distance, 1.0);
                data.lastConverted = target.converted();
            }
            data.startDistance = Math.max(data.startDistance, distance);
            progress = (float) Math.max(0.0, Math.min(1.0, 1.0 - distance / data.startDistance));

            String arrow = NavMath.arrow(loc.getX(), loc.getZ(), loc.getYaw(), target.x(), target.z());
            title = Text.bold(name, NamedTextColor.GOLD)
                    .append(Text.of("   ", NamedTextColor.WHITE))
                    .append(Text.bold(arrow, NamedTextColor.WHITE))
                    .append(Text.of(" " + NavMath.distance(horizontal, lang), NamedTextColor.AQUA));
            if (Math.abs(dy) >= 8) {
                title = title.append(Text.of(dy > 0 ? "  ▲ " + Math.round(dy) : "  ▼ " + Math.round(-dy),
                        NamedTextColor.YELLOW));
            }
            title = title.append(Text.of("   " + NavMath.coords(target.x(), target.y(), target.z()), NamedTextColor.GRAY));
            if (target.converted()) {
                title = title.append(Text.of(I18n.t(lang, "nav.converted"), NamedTextColor.LIGHT_PURPLE));
            }
            color = distance < 50 ? BossBar.Color.GREEN : BossBar.Color.BLUE;
        }

        if (data.bar == null) {
            data.bar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
            player.showBossBar(data.bar);
        } else {
            data.bar.name(title);
            data.bar.progress(progress);
            data.bar.color(color);
        }
    }
}
